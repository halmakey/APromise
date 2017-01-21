package jp.rubi3.apromise;

import android.support.annotation.Nullable;

/**
 *
 * Resolver
 *
 * Created by halmakey on 2016/02/09.
 */
public interface Resolver<D> {
    /***
     * resolve with result
     *
     * this will throw IllegalStateException if already resolve or reject
     *
     * @param result is fulfill. reject with result if instance of Exception
     */
    void fulfill(@Nullable D result);

    /***
     * reject with e
     *
     * this will throw IllegalStateException if already resolve or reject.
     *
     * @param e is reject. reject with NullPointerException if null
     */
    void reject(@Nullable Exception e);
}
