package io.hefuyi.listener;

import android.app.Application;
import android.media.MediaScannerConnection;
import android.net.Uri;

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
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by hefuyi on 2016/10/4.
 */

public class ListenerApp extends Application {

    private ApplicationComponent mApplicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();

//        initLeakCanary();
//        setCrashHandler();
//        initStetho();
//        setStrictMode();
        setupInjector();
        PermissionManager.init(this);
        updataMedia();
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
    private void updataMedia() {
        if (!PermissionManager.checkPermission(ListenerUtil.getStoragePermission())) {
            return;
        }
        SongLoader.getAllSongs(this)
                .map(new Func1<List<Song>, String[]>() {
                    @Override
                    public String[] call(List<Song> songList) {
                        List<String> folderPath = new ArrayList<String>();
                        int i = 0;
                        for (Song song : songList) {
                            folderPath.add(i, song.path);
                            i++;
                        }
                        return folderPath.toArray(new String[0]);
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<String[]>() {
                    @Override
                    public void call(String[] paths) {
                        MediaScannerConnection.scanFile(getApplicationContext(), paths, null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    @Override
                                    public void onScanCompleted(String path, Uri uri) {
                                        if (uri == null) {
                                            RxBus.getInstance().post(new MediaUpdateEvent());
                                        }
                                    }
                                });
                    }
                });

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
