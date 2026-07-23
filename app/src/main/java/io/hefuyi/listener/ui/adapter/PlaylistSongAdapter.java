package io.hefuyi.listener.ui.adapter;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.dataloader.PlaylistSongLoader;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.util.ATEUtil;
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

        if (arraylist == null) {
            this.arraylist = new ArrayList<>();
        } else {
            this.arraylist = arraylist;

        }
        this.mContext = context;
        this.songIDs = getSongIds();
        this.playlistId = playlistId;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list_linear_layout_item, viewGroup, false);
        return new ItemHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ItemHolder itemHolder = (ItemHolder) holder;
        Song localItem = arraylist.get(holder.getAdapterPosition());

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
            itemHolder.title.setTextColor(ATEUtil.getThemeAccentColor(mContext));
        } else {
            itemHolder.title.setTextColor(ATEUtil.getThemeTextColorPrimary(mContext));
        }

        setOnPopupMenuListener(itemHolder, position);
    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    private void setOnPopupMenuListener(final ItemHolder itemHolder, final int position) {
        itemHolder.popupMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu menu = new PopupMenu(mContext, v);
                final int adapterPosition = itemHolder.getAdapterPosition();
                final Song song = arraylist.get(adapterPosition);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
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
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            PlaylistSongLoader.removeFromPlaylist(mContext, new long[]{song.id}, playlistId);
                                            arraylist.remove(adapterPosition);
                                            songIDs = getSongIds();
                                            notifyItemRemoved(adapterPosition);
                                        }
                                    })
                                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            dialog.dismiss();
                                        }
                                    })
                                    .show();
                        }
                        return false;
                    }
                });
                menu.inflate(R.menu.popup_song);
                menu.show();
            }
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

    public void setSongList(List<Song> arraylist) {
        this.arraylist = arraylist;
        this.songIDs = getSongIds();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        if (arraylist == null || arraylist.size() == 0)
            return "";

        Character ch = arraylist.get(position).title.charAt(0);
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
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    MusicPlayer.playAll(mContext, songIDs, getAdapterPosition(), -1, ListenerUtil.IdType.Playlist, false);
                    Handler handler1 = new Handler();
                    handler1.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            notifyItemChanged(getAdapterPosition());
                        }
                    }, 50);
                }
            }, 100);

        }
    }
}
