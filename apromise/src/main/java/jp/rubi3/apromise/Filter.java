package jp.rubi3.apromise;

import android.support.annotation.Nullable;

/**
 * Created by halmakey on 2015/03/12.
 */
public interface Filter<D, N> {
    /**
     * filter
     *
     * @param result
     * @return next result object. reject if Exception object
     * @throws Exception to reject
     */
    @Nullable
    N filter(D result) throws Exception;
}
