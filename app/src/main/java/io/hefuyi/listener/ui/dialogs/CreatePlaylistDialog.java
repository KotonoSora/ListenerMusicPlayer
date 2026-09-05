package io.hefuyi.listener.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;

import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.event.PlaylistUpdateEvent;
import io.hefuyi.listener.mvp.model.Song;

/**
 * Created by naman on 20/12/15.
 */
@SuppressWarnings("SpellCheckingInspection")
public class CreatePlaylistDialog extends DialogFragment {

    public static CreatePlaylistDialog newInstance() {
        return newInstance((Song) null);
    }

    public static CreatePlaylistDialog newInstance(Song song) {
        long[] songs;
        if (song == null) {
            songs = new long[0];
        } else {
            songs = new long[1];
            songs[0] = song.id;
        }
        return newInstance(songs);
    }

    public static CreatePlaylistDialog newInstance(long[] songList) {
        CreatePlaylistDialog dialog = new CreatePlaylistDialog();
        Bundle bundle = new Bundle();
        bundle.putLongArray("songs", songList);
        dialog.setArguments(bundle);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new MaterialDialog.Builder(requireActivity())
                .title(R.string.create_new_playlist)
                .positiveText(R.string.create)
                .negativeText(R.string.cancel)
                .input(getString(R.string.playlist_name), "", false, (dialog, input) -> {
                    Bundle args = getArguments();
                    long[] songs = args != null ? args.getLongArray("songs") : null;
                    long playlistId = MusicPlayer.createPlaylist(requireActivity(), input.toString());

                    if (playlistId != -1) {
                        if (songs != null && songs.length != 0) {
                            MusicPlayer.addToPlaylist(requireActivity(), songs, playlistId);
                        } else {
                            Toast.makeText(requireActivity(), R.string.create_playlist_success, Toast.LENGTH_SHORT).show();
                        }
                        RxBus.getInstance().post(new PlaylistUpdateEvent());
                    } else {
                        Toast.makeText(requireActivity(), R.string.create_playlist_fail, Toast.LENGTH_SHORT).show();
                    }
                }).build();
    }
}
