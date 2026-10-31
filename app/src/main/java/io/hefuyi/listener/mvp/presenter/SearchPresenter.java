package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.contract.SearchContract;
import io.hefuyi.listener.mvp.usecase.GetSearchResult;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Created by hefuyi on 2017/1/3.
 */

public class SearchPresenter implements SearchContract.Presenter {

    private final GetSearchResult mUseCase;
    private SearchContract.View mView;
    private Disposable mSubscription;

    public SearchPresenter(GetSearchResult getSearchResult) {
        mUseCase = getSearchResult;
    }

    @Override
    public void attachView(@NonNull SearchContract.View view) {
        mView = view;
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {
        if (mSubscription != null && !mSubscription.isDisposed()) {
            mSubscription.dispose();
        }
    }

    @Override
    public void search(String queryString) {
        if (mSubscription != null && !mSubscription.isDisposed()) {
            mSubscription.dispose();
        }
        mSubscription = mUseCase.execute(new GetSearchResult.RequestValues(queryString))
                .getResultList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    if (list != null && list.size() == 3) {
                        mView.showEmptyView();
                    } else {
                        mView.showSearchResult(list);
                    }
                });
    }
}
