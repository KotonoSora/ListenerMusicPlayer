package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.view.BaseView;

/**
 * Created by hefuyi on 2016/11/7.
 */

public interface BasePresenter<T extends BaseView> {

    void attachView(@NonNull T view);

    void subscribe();

    void unsubscribe();
}
