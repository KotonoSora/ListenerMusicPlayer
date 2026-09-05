package io.hefuyi.listener.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by hefuyi on 2016/11/5.
 */

public class RecentStore {

    private static final int MAX_ITEMS_IN_DB = 100;

    private static volatile RecentStore sInstance;

    private final MusicDB mMusicDatabase;

    private RecentStore(final Context context) {
        mMusicDatabase = MusicDB.getInstance(context);
    }

    public static RecentStore getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (RecentStore.class) {
                if (sInstance == null) {
                    sInstance = new RecentStore(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    public void onCreate(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + RecentStoreColumns.NAME + " ("
                + RecentStoreColumns.ID + " LONG NOT NULL," + RecentStoreColumns.TIME_PLAYED
                + " LONG NOT NULL);");
    }

    @SuppressWarnings("unused")
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
    }

    @SuppressWarnings("unused")
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RecentStoreColumns.NAME);
        onCreate(db);
    }

    /**
     * 将新播放曲目id插入表中,并保持总记录数在100条
     *
     * @param songId 新播放曲目的ID
     */
    public void addSongId(final long songId) {
        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();
        database.beginTransaction();

        try {
            try (Cursor mostRecentItem = queryRecentIds("1")) {
                if (mostRecentItem.moveToFirst()) {
                    if (songId == mostRecentItem.getLong(0)) {
                        database.setTransactionSuccessful();
                        return;
                    }
                }
            }

            final ContentValues values = new ContentValues(2);
            values.put(RecentStoreColumns.ID, songId);
            values.put(RecentStoreColumns.TIME_PLAYED, System.currentTimeMillis());
            database.insert(RecentStoreColumns.NAME, null, values);

            try (Cursor oldest = database.query(RecentStoreColumns.NAME,
                    new String[]{RecentStoreColumns.TIME_PLAYED}, null, null, null, null,
                    RecentStoreColumns.TIME_PLAYED + " ASC")) {

                if (oldest.getCount() > MAX_ITEMS_IN_DB) {
                    oldest.moveToPosition(oldest.getCount() - MAX_ITEMS_IN_DB);
                    long timeOfRecordToKeep = oldest.getLong(0);

                    database.delete(RecentStoreColumns.NAME,
                            RecentStoreColumns.TIME_PLAYED + " < ?",
                            new String[]{String.valueOf(timeOfRecordToKeep)});

                }
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    /**
     * 删除某曲目的播放记录
     *
     * @param ids The array of song IDs to remove from recent history.
     */
    public void removeItem(final long[] ids) {
        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();

        StringBuilder selection = new StringBuilder();
        selection.append(RecentStoreColumns.ID);
        selection.append(" IN (");

        for (int i = 0; i < ids.length - 1; i++) {
            selection.append(ids[i]);
            selection.append(",");
        }
        if (ids.length != 0) {
            selection.append(ids[ids.length - 1]);
        }
        selection.append(")");
        database.delete(RecentStoreColumns.NAME, selection.toString(), null);
    }

    /**
     * 获取最近播放的n首歌曲的id
     *
     * @param limit The maximum number of recent song IDs to return.
     * @return A cursor containing the queried recent song IDs.
     */
    public Cursor queryRecentIds(final String limit) {
        final SQLiteDatabase database = mMusicDatabase.getReadableDatabase();
        return database.query(RecentStoreColumns.NAME,
                new String[]{RecentStoreColumns.ID}, null, null, null, null,
                RecentStoreColumns.TIME_PLAYED + " DESC", limit);
    }

    interface RecentStoreColumns {
        /* Table name */
        String NAME = "recenthistory";

        /* Album IDs column */
        String ID = "songid";

        /* Time played column */
        String TIME_PLAYED = "timeplayed";
    }
}
