package io.hefuyi.listener.mvp.presenter;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.request.target.CustomTarget;
import com.google.gson.Gson;

import io.hefuyi.listener.mvp.contract.ArtistDetailContract;
import io.hefuyi.listener.mvp.model.ArtistArt;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.PreferencesUtility;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by hefuyi on 2016/11/24.
 */

public class ArtistDetailPresenter implements ArtistDetailContract.Presenter {

    private ArtistDetailContract.View mView;
    private CompositeDisposable mCompositeDisposable;

    public ArtistDetailPresenter() {

    }

    @Override
    public void attachView(@NonNull ArtistDetailContract.View view) {
        mView = view;
        mCompositeDisposable = new CompositeDisposable();
    }

    @Override
    public void subscribe() {
        throw new RuntimeException("please call subscribe(long artistID)");
    }

    @Override
    public void subscribe(long artistID) {
        loadArtistArt(artistID);
    }

    @Override
    public void unsubscribe() {
        mCompositeDisposable.clear();
    }

    @Override
    public void loadArtistArt(long artistID) {
        String artistArtJson = PreferencesUtility.getInstance(mView.getContext()).getArtistArt(artistID);
        if (!TextUtils.isEmpty(artistArtJson)) {
            ArtistArt artistArt = new Gson().fromJson(artistArtJson, ArtistArt.class);
            Glide.with(mView.getContext())
                    .asBitmap()
                    .load(artistArt.getExtralarge())
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .priority(Priority.IMMEDIATE)
                    .error(ATEUtil.getDefaultSingerDrawable(mView.getContext()))
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onLoadFailed(Drawable errorDrawable) {
                            mView.showArtistArt(errorDrawable);
                        }

                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            mView.showArtistArt(resource);
                        }
                        @Override
                        public void onLoadCleared(Drawable placeholder) {
                        }
                    });
        }
    }

}
