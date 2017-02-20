package jp.rubi3.apromise;

/**
 * PendingException
 *
 * Created by kikuchi on 2017/02/19.
 */

public class PendingException extends RuntimeException {
    private final Promise promise;

    public PendingException(Promise promise) {
        super("Can't get result due to pending.");
        this.promise = promise;
    }

    public Promise getPromise() {
        return promise;
    }
}
