package io.hefuyi.listener.mvp.presenter;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.target.CustomTarget;

import java.io.File;

import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.mvp.contract.QuickControlsContract;
import io.hefuyi.listener.respository.interfaces.Repository;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.LyricUtil;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by hefuyi on 2016/11/8.
 */

public class QuickControlsPresenter implements QuickControlsContract.Presenter {

    private final Repository mRepository;
    private CompositeDisposable mCompositeDisposable;
    private QuickControlsContract.View mView;

    private boolean mDuetoplaypause = false;

    public QuickControlsPresenter(Repository repository) {
        this.mRepository = repository;
    }

    @Override
    public void attachView(@NonNull QuickControlsContract.View view) {
        this.mView = view;
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
    public void onPlayPauseClick() {
        mDuetoplaypause = true;
        Schedulers.io().createWorker().schedule(MusicPlayer::playOrPause);
    }

    @Override
    public void onPreviousClick() {
        if (mView.getContext() != null) {
            MusicPlayer.previous(mView.getContext(), true);
        }
    }

    @Override
    public void loadLyric() {
        if (mView.getContext() == null) {
            return;
        }
        mCompositeDisposable.clear();
        String title = MusicPlayer.getTrackName();
        String artist = MusicPlayer.getArtistName();
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(artist)) {
            return;
        }

        Disposable subscription = LyricUtil.getLocalLyricFile(title, artist)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<>() {
                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        mView.showLyric(null);
                    }

                    @Override
                    public void onNext(File file) {
                        mView.showLyric(file);
                    }
                });
        mCompositeDisposable.add(subscription);
    }

    @Override
    public void onNextClick() {
        MusicPlayer.next();
    }

    @Override
    public void updateNowPlayingCard() {
        if (mView.getContext() == null) {
            return;
        }
        if (MusicPlayer.isPlaying()) {
            if (!mView.getPlayPauseStatus()) {//true表示按钮为待暂停状态
                mView.setPlayPauseButton(true);
            }
        } else {
            if (mView.getPlayPauseStatus()) {
                mView.setPlayPauseButton(false);
            }
        }

        final String title = MusicPlayer.getTrackName();
        final String artist = MusicPlayer.getArtistName();
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(artist)) {
            mView.setTitle(mView.getContext().getResources().getString(R.string.app_name));
            mView.setArtist("");
        } else {
            mView.setTitle(title);
            mView.setArtist(artist);
        }

        if (!mDuetoplaypause) {
            Glide.with(mView.getContext())
                    .asBitmap()
                    .load(ListenerUtil.getAlbumArtUri(MusicPlayer.getCurrentAlbumId()))
                    .error(ATEUtil.getDefaultAlbumDrawable(mView.getContext()))
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onLoadFailed(Drawable errorDrawable) {
                            mView.setAlbumArt(errorDrawable);
                            if (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(artist)) {
                                if (errorDrawable instanceof BitmapDrawable) {
                                    new Palette.Builder(((BitmapDrawable) errorDrawable).getBitmap()).generate(mView::setPalette);
                                } else {
                                    mView.setPalette(null);
                                }
                            }
                        }

                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            mView.setAlbumArt(resource);
                            new Palette.Builder(resource).generate(mView::setPalette);
                        }
                        @Override
                        public void onLoadCleared(Drawable placeholder) {
                        }
                    });
        }

        mDuetoplaypause = false;
        mView.setProgressMax((int) MusicPlayer.duration());
        mView.startUpdateProgress();
    }
}
