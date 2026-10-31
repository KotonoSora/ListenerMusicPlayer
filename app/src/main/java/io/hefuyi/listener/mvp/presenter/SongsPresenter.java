package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.contract.SongsContract;
import io.hefuyi.listener.mvp.usecase.GetSongs;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by hefuyi on 2016/11/12.
 */

public class SongsPresenter implements SongsContract.Presenter {

    private final GetSongs mUseCase;
    private SongsContract.View mView;
    private CompositeDisposable mCompositeDisposable;

    public SongsPresenter(GetSongs getSongs) {
        mUseCase = getSongs;
    }

    @Override
    public void attachView(@NonNull SongsContract.View view) {
        mView = view;
        mCompositeDisposable = new CompositeDisposable();
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void unsubscribe() {
        mCompositeDisposable.clear();
    }

    @Override
    public void loadSongs(String action) {
        mCompositeDisposable.clear();
        Disposable subscription = mUseCase.execute(new GetSongs.RequestValues(action))
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
        mCompositeDisposable.add(subscription);
    }
}
