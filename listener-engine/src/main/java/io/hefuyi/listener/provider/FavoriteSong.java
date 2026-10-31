package io.hefuyi.listener.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by hefuyi on 2016/12/31.
 */

public class FavoriteSong {

    private static volatile FavoriteSong sInstance;

    private final MusicDB mMusicDatabase;

    private FavoriteSong(final Context context) {
        mMusicDatabase = MusicDB.getInstance(context);
    }

    public static FavoriteSong getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (FavoriteSong.class) {
                if (sInstance == null) {
                    sInstance = new FavoriteSong(context);
                }
            }
        }
        return sInstance;
    }

    public void onCreate(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + FavoriteSongColumns.NAME + " ("
                + FavoriteSongColumns.SONG_ID + " LONG NOT NULL,"
                + FavoriteSongColumns.TIME_ADDED + " LONG NOT NULL);");
    }

    @SuppressWarnings("unused")
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
    }

    @SuppressWarnings("unused")
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FavoriteSongColumns.NAME);
        onCreate(db);
    }

    public int addFavoriteSong(final long[] songId) {
        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();
        database.beginTransaction();

        int insert = 0;
        try {
            for (long aSongId : songId) {
                try (Cursor cursor = database.query(FavoriteSongColumns.NAME, new String[]{FavoriteSongColumns.SONG_ID},
                        FavoriteSongColumns.SONG_ID + " =? ", new String[]{String.valueOf(aSongId)}, null, null, null)) {
                    if (cursor.getCount() == 0) { //若无重复则插入
                        ContentValues values = new ContentValues(2);
                        values.put(FavoriteSongColumns.SONG_ID, aSongId);
                        values.put(FavoriteSongColumns.TIME_ADDED, System.currentTimeMillis());
                        database.insert(FavoriteSongColumns.NAME, null, values);
                        insert++;
                    }
                }
            }
            database.setTransactionSuccessful();
            return insert;
        } finally {
            database.endTransaction();
        }
    }

    public int removeFavoriteSong(final long[] songId) {
        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();
        database.beginTransaction();

        int deleted = 0;
        try {
            for (long aSongId : songId) {
                try (Cursor cursor = database.query(FavoriteSongColumns.NAME, new String[]{FavoriteSongColumns.SONG_ID},
                        FavoriteSongColumns.SONG_ID + " =? ", new String[]{String.valueOf(aSongId)}, null, null, null)) {
                    if (cursor.getCount() > 0) {
                        database.delete(FavoriteSongColumns.NAME, FavoriteSongColumns.SONG_ID + " =? ",
                                new String[]{String.valueOf(aSongId)});
                        deleted++;
                    }
                }
            }
            database.setTransactionSuccessful();
            return deleted;
        } finally {
            database.endTransaction();
        }
    }

    public Cursor getFavoriteSong() {
        final SQLiteDatabase database = mMusicDatabase.getReadableDatabase();
        return database.query(FavoriteSongColumns.NAME,
                new String[]{FavoriteSongColumns.SONG_ID}, null, null, null, null,
                FavoriteSongColumns.TIME_ADDED + " DESC", null);
    }


    public boolean isFavorite(long songId) {
        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();
        database.beginTransaction();

        try {
            try (Cursor cursor = database.query(FavoriteSongColumns.NAME, new String[]{FavoriteSongColumns.SONG_ID},
                    FavoriteSongColumns.SONG_ID + " =? ", new String[]{String.valueOf(songId)}, null, null, null)) {
                if (cursor.getCount() > 0) {
                    database.setTransactionSuccessful();
                    return true;
                }
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return false;
    }

    public interface FavoriteSongColumns {
        /* Table name */
        String NAME = "favoritesong";

        /* What was searched */
        String SONG_ID = "songid";

        /* Time of search */
        String TIME_ADDED = "timeadded";
    }

}
