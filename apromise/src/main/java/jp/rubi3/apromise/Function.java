package jp.rubi3.apromise;

/**
 * Created by halmakey on 2016/02/09.
 */
public interface Function<D> {
    /***
     * function
     *
     * @param resolver
     * @throws Exception will cause reject Promise.
     */
    void function(Resolver<D> resolver) throws Exception;
}
