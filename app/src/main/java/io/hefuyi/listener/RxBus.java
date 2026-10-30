package io.hefuyi.listener;

import android.util.Log;

import java.util.HashMap;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

/**
 * Created by hefuyi on 2017/1/2.
 */

public class RxBus {
    private static volatile RxBus mInstance;
    private final Subject<Object> mSubject;
    private HashMap<Object, CompositeDisposable> mSubscriptionMap;

    private RxBus() {
        mSubject = PublishSubject.create().toSerialized();
    }

    public static RxBus getInstance() {
        if (mInstance == null) {
            synchronized (RxBus.class) {
                if (mInstance == null) {
                    mInstance = new RxBus();
                }
            }
        }
        return mInstance;
    }

    /**
     * 发送事件
     *
     * @param o The event object to post.
     */
    public void post(Object o) {
        Log.d("RxBus", "Post event: " + o.getClass().getSimpleName());
        mSubject.onNext(o);
    }

    /**
     * 返回指定类型的Observable实例
     *
     * @param type Class type of the event.
     * @param <T> Event type parameter.
     * @return Observable for the specified event class.
     */
    public <T> Observable<T> toObservable(final Class<T> type) {
        return mSubject.ofType(type);
    }

    /**
     * 是否已有观察者订阅
     *
     * @return True if observers exist.
     */
    @SuppressWarnings("unused")
    public boolean hasObservers() {
        return mSubject.hasObservers();
    }

    /**
     * 一个默认的订阅方法
     *
     * @param type Event class type.
     * @param next Action for onNext.
     * @param error Action for onError.
     * @param <T> Event type.
     * @return Disposable instance.
     */
    @SuppressWarnings("unused")
    public <T> Disposable doSubscribe(Class<T> type, Consumer<T> next, Consumer<Throwable> error) {
        return toObservable(type)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(next, error);
    }

    /**
     * 保存订阅后的disposable
     *
     * @param o Target object subscriber.
     * @param disposable Disposable to add.
     */
    public void addSubscription(Object o, Disposable disposable) {
        if (mSubscriptionMap == null) {
            mSubscriptionMap = new HashMap<>();
        }
        CompositeDisposable compositeDisposable = mSubscriptionMap.get(o);
        if (compositeDisposable != null) {
            compositeDisposable.add(disposable);
        } else {
            compositeDisposable = new CompositeDisposable();
            compositeDisposable.add(disposable);
            mSubscriptionMap.put(o, compositeDisposable);
        }
    }

    /**
     * 取消订阅
     *
     * @param o Target object subscriber.
     */
    public void unSubscribe(Object o) {
        if (mSubscriptionMap == null) {
            return;
        }

        if (!mSubscriptionMap.containsKey(o)) {
            return;
        }
        CompositeDisposable compositeDisposable = mSubscriptionMap.get(o);
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }

        mSubscriptionMap.remove(o);
    }
}
