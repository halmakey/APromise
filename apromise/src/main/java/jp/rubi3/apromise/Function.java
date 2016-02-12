package jp.rubi3.apromise;

/**
 *
 * Function
 *
 * Created by halmakey on 2016/02/09.
 */
public interface Function<D> {
    /***
     * function
     *
     * @param resolver Resolver
     * @throws Exception will cause reject Promise.
     */
    void function(Resolver<D> resolver) throws Exception;
}
