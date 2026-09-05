package io.hefuyi.listener;

import android.app.Application;
import android.media.MediaScannerConnection;

import com.afollestad.appthemeengine.ATE;

import java.util.ArrayList;
import java.util.List;

import io.hefuyi.listener.dataloader.SongLoader;
import io.hefuyi.listener.event.MediaUpdateEvent;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.DaggerApplicationComponent;
import io.hefuyi.listener.injector.module.ApplicationModule;
import io.hefuyi.listener.injector.module.NetworkModule;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.permission.PermissionManager;
import io.hefuyi.listener.util.ListenerUtil;
import rx.schedulers.Schedulers;

/**
 * Created by hefuyi on 2016/10/4.
 */

public class ListenerApp extends Application {

    private ApplicationComponent mApplicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        setupInjector();
        PermissionManager.init(this);
        updateMedia();
        setupATE();
    }


    private void setupInjector() {
        mApplicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .networkModule(new NetworkModule(this)).build();
    }

    public ApplicationComponent getApplicationComponent() {
        return mApplicationComponent;
    }

    //应用启动时通知系统刷新媒体库,
    private void updateMedia() {
        if (!PermissionManager.checkPermission(ListenerUtil.getStoragePermission())) {
            return;
        }
        SongLoader.getAllSongs(this)
                .map(songList -> {
                    List<String> folderPath = new ArrayList<>();
                    int i = 0;
                    for (Song song : songList) {
                        folderPath.add(i, song.path);
                        i++;
                    }
                    return folderPath.toArray(new String[0]);
                })
                .subscribeOn(Schedulers.io())
                .subscribe(paths -> MediaScannerConnection.scanFile(getApplicationContext(), paths, null,
                        (path, uri) -> {
                            if (uri == null) {
                                RxBus.getInstance().post(new MediaUpdateEvent());
                            }
                        }));

    }

    private void setupATE() {
        if (!ATE.config(this, "light_theme").isConfigured()) {
            ATE.config(this, "light_theme")
                    .activityTheme(R.style.AppThemeLight)
                    .coloredNavigationBar(false)
                    .usingMaterialDialogs(true)
                    .commit();
        }
        if (!ATE.config(this, "dark_theme").isConfigured()) {
            ATE.config(this, "dark_theme")
                    .activityTheme(R.style.AppThemeDark)
                    .coloredNavigationBar(false)
                    .usingMaterialDialogs(true)
                    .commit();
        }
    }
}
