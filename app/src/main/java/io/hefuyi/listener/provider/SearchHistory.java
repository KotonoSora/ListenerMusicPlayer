package io.hefuyi.listener.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by hefuyi on 2016/11/5.
 */

public class SearchHistory {

    private static final int MAX_ITEMS_IN_DB = 25;

    private static volatile SearchHistory sInstance;

    private final MusicDB mMusicDatabase;

    private SearchHistory(final Context context) {
        mMusicDatabase = MusicDB.getInstance(context);
    }

    public static SearchHistory getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (SearchHistory.class) {
                if (sInstance == null) {
                    sInstance = new SearchHistory(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    public void onCreate(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + SearchHistoryColumns.NAME + " ("
                + SearchHistoryColumns.SEARCH_STRING + " TEXT NOT NULL,"
                + SearchHistoryColumns.TIME_SEARCHED + " LONG NOT NULL);");
    }

    @SuppressWarnings("unused")
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
    }

    @SuppressWarnings("unused")
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + SearchHistoryColumns.NAME);
        onCreate(db);
    }

    /**
     * 添加搜索记录,并删除溢出记录
     *
     * @param searchString The search query string to add to history.
     */
    public void addSearchString(final String searchString) {
        if (searchString == null) {
            return;
        }

        String trimmedString = searchString.trim();

        if (trimmedString.isEmpty()) {
            return;
        }

        final SQLiteDatabase database = mMusicDatabase.getWritableDatabase();
        database.beginTransaction();

        try {

            database.delete(SearchHistoryColumns.NAME,
                    SearchHistoryColumns.SEARCH_STRING + " = ? COLLATE NOCASE",
                    new String[]{trimmedString});

            final ContentValues values = new ContentValues(2);
            values.put(SearchHistoryColumns.SEARCH_STRING, trimmedString);
            values.put(SearchHistoryColumns.TIME_SEARCHED, System.currentTimeMillis());
            database.insert(SearchHistoryColumns.NAME, null, values);

            try (Cursor oldest = database.query(SearchHistoryColumns.NAME,
                    new String[]{SearchHistoryColumns.TIME_SEARCHED}, null, null, null, null,
                    SearchHistoryColumns.TIME_SEARCHED + " ASC")) {

                if (oldest.getCount() > MAX_ITEMS_IN_DB) {
                    oldest.moveToPosition(oldest.getCount() - MAX_ITEMS_IN_DB);
                    long timeOfRecordToKeep = oldest.getLong(0);

                    database.delete(SearchHistoryColumns.NAME,
                            SearchHistoryColumns.TIME_SEARCHED + " < ?",
                            new String[]{String.valueOf(timeOfRecordToKeep)});

                }
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    /**
     * 获取最近搜索的n条记录
     *
     * @param limit The maximum number of search history entries to return.
     * @return A cursor containing the recent search queries.
     */
    @SuppressWarnings("unused")
    public Cursor queryRecentSearches(final String limit) {
        final SQLiteDatabase database = mMusicDatabase.getReadableDatabase();
        return database.query(SearchHistoryColumns.NAME,
                new String[]{SearchHistoryColumns.SEARCH_STRING}, null, null, null, null,
                SearchHistoryColumns.TIME_SEARCHED + " DESC", limit);
    }

    interface SearchHistoryColumns {
        /* Table name */
        String NAME = "searchhistory";

        /* What was searched */
        String SEARCH_STRING = "searchstring";

        /* Time of search */
        String TIME_SEARCHED = "timesearched";
    }

}
