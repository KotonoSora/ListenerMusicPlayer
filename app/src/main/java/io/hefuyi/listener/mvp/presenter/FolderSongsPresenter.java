package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.contract.FolderSongsContract;
import io.hefuyi.listener.mvp.usecase.GetFolderSongs;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by hefuyi on 2016/12/12.
 */

public class FolderSongsPresenter implements FolderSongsContract.Presenter {

    private final GetFolderSongs mUseCase;
    private FolderSongsContract.View mView;
    private CompositeSubscription mCompositeSubscription;

    public FolderSongsPresenter(GetFolderSongs getFolderSongs) {
        mUseCase = getFolderSongs;
    }

    @Override
    public void attachView(@NonNull FolderSongsContract.View view) {
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
    public void loadSongs(String path) {
        mCompositeSubscription.clear();
        Subscription subscription = mUseCase.execute(new GetFolderSongs.RequestValues(path))
                .getSongList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mView::showSongs);
        mCompositeSubscription.add(subscription);
    }

}
