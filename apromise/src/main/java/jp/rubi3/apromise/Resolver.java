package jp.rubi3.apromise;

/**
 * Created by halmakey on 2016/02/09.
 */
public interface Resolver<D> {
    /***
     * resolve with result
     *
     * this will throw IllegalStateException if already resolve or reject
     *
     * @param result is fulfill. reject with result if instance of Exception
     */
    void resolve(D result);

    /***
     * reject with e
     *
     * this will throw IllegalStateException if already resolve or reject.
     *
     * @param e is reject. reject with NullPointerException if null
     */
    void reject(Exception e);
}
