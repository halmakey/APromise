package jp.rubi3.apromise;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 *
 * PipeNonNull
 *
 * Created by kikuchi on 2017/02/20.
 */
public interface PipeNonNull<D, N> {
    @Nullable
    Promise<N> pipe(@NonNull D result) throws Exception;
}
