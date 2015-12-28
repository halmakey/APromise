package jp.rubi3.apromise;

/**
 * Created by halmakey on 2015/03/12.
 */
public interface Callback<D> {
    void callback(D result) throws Exception;
}
