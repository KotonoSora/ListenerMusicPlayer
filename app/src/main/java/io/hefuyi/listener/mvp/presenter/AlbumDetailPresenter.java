package io.hefuyi.listener.mvp.presenter;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.target.CustomTarget;

import java.util.List;

import io.hefuyi.listener.mvp.contract.AlbumDetailContract;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.mvp.usecase.GetAlbumSongs;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by hefuyi on 2016/12/3.
 */

public class AlbumDetailPresenter implements AlbumDetailContract.Presenter {

    private final GetAlbumSongs mUseCase;
    private AlbumDetailContract.View mView;
    private CompositeDisposable mCompositeDisposable;

    public AlbumDetailPresenter(GetAlbumSongs getAlbumSongs) {
        mUseCase = getAlbumSongs;
    }

    @Override
    public void subscribe(long albumID) {
        loadAlbumArt(albumID);
        loadAlbumSongs(albumID);
    }

    @Override
    public void attachView(@NonNull AlbumDetailContract.View view) {
        mView = view;
        mCompositeDisposable = new CompositeDisposable();
    }

    @Override
    public void subscribe() {
        throw new RuntimeException("please call subscribe(long albumID)");
    }

    @Override
    public void unsubscribe() {
        mCompositeDisposable.clear();
    }

    @Override
    public void loadAlbumSongs(long albumID) {
        mCompositeDisposable.clear();
        Disposable subscription = mUseCase.execute(new GetAlbumSongs.RequestValues(albumID))
                .getSongList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<>() {
                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(List<Song> songList) {
                        mView.showAlbumSongs(songList);
                    }
                });
        mCompositeDisposable.add(subscription);
    }

    @Override
    public void loadAlbumArt(long albumID) {
        Glide.with(mView.getContext())
                .asBitmap()
                .load(ListenerUtil.getAlbumArtUri(albumID))
                .priority(Priority.IMMEDIATE)
                .error(ATEUtil.getDefaultAlbumDrawable(mView.getContext()))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        mView.showAlbumArt(errorDrawable);
                    }

                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        mView.showAlbumArt(resource);
                    }
                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                    }
                });
    }
}
