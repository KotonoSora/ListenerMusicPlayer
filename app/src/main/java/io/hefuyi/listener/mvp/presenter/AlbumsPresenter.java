package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.contract.AlbumsContract;
import io.hefuyi.listener.mvp.usecase.GetAlbums;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by hefuyi on 2016/11/12.
 */

public class AlbumsPresenter implements AlbumsContract.Presenter {

    private final GetAlbums mUseCase;
    private AlbumsContract.View mView;
    private CompositeDisposable mCompositeDisposable;

    public AlbumsPresenter(GetAlbums getAlbums) {
        mUseCase = getAlbums;
    }

    @Override
    public void attachView(@NonNull AlbumsContract.View view) {
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
    public void loadAlbums(String action) {
        mCompositeDisposable.clear();
        Disposable subscription = mUseCase.execute(new GetAlbums.RequestValues(action))
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
        mCompositeDisposable.add(subscription);
    }
}
