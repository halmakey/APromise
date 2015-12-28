package jp.rubi3.apromise;

/**
 * Created by halmakey on 2015/12/25.
 */
public interface Dispatch<D> {
    D dispatch() throws Exception;
}
