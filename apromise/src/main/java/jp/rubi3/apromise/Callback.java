package jp.rubi3.apromise;

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
    void callback(D result) throws Exception;
}
