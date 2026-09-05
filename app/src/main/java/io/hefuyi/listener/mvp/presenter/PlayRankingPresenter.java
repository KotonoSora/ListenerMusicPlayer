package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.mvp.contract.PlayRankingContract;
import io.hefuyi.listener.mvp.usecase.GetSongs;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by hefuyi on 2016/12/9.
 */

public class PlayRankingPresenter implements PlayRankingContract.Presenter {

    private final GetSongs mUseCase;
    private PlayRankingContract.View mView;
    private CompositeSubscription mCompositeSubscription;

    public PlayRankingPresenter(GetSongs getSongs) {
        mUseCase = getSongs;
    }

    @Override
    public void attachView(@NonNull PlayRankingContract.View view) {
        mView = view;
        mCompositeSubscription = new CompositeSubscription();
    }

    @Override
    public void subscribe() {
        loadRanking();
    }

    @Override
    public void unsubscribe() {
        mCompositeSubscription.clear();
    }

    @Override
    public void loadRanking() {
        mCompositeSubscription.clear();
        Subscription subscription = mUseCase.execute(new GetSongs.RequestValues(Constants.NAVIGATE_PLAYLIST_TOPPLAYED))
                .getSongList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songList -> {
                    if (songList == null || songList.isEmpty()) {
                        mView.showEmptyView();
                    } else {
                        mView.showRanking(songList);
                    }
                });
        mCompositeSubscription.add(subscription);
    }
}
