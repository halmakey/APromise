package jp.rubi3.apromise;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 *
 * Promise
 *
 * Created by Ryo Kikuchi on 2015/03/12.
 */
public final class Promise<D> {
    /**
     * create resolved promise.
     *
     * @param object resolve with object
     * @param <D> result object type
     * @return returns resolved promise with object
     */
    @NonNull
    public static <D> Promise<D> resolve(@Nullable D object) {
        return new Promise<D>(getLooper()).doFulfill(object);
    }

    /**
     * create rejected promise.
     *
     * @param exception reject with exception
     * @return returns promise rejected with exception or NullPointerException if exception is null.
     */
    @NonNull
    public static <F> Promise<F> reject(@Nullable final Exception exception) {
        return new Promise<F>(getLooper()).doReject(exception);
    }

    /**
     * create rejected promise with fulfilled class.
     *
     * @param clazz class for fulfilled. (for generics)
     * @param exception reject with exception
     * @param <D> clazz's type
     * @return returns promise rejected with exception or NullPointerException if exception is null.
     */
    @NonNull
    public static <D> Promise<D> reject(@SuppressWarnings("UnusedParameters") Class<D> clazz, @Nullable final Exception exception) {
        return new Promise<D>(getLooper()).doReject(exception);
    }

    @NonNull
    public static <D> Promise<List<D>> all(@NonNull final List<Promise<D>> promises) {
        final Promise<List<D>> promise = new Promise<>(getLooper());
        if (promises.size() == 0) {
            return promise.doFulfill(new ArrayList<D>(0));
        }
        final List<D> results = new ArrayList<>(promises.size());
        for (Promise<D> one : promises) {
            one.chain(new Chain<D>() {
                @Override
                public void chain(Promise<D> from) {
                    if (from.status != STATUS_FULFILLED) {
                        promise.doReject(from.exception);
                        return;
                    }
                    results.add(from.result);
                    if (results.size() == promises.size()) {
                        promise.doFulfill(results);
                    }
                }
            });
        }
        return promise;
    }

    @Retention(SOURCE)
    @IntDef({STATUS_PENDING, STATUS_FULFILLED, STATUS_REJECTED})
    private @interface Status {}
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_FULFILLED = 1;
    private static final int STATUS_REJECTED = 2;

    private Handler handler;
    private @Status int status = STATUS_PENDING;
    private D result;
    private Exception exception;
    private List<Chain<D>> chain;
    private interface Chain<C> {
        void chain(Promise<C> from);
    }
    private Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            synchronized (Promise.this) {
                @SuppressWarnings("unchecked")
                Chain<D> next = (Chain<D>) message.obj;
                if (status != STATUS_PENDING) {
                    if (chain != null) {
                        for (Chain<D> one : chain) {
                            one.chain(Promise.this);
                        }
                        chain.clear();
                    }
                    if (next != null) {
                        next.chain(Promise.this);
                    }
                    return true;
                }
                if (chain == null) {
                    chain = new LinkedList<>();
                }
                chain.add(next);
                return true;
            }
        }
    };

    public Promise(@NonNull Function<D> function) {
        this(getLooper(), function);
    }

    public Promise(@Nullable Looper looper, @NonNull final Function<D> function) {
        if (looper == null) {
            looper = getLooper();
        }
        assertNonNull("Function should not be null.", function);
        this.handler = new Handler(looper, callback);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    function.function(new Resolver<D>() {
                        @Override
                        public void fulfill(D result) {
                            Promise.this.doFulfill(result);
                        }
                        @Override
                        public void reject(Exception e) {
                            Promise.this.doReject(e);
                        }
                    });
                } catch (Exception e) {
                    if (!isPending()) {
                        throw new RuntimeException(e);
                    }
                    doReject(e);
                }
            }
        };
        if (handler.getLooper() == Looper.myLooper()) {
            runnable.run();
            return;
        }
        this.handler.post(runnable);
    }

    private Promise(@NonNull Looper looper) {
        this.handler = new Handler(looper, callback);
    }

    public synchronized boolean isPending() {
        return status == STATUS_PENDING;
    }

    public synchronized boolean isFulfilled() {
        return status == STATUS_FULFILLED;
    }

    public synchronized boolean isRejected() {
        return status == STATUS_REJECTED;
    }

    @Nullable
    public synchronized D getResult() throws Exception {
        if (status == STATUS_PENDING) {
            throw new PendingException(this);
        }
        if (status == STATUS_REJECTED) {
            throw exception;
        }
        return result;
    }

    private synchronized Promise<D> doFulfill(final D result) {
        if (status != STATUS_PENDING) {
            return this;
        }
        this.result = result;
        status = STATUS_FULFILLED;
        notifyAll();
        handler.sendEmptyMessage(1);
        return this;
    }

    private synchronized Promise<D> doReject(final Exception exception) {
        if (status != STATUS_PENDING) {
            return this;
        }
        this.exception = (exception == null)
                ? new NullPointerException("Rejected with null") : exception;
        status = STATUS_REJECTED;
        notifyAll();
        handler.sendEmptyMessage(1);
        return this;
    }

    private synchronized Promise<D> doApply(final Promise<D> from) {
        if (status != STATUS_PENDING) {
            return this;
        }
        result = from.result;
        exception = from.exception;
        status = from.status;
        notifyAll();
        handler.sendEmptyMessage(1);
        return this;
    }

    private Promise<D> chain(Chain<D> chain) {
        handler.sendMessage(handler.obtainMessage(0, chain));
        return this;
    }

    @NonNull
    public Promise<D> onThen(@Nullable Callback<D> fulfilled) {
        return onThen(fulfilled, null);
    }

    @NonNull
    public Promise<D> onThen(@Nullable final Callback<D> fulfilled, @Nullable final Callback<Exception> rejected) {
        final Promise<D> promise = new Promise<>(handler.getLooper());
        chain(new Chain<D>() {
            @Override
            public void chain(Promise<D> from) {
                try {
                    if (from.status == STATUS_FULFILLED && fulfilled != null) {
                        fulfilled.callback(from.result);
                    } else if (from.status == STATUS_REJECTED && rejected != null) {
                        rejected.callback(from.exception);
                    }
                    promise.doApply(from);
                } catch (Exception e) {
                    promise.doReject(e);
                }
            }
        });
        return promise;
    }

    @NonNull
    public <N> Promise<N> onThen(@NonNull final Filter<D, N> fulfilled) {
        return onThen(fulfilled, null);
    }

    @NonNull
    public <N> Promise<N> onThen(@NonNull final Filter<D, N> fulfilled, @Nullable final Filter<Exception, N> rejected) {
        assertNonNull("fulfilled should be not null.", fulfilled);
        final Promise<N> promise = new Promise<>(handler.getLooper());
        chain(new Chain<D>() {
            @Override
            public void chain(Promise<D> from) {
                try {
                    if (from.status == STATUS_FULFILLED) {
                        promise.doFulfill(fulfilled.filter(from.result));
                    } else {
                        if (rejected != null) {
                            promise.doFulfill(rejected.filter(from.exception));
                        } else {
                            promise.doReject(from.exception);
                        }
                    }
                } catch (Exception e) {
                    promise.doReject(e);
                }
            }
        });
        return promise;
    }

    @NonNull
    public <N> Promise<N> onThen(@NonNull final Pipe<D, N> fulfilled) {
        return onThen(fulfilled, null);
    }

    @NonNull
    public <N> Promise<N> onThen(@NonNull final Pipe<D, N> fulfilled, @Nullable final Pipe<Exception, N> rejected) {
        assertNonNull("fulfilled should be not null.", fulfilled);
        final Promise<N> promise = new Promise<>(handler.getLooper());
        chain(new Chain<D>() {
            @Override
            public void chain(Promise<D> from) {
                try {
                    if (from.status == STATUS_FULFILLED || rejected != null) {
                        Promise<N> piped = from.status == STATUS_FULFILLED ?
                                fulfilled.pipe(from.result) :
                                rejected.pipe(from.exception);
                        if (piped == null) {
                            promise.doFulfill(null);
                            return;
                        }
                        piped.chain(new Chain<N>() {
                            @Override
                            public void chain(Promise<N> from) {
                                promise.doApply(from);
                            }
                        });
                    } else {
                        promise.doReject(from.exception);
                    }
                } catch (Exception e) {
                    promise.doReject(e);
                }
            }
        });
        return promise;
    }

    @NonNull
    public Promise<D> onCatch(@Nullable final Callback<Exception> rejected) {
        return onThen(null, rejected);
    }

    @NonNull
    public Promise<D> onCatch(@Nullable final Filter<Exception, D> rejected) {
        return onThen(new Filter<D, D>() {
            @Nullable
            @Override
            public D filter(@Nullable D result) throws Exception {
                return result;
            }
        }, rejected);
    }

    @NonNull
    public Promise<D> onCatch(@Nullable final Pipe<Exception, D> rejected) {
        return onThen(new Pipe<D, D>() {
            @NonNull
            @Override
            public Promise<D> pipe(@Nullable D result) throws Exception {
                return Promise.resolve(result);
            }
        }, rejected);
    }

    @NonNull
    public Promise<D> onFinally(@Nullable final Callback<Promise<D>> callback) {
        final Promise<D> promise = new Promise<>(handler.getLooper());
        chain(new Chain<D>() {
            @Override
            public void chain(Promise<D> from) {
                try {
                    if (callback != null) {
                        callback.callback(from);
                    }
                    promise.doApply(from);
                } catch (Exception e) {
                    promise.doReject(e);
                }
            }
        });
        return promise;
    }

    @NonNull
    public <N> Promise<N> onFinally(@NonNull final Filter<Promise<D>, N> filter) {
        assertNonNull("filter should be not null.", filter);
        final Promise<N> promise = new Promise<>(handler.getLooper());
        chain(new Chain<D>() {
            @Override
            public void chain(Promise<D> from) {
                try {
                    promise.doFulfill(filter.filter(from));
                } catch (Exception e) {
                    promise.doReject(e);
                }
            }
        });
        return promise;
    }

    @NonNull
    public <N> Promise<N> onFinally(@NonNull final Pipe<Promise<D>, N> pipe) {
        assertNonNull("Pipe should be not null.", pipe);
        final Promise<N> promise = new Promise<>(handler.getLooper());
        chain(new Chain<D>() {
            @Override
            public void chain(Promise<D> from) {
                try {
                    Promise<N> piped = pipe.pipe(from);
                    if (piped == null) {
                        promise.doFulfill(null);
                        return;
                    }
                    piped.chain(new Chain<N>() {
                        @Override
                        public void chain(Promise<N> from) {
                            promise.doApply(from);
                        }
                    });
                } catch (Exception e) {
                    promise.doReject(e);
                }
            }
        });
        return promise;
    }

    private static void assertNonNull(String message, Object... objects) {
        for (Object one : objects) {
            if (one == null) {
                throw new NullPointerException(message);
            }
        }
    }

    @NonNull
    private static Looper getLooper() {
        Looper looper = Looper.myLooper();
        if (looper != null) {
            return looper;
        }
        looper = Looper.getMainLooper();
        if (looper == null) {
            throw new IllegalStateException("Looper.getMainLooper() returns null.");
        }
        return looper;
    }

    @NonNull
    public synchronized Promise<D> sync() {
        if (status != STATUS_PENDING) {
            return this;
        }
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this;
    }
}
