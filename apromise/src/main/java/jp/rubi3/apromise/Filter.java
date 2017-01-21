package jp.rubi3.apromise;

import android.support.annotation.Nullable;

/**
 *
 * Filter
 *
 * Created by halmakey on 2015/03/12.
 */
public interface Filter<D, N> {
    /**
     * filter
     *
     * @param result previous result
     * @return next result object. reject if Exception object
     * @throws Exception to reject
     */
    @Nullable
    N filter(@Nullable D result) throws Exception;
}
