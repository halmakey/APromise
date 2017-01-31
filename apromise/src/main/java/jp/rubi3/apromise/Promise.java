package jp.rubi3.apromise;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Promise
 *
 * Created by halmakey on 2015/03/12.
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
    public static <D> Promise<D> resolve(@Nullable final D object) {
        return new Promise<D>().doResolve(object);
    }

    /**
     * create resolved promise with Void.
     *
     * @return returns resolve promise with null
     */
    @NonNull
    public static Promise<Void> resolve() {
        return new Promise<Void>().doResolve(null);
    }

    /**
     * create rejected promise.
     *
     * @param exception reject with exception
     * @return returns promise rejected with exception or NullPointerException if exception is null.
     */
    @NonNull
    public static Promise reject(@Nullable final Exception exception) {
        return new Promise<>().doReject(exception);
    }

    @SafeVarargs
    @NonNull
    public static <D> Promise<List<D>> all(@NonNull final Promise<D>... promises) {
        return all(new Handler(), promises);
    }

    @SafeVarargs
    @NonNull
    public static <D> Promise<List<D>> all(@NonNull final Handler handler, @NonNull final Promise<D>... promises) {
        final Promise<List<D>> promise = new Promise<>(handler);
        if (promises.length == 0) {
            return promise.doResolve(new ArrayList<D>(0));
        }
        final List<D> results = new ArrayList<>(promises.length);
        for (Promise<D> one:
             promises) {
            one.doChain(new Chain<D, List<D>>(promise) {
                @Override
                void onResolve(D next) {
                    results.add(next);
                    if (results.size() == promises.length) {
                        promise.doResolve(results);
                    }
                }

                @Override
                void onReject(Exception exception) {
                    if (promise.resolved) {
                        return;
                    }
                    promise.doReject(exception);
                }
            });
        }
        return promise;
    }

    private static abstract class Chain<D, N> {
        Promise<N> promise;
        Chain<D, ?> chain;

        Chain(Promise<N> promise) {
            this.promise = promise;
        }

        void doResolve(D next) {
            try {
                onResolve(next);
            } catch (Exception e) {
                promise.doReject(e);
            } finally {
                if (chain != null) {
                    chain.doResolve(next);
                }
            }
        }
        abstract void onResolve(D next) throws Exception;
        void doReject(Exception exception) {
            try {
                onReject(exception);
            } catch (Exception e) {
                promise.doReject(e);
            } finally {
                if (chain != null) {
                    chain.doReject(exception);
                }
            }
        }
        abstract void onReject(Exception exception) throws Exception;
    }

    private Handler handler;
    private D result;
    private Exception exception;
    private boolean resolved;
    private Chain<D, ?> chain;

    public Promise(@NonNull Function<D> function) {
        this(new Handler(), function);
    }

    public Promise(@NonNull Handler handler,@NonNull final Function<D> function) {
        this(handler);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    function.function(new Resolver<D>() {
                        @Override
                        public void fulfill(D result) {
                            Promise.this.doResolve(result);
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

    private Promise() {
        this.handler = new Handler();
    }

    private Promise(Handler handler) {
        this.handler = handler;
    }

    public final boolean isPending() {
        return !resolved;
    }

    public final boolean isFulfilled() {
        return resolved && exception == null;
    }

    public final boolean isRejected() {
        return exception != null;
    }

    @Nullable
    public final D getResult() {
        return result;
    }

    @Nullable
    public Exception getException() {
        return exception;
    }

    private synchronized Promise<D> doResolve(final D result) {
        if (resolved) {
            return this;
        }
        this.result = result;
        resolved = true;
        notifyAll();
        return doChain(chain);
    }

    private synchronized Promise<D> doReject(final Exception exception) {
        if (resolved) {
            return this;
        }
        this.exception = (exception == null)
                ? new NullPointerException("reject with null") : exception;
        resolved = true;
        notifyAll();
        return doChain(chain);
    }

    private synchronized Promise<D> doChain(final Chain<D, ?> chain) {
        if (chain == null) {
            return this;
        }
        if (resolved) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (exception == null) {
                        chain.doResolve(result);
                    } else {
                        chain.doReject(exception);
                    }
                }
            });
            return this;
        }
        if (this.chain == null) {
            this.chain = chain;
            return this;
        }
        Chain<D, ?> last = this.chain;
        while (last.chain != null) {
            last = last.chain;
        }
        last.chain = chain;
        return this;
    }

    @NonNull
    public Promise<D> onThen(@Nullable final Callback<D> fulfilled) {
        return onThen(fulfilled, null);
    }

    @NonNull
    public Promise<D> onThen(@Nullable final Callback<D> fulfilled, @Nullable final Callback<Exception> rejected) {
        final Promise<D> promise = new Promise<>(handler);
        doChain(new Chain<D, D>(promise) {

            @Override
            void onResolve(D next) throws Exception {
                if (fulfilled != null) {
                    fulfilled.callback(result);
                }
                promise.doResolve(next);
            }

            @Override
            void onReject(Exception exception) throws Exception {
                if (rejected != null) {
                    rejected.callback(exception);
                }
                promise.doReject(exception);
            }
        });
        return promise;
    }

    @NonNull
    public <N> Promise<N> onThen(@NonNull final Filter<D, N> fulfilled) {
        assertNonNull("fulfilled should be not null.");
        return onThen(fulfilled, null);
    }

    @NonNull
    public <N> Promise<N> onThen(@NonNull final Filter<D, N> fulfilled, @Nullable final Filter<Exception, N> rejected) {
        assertNonNull("fulfilled should be not null.", fulfilled);
        Promise<N> promise = new Promise<>(handler);
        doChain(new Chain<D, N>(promise) {
            @Override
            void onResolve(D next) throws Exception {
                N result = fulfilled.filter(next);
                promise.doResolve(result);
            }

            @Override
            void onReject(Exception exception) throws Exception {
                if (rejected == null) {
                    promise.doReject(exception);
                    return;
                }
                N result = rejected.filter(exception);
                promise.doResolve(result);
            }
        });
        return promise;
    }

    @NonNull
    public <N> Promise<N> onThen(@NonNull final Pipe<D, N> fulfilled) {
        assertNonNull("fulfilled should be not null.", fulfilled);
        return onThen(fulfilled, null);
    }

    @NonNull
    public <N> Promise<N> onThen(@NonNull final Pipe<D, N> fulfilled, @Nullable final Pipe<Exception, N> rejected) {
        assertNonNull("fulfilled should be not null.", fulfilled);
        final Promise<N> promise = new Promise<>(handler);
        doChain(new Chain<D, N>(promise) {
            @Override
            void onResolve(D next) throws Exception {
                Promise<N> piped = fulfilled.pipe(next);
                if (piped == null) {
                    promise.doResolve(null);
                    return;
                }
                piped.doChain(new Chain<N, N>(promise) {
                    @Override
                    void onResolve(N next) throws Exception {
                        promise.doResolve(next);
                    }

                    @Override
                    void onReject(Exception exception) throws Exception {
                        promise.doReject(exception);
                    }
                });
            }

            @Override
            void onReject(Exception exception) throws Exception {
                if (rejected == null) {
                    promise.doReject(exception);
                    return;
                }
                Promise<N> piped = rejected.pipe(exception);
                if (piped == null) {
                    promise.doResolve(null);
                    return;
                }
                piped.doChain(new Chain<N, N>(promise) {
                    @Override
                    void onResolve(N next) throws Exception {
                        promise.doResolve(next);
                    }

                    @Override
                    void onReject(Exception exception) throws Exception {
                        promise.doReject(exception);
                    }
                });
            }
        });
        return promise;
    }

    @NonNull
    public Promise<D> onCatch(@Nullable final Callback<Exception> rejected) {
        final Promise<D> promise = new Promise<>(handler);
        doChain(new Chain<D, D>(promise) {
            @Override
            void onResolve(D next) throws Exception {
                promise.doResolve(next);
            }

            @Override
            void onReject(Exception exception) throws Exception {
                if (rejected != null) {
                    rejected.callback(exception);
                }
                promise.doReject(exception);
            }
        });
        return promise;
    }

    @NonNull
    public Promise<D> onCatch(@Nullable final Filter<Exception, D> rejected) {
        final Promise<D> promise = new Promise<>(handler);
        doChain(new Chain<D, D>(promise) {
            @Override
            void onResolve(D next) throws Exception {
                promise.doResolve(next);
            }

            @Override
            void onReject(Exception exception) throws Exception {
                if (rejected != null) {
                    D result = rejected.filter(exception);
                    promise.doResolve(result);
                } else {
                    promise.doReject(exception);
                }
            }
        });
        return promise;
    }

    @NonNull
    public Promise<D> onCatch(@Nullable final Pipe<Exception, D> rejected) {
        final Promise<D> promise = new Promise<>(handler);
        doChain(new Chain<D, D>(promise) {
            @Override
            void onResolve(D next) throws Exception {
                promise.doResolve(next);
            }

            @Override
            void onReject(Exception exception) throws Exception {
                if (rejected == null) {
                    promise.doReject(exception);
                    return;
                }
                Promise<D> piped = rejected.pipe(exception);
                if (piped == null) {
                    promise.doResolve(null);
                    return;
                }
                piped.doChain(new Chain<D, D>(promise) {
                    @Override
                    void onResolve(D next) throws Exception {
                        promise.doResolve(next);
                    }

                    @Override
                    void onReject(Exception exception) throws Exception {
                        promise.doReject(exception);
                    }
                });
            }
        });
        return promise;
    }

    @NonNull
    public Promise<D> onFinally(@Nullable final Callback<Promise<D>> callback) {
        final Promise<D> promise = new Promise<>(handler);
        doChain(new Chain<D, D>(promise) {
            @Override
            void onResolve(D next) throws Exception {
                if (callback != null) {
                    callback.callback(Promise.this);
                }
                promise.doResolve(next);
            }

            @Override
            void onReject(Exception exception) throws Exception {
                if (callback != null) {
                    callback.callback(Promise.this);
                }
                promise.doReject(exception);
            }
        });
        return promise;
    }

    @NonNull
    public <N> Promise<N> onFinally(@NonNull final Filter<Promise<D>, N> filter) {
        assertNonNull("filter should be not null.", filter);
        final Promise<N> promise = new Promise<>(handler);
        doChain(new Chain<D, N>(promise) {
            @Override
            void onResolve(D next) throws Exception {
                N result = filter.filter(Promise.this);
                promise.doResolve(result);
            }

            @Override
            void onReject(Exception exception) throws Exception {
                N result = filter.filter(Promise.this);
                promise.doResolve(result);
            }
        });
        return promise;
    }

    @NonNull
    public <N> Promise<N> onFinally(@NonNull final Pipe<Promise<D>, N> pipe) {
        assertNonNull("pipe should be not null.", pipe);
        final Promise<N> promise = new Promise<>(handler);
        doChain(new Chain<D, N>(promise) {
            @Override
            void onResolve(D next) throws Exception {
                common();
            }

            @Override
            void onReject(Exception exception) throws Exception {
                common();
            }

            void common() throws Exception {
                Promise<N> piped = pipe.pipe(Promise.this);
                if (piped == null) {
                    promise.doResolve(null);
                    return;
                }
                piped.doChain(new Chain<N, N>(promise) {
                    @Override
                    void onResolve(N next) throws Exception {
                        promise.doResolve(next);
                    }

                    @Override
                    void onReject(Exception exception) throws Exception {
                        promise.doReject(exception);
                    }
                });

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

    public synchronized Promise<D> await() {
        if (resolved) {
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
