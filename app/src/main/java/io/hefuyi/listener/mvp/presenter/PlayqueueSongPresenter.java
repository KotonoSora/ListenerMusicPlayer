package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.mvp.contract.PlayqueueSongContract;
import io.hefuyi.listener.mvp.usecase.GetSongs;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by hefuyi on 2016/12/27.
 */

public class PlayqueueSongPresenter implements PlayqueueSongContract.Presenter {

    private final GetSongs mUseCase;
    private final CompositeSubscription mCompositeSubscription;
    private PlayqueueSongContract.View mView;

    public PlayqueueSongPresenter(GetSongs getSongs) {
        mUseCase = getSongs;
        mCompositeSubscription = new CompositeSubscription();
    }

    @Override
    public void attachView(@NonNull PlayqueueSongContract.View view) {
        mView = view;
    }

    @Override
    public void subscribe() {
        loadSongs();
    }

    @Override
    public void unsubscribe() {
        mCompositeSubscription.clear();
    }

    @Override
    public void loadSongs() {
        Subscription subscription = mUseCase.execute(new GetSongs.RequestValues(Constants.NAVIGATE_QUEUE))
                .getSongList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mView::showSongs);
        mCompositeSubscription.add(subscription);
    }
}
