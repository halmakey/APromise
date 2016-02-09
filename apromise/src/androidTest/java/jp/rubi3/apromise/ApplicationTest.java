package jp.rubi3.apromise;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.test.ApplicationTestCase;

import java.util.concurrent.CountDownLatch;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void testCallback() throws Exception {
        final StringBuilder builder = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Promise.resolve(
                        builder.append("A")
                ).then(new Callback<StringBuilder>() {
                    @Override
                    public void callback(StringBuilder result) throws Exception {
                        result.append("B");
                        throw new Exception("C"); // throw on then callback
                    }
                }).then(new Callback<StringBuilder>() {
                    @Override
                    public void callback(StringBuilder result) throws Exception {
                        result.append("X"); // through
                    }
                }).catche(new Callback<Exception>() {
                    @Override
                    public void callback(Exception result) throws Exception {
                        builder.append(result.getMessage());
                    }
                }).all(new Callback<Object>() {
                    @Override
                    public void callback(Object result) throws Exception {
                        if (result instanceof Exception) {
                            builder.append("D"); // through result object
                        } else {
                            builder.append("X");
                        }
                        latch.countDown();
                    }
                });
            }
        });

        latch.await();
        assertEquals("ABCD", builder.toString());
    }

    public void testFilter() throws Exception {
        final StringBuilder builder = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Promise.resolve(
                        builder.append("A")
                ).then(new Filter<StringBuilder, String>() {
                    @Override
                    public String filter(StringBuilder result) throws Exception {
                        builder.append("B");
                        return "C";
                    }
                }).then(new Filter<String, Exception>() {
                    @Override
                    public Exception filter(String result) throws Exception {
                        builder.append(result);
                        return new Exception("D"); // filter returns Exception to reject
                    }
                }).then(new Filter<Exception, String>() {
                    @Nullable
                    @Override
                    public String filter(Exception result) throws Exception {
                        return "X"; // through
                    }
                }).catche(new Filter<Exception, String>() {
                    @Override
                    public String filter(Exception result) throws Exception {
                        builder.append(result.getMessage());
                        return "E";
                    }
                }).all(new Filter<Object, String>() {
                    @Override
                    public String filter(Object result) throws Exception {
                        builder.append(result);
                        return "F";
                    }
                }).all(new Filter<Object, Object>() {
                    @Override
                    public Object filter(Object result) throws Exception {
                        builder.append(result);
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        latch.await();
        assertEquals("ABCDEF", builder.toString());
    }

    public void testPipe() throws Exception {
        final StringBuilder builder = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Promise.resolve(
                        "A"
                ).then(new Pipe<String, String>() {
                    @NonNull
                    @Override
                    public Promise<String> pipe(String result) throws Exception {
                        builder.append(result);
                        return Promise.resolve("B");
                    }
                }).then(new Pipe<String, String>() {
                    @NonNull
                    @Override
                    public Promise<String> pipe(String result) throws Exception {
                        builder.append(result);
                        throw new Exception("C");
                    }
                }).then(new Pipe<String, Character>() {
                    @NonNull
                    @Override
                    public Promise<Character> pipe(String result) throws Exception {
                        builder.append("X");
                        return null; // cause nullpo
                    }
                }).catche(new Pipe<Exception, Character>() {
                    @NonNull
                    @Override
                    public Promise<Character> pipe(Exception result) throws Exception {
                        builder.append(result.getMessage());
                        return Promise.reject(new Exception("D"));
                    }
                }).catche(new Pipe<Exception, Character>() {
                    @NonNull
                    @Override
                    public Promise<Character> pipe(Exception result) throws Exception {
                        builder.append(result.getMessage());
                        return Promise.resolve('E');
                    }
                }).all(new Pipe<Object, CountDownLatch>() {
                    @NonNull
                    @Override
                    public Promise<CountDownLatch> pipe(Object result) throws Exception {
                        builder.append(result);
                        latch.countDown();
                        return Promise.resolve(latch);
                    }
                });
            }
        });

        latch.await();
        assertEquals("ABCDE", builder.toString());
    }

    public void testResolved() throws Exception {
        assertTrue(Promise.resolve(null).isFulfilled());
        assertTrue(Promise.reject(null).isRejected());
    }

    public void testStatus() throws Exception {
        Handler handler = new Handler(Looper.getMainLooper());

        Promise pending = new Promise(handler, new Function() {
            @Override
            public void function(Resolver resolver) throws Exception {
            }
        });
        assertTrue(pending.isPending());
        assertFalse(pending.isFulfilled());
        assertFalse(pending.isRejected());

        Promise fulfilled = Promise.resolve(null);
        assertFalse(fulfilled.isPending());
        assertTrue(fulfilled.isFulfilled());
        assertFalse(fulfilled.isRejected());

        Promise rejected = Promise.reject(null);
        assertFalse(rejected.isPending());
        assertFalse(rejected.isFulfilled());
        assertTrue(rejected.isRejected());
    }

    public void testMultipleChain() throws Exception {
        final StringBuilder builder = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Promise resolved = Promise.resolve(null);
                resolved.then(new Callback() {
                    @Override
                    public void callback(Object result) throws Exception {
                        builder.append("A");
                    }
                });
                resolved.then(new Callback() {
                    @Override
                    public void callback(Object result) throws Exception {
                        builder.append("B");
                    }
                });

                final Promise<Character> pending = new Promise<>(new Function() {
                    @Override
                    public void function(final Resolver resolver) throws Exception {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                resolver.resolve('D');
                            }
                        }, 1000);
                    }
                });
                pending.then(new Callback<Character>() {
                    @Override
                    public void callback(Character result) throws Exception {
                        builder.append("C");
                    }
                });
                pending.then(new Callback<Character>() {
                    @Override
                    public void callback(Character result) throws Exception {
                        builder.append(result);
                        latch.countDown();
                    }
                });
            }
        });

        latch.await();

        assertEquals("ABCD", builder.toString());
    }

    public void testFunction() throws Exception {
        final StringBuilder builder = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);

        final Handler handler = new Handler(Looper.getMainLooper());
        new Promise<>(handler, new Function() {
            @Override
            public void function(Resolver resolver) throws Exception {
                try {
                    resolver.resolve(null);
                    resolver.resolve(null); // throws IllegalStateException
                } catch (IllegalStateException ise) {
                    builder.append("A");
                    latch.countDown();
                }
            }
        }).catche(new Callback<Exception>() {
            @Override
            public void callback(Exception result) throws Exception {
                builder.append("X");
                latch.countDown();
            }
        });

        latch.await();

        assertEquals("A", builder.toString());
    }

    public void testResult() throws Exception {
        Promise promise = Promise.resolve("testResult");
        assertEquals("testResult", promise.result());
    }
}