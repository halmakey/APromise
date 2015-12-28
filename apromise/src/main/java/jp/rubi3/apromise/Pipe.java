package jp.rubi3.apromise;

/**
 * Created by halmakey on 2015/03/12.
 */
public interface Pipe<D, N> {
    Promise<N> pipe(D result) throws Exception;
}
