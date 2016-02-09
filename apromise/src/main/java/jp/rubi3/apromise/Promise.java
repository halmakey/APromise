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

    private interface Chain {
        void chain(Object object);
    }

    private static final int PENDING      = 0;
    private static final int FULFILLED    = 1;
    private static final int REJECTED     = 2;
    private static final int ALL          = 3;

    private Handler handler;
    private int status;
    private Object result;
    private List<Chain> children;

    public final boolean isPending() {
        return status == PENDING;
    }

    public final boolean isFulfilled() {
        return status == FULFILLED;
    }

    public final boolean isRejected() {
        return status == REJECTED;
    }

    public final Object result() {
        return result;
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

    public final Promise<D> catche(@NonNull Callback<Exception> callback) {
        return attach(REJECTED, callback);
    }

    public final Promise<D> catche(@NonNull Filter<Exception, D> filter) {
        return attach(REJECTED, filter);
    }

    public final Promise<D> catche(@NonNull Pipe<Exception, D> pipe) {
        return attach(REJECTED, pipe);
    }

    public final Promise<D> all(@NonNull Callback<Object> callback) {
        return attach(ALL, callback);
    }

    public final <N> Promise<N> all(@NonNull Filter<Object, N> filter) {
        return attach(ALL, filter);
    }

    public final <N> Promise<N> all(@NonNull Pipe<Object, N> pipe) {
        return attach(ALL, pipe);
    }

    public Promise(@NonNull Function function) {
        this(new Handler(), function);
    }

    public Promise(@NonNull Handler handler,@NonNull final Function function) {
        this(handler);
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    function.function(new jp.rubi3.apromise.Resolver() {
                        @Override
                        public void resolve(Object result) {
                            if (!isPending()) {
                                throw new IllegalStateException("already resolved");
                            }
                            Promise.this.innerResolve(result);
                        }

                        @Override
                        public void reject(Exception e) {
                            if (!isPending()) {
                                throw new IllegalStateException("already rejected");
                            }
                            Promise.this.innerResolve(e);
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

    private synchronized Promise<D> innerReject(Exception e) {
        if (e == null) {
            return innerResolve(new NullPointerException("reject with null"));
        }
        return innerResolve(e);
    }

    private synchronized Promise<D> innerResolve(final Object object) {
        status = object instanceof Exception ? REJECTED : FULFILLED;
        result = object;

        if (children == null) {
            return this;
        }
        for (final Chain child : children) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    child.chain(object);
                }
            });
        }
        children.clear();
        return this;
    }

    private synchronized void attach(final Chain chain) {
        if (status != PENDING) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    chain.chain(result);
                }
            });
        }
        if (children == null) {
            children = new ArrayList<>(1);
        }
        children.add(chain);
    }

    private Promise attach(final int mask, final Callback callback) {
        final Promise promise = new Promise(handler);
        attach(new Chain() {
            @Override
            public void chain(Object object) {
                try {
                    if ((status & mask) != 0) {
                        callback.callback(object);
                    }
                    promise.innerResolve(object);
                } catch (Exception e) {
                    promise.innerResolve(e);
                }
            }
        });
        return promise;
    }

    private Promise attach(final int mask, final Filter filter) {
        final Promise promise = new Promise(handler);
        attach(new Chain() {
            @Override
            public void chain(Object object) {
                try {
                    if ((status & mask) == 0) {
                        promise.innerResolve(object);
                        return;
                    }
                    promise.innerResolve(filter.filter(object));
                } catch (Exception e) {
                    promise.innerResolve(e);
                }
            }
        });
        return promise;
    }

    private Promise attach(final int mask, final Pipe pipe) {
        final Promise promise = new Promise(handler);
        attach(new Chain() {
            @Override
            public void chain(Object object) {
                try {
                    if ((status & mask) == 0) {
                        promise.innerResolve(object);
                        return;
                    }
                    pipe.pipe(object).attach(new Chain() {
                        @Override
                        public void chain(Object object) {
                            promise.innerResolve(object);
                        }
                    });
                } catch (Exception e) {
                    promise.innerResolve(e);
                }
            }
        });
        return promise;
    }
}
