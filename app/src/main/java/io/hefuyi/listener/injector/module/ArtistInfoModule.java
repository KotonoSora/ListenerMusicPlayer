package io.hefuyi.listener.injector.module;

import dagger.Module;
import dagger.Provides;
import io.hefuyi.listener.mvp.contract.ArtistDetailContract;
import io.hefuyi.listener.mvp.presenter.ArtistDetailPresenter;

/**
 * Created by hefuyi on 2016/11/13.
 */
@Module
public class ArtistInfoModule {

    @Provides
    ArtistDetailContract.Presenter getArtistDetailPresenter() {
        return new ArtistDetailPresenter();
    }
}
