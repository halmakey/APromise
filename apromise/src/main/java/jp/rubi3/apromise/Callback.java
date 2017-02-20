package jp.rubi3.apromise;

import android.support.annotation.Nullable;

/**
 *
 * Callback
 *
 * Created by halmakey on 2015/03/12.
 */
public interface Callback<D> {
    /***
     * callback
     *
     * @param result previous result
     * @throws Exception to reject
     */
    void callback(@Nullable D result) throws Exception;
}
