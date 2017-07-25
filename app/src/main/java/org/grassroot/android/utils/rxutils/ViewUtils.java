package org.grassroot.android.utils.rxutils;

import android.view.View;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by luke on 2017/07/24.
 */
public final class ViewUtils {

    public static Single<Long> showViewAndVanish(final View view, long delayInMillis) {
        return Single.timer(delayInMillis, TimeUnit.MILLISECONDS, Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(new Consumer<Long>() {
                    @Override
                    public void accept(@NonNull Long aLong) throws Exception {
                        view.setVisibility(View.GONE);
                    }
                });
    }

}
