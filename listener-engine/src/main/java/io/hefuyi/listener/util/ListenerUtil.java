package io.hefuyi.listener.util;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.dataloader.PlaylistLoader;
import io.hefuyi.listener.event.FavoriteSongEvent;
import io.hefuyi.listener.event.MediaUpdateEvent;
import io.hefuyi.listener.event.PlaylistUpdateEvent;
import io.hefuyi.listener.event.RecentlyPlayEvent;
import io.hefuyi.listener.provider.FavoriteSong;
import io.hefuyi.listener.provider.RecentStore;
import io.hefuyi.listener.provider.SongPlayCount;
import io.hefuyi.listener.ui.dialogs.CreatePlaylistDialog;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * Created by hefuyi on 2016/11/4.
 */

public class ListenerUtil {

    public static final String MUSIC_ONLY_SELECTION = MediaStore.Audio.AudioColumns.IS_MUSIC + "=1"
            + " AND " + MediaStore.Audio.AudioColumns.TITLE + " != ''";


    public static boolean isTiramisu() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    public static String getStoragePermission() {
        if (isTiramisu()) {
            return Manifest.permission.READ_MEDIA_AUDIO;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    /**
     * Legacy bundled permissions. Do not use for blocking requests:
     * POST_NOTIFICATIONS is optional and must be requested separately.
     */
    public static String[] getRequiredPermissions() {
        return getStoragePermissions();
    }

    public static String getNotificationPermission() {
        return Manifest.permission.POST_NOTIFICATIONS;
    }

    public static boolean isNotificationPermissionRequired() {
        return isTiramisu();
    }

    public static String[] getStoragePermissions() {
        return new String[]{getStoragePermission()};
    }


    public static Uri getAlbumArtUri(long paramInt) {
        if (paramInt < 0) {
            return null;
        }
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), paramInt);
    }

    @SuppressWarnings("unused")
    public static String makeLabel(final Context context, final int pluralInt,
                                   final int number) {
        return context.getResources().getQuantityString(pluralInt, number, number);
    }

    @SuppressWarnings("unused")
    public static String makeShortTimeString(final Context context, long secs) {
        long hours = secs / 3600;
        long mins = (secs % 3600) / 60;
        long remainingSecs = secs % 60;

        final String durationFormat = context.getResources().getString(
                hours == 0 ? R.string.duration_format_short : R.string.duration_format_long);
        return String.format(durationFormat, hours, mins, remainingSecs);
    }

    public static boolean hasEffectsPanel(final Activity activity) {
        final PackageManager packageManager = activity.getPackageManager();
        return packageManager.resolveActivity(createEffectsIntent(),
                PackageManager.MATCH_DEFAULT_ONLY) != null;
    }

    public static Intent createEffectsIntent() {
        final Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicPlayer.getAudioSessionId());
        return effects;
    }

    public static void showDeleteDialog(final Context context, final String name, final long[] list,
                                        final DialogInterface.OnClickListener deleteCallback) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.delete_song)
                .setMessage(context.getString(R.string.delete_song_confirmation, name))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    ListenerUtil.deleteTracks(context, list);
                    deleteCallback.onClick(dialog, which);
                    RxBus.getInstance().post(new MediaUpdateEvent());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public static void showAddPlaylistDialog(final Context context, final long[] songIds) {
        PlaylistLoader.getPlaylists(context, true)
                .map(playlists -> {
                    final CharSequence[] chars = new CharSequence[playlists.size() + 1];
                    chars[0] = context.getResources().getString(R.string.create_new_playlist);
                    for (int i = 0; i < playlists.size(); i++) {
                        chars[i + 1] = playlists.get(i).name;
                    }
                    return new MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.add_to_playlist)
                            .setItems(chars, (dialog, which) -> {
                                if (which == 0) {
                                    CreatePlaylistDialog.newInstance(songIds)
                                            .show(((AppCompatActivity) context)
                                                    .getSupportFragmentManager(), context.getString(R.string.create_new_playlist));
                                    return;
                                } else if (which == 1) {
                                    //我喜欢
                                    FavoriteSong.getInstance(context).addFavoriteSong(songIds);
                                    Toast.makeText(context, R.string.add_favorite_success, Toast.LENGTH_SHORT).show();
                                    RxBus.getInstance().post(new FavoriteSongEvent());
                                    dialog.dismiss();
                                    return;
                                }

                                MusicPlayer.addToPlaylist(context, songIds, playlists.get(which - 1).id);
                                RxBus.getInstance().post(new PlaylistUpdateEvent());
                                dialog.dismiss();

                            }).create();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Dialog::show);
    }

    public static void showDeleteFromFavorite(final Context context, final long[] ids) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.delete_song_favorite)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    FavoriteSong.getInstance(context).removeFavoriteSong(ids);
                    RxBus.getInstance().post(new FavoriteSongEvent());
                    Toast.makeText(context, R.string.remove_favorite_success, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public static void showDeleteFromRecentlyPlay(final Context context, final long[] ids) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.delete_song_recently_play)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    RecentStore.getInstance(context).removeItem(ids);
                    RxBus.getInstance().post(new RecentlyPlayEvent());
                    Toast.makeText(context, R.string.remove_recently_play_success, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public static void deleteTracks(final Context context, final long[] list) {
        final String[] projection = new String[]{
                BaseColumns._ID, MediaStore.MediaColumns.DATA, MediaStore.Audio.AudioColumns.ALBUM_ID
        };
        final StringBuilder selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < list.length; i++) {
            selection.append(list[i]);
            if (i < list.length - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
        final Cursor c;
        try {
            c = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection.toString(),
                    null, null);
        } catch (final SecurityException | IllegalArgumentException ex) {
            Log.e("MusicUtils", "Query failed, cannot delete tracks", ex);
            return;
        }
        if (c != null) {
            // Step 1: Remove selected tracks from the current playlist, as well
            // as from the album art cache
            c.moveToFirst();
            while (!c.isAfterLast()) {
                // Remove from current playlist
                final long id = c.getLong(0);
                MusicPlayer.removeTrack(id);
                // Remove the track from the play count
                SongPlayCount.getInstance(context).removeItem(id);
                // Remove any items in the recents database
                RecentStore.getInstance(context).removeItem(new long[]{id});
                c.moveToNext();
            }

            // Step 2: Remove selected tracks from the database.
            // On Android 10+ this can throw SecurityException
            // (including RecoverableSecurityException) when the app
            // is not the owner. Never crash the app here.
            try {
                context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        selection.toString(), null);
            } catch (final SecurityException | IllegalArgumentException ex) {
                Log.e("MusicUtils", "MediaStore delete blocked on scoped storage", ex);
            }

            // Step 3: Remove files from card
            c.moveToFirst();
            while (!c.isAfterLast()) {
                final String name = c.getString(1);
                final File f = new File(name);
                try { // File.delete can throw a security exception
                    if (!f.delete()) {
                        // I'm not sure if we'd ever get here (deletion would
                        // have to fail, but no exception thrown)
                        Log.e("MusicUtils", "Failed to delete file " + name);
                    }
                    c.moveToNext();
                } catch (final SecurityException ex) {
                    c.moveToNext();
                }
            }
            c.close();
        }

        final String message = makeLabel(context, R.plurals.n_tracks_deleted, list.length);

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        context.getContentResolver().notifyChange(Uri.parse("content://media"), null);
        MusicPlayer.refresh();
    }

    public static boolean isRtl(Resources res) {
        return res.getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    public static void applySystemBarPadding(View view, boolean top, boolean bottom) {
        final int initialPaddingTop = view.getPaddingTop();
        final int initialPaddingBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int topInset = top ? insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()).top : initialPaddingTop;
            int bottomInset = bottom ? insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom : initialPaddingBottom;
            v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), bottomInset);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    public static void applyBottomInsetWithPlayer(View view) {
        final int initialPaddingBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int playerHeight = v.getResources().getDimensionPixelSize(R.dimen.sliding_up_header);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBottom + playerHeight + initialPaddingBottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    public static void applyBottomInsetWithPlayerAndIme(View view) {
        final int initialPaddingBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int playerHeight = v.getResources().getDimensionPixelSize(R.dimen.sliding_up_header);

            int totalBottom = Math.max(systemBottom + playerHeight, imeBottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), totalBottom + initialPaddingBottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    public static void applySystemBarPaddingAndHeight(View view, boolean top, boolean bottom) {
        final int initialPaddingTop = view.getPaddingTop();
        final int initialPaddingBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()).top;
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            int newPaddingTop = top ? topInset + initialPaddingTop : initialPaddingTop;
            int newPaddingBottom = bottom ? bottomInset + initialPaddingBottom : initialPaddingBottom;

            v.setPadding(v.getPaddingLeft(), newPaddingTop, v.getPaddingRight(), newPaddingBottom);

            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp != null && lp.height > 0) {
                Object tag = v.getTag(R.id.original_height);
                int baseHeight;
                if (tag instanceof Integer) {
                    baseHeight = (Integer) tag;
                } else {
                    baseHeight = lp.height;
                    v.setTag(R.id.original_height, baseHeight);
                }

                int extra = (top ? topInset : 0) + (bottom ? bottomInset : 0);
                lp.height = baseHeight + extra;
                v.setLayoutParams(lp);

                if (top) {
                    v.setMinimumHeight(baseHeight + topInset);
                }
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    public enum IdType {
        NA(0),
        Artist(1),
        Album(2),
        Playlist(3),
        Folder(4);

        public final int mId;

        IdType(final int id) {
            mId = id;
        }

        @SuppressWarnings("unused")
        public static IdType getTypeById(int id) {
            for (IdType type : values()) {
                if (type.mId == id) {
                    return type;
                }
            }

            throw new IllegalArgumentException("Unrecognized id: " + id);
        }
    }

    public enum PlaylistType {
        LastAdded(-1, R.string.playlist_last_added),
        RecentlyPlayed(-2, R.string.playlist_recently_played),
        Favorite(-3, R.string.playlist_top_tracks);

        @SuppressWarnings("unused")
        public final long mId;
        @SuppressWarnings("unused")
        public final int mTitleId;

        PlaylistType(long id, int titleId) {
            mId = id;
            mTitleId = titleId;
        }
    }
}