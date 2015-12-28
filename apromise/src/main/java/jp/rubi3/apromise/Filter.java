package jp.rubi3.apromise;

/**
 * Created by halmakey on 2015/03/12.
 */
public interface Filter<D, N> {
    N filter(D result) throws Exception;
}
