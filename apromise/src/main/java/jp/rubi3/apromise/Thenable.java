package jp.rubi3.apromise;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Thenable
 *
 * Created by kikuchi on 2017/02/15.
 */

interface Thenable<D> {
    boolean isPending();

    boolean isFulfilled();

    boolean isRejected();

    @NonNull
    Thenable<D> onThen(@Nullable final Callback<D> fulfilled);

    @NonNull
    Thenable<D> onThen(@Nullable final Callback<D> fulfilled, @Nullable final Callback<Exception> rejected);

    @NonNull
    <N> Thenable<N> onThen(@NonNull final Filter<D, N> fulfilled);

    @NonNull
    <N> Thenable<N> onThen(@NonNull final Filter<D, N> fulfilled, @Nullable final Filter<Exception, N> rejected);

    @NonNull
    <N> Thenable<N> onThen(@NonNull final Pipe<D, N> fulfilled);

    @NonNull
    <N> Thenable<N> onThen(@NonNull final Pipe<D, N> fulfilled, @Nullable final Pipe<Exception, N> rejected);

    @NonNull
    Thenable<D> onCatch(@Nullable final Callback<Exception> rejected);

    @NonNull
    Thenable<D> onCatch(@Nullable final Filter<Exception, D> rejected);

    @NonNull
    Thenable<D> onCatch(@Nullable final Pipe<Exception, D> rejected);

    @NonNull
    Thenable<D> onFinally(@Nullable final Callback<Promise<D>> callback);

    @NonNull
    <N> Thenable<N> onFinally(@NonNull final Filter<Promise<D>, N> filter);

    @NonNull
    <N> Thenable<N> onFinally(@NonNull final Pipe<Promise<D>, N> pipe);

    @NonNull
    Thenable<D> await();
}
