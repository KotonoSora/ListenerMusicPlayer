package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.contract.SongsContract;
import io.hefuyi.listener.mvp.usecase.GetSongs;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by hefuyi on 2016/11/12.
 */

public class SongsPresenter implements SongsContract.Presenter {

    private final GetSongs mUseCase;
    private SongsContract.View mView;
    private CompositeSubscription mCompositeSubscription;

    public SongsPresenter(GetSongs getSongs) {
        mUseCase = getSongs;
    }

    @Override
    public void attachView(@NonNull SongsContract.View view) {
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
    public void loadSongs(String action) {
        mCompositeSubscription.clear();
        Subscription subscription = mUseCase.execute(new GetSongs.RequestValues(action))
                .getSongList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songList -> {
                    if (songList == null || songList.isEmpty()) {
                        mView.showEmptyView();
                    } else {
                        mView.showSongs(songList);
                    }
                });
        mCompositeSubscription.add(subscription);
    }
}
