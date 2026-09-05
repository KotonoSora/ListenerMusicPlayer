package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.contract.FoldersContract;
import io.hefuyi.listener.mvp.usecase.GetFolders;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by hefuyi on 2016/12/11.
 */

public class FolderPresenter implements FoldersContract.Presenter {

    private final GetFolders mUseCase;
    private FoldersContract.View mView;
    private CompositeSubscription mCompositeSubscription;

    public FolderPresenter(GetFolders getFolders) {
        this.mUseCase = getFolders;
    }

    @Override
    public void attachView(@NonNull FoldersContract.View view) {
        this.mView = view;
        mCompositeSubscription = new CompositeSubscription();
    }

    @Override
    public void subscribe() {
        loadFolders();
    }

    @Override
    public void unsubscribe() {
        mCompositeSubscription.clear();
    }

    @Override
    public void loadFolders() {
        mCompositeSubscription.clear();
        Subscription subscription = mUseCase.execute(new GetFolders.RequestValues())
                .getFolderList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(folderInfos -> {
                    if (folderInfos == null || folderInfos.isEmpty()) {
                        mView.showEmptyView();
                    } else {
                        mView.showFolders(folderInfos);
                    }
                });
        mCompositeSubscription.add(subscription);
    }
}
