package jp.rubi3.apromise;

import android.support.annotation.NonNull;

/**
 *
 * CallbackNonNull
 *
 * Created by kikuchi on 2017/02/20.
 */
public interface CallbackNonNull<D> {
    void callback(@NonNull D result) throws Exception;
}
