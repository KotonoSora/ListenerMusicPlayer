package io.hefuyi.listener.mvp.presenter;

import androidx.annotation.NonNull;
import java.util.List;

import io.hefuyi.listener.mvp.contract.ArtistSongContract;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.mvp.usecase.GetArtistSongs;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by hefuyi on 2016/11/25.
 */

public class ArtistSongPresenter implements ArtistSongContract.Presenter {

    private final GetArtistSongs mUseCase;
    private ArtistSongContract.View mView;
    private CompositeDisposable mCompositeDisposable;

    public ArtistSongPresenter(GetArtistSongs getArtistSongs) {
        mUseCase = getArtistSongs;
    }

    @Override
    public void attachView(@NonNull ArtistSongContract.View view) {
        mView = view;
        mCompositeDisposable = new CompositeDisposable();
    }

    @Override
    public void subscribe() {
        throw new RuntimeException("please to call subscribe(long artistID)");
    }

    @Override
    public void subscribe(long artistID) {
        loadSongs(artistID);
    }

    @Override
    public void unsubscribe() {
        mCompositeDisposable.clear();
    }

    @Override
    public void loadSongs(long artistID) {
        mCompositeDisposable.clear();
        Disposable subscription = mUseCase.execute(new GetArtistSongs.RequestValues(artistID))
                .getSongList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<List<Song>>() {
                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(List<Song> songList) {
                        mView.showSongs(songList);
                    }
                });
        mCompositeDisposable.add(subscription);
    }
}
