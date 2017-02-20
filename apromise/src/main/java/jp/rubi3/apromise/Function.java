package jp.rubi3.apromise;

import android.support.annotation.NonNull;

/**
 *
 * Function
 *
 * Created by halmakey on 2016/02/09.
 */
public interface Function<D> {
    /***
     * function
     *
     * @param resolver Resolver
     * @throws Exception will cause reject Promise.
     */
    void function(@NonNull Resolver<D> resolver) throws Exception;
}
