package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.contract.PlaylistContract;
import io.hefuyi.listener.mvp.usecase.GetPlaylists;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by hefuyi on 2016/12/4.
 */

public class PlaylistPresenter implements PlaylistContract.Presenter {

    private final GetPlaylists mUseCase;
    private PlaylistContract.View mView;
    private CompositeDisposable mCompositeDisposable;

    public PlaylistPresenter(GetPlaylists getPlaylists) {
        mUseCase = getPlaylists;
    }

    @Override
    public void attachView(@NonNull PlaylistContract.View view) {
        mView = view;
        mCompositeDisposable = new CompositeDisposable();
    }

    @Override
    public void subscribe() {
        loadPlaylist();
    }

    @Override
    public void unsubscribe() {
        mCompositeDisposable.clear();
    }

    @Override
    public void loadPlaylist() {
        mCompositeDisposable.clear();
        Disposable subscription = mUseCase.execute(new GetPlaylists.RequestValues(false))
                .getPlaylists()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(playlists -> {
                    if (playlists == null || playlists.isEmpty()) {
                        mView.showEmptyView();
                    } else {
                        mView.showPlaylist(playlists);
                    }
                });
        mCompositeDisposable.add(subscription);
    }
}
