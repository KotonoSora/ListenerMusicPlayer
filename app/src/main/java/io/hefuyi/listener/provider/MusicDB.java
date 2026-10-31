package io.hefuyi.listener.provider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by hefuyi on 2016/11/5.
 */

public class MusicDB extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "musicdb.db";
    private static final int VERSION = 1;

    @SuppressLint("StaticFieldLeak")
    private static volatile MusicDB sInstance;

    private final Context mContext;

    private MusicDB(final Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, VERSION);

        mContext = context.getApplicationContext();
    }

    public static MusicDB getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (MusicDB.class) {
                if (sInstance == null) {
                    sInstance = new MusicDB(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        MusicPlaybackState.getInstance(mContext).onCreate(db);
        RecentStore.getInstance(mContext).onCreate(db);
        SongPlayCount.getInstance(mContext).onCreate(db);
        SearchHistory.getInstance(mContext).onCreate(db);
        FavoriteSong.getInstance(mContext).onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        MusicPlaybackState.getInstance(mContext).onUpgrade(db, oldVersion, newVersion);
        RecentStore.getInstance(mContext).onUpgrade(db, oldVersion, newVersion);
        SongPlayCount.getInstance(mContext).onUpgrade(db, oldVersion, newVersion);
        SearchHistory.getInstance(mContext).onUpgrade(db, oldVersion, newVersion);
        FavoriteSong.getInstance(mContext).onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        MusicPlaybackState.getInstance(mContext).onDowngrade(db, oldVersion, newVersion);
        RecentStore.getInstance(mContext).onDowngrade(db, oldVersion, newVersion);
        SongPlayCount.getInstance(mContext).onDowngrade(db, oldVersion, newVersion);
        SearchHistory.getInstance(mContext).onDowngrade(db, oldVersion, newVersion);
        FavoriteSong.getInstance(mContext).onDowngrade(db, oldVersion, newVersion);
    }
}
