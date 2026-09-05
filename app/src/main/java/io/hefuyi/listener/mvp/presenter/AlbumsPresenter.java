package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.contract.AlbumsContract;
import io.hefuyi.listener.mvp.usecase.GetAlbums;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by hefuyi on 2016/11/12.
 */

public class AlbumsPresenter implements AlbumsContract.Presenter {

    private final GetAlbums mUseCase;
    private AlbumsContract.View mView;
    private CompositeSubscription mCompositeSubscription;

    public AlbumsPresenter(GetAlbums getAlbums) {
        mUseCase = getAlbums;
    }

    @Override
    public void attachView(@NonNull AlbumsContract.View view) {
        mView = view;
        mCompositeSubscription = new CompositeSubscription();
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void unsubscribe() {
        mCompositeSubscription.clear();
    }

    @Override
    public void loadAlbums(String action) {
        mCompositeSubscription.clear();
        Subscription subscription = mUseCase.execute(new GetAlbums.RequestValues(action))
                .getSongList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(albumList -> {
                    if (albumList == null || albumList.isEmpty()) {
                        mView.showEmptyView();
                    } else {
                        mView.showAlbums(albumList);
                    }
                });
        mCompositeSubscription.add(subscription);
    }
}
