package io.hefuyi.listener.util;

import android.os.Environment;

import java.io.File;

import rx.Observable;

/**
 * Created by hefuyi on 2016/11/8.
 */

public class LyricUtil {

    private static final String lrcRootPath = Environment
            .getExternalStorageDirectory().toString()
            + "/Listener/lyric/";


    @SuppressWarnings("unused")
    public static boolean isLrcFileExist(String title, String artist) {
        File file = new File(getLrcPath(title, artist));
        return file.exists();
    }

    public static Observable<File> getLocalLyricFile(String title, String artist) {
        File file = new File(getLrcPath(title, artist));
        if (file.exists()) {
            return Observable.just(file);
        } else {
            return Observable.error(new Throwable("lyric file not exist"));
        }
    }

    private static String getLrcPath(String title, String artist) {
        return lrcRootPath + title + " - " + artist + ".lrc";
    }
}
