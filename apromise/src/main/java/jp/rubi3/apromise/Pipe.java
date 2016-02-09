package jp.rubi3.apromise;

import android.support.annotation.NonNull;

/**
 * Created by halmakey on 2015/03/12.
 */
public interface Pipe<D, N> {
    /**
     * pipe
     *
     * @param result
     * @return promise resolved when returned promise will be resolved.
     * @throws Exception to reject
     */
    @NonNull
    Promise<N> pipe(D result) throws Exception;
}
