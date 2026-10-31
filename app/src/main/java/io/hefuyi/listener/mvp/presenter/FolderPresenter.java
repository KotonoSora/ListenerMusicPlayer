package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.contract.FoldersContract;
import io.hefuyi.listener.mvp.usecase.GetFolders;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by hefuyi on 2016/12/11.
 */

public class FolderPresenter implements FoldersContract.Presenter {

    private final GetFolders mUseCase;
    private FoldersContract.View mView;
    private CompositeDisposable mCompositeDisposable;

    public FolderPresenter(GetFolders getFolders) {
        this.mUseCase = getFolders;
    }

    @Override
    public void attachView(@NonNull FoldersContract.View view) {
        this.mView = view;
        mCompositeDisposable = new CompositeDisposable();
    }

    @Override
    public void subscribe() {
        loadFolders();
    }

    @Override
    public void unsubscribe() {
        mCompositeDisposable.clear();
    }

    @Override
    public void loadFolders() {
        mCompositeDisposable.clear();
        Disposable subscription = mUseCase.execute(new GetFolders.RequestValues())
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
        mCompositeDisposable.add(subscription);
    }
}
