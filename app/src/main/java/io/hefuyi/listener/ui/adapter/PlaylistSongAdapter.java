package io.hefuyi.listener.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.dataloader.PlaylistSongLoader;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ColorUtil;
import androidx.core.content.ContextCompat;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.NavigationUtil;
import io.hefuyi.listener.widget.fastscroller.FastScrollRecyclerView;

/**
 * Created by hefuyi on 2017/1/16.
 */

public class PlaylistSongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FastScrollRecyclerView.SectionedAdapter {

    private final Context mContext;
    private final long playlistId;
    private List<Song> arraylist;
    private long[] songIDs;

    public PlaylistSongAdapter(Context context, long playlistId, @Nullable List<Song> arraylist) {
        this.arraylist = Objects.requireNonNullElseGet(arraylist, ArrayList::new);
        this.mContext = context;
        this.songIDs = getSongIds();
        this.playlistId = playlistId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list_linear_layout_item, viewGroup, false);
        return new ItemHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemHolder itemHolder = (ItemHolder) holder;
        Song localItem = arraylist.get(position);

        itemHolder.title.setText(localItem.title);
        itemHolder.artist.setText(localItem.artistName);
        itemHolder.album.setText(localItem.albumName);

        Glide.with(holder.itemView.getContext()).load(ListenerUtil.getAlbumArtUri(localItem.albumId))
                .error(ATEUtil.getDefaultAlbumDrawable(mContext))
                .placeholder(ATEUtil.getDefaultAlbumDrawable(mContext))
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .centerCrop()
                .into(itemHolder.albumArt);

        if (MusicPlayer.getCurrentAudioId() == localItem.id) {
            int accent = ATEUtil.getThemePrimaryColor(mContext);
            int songBg = ContextCompat.getColor(mContext, ATEUtil.isDarkTheme(mContext) ? R.color.window_background_dark : R.color.window_background);
            itemHolder.title.setTextColor(ColorUtil.ensureContrastRatio(accent, songBg, 4.5));
        } else {
            itemHolder.title.setTextColor(ATEUtil.getThemeTextColorPrimary(mContext));
        }

        setOnPopupMenuListener(itemHolder);
    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    private void setOnPopupMenuListener(final ItemHolder itemHolder) {
        itemHolder.popupMenu.setOnClickListener(v -> {
            final PopupMenu menu = new PopupMenu(mContext, v);
            final int adapterPosition = itemHolder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;
            final Song song = arraylist.get(adapterPosition);
            menu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.popup_song_play_next) {
                    long[] ids = new long[1];
                    ids[0] = arraylist.get(adapterPosition).id;
                    MusicPlayer.playNext(mContext, ids, -1, ListenerUtil.IdType.NA);
                } else if (itemId == R.id.popup_song_goto_album) {
                    NavigationUtil.goToAlbum(mContext, song.albumId, song.title);
                } else if (itemId == R.id.popup_song_goto_artist) {
                    NavigationUtil.goToArtist(mContext, song.artistId, song.artistName);
                } else if (itemId == R.id.popup_song_addto_queue) {
                    long[] id = new long[1];
                    id[0] = song.id;
                    MusicPlayer.addToQueue(mContext, id, -1, ListenerUtil.IdType.Playlist);
                } else if (itemId == R.id.popup_song_addto_playlist) {
                    ListenerUtil.showAddPlaylistDialog(mContext, new long[]{song.id});
                } else if (itemId == R.id.popup_song_delete) {
                    new MaterialDialog.Builder(mContext)
                            .title(R.string.delete_playlist_song)
                            .positiveText(R.string.delete)
                            .negativeText(R.string.cancel)
                            .onPositive((dialog, which) -> {
                                PlaylistSongLoader.removeFromPlaylist(mContext, new long[]{song.id}, playlistId);
                                arraylist.remove(adapterPosition);
                                songIDs = getSongIds();
                                notifyItemRemoved(adapterPosition);
                            })
                            .onNegative((dialog, which) -> dialog.dismiss())
                            .show();
                }
                return false;
            });
            menu.inflate(R.menu.popup_song);
            menu.show();
        });
    }

    public long[] getSongIds() {
        int songNum = arraylist.size();
        long[] ret = new long[songNum];
        for (int i = 0; i < songNum; i++) {
            ret[i] = arraylist.get(i).id;
        }

        return ret;
    }

    public List<Song> getSongList() {
        return arraylist;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSongList(List<Song> arraylist) {
        this.arraylist = arraylist;
        this.songIDs = getSongIds();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        if (arraylist == null || arraylist.isEmpty())
            return "";

        char ch = arraylist.get(position).title.charAt(0);
        if (Character.isDigit(ch)) {
            return "#";
        } else
            return Character.toString(ch);
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView title;
        private final TextView artist;
        private final TextView album;
        private final ImageView albumArt;
        private final ImageView popupMenu;

        public ItemHolder(View view) {
            super(view);
            this.title = view.findViewById(R.id.text_item_title);
            this.artist = view.findViewById(R.id.text_item_subtitle);
            this.album = view.findViewById(R.id.text_item_subtitle_2);
            this.albumArt = view.findViewById(R.id.image);
            this.popupMenu = view.findViewById(R.id.popup_menu);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            final Handler handler = new Handler(Looper.getMainLooper());
            final int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            handler.postDelayed(() -> {
                MusicPlayer.playAll(mContext, songIDs, pos, -1, ListenerUtil.IdType.Playlist, false);
                handler.postDelayed(() -> notifyItemChanged(pos), 50);
            }, 100);
        }
    }
}
