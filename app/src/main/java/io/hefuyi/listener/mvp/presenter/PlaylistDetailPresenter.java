package io.hefuyi.listener.mvp.presenter;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.target.CustomTarget;

import androidx.annotation.NonNull;

import io.hefuyi.listener.mvp.contract.PlaylistDetailContract;
import io.hefuyi.listener.mvp.usecase.GetPlaylistSongs;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by hefuyi on 2016/12/6.
 */

public class PlaylistDetailPresenter implements PlaylistDetailContract.Presenter {

    private final GetPlaylistSongs mUseCase;
    private PlaylistDetailContract.View mView;
    private CompositeDisposable mCompositeDisposable;

    public PlaylistDetailPresenter(GetPlaylistSongs getPlaylistSongs) {
        mUseCase = getPlaylistSongs;
    }

    @Override
    public void loadPlaylistSongs(long playlistID) {
        mCompositeDisposable.clear();
        Disposable subscription = mUseCase.execute(new GetPlaylistSongs.RequestValues(playlistID))
                .getSongList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mView::showPlaylistSongs);
        mCompositeDisposable.add(subscription);
    }

    @Override
    public void loadPlaylistArt(long firstAlbumID) {
        Glide.with(mView.getContext())
                .asBitmap()
                .load(ListenerUtil.getAlbumArtUri(firstAlbumID))
                .priority(Priority.IMMEDIATE)
                .error(ATEUtil.getDefaultAlbumDrawable(mView.getContext()))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        mView.showPlaylistArt(errorDrawable);
                    }

                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        mView.showPlaylistArt(resource);
                    }
                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                    }
                });
    }

    @Override
    public void attachView(@NonNull PlaylistDetailContract.View view) {
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
}
