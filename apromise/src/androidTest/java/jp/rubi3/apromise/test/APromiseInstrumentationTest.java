package jp.rubi3.apromise.test;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.rubi3.apromise.Callback;
import jp.rubi3.apromise.Filter;
import jp.rubi3.apromise.Function;
import jp.rubi3.apromise.PendingException;
import jp.rubi3.apromise.Pipe;
import jp.rubi3.apromise.Promise;
import jp.rubi3.apromise.Resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
@RunWith(AndroidJUnit4.class)
public class APromiseInstrumentationTest {
    private static final String TAG = "APromiseTest";
    private Handler handler;

    @Before
    public void setUp() throws Exception {
        Log.d(TAG, "setUp: ");
        HandlerThread thread = new HandlerThread("APromiseTest");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    private <F> Promise<F> delayedResolve(final F result, final long delayMillis) {
        return new Promise<>(new Function<F>() {
            @Override
            public void function(final Resolver<F> resolver) throws Exception {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resolver.fulfill(result);
                    }
                }, delayMillis);
            }
        });
    }

    private <F> Promise<F> delayedReject(final Exception exception, final long delayMillis) {
        return new Promise<>(new Function<F>() {
            @Override
            public void function(final Resolver<F> resolver) throws Exception {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resolver.reject(exception);
                    }
                }, delayMillis);
            }
        });
    }

    private Exception getException(Promise promise) throws IllegalStateException {
        try {
            promise.getResult();
            throw new IllegalStateException("Fulfilled promise.");
        } catch (Exception e) {
            return e;
        }
    }

    @Test
    public void testPendingResolve() throws Exception {
        Promise<String> promise = delayedResolve("OK", 100);
        assertTrue(promise.isPending());
        assertTrue(getException(promise) instanceof PendingException);
        assertTrue(promise.sync().isFulfilled());
        assertEquals("OK", promise.getResult());
    }

    @Test
    public void testPendingReject() throws Exception {
        Promise<Object> promise = delayedReject(new Exception("OK"), 100);
        assertTrue(promise.isPending());
        assertTrue(getException(promise) instanceof PendingException);
        assertTrue(promise.sync().isRejected());
        assertEquals("OK", getException(promise).getMessage());
    }

    @Test
    public void testFunctionResolve() throws Exception {
        assertEquals("OK", new Promise<>(new Function<String>() {
            @Override
            public void function(Resolver<String> resolver) throws Exception {
                resolver.fulfill("OK");
                resolver.reject(new Exception("NG")); // it will be ignored.
            }
        }).sync().getResult());
    }

    @Test
    public void testFunctionRejectByResolver() throws Exception {
        Promise<String> promise = new Promise<>(new Function<String>() {
            @Override
            public void function(Resolver<String> resolver) throws Exception {
                resolver.reject(new Exception("OK"));
            }
        });
        assertTrue(promise.sync().isRejected());
        assertEquals("OK", getException(promise).getMessage());
    }

    @Test
    public void testFunctionRejectByThrow() throws Exception {
        Promise<String> promise = new Promise<>(new Function<String>() {
            @Override
            public void function(Resolver<String> resolver) throws Exception {
                throw new Exception("OK");
            }
        });
        assertEquals("OK", getException(promise.sync()).getMessage());
    }

    @Test
    public void testPendingState() throws Exception {
        Promise<String> promise = delayedResolve(null, 10000);
        assertTrue(promise.isPending());
        assertFalse(promise.isFulfilled());
        assertFalse(promise.isRejected());
    }

    @Test
    public void testFulfilledState() throws Exception {
        Promise<Object> resolve = Promise.resolve(null);
        assertFalse(resolve.isPending());
        assertTrue(resolve.isFulfilled());
        assertFalse(resolve.isRejected());
    }

    @Test
    public void testRejectedState() throws Exception {
        Promise reject = Promise.reject(null);
        assertFalse(reject.isPending());
        assertFalse(reject.isFulfilled());
        assertTrue(reject.isRejected());
    }

    @Test
    public void testResolveWithException() throws Exception {
        Promise<Exception> one = Promise.resolve(new Exception("e"));
        assertTrue(one.isFulfilled());
        assertNotNull(one.getResult());
    }

    @Test
    public void testReject() throws Exception {
        Promise rejected = Promise.reject(new Exception("OK"));
        assertTrue(rejected.isRejected());
        assertEquals("OK", getException(rejected.sync()).getMessage());
    }

    @Test
    public void testInitWithLooper() throws Exception {
        final HandlerThread handlerThread = new HandlerThread("testAll");
        handlerThread.start();

        String string = new Promise<>(handlerThread.getLooper(), new Function<String>() {
            @Override
            public void function(Resolver<String> resolver) throws Exception {
                assertEquals(handlerThread.getLooper(), Looper.myLooper());
                resolver.fulfill("OK");
            }
        }).onThen(new Callback<String>() {
            @Override
            public void callback(String result) throws Exception {
                assertEquals(handlerThread.getLooper(), Looper.myLooper());
            }
        }).sync().getResult();
        assertEquals("OK", string);
    }

    @Test
    public void testAllResolved() throws Exception {
        HandlerThread handlerThread = new HandlerThread("testAll");
        handlerThread.start();
        final Handler handler = new Handler(handlerThread.getLooper());

        ArrayList<Promise<Integer>> promises = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            final int finalI = i;
            promises.add(new Promise<>(new Function<Integer>() {
                @Override
                public void function(final Resolver<Integer> resolver) throws Exception {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            resolver.fulfill(finalI);
                        }
                    }, finalI * 100);
                }
            }));
        }
        Promise<List<Integer>> result = Promise.all(promises);
        assertEquals(result.sync().getResult(), Arrays.asList(0, 1, 2, 3, 4));
    }

    @Test
    public void testAllOneRejected() throws Exception {
        HandlerThread handlerThread = new HandlerThread("testAll");
        handlerThread.start();
        final Handler handler = new Handler(handlerThread.getLooper());

        ArrayList<Promise<Integer>> promises = new ArrayList<>(3);
        for (int i = 0; i < 2; i++) {
            final int finalI = i;
            promises.add(new Promise<>(new Function<Integer>() {
                @Override
                public void function(final Resolver<Integer> resolver) throws Exception {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            resolver.fulfill(finalI);
                        }
                    }, finalI * 500);
                }
            }));
            promises.add(new Promise<>(new Function<Integer>() {
                @Override
                public void function(final Resolver<Integer> resolver) throws Exception {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            resolver.reject(new Exception("OK"));
                        }
                    }, 300);
                }
            }));
        }
        Promise<List<Integer>> result = Promise.all(promises);
        assertTrue(result.sync().isRejected());
        assertEquals("OK", getException(result).getMessage());
    }

    @Test
    public void testNullCheckNewPromise() throws Exception {
        try {
            //noinspection ConstantConditions
            new Promise<Void>(null);
            fail();
        } catch (NullPointerException e) {
            // NOP
        }
    }

    @Test
    public void testCallbackBefore() throws Exception {
        final StringBuilder builder = new StringBuilder();
        Promise<String> promise = delayedResolve("OK", 100);
        Promise<String> result = promise.onThen(new Callback<String>() {
            @Override
            public void callback(String result) throws Exception {
                builder.append(result);
            }
        }, new Callback<Exception>() {
            @Override
            public void callback(Exception result) throws Exception {
                fail();
            }
        });
        assertEquals("", builder.toString());
        assertEquals("OK", result.sync().getResult());
        assertEquals("OK", builder.toString());
    }

    @Test
    public void testCallbackAfter() throws Exception {
        final StringBuilder builder = new StringBuilder();

        Promise<String> promise = Promise.resolve("OK");
        assertTrue(promise.isFulfilled());

        Promise<String> result = promise.onThen(new Callback<String>() {
            @Override
            public void callback(String result) throws Exception {
                builder.append(result);
            }
        });
        assertEquals("OK", result.sync().getResult());
        assertEquals("OK", builder.toString());
    }

    @Test
    public void testCallbackFulfill() throws Exception {
        final StringBuilder builder = new StringBuilder();

        Promise<String> promise = Promise.resolve("OK");
        Promise<String> result = promise.onThen(new Callback<String>() {
            @Override
            public void callback(String result) throws Exception {
                builder.append(result);
            }
        }, new Callback<Exception>() {
            @Override
            public void callback(Exception result) throws Exception {
                fail();
            }
        });
        assertEquals("OK", result.sync().getResult());
        assertEquals("OK", builder.toString());
    }

    @Test
    public void testCallbackReject() throws Exception {
        final StringBuilder builder = new StringBuilder();
        Promise<String> promise = Promise.reject(String.class, new Exception("OK"));
        Promise<String> result = promise.onThen(new Callback<String>() {
            @Override
            public void callback(String result) throws Exception {
                fail();
            }
        }, new Callback<Exception>() {
            @Override
            public void callback(Exception result) throws Exception {
                builder.append(result.getMessage());
            }
        });
        assertEquals("OK", getException(result.sync()).getMessage());
        assertEquals("OK", builder.toString());
    }

    @Test
    public void testCallbackThrow() throws Exception {
        Promise<Object> promise = Promise.resolve(null).onThen(new Callback<Object>() {
            @Override
            public void callback(Object result) throws Exception {
                throw new Exception("OK");
            }
        });
        assertTrue(promise.sync().isRejected());
        assertEquals("OK", getException(promise).getMessage());
    }

    @Test
    public void testCallbackNullCheck() throws Exception {
        Callback<String> fulfilled = null;
        Callback<Exception> rejected = null;
        //noinspection ConstantConditions
        Promise promise = Promise.resolve("OK").onThen(fulfilled, rejected);
        assertTrue(promise.sync().isFulfilled());
        assertEquals("OK", promise.getResult());
    }

    @Test
    public void testFilterFulfill() throws Exception {
        Promise<Character[]> promise = Promise.resolve(new Character[] {'O', 'K'});
        assertTrue(promise.sync().isFulfilled());

        Promise<String> result = promise.onThen(new Filter<Character[], String>() {
            @Override
            public String filter(Character[] result) throws Exception {
                StringBuilder builder = new StringBuilder();
                for (Character c :
                        result) {
                    builder.append(c);
                }
                return builder.toString();
            }
        }, new Filter<Exception, String>() {
            @Nullable
            @Override
            public String filter(@Nullable Exception result) throws Exception {
                return "NG";
            }
        });
        assertEquals("OK", result.sync().getResult());
    }

    @Test
    public void testFilterReject() throws Exception {
        Promise<Object> promise = Promise.reject(new Exception("OK"));
        assertTrue(promise.isRejected());

        Promise<String> result = promise.onThen(new Filter<Object, String>() {
            @Nullable
            @Override
            public String filter(@Nullable Object result) throws Exception {
                fail();
                return null;
            }
        }, new Filter<Exception, String>() {
            @Nullable
            @Override
            public String filter(Exception result) throws Exception {
                return result.getMessage();
            }
        });
        assertEquals("OK", result.sync().getResult());
    }

    @Test
    public void testFilterThrow() throws Exception {
        Promise<String> promise = Promise.resolve("OK");
        Promise<String> result = promise.onThen(new Filter<String, String>() {
            @Nullable
            @Override
            public String filter(@Nullable String result) throws Exception {
                throw new Exception(result);
            }
        });
        assertTrue(result.sync().isRejected());
        assertEquals("OK", getException(result).getMessage());
    }

    @Test
    public void testFilterNullCheck() throws Exception {
        Filter<Object, String> fulfilled = null;
        try {
            //noinspection ConstantConditions
            Promise.resolve(null).onThen(fulfilled);
            fail();
        } catch (NullPointerException e) {
            // NOP
        }
    }

    @Test
    public void testPipeFulfillToFulfill() throws Exception {
        Promise<Character> promise = Promise.resolve('O');
        Promise<String> result = promise.onThen(new Pipe<Character, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(Character result) throws Exception {
                return Promise.resolve(result.toString() + "K");
            }
        }, new Pipe<Exception, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(@Nullable Exception result) throws Exception {
                fail();
                return null;
            }
        });
        assertTrue(result.sync().isFulfilled());
        assertEquals("OK", result.getResult());
    }

    @Test
    public void testPipeFulfillToReject() throws Exception {
        Promise<Character> promise = Promise.resolve('O');
        Promise<String> result = promise.onThen(new Pipe<Character, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(Character result) throws Exception {
                return Promise.reject(new Exception(result.toString() + "K"));
            }
        }, new Pipe<Exception, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(@Nullable Exception result) throws Exception {
                fail();
                return null;
            }
        });
        assertTrue(result.sync().isRejected());
        assertEquals("OK", getException(result).getMessage());
    }

    @Test
    public void testPipeRejectToFulfill() throws Exception {
        Promise<Character> promise = Promise.reject(new Exception("O"));
        Promise<String> result = promise.onThen(new Pipe<Character, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(Character result) throws Exception {
                fail();
                return null;
            }
        }, new Pipe<Exception, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(Exception result) throws Exception {
                return Promise.resolve(result.getMessage() + "K");
            }
        });
        assertTrue(result.sync().isFulfilled());
        assertEquals("OK", result.getResult());
    }

    @Test
    public void testPipeFulfillToNull() throws Exception {
        Promise<Character> promise = Promise.resolve(null);
        Promise<String> result = promise.onThen(new Pipe<Character, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(Character result) throws Exception {
                return null;
            }
        });
        assertTrue(result.sync().isFulfilled());
        assertNull(result.getResult());
    }

    @Test
    public void testPipeRejectToNull() throws Exception {
        Promise<Character> promise = Promise.reject(new Exception());
        Promise<String> result = promise.onThen(new Pipe<Character, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(Character result) throws Exception {
                fail();
                return null;
            }
        }, new Pipe<Exception, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(Exception result) throws Exception {
                return null;
            }
        });
        assertTrue(result.sync().isFulfilled());
        assertNull(result.getResult());
    }

    @Test
    public void testPipeThrow() throws Exception {
        Promise<Character> promise = Promise.resolve(null);
        Promise<String> result = promise.onThen(new Pipe<Character, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(Character result) throws Exception {
                throw new Exception("OK");
            }
        });
        assertTrue(result.sync().isRejected());
        assertEquals("OK", getException(result).getMessage());
    }

    @Test
    public void testPipeNullCheck() throws Exception {
        Pipe pipe = null;
        Promise<Object> promise = Promise.resolve(null);
        try {
            //noinspection ConstantConditions
            promise.onThen(pipe);
            fail();
        } catch (NullPointerException e) {
            // NOP
        }
    }

    @Test
    public void testCatchCallbackWithFulfilled() throws Exception {
        Promise<String> promise = Promise.resolve("OK").onCatch(new Callback<Exception>() {
            @Override
            public void callback(Exception result) throws Exception {
                fail();
                throw new Exception("NG");
            }
        });
        assertTrue(promise.sync().isFulfilled());
        assertEquals("OK", promise.getResult());
    }

    @Test
    public void testCatchCallbackWithRejected() throws Exception {
        final StringBuilder builder = new StringBuilder();
        Promise<Object> promise = Promise.reject(new Exception("OK")).onCatch(new Callback<Exception>() {
            @Override
            public void callback(Exception result) throws Exception {
                builder.append(result.getMessage());
            }
        });
        assertTrue(promise.sync().isRejected());
        assertEquals("OK", builder.toString());
    }

    @Test
    public void testCatchFilterWithFulfilled() throws Exception {
        Promise<String> promise = Promise.resolve("OK").onCatch(new Filter<Exception, String>() {
            @Nullable
            @Override
            public String filter(@Nullable Exception result) throws Exception {
                fail();
                return "NG";
            }
        });
        assertTrue(promise.sync().isFulfilled());
        assertEquals("OK", promise.getResult());
    }

    @Test
    public void testCatchFilterWithRejected() throws Exception {
        Promise<String> promise = Promise.reject(String.class, new Exception("O")).onCatch(new Filter<Exception, String>() {
            @Nullable
            @Override
            public String filter(Exception result) throws Exception {
                return result.getMessage() + "K";
            }
        });
        assertTrue(promise.sync().isFulfilled());
        assertEquals("OK", promise.getResult());
    }

    @Test
    public void testCatchPipeWithFulfilled() throws Exception {
        Promise<String> promise = Promise.resolve("OK").onCatch(new Pipe<Exception, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(@Nullable Exception result) throws Exception {
                fail();
                return Promise.resolve("NG");
            }
        });
        assertTrue(promise.sync().isFulfilled());
        assertEquals("OK", promise.getResult());
    }

    @Test
    public void testCatchPipeWithRejected() throws Exception {
        Promise<String> promise = Promise.reject(String.class, new Exception("O")).onCatch(new Pipe<Exception, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(Exception result) throws Exception {
                return Promise.resolve(result.getMessage() + "K");
            }
        });
        assertTrue(promise.sync().isFulfilled());
        assertEquals("OK", promise.getResult());
    }

    @Test
    public void testFinallyCallback() throws Exception {
        final StringBuilder builder = new StringBuilder();
        Promise<String> promise = Promise.resolve("OK").onFinally(new Callback<Promise<String>>() {
            @Override
            public void callback(Promise<String> result) throws Exception {
                builder.append(result.getResult());
            }
        });
        assertTrue(promise.sync().isFulfilled());
        assertEquals("OK", builder.toString());
    }

    @Test
    public void testFinallyCallbackThrow() throws Exception {
        Promise<String> promise = Promise.resolve("NG").onFinally(new Callback<Promise<String>>() {
            @Override
            public void callback(Promise<String> result) throws Exception {
                throw new Exception("OK");
            }
        });
        assertTrue(promise.sync().isRejected());
        assertEquals("OK", getException(promise).getMessage());
    }

    @Test
    public void testFinallyFilter() throws Exception {
        Promise<String> promise = Promise.resolve('O').onFinally(new Filter<Promise<Character>, String>() {
            @Nullable
            @Override
            public String filter(Promise<Character> result) throws Exception {
                return String.valueOf(result.getResult()) + "K";
            }
        });
        assertTrue(promise.sync().isFulfilled());
        assertEquals("OK", promise.getResult());
    }

    @Test
    public void testFinallyFilterThrow() throws Exception {
        Promise<String> promise = Promise.resolve("NG").onFinally(new Filter<Promise<String>, String>() {
            @Nullable
            @Override
            public String filter(@Nullable Promise<String> result) throws Exception {
                throw new Exception("OK");
            }
        });
        assertTrue(promise.sync().isRejected());
        assertEquals("OK", getException(promise).getMessage());
    }

    @Test
    public void testFinallyPipe() throws Exception {
        Promise<String> promise = Promise.resolve('O').onFinally(new Pipe<Promise<Character>, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(Promise<Character> result) throws Exception {
                return Promise.reject(new Exception(String.valueOf(result.getResult()) + "K"));
            }
        });
        assertTrue(promise.sync().isRejected());
        assertEquals("OK", getException(promise).getMessage());
    }

    @Test
    public void testFinallyPipeThrow() throws Exception {
        Promise<String> promise = Promise.resolve("NG").onFinally(new Pipe<Promise<String>, String>() {
            @Nullable
            @Override
            public Promise<String> pipe(@Nullable Promise<String> result) throws Exception {
                throw new Exception("OK");
            }
        });
        assertTrue(promise.sync().isRejected());
        assertEquals("OK", getException(promise).getMessage());
    }
}