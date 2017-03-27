package org.grassroot.android.utils.rxutils;

import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by luke on 2017/03/27.
 */

public class SingleObserverFromConsumer<T> implements SingleObserver<T> {

    private final Consumer<T> successConsumer;
    private final Consumer<Throwable> errrorConsumer;

    public SingleObserverFromConsumer(@NonNull final Consumer<T> onSuccess) {
        this.successConsumer = onSuccess;
        this.errrorConsumer = null;
    }

    public SingleObserverFromConsumer(@NonNull final Consumer<T> onSuccess, @NonNull final Consumer<Throwable> onError) {
        this.successConsumer = onSuccess;
        this.errrorConsumer = onError;
    }

    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onSuccess(T t) {
        try {
            successConsumer.accept(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Throwable e) {
        if (errrorConsumer != null) {
            try {
                errrorConsumer.accept(e);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }
}
