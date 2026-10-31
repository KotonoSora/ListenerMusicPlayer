package io.hefuyi.listener.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
        final EditText input = new EditText(requireActivity());
        input.setHint(R.string.playlist_name);
        input.setSingleLine(true);
        int margin = (int) (24 * requireActivity().getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(requireActivity());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = margin;
        params.rightMargin = margin;
        input.setLayoutParams(params);
        container.addView(input);
        return new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.create_new_playlist)
                .setView(container)
                .setPositiveButton(R.string.create, (dialog, which) -> {
                    Bundle args = getArguments();
                    long[] songs = args != null ? args.getLongArray("songs") : null;
                    long playlistId = MusicPlayer.createPlaylist(requireActivity(), input.getText().toString());

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
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }
}
