package jp.rubi3.apromise;

import android.os.Handler;
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
     * create resolve promise.
     *
     * @param object resolve with object
     * @param <D> result object type
     * @return returns reject promise if object instanceof Exception
     */
    public static <D> Promise<D> resolve(@Nullable final D object) {
        return new Promise<D>().innerResolve(object);
    }

    /**
     * create reject promise.
     *
     * @param exception reject with exception
     * @return returns reject promise with NullPointerException instance if object is null.
     */
    public static <D> Promise<D> reject(@Nullable final Exception exception) {
        return new Promise<D>().innerReject(exception);
    }

    @SafeVarargs
    public static <D> Promise<List<D>> all(@NonNull final Promise<D>... promises) {
        return all(new Handler(), promises);
    }

    @SafeVarargs
    public static <D> Promise<List<D>> all(@NonNull final Handler handler, @NonNull final Promise<D>... promises) {
        Chain<D, List<D>> chain = new Chain<D, List<D>>() {
            { promise = new Promise<>(handler); }
            int count = 0;

            @Override
            void onChain(Promise<D> resolved) throws Exception {
                if (!promise.isPending()) {
                    return;
                }
                if (resolved.isRejected()) {
                    promise.innerResolve(resolved.result);
                    return;
                }
                count++;
                if (count < promises.length) {
                    return;
                }
                ArrayList<D> results = new ArrayList<>(promises.length);
                for (Promise<D> one : promises) {
                    results.add(one.getResult());
                }
                promise.innerResolve(results);
            }
        };
        for (Promise<D> one : promises) {
            one.attach(chain);
        }
        return chain.promise;
    }

    private static abstract class Chain<D, N> {
        Promise<N> promise;
        void chainFrom(Promise<D> resolved) {
            try {
                onChain(resolved);
            } catch (Exception e) {
                promise.innerReject(e);
            }
        }
        abstract void onChain(Promise<D> resolved) throws Exception;
    }

    private static final int PENDING      = 0;
    private static final int FULFILLED    = 1;
    private static final int REJECTED     = 2;

    private Handler handler;
    private int status;
    private Object result;
    private List<Chain<D, ?>> children;

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
                            Promise.this.innerResolve(result);
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

    public final boolean isPending() {
        return status == PENDING;
    }

    public final boolean isFulfilled() {
        return status == FULFILLED;
    }

    public final boolean isRejected() {
        return status == REJECTED;
    }

    @SuppressWarnings("unchecked")
    public final D getResult() {
        if (isFulfilled()) {
            return (D) result;
        }
        return null;
    }

    public Exception getException() {
        if (isRejected()) {
            return (Exception) result;
        }
        return null;
    }

    public final Promise<D> onThen(@NonNull Callback<D> callback) {
        return attach(nonNull(callback), null);
    }

    public final <N> Promise<N> onThen(@NonNull Filter<D, N> filter) {
        return attach(nonNull(filter), null);
    }

    public final <N> Promise<N> onThen(@NonNull Pipe<D, N> pipe) {
        return attach(nonNull(pipe), null);
    }

    public final Promise<D> onCatch(@NonNull Callback<Exception> callback) {
        return attach(null, nonNull(callback));
    }

    public final Promise<D> onCatch(@NonNull Filter<Exception, D> filter) {
        return attach(null, nonNull(filter));
    }

    public final Promise<D> onCatch(@NonNull Pipe<Exception, D> pipe) {
        return attach(null, nonNull(pipe));
    }

    public final Promise<D> onFinally(Callback<Promise<D>> callback) {
        return attachFinally(nonNull(callback));
    }

    public final <N> Promise<N> onFinally(@NonNull Filter<Promise<D>, N> filter) {
        return attachFinally(nonNull(filter));
    }

    public final <N> Promise<N> onFinally(@NonNull Pipe<Promise<D>, N> pipe) {
        return attachFinally(nonNull(pipe));
    }

    private synchronized Promise<D> innerResolve(Object object) {
        if (status != PENDING) {
            return this;
        }
        status = object instanceof Exception ? REJECTED : FULFILLED;
        this.result = object;
        if (children == null) {
            return this;
        }
        for (final Chain<D, ?> child : children) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    child.chainFrom(Promise.this);
                }
            });
        }
        children.clear();
        return this;
    }

    private synchronized Promise<D> innerReject(Exception e) {
        if (e == null) {
            e = new NullPointerException("reject with null");
        }
        return innerResolve(e);
    }

    private synchronized <N> Promise<N> attach(final Chain<D, N> chain) {
        if (status != PENDING) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    chain.chainFrom(Promise.this);
                }
            });
            return chain.promise;
        }
        if (children == null) {
            children = new ArrayList<>(1);
        }
        children.add(chain);
        return chain.promise;
    }

    @SuppressWarnings("unchecked")
    private Promise<D> attach(final Callback<D> fulfilled, final Callback<Exception> rejected) {
        return attach(new Chain<D, D>() {
            {
                promise = new Promise<>(handler);
            }

            @Override
            void onChain(Promise<D> resolved) throws Exception {
                Callback callback = resolved.isFulfilled() ? fulfilled : rejected;
                if (callback != null) {
                    callback.callback(resolved.result);
                }
                promise.innerResolve(resolved.result);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <N> Promise<N> attach(final Filter<D, N> fulfilled, final Filter<Exception, N> rejected) {
        return attach(new Chain<D, N>() {
            {
                promise = new Promise<>(handler);
            }

            @Override
            void onChain(Promise resolved) throws Exception {
                Filter filter = resolved.isFulfilled() ? fulfilled : rejected;
                if (filter == null) {
                    promise.innerResolve(resolved.result);
                    return;
                }
                N next = (N) filter.filter(resolved.result);
                promise.innerResolve(next);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <N> Promise<N> attach(final Pipe<D, N> fulfilled, final Pipe<Exception, N> rejected) {
        final Promise<N> attached = new Promise<>(handler);
        return attach(new Chain<D, N>() {
            {
                promise = attached;
            }

            @Override
            void onChain(Promise<D> resolved) throws Exception {
                Pipe pipe = resolved.isFulfilled() ? fulfilled : rejected;
                if (pipe == null) {
                    promise.innerResolve(resolved.result);
                    return;
                }
                Promise<N> next = pipe.pipe(resolved.result);
                if (next == null) {
                    promise.innerResolve(null);
                    return;
                }
                next.attach(new Chain<N, N>() {
                    {
                        promise = attached;
                    }

                    @Override
                    void onChain(Promise<N> resolved) throws Exception {
                        attached.innerResolve(resolved.result);
                    }
                });
            }
        });
    }

    private Promise<D> attachFinally(final Callback<Promise<D>> callback) {
        return attach(new Chain<D, D>() {
            {
                promise = new Promise<>(handler);
            }

            @Override
            void onChain(Promise<D> resolved) throws Exception {
                callback.callback(resolved);
                promise.innerResolve(resolved);
            }
        });
    }

    private <N> Promise<N> attachFinally(final Filter<Promise<D>, N> filter) {
        return attach(new Chain<D, N>() {
            { promise = new Promise<>(handler); }
            @Override
            void onChain(Promise<D> resolved) throws Exception {
                N next = filter.filter(resolved);
                promise.innerResolve(next);
            }
        });
    }

    private <N> Promise<N> attachFinally(final Pipe<Promise<D>, N> pipe) {
        final Promise<N> attached = new Promise<>(handler);
        return attach(new Chain<D, N>() {
            { promise = attached; }

            @Override
            void onChain(Promise<D> resolved) throws Exception {
                Promise<N> next = pipe.pipe(resolved);
                if (next == null) {
                    promise.innerResolve(null);
                    return;
                }
                next.attach(new Chain<N, N>() {
                    {
                        promise = attached;
                    }

                    @Override
                    void onChain(Promise<N> resolved) throws Exception {
                        attached.innerResolve(resolved.result);
                    }
                });
            }
        });
    }

    private static <T> T nonNull(T object) {
        if (object == null) {
            throw new NullPointerException();
        }
        return object;
    }
}
