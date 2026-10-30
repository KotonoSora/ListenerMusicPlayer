package io.hefuyi.listener.mvp.presenter;


import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.contract.ArtistContract;
import io.hefuyi.listener.mvp.usecase.GetArtists;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by hefuyi on 2016/11/13.
 */

public class ArtistPresenter implements ArtistContract.Presenter {

    private final GetArtists mUseCase;
    private ArtistContract.View mView;
    private CompositeDisposable mCompositeDisposable;

    public ArtistPresenter(GetArtists getArtists) {
        mUseCase = getArtists;
    }

    @Override
    public void attachView(@NonNull ArtistContract.View view) {
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
    public void loadArtists(String action) {
        mCompositeDisposable.clear();
        Disposable subscription = mUseCase.execute(new GetArtists.RequestValues(action))
                .getArtistList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(artists -> {
                    if (artists == null || artists.isEmpty()) {
                        mView.showEmptyView();
                    } else {
                        mView.showArtists(artists);
                    }
                });
        mCompositeDisposable.add(subscription);
    }
}
