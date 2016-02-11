package jp.rubi3.apromise;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by halmakey on 2015/03/12.
 */
public final class Promise<D> {
    /**
     * create resolve promise.
     *
     * @param object
     * @param <D>
     * @return returns reject promise if object instanceof Exception
     */
    public static <D> Promise<D> resolve(@Nullable final D object) {
        return new Promise().innerResolve(object);
    }

    /**
     * create reject promise.
     *
     * @param object
     * @return returns reject promise with NullPointerException instance if object is null.
     */
    public static Promise reject(@Nullable final Exception object) {
        return new Promise().innerReject(object);
    }

    public static <D> Promise<List<D>> all(@NonNull final Promise<D>... promises) {
        return all(new Handler(), promises);
    }

    public static <D> Promise<List<D>> all(@NonNull Handler handler, @NonNull final Promise<D>... promises) {
        final Promise<List<D>> promise = new Promise<>(handler);
        for (Promise<D> one : promises) {
            one.always(new Callback<Object>() {
                @Override
                public void callback(Object result) throws Exception {
                    if (!promise.isPending()) {
                        return;
                    }
                    if (result instanceof Exception) {
                        promise.innerReject((Exception) result);
                        return;
                    }
                    ArrayList<D> results = new ArrayList<>(promises.length);
                    for (Promise one : promises) {
                        if (one.isPending()) {
                            return;
                        }
                        results.add((D) one.result);
                    }
                    promise.innerFulfill(results);
                }
            });
        }
        return promise;
    }

    private interface Chain<D> {
        void onFulfilled(D result);
        void onRejected(Exception exception);
    }

    private static final int PENDING      = 0;
    private static final int FULFILLED    = 1;
    private static final int REJECTED     = 2;
    private static final int ALL          = 3;

    private Handler handler;
    private int status;
    private D result;
    private Exception exception;
    private List<Chain<D>> children;

    public final boolean isPending() {
        return status == PENDING;
    }

    public final boolean isFulfilled() {
        return status == FULFILLED;
    }

    public final boolean isRejected() {
        return status == REJECTED;
    }

    public final D getResult() {
        return result;
    }

    public Exception getException() {
        return exception;
    }

    public final Promise<D> then(@NonNull Callback<D> callback) {
        return attach(FULFILLED, callback);
    }

    public final <N> Promise<N> then(@NonNull Filter<D, N> filter) {
        return attach(FULFILLED, filter);
    }

    public final <N> Promise<N> then(@NonNull Pipe<D, N> pipe) {
        return attach(FULFILLED, pipe);
    }

    public final Promise<D> fail(@NonNull Callback<Exception> callback) {
        return attach(REJECTED, callback);
    }

    public final Promise<D> fail(@NonNull Filter<Exception, D> filter) {
        return attach(REJECTED, filter);
    }

    public final Promise<D> fail(@NonNull Pipe<Exception, D> pipe) {
        return attach(null, pipe);
    }

    public final Promise<D> always(@NonNull Callback<Object> callback) {
        return attach(callback, callback);
    }

    public final <N> Promise<N> always(@NonNull Filter<Object, N> filter) {
        return attach(filter, filter);
    }

    public final <N> Promise<N> always(@NonNull Pipe<Object, N> pipe) {
        return attach(pipe, pipe);
    }

    public Promise(@NonNull Function<D> function) {
        this(new Handler(), function);
    }

    public Promise(@NonNull Handler handler,@NonNull final Function<D> function) {
        this(handler);
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    function.function(new Resolver<D>() {
                        @Override
                        public void fulfill(D result) {
                            Promise.this.innerFulfill(result);
                        }

                        @Override
                        public void reject(Exception e) {
                            Promise.this.innerReject(e);
                        }
                    });
                } catch (Exception e) {
                    if (!isPending()) {
                        throw new RuntimeException(e);
                    }
                    innerReject(e);
                }
            }
        });
    }

    private Promise() {
        this.handler = new Handler();
    }

    private Promise(Handler handler) {
        this.handler = handler;
    }

    private synchronized Promise<D> innerFulfill(final D result) {
        if (status != PENDING) {
            throw new IllegalStateException("already resolved");
        }
        this.result = result;
        status = FULFILLED;
        if (children == null) {
            return this;
        }
        for (final Chain<D> child : children) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    child.onFulfilled(result);
                }
            });
        }
        children.clear();
        return this;
    }

    private synchronized Promise<D> innerReject(final Exception e) {
        if (status != PENDING) {
            throw new IllegalStateException("already resolved");
        }
        if (e == null) {
            this.exception = new NullPointerException("reject with null");
        } else {
            this.exception = e;
        }
        status = REJECTED;
        if (children == null) {
            return this;
        }
        for (final Chain child : children) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    child.onRejected(e);
                }
            });
        }
        children.clear();
        return this;
    }

    private synchronized void attach(final Chain<D> chain) {
        if (status != PENDING) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (status == FULFILLED) {
                        chain.onFulfilled(result);
                    } else {
                        chain.onRejected(exception);
                    }
                }
            });
            return;
        }
        if (children == null) {
            children = new ArrayList<>(1);
        }
        children.add(chain);
    }

    private Promise<D> attach(final Callback fulfilled, final Callback rejected) {
        final Promise<D> promise = new Promise<>(handler);
        attach(new Chain<D>() {
            @Override
            public void onFulfilled(D result) {
                try {
                    fulfilled.callback(result);
                    promise.innerFulfill(result);
                } catch (Exception e) {
                    promise.innerReject(e);
                }
            }

            @Override
            public void onRejected(Exception exception) {
                try {
                    rejected.callback(exception);
                    promise.innerReject(exception);
                } catch (Exception e) {
                    promise.innerReject(e);
                }
            }
        });
        return promise;
    }

    private <N> Promise<N> attach(final Filter<Object, N> fulfilled, final Filter<Object, N> rejected) {
        final Promise<N> promise = new Promise<>(handler);
        attach(new Chain<D>() {
            @Override
            public void onFulfilled(D result) {
                try {
                    N next = fulfilled.filter(result);
                    promise.innerFulfill(next);
                } catch (Exception e) {
                    promise.innerReject(e);
                }
            }

            @Override
            public void onRejected(Exception exception) {
                try {
                    N next = rejected.filter(exception);
                    promise.innerFulfill(next);
                } catch (Exception e) {
                    promise.innerReject(e);
                }
            }
        });
        return promise;
    }

    private <N> Promise<N> attach(final Pipe<Object, N> fulfilled, final Pipe<Object, N> rejected) {
        final Promise<N> promise = new Promise<>(handler);
        attach(new Chain<D>() {
            @Override
            public void onFulfilled(D result) {
                try {
                    fulfilled.pipe(result).attach(new Chain<N>() {
                        @Override
                        public void onFulfilled(N result) {
                            promise.innerFulfill(result);
                        }

                        @Override
                        public void onRejected(Exception exception) {
                            promise.innerReject(exception);
                        }
                    });
                } catch (Exception e) {
                    promise.innerReject(e);
                }
            }

            @Override
            public void onRejected(Exception exception) {
                try {
                    rejected.pipe(exception).attach(new Chain<N>() {
                        @Override
                        public void onFulfilled(N result) {
                            promise.innerFulfill(result);
                        }

                        @Override
                        public void onRejected(Exception exception) {
                            promise.innerReject(exception);
                        }
                    });
                } catch (Exception e) {
                    promise.innerReject(e);
                }
            }
        });
        return promise;
    }
}
