package io.hefuyi.listener;

import android.util.Log;

import java.util.HashMap;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by hefuyi on 2017/1/2.
 */

public class RxBus {
    private static volatile RxBus mInstance;
    private final SerializedSubject<Object, Object> mSubject;
    private HashMap<Object, CompositeSubscription> mSubscriptionMap;

    private RxBus() {
        mSubject = new SerializedSubject<>(PublishSubject.create());
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
     * @return Subscription instance.
     */
    @SuppressWarnings("unused")
    public <T> Subscription doSubscribe(Class<T> type, Action1<T> next, Action1<Throwable> error) {
        return toObservable(type)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(next, error);
    }

    /**
     * 保存订阅后的subscription
     *
     * @param o Target object subscriber.
     * @param subscription Subscription to add.
     */
    public void addSubscription(Object o, Subscription subscription) {
        if (mSubscriptionMap == null) {
            mSubscriptionMap = new HashMap<>();
        }
        CompositeSubscription compositeSubscription = mSubscriptionMap.get(o);
        if (compositeSubscription != null) {
            compositeSubscription.add(subscription);
        } else {
            compositeSubscription = new CompositeSubscription();
            compositeSubscription.add(subscription);
            mSubscriptionMap.put(o, compositeSubscription);
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
        CompositeSubscription compositeSubscription = mSubscriptionMap.get(o);
        if (compositeSubscription != null) {
            compositeSubscription.unsubscribe();
        }

        mSubscriptionMap.remove(o);
    }
}
