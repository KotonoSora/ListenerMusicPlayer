package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.mvp.contract.PlayqueueSongContract;
import io.hefuyi.listener.mvp.usecase.GetSongs;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by hefuyi on 2016/12/27.
 */

public class PlayqueueSongPresenter implements PlayqueueSongContract.Presenter {

    private final GetSongs mUseCase;
    private final CompositeDisposable mCompositeDisposable;
    private PlayqueueSongContract.View mView;

    public PlayqueueSongPresenter(GetSongs getSongs) {
        mUseCase = getSongs;
        mCompositeDisposable = new CompositeDisposable();
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
        mCompositeDisposable.clear();
    }

    @Override
    public void loadSongs() {
        Disposable subscription = mUseCase.execute(new GetSongs.RequestValues(Constants.NAVIGATE_QUEUE))
                .getSongList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mView::showSongs);
        mCompositeDisposable.add(subscription);
    }
}
