package jp.rubi3.apromise;

import android.os.Handler;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by halmakey on 2015/03/12.
 */
public final class Promise<D> {
    /**
     * create resolved promise.
     *
     * @param object
     * @param <D>
     * @return returns rejected promise if object instanceof Exception
     */
    public static <D> Promise<D> resolved(@Nullable final D object) {
        return new Promise().resolve(object);
    }

    /**
     * create rejected promise.
     *
     * @param object
     * @return returns rejected promise with NullPointerException instance if object is null.
     */
    public static Promise rejected(@Nullable final Exception object) {
        return new Promise().reject(object);
    }

    private interface Resolver {
        void resolve(Object object);
    }

    private static final int PENDING      = 0;
    private static final int FULFILLED    = 1;
    private static final int REJECTED     = 2;

    private Handler handler;
    private int status;
    private Object result;
    private List<Resolver> children;

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

    public final Promise<D> then(Callback<D> callback) {
        return attach(callback, null);
    }

    public final <N> Promise<N> then(Filter<D, N> filter) {
        return attach(filter, null);
    }

    public final <N> Promise<N> then(Pipe<D, N> pipe) {
        return attach(pipe, null);
    }

    public final Promise<D> then(final Callback<D> fulfilled, final Callback<Exception> rejected) {
        return attach(fulfilled, rejected);
    }

    public final <N> Promise<N> then(final Filter<D, N> fulfilled, final Filter<Exception, N> rejected) {
        return attach(fulfilled, rejected);
    }

    public final <N> Promise<N> then(final Pipe<D, N> fulfilled, final Pipe<Exception, N> rejected) {
        return attach(fulfilled, rejected);
    }

    public final Promise<D> catche(Callback<Exception> callback) {
        return attach(null, callback);
    }

    public final Promise<D> catche(Filter<Exception, D> filter) {
        return attach(null, filter);
    }

    public final Promise<D> catche(Pipe<Exception, D> pipe) {
        return attach(null, pipe);
    }

    public final Promise<D> all(Callback<Object> callback) {
        return attach(callback, callback);
    }

    public final <N> Promise<N> all(Filter<Object, N> filter) {
        return attach(filter, filter);
    }

    public final <N> Promise<N> all(Pipe<Object, N> pipe) {
        return attach(pipe, pipe);
    }

    public Promise() {
        this(new Handler());
    }

    public Promise(Handler handler) {
        this.handler = handler;
    }

    public synchronized Promise<D> reject(Exception e) {
        return resolve(e != null ? e : new NullPointerException("rejected with null"));
    }

    public synchronized Promise<D> resolve(final Object object) {
        if (!isPending()) {
            throw new RuntimeException("already resolved/rejected");
        }
        status = object instanceof Exception ? REJECTED : FULFILLED;
        result = object;

        if (children == null) {
            return this;
        }
        for (final Resolver child : children) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    child.resolve(object);
                }
            });
        }
        children.clear();
        return this;
    }

    private synchronized void attach(final Resolver resolver) {
        if (status != PENDING) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    resolver.resolve(result);
                }
            });
        }
        if (children == null) {
            children = new ArrayList<>(1);
        }
        children.add(resolver);
    }

    private Promise attach(final Callback fulfilled, final Callback rejected) {
        final Promise promise = new Promise(handler);
        attach(new Resolver() {
            @Override
            public void resolve(Object object) {
                try {
                    Callback callback = (object instanceof Exception) ? rejected : fulfilled;
                    if (callback == null) {
                        promise.resolve(object);
                        return;
                    }
                    callback.callback(object);
                    promise.resolve(object);
                } catch (Exception e) {
                    promise.resolve(e);
                }
            }
        });
        return promise;
    }

    private Promise attach(final Filter fulfilled, final Filter rejected) {
        final Promise promise = new Promise(handler);
        attach(new Resolver() {
            @Override
            public void resolve(Object object) {
                try {
                    Filter filter = (object instanceof Exception) ? rejected : fulfilled;
                    if (filter == null) {
                        promise.resolve(object);
                        return;
                    }
                    promise.resolve(filter.filter(object));
                } catch (Exception e) {
                    promise.resolve(e);
                }
            }
        });
        return promise;
    }

    private Promise attach(final Pipe fulfilled, final Pipe rejected) {
        final Promise promise = new Promise(handler);
        attach(new Resolver() {
            @Override
            public void resolve(Object object) {
                try {
                    final Pipe pipe = object instanceof Exception ? rejected : fulfilled;
                    if (pipe == null) {
                        promise.resolve(object);
                        return;
                    }
                    pipe.pipe(object).attach(new Resolver() {
                        @Override
                        public void resolve(Object object) {
                            promise.resolve(object);
                        }
                    });
                } catch (Exception e) {
                    promise.resolve(e);
                }
            }
        });
        return promise;
    }
}
