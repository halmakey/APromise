package jp.rubi3.apromise;

import android.support.annotation.Nullable;

/**
 *
 * Pipe
 *
 * Created by halmakey on 2015/03/12.
 */
public interface Pipe<D, N> {
    /**
     * pipe
     *
     * @param result previous result
     * @return promise resolve when returned promise will be resolve. or resolved with null if return is null.
     * @throws Exception to reject
     */
    @Nullable
    Promise<N> pipe(@Nullable D result) throws Exception;
}
