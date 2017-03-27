package org.grassroot.android.utils.rxutils;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by luke on 2017/03/27.
 */

public class ObserverFromConsumer<T> implements Observer<T> {

    private final Consumer<T> nextConsumer;
    private final Consumer<Throwable> errrorConsumer;

    public ObserverFromConsumer(@NonNull final Consumer<T> nextConsumer) {
        this.nextConsumer = nextConsumer;
        this.errrorConsumer = null;
    }

    public ObserverFromConsumer(@NonNull final Consumer<T> nextConsumer, @NonNull final Consumer<Throwable> onError) {
        this.nextConsumer = nextConsumer;
        this.errrorConsumer = onError;
    }

    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onNext(T t) {
        try {
            nextConsumer.accept(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Throwable e) {
        if (errrorConsumer != null) {
            try{
                errrorConsumer.accept(e);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    @Override
    public void onComplete() {

    }
}
