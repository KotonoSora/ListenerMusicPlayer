package io.hefuyi.listener.mvp.presenter;


import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.contract.ArtistContract;
import io.hefuyi.listener.mvp.usecase.GetArtists;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by hefuyi on 2016/11/13.
 */

public class ArtistPresenter implements ArtistContract.Presenter {

    private final GetArtists mUseCase;
    private ArtistContract.View mView;
    private CompositeSubscription mCompositeSubscription;

    public ArtistPresenter(GetArtists getArtists) {
        mUseCase = getArtists;
    }

    @Override
    public void attachView(@NonNull ArtistContract.View view) {
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
    public void loadArtists(String action) {
        mCompositeSubscription.clear();
        Subscription subscription = mUseCase.execute(new GetArtists.RequestValues(action))
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
        mCompositeSubscription.add(subscription);
    }
}
