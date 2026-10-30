package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.contract.FolderSongsContract;
import io.hefuyi.listener.mvp.usecase.GetFolderSongs;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by hefuyi on 2016/12/12.
 */

public class FolderSongsPresenter implements FolderSongsContract.Presenter {

    private final GetFolderSongs mUseCase;
    private FolderSongsContract.View mView;
    private CompositeDisposable mCompositeDisposable;

    public FolderSongsPresenter(GetFolderSongs getFolderSongs) {
        mUseCase = getFolderSongs;
    }

    @Override
    public void attachView(@NonNull FolderSongsContract.View view) {
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
    public void loadSongs(String path) {
        mCompositeDisposable.clear();
        Disposable subscription = mUseCase.execute(new GetFolderSongs.RequestValues(path))
                .getSongList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mView::showSongs);
        mCompositeDisposable.add(subscription);
    }

}
