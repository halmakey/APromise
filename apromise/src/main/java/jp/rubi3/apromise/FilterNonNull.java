package jp.rubi3.apromise;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 *
 * FilterNonNull
 *
 * Created by kikuchi on 2017/02/20.
 */
public interface FilterNonNull<D, N> {
    @Nullable
    N filter(@NonNull D result) throws Exception;
}
