package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.mvp.contract.PlayRankingContract;
import io.hefuyi.listener.mvp.usecase.GetSongs;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by hefuyi on 2016/12/9.
 */

public class PlayRankingPresenter implements PlayRankingContract.Presenter {

    private final GetSongs mUseCase;
    private PlayRankingContract.View mView;
    private CompositeDisposable mCompositeDisposable;

    public PlayRankingPresenter(GetSongs getSongs) {
        mUseCase = getSongs;
    }

    @Override
    public void attachView(@NonNull PlayRankingContract.View view) {
        mView = view;
        mCompositeDisposable = new CompositeDisposable();
    }

    @Override
    public void subscribe() {
        loadRanking();
    }

    @Override
    public void unsubscribe() {
        mCompositeDisposable.clear();
    }

    @Override
    public void loadRanking() {
        mCompositeDisposable.clear();
        Disposable subscription = mUseCase.execute(new GetSongs.RequestValues(Constants.NAVIGATE_PLAYLIST_TOPPLAYED))
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
        mCompositeDisposable.add(subscription);
    }
}
