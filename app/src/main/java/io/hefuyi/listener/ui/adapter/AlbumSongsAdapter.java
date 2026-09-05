package io.hefuyi.listener.ui.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ColorUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.NavigationUtil;

/**
 * Created by hefuyi on 2016/12/3.
 */

public class AlbumSongsAdapter extends RecyclerView.Adapter<AlbumSongsAdapter.ItemHolder> {

    private final Activity mContext;
    private final long albumID;
    private List<Song> arraylist;
    private long[] songIDs;

    public AlbumSongsAdapter(Activity context, long albumID) {
        this.mContext = context;
        this.albumID = albumID;
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list_linear_layout_item, viewGroup, false);
        return new ItemHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemHolder itemHolder, int i) {
        Song localItem = arraylist.get(i);
        itemHolder.title.setText(localItem.title);
        itemHolder.artist.setText(localItem.artistName);
        itemHolder.album.setText(localItem.albumName);

        Glide.with(mContext)
                .load(ListenerUtil.getAlbumArtUri(localItem.albumId))
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .error(ATEUtil.getDefaultAlbumDrawable(mContext))
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

    private void setOnPopupMenuListener(final ItemHolder itemHolder) {
        itemHolder.popupMenu.setOnClickListener(v -> {
            final PopupMenu menu = new PopupMenu(mContext, v);
            menu.setOnMenuItemClickListener(item -> {
                int pos = itemHolder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) {
                    return false;
                }
                int itemId = item.getItemId();
                if (itemId == R.id.popup_song_play_next) {
                    long[] ids = new long[]{arraylist.get(pos).id};
                    MusicPlayer.playNext(mContext, ids, -1, ListenerUtil.IdType.NA);
                } else if (itemId == R.id.popup_song_goto_album) {
                    NavigationUtil.goToAlbum(mContext, arraylist.get(pos).albumId,
                            arraylist.get(pos).title);
                } else if (itemId == R.id.popup_song_goto_artist) {
                    NavigationUtil.goToArtist(mContext, arraylist.get(pos).artistId,
                            arraylist.get(pos).artistName);
                } else if (itemId == R.id.popup_song_addto_queue) {
                    long[] id = new long[]{arraylist.get(pos).id};
                    MusicPlayer.addToQueue(mContext, id, -1, ListenerUtil.IdType.NA);
                } else if (itemId == R.id.popup_song_addto_playlist) {
                    ListenerUtil.showAddPlaylistDialog(mContext, new long[]{arraylist.get(pos).id});
                } else if (itemId == R.id.popup_song_delete) {
                    long[] deleteIds = {arraylist.get(pos).id};
                    ListenerUtil.showDeleteDialog(mContext, arraylist.get(pos).title, deleteIds,
                            (dialog, which) -> {
                                arraylist.remove(pos);
                                songIDs = getSongIds();
                                notifyItemRemoved(pos);
                            });
                }
                return false;
            });
            menu.inflate(R.menu.popup_song);
            menu.show();
        });
    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    public long[] getSongIds() {
        long[] ret = new long[getItemCount()];
        for (int i = 0; i < getItemCount(); i++) {
            ret[i] = arraylist.get(i).id;
        }

        return ret;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSongList(List<Song> songList) {
        arraylist = songList;
        songIDs = getSongIds();
        notifyDataSetChanged();
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
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    MusicPlayer.playAll(mContext, songIDs, pos, albumID, ListenerUtil.IdType.Album, false);
                }
            }, 100);
        }
    }
}
