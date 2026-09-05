package io.hefuyi.listener.ui.adapter;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ColorUtil;
import io.hefuyi.listener.R;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.DensityUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.NavigationUtil;
import io.hefuyi.listener.widget.fastscroller.FastScrollRecyclerView;

public class SongsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FastScrollRecyclerView.SectionedAdapter {

    private final AppCompatActivity mContext;
    private final boolean withHeader;
    private final String action;
    public int currentlyPlayingPosition;
    private List<Song> arraylist;
    private long[] songIDs;
    private float topPlayScore;

    public SongsListAdapter(AppCompatActivity context, List<Song> arraylist, String action, boolean withHeader) {
        this.arraylist = Objects.requireNonNullElseGet(arraylist, ArrayList::new);
        this.mContext = context;
        this.songIDs = getSongIds();
        this.withHeader = withHeader;
        this.action = action;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && withHeader) {
            return Type.TYPE_PLAY_SHUFFLE;
        } else {
            return Type.TYPE_SONG;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        switch (viewType) {
            case Type.TYPE_PLAY_SHUFFLE:
                View playShuffle = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_play_shuffle, viewGroup, false);
                viewHolder = new PlayShuffleViewHoler(playShuffle);
                break;
            case Type.TYPE_SONG:
                View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list_linear_layout_item, viewGroup, false);
                viewHolder = new ItemHolder(v);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        switch (viewType) {
            case Type.TYPE_PLAY_SHUFFLE:
                PlayShuffleViewHoler shuffleHolder = (PlayShuffleViewHoler) holder;
                int primaryColor = ATEUtil.getThemePrimaryColor(mContext);
                int bg = ContextCompat.getColor(mContext, ATEUtil.isDarkTheme(mContext) ? R.color.window_background_dark : R.color.window_background);
                ImageViewCompat.setImageTintList(shuffleHolder.playShuffle, ColorStateList.valueOf(ColorUtil.ensureContrastRatio(primaryColor, bg, 4.5)));
                shuffleHolder.textView.setTextColor(ATEUtil.getThemeTextColorPrimary(mContext));
                break;
            case Type.TYPE_SONG:
                ItemHolder itemHolder = (ItemHolder) holder;
                Song localItem;
                if (withHeader) {
                    localItem = arraylist.get(position - 1);
                } else {
                    localItem = arraylist.get(position);
                }

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

                if (topPlayScore != 0) {
                    itemHolder.playscore.setVisibility(View.VISIBLE);
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) itemHolder.playscore.getLayoutParams();
                    int full = DensityUtil.getScreenWidth(mContext);
                    layoutParams.width = (int) (full * (localItem.getPlayCountScore() / topPlayScore));
                }

                setOnPopupMenuListener(itemHolder, position);
                break;
        }
    }

    @Override
    public int getItemCount() {
        if (withHeader && !arraylist.isEmpty()) {
            return (null != arraylist ? arraylist.size() + 1 : 0);
        } else {
            return (null != arraylist ? arraylist.size() : 0);
        }
    }


    private void setOnPopupMenuListener(ItemHolder itemHolder, final int position) {

        final int realSongPosition;
        if (withHeader) {
            realSongPosition = position - 1;
        } else {
            realSongPosition = position;
        }

        itemHolder.popupMenu.setOnClickListener(v -> {
            final PopupMenu menu = new PopupMenu(mContext, v);
            menu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.popup_song_play_next) {
                    long[] ids = new long[1];
                    ids[0] = arraylist.get(realSongPosition).id;
                    MusicPlayer.playNext(mContext, ids, -1, ListenerUtil.IdType.NA);
                } else if (itemId == R.id.popup_song_goto_album) {
                    NavigationUtil.goToAlbum(mContext, arraylist.get(realSongPosition).albumId,
                            arraylist.get(realSongPosition).title);
                } else if (itemId == R.id.popup_song_goto_artist) {
                    NavigationUtil.goToArtist(mContext, arraylist.get(realSongPosition).artistId,
                            arraylist.get(realSongPosition).artistName);
                } else if (itemId == R.id.popup_song_addto_queue) {
                    long[] id = new long[1];
                    id[0] = arraylist.get(realSongPosition).id;
                    MusicPlayer.addToQueue(mContext, id, -1, ListenerUtil.IdType.NA);
                } else if (itemId == R.id.popup_song_addto_playlist) {
                    ListenerUtil.showAddPlaylistDialog(mContext, new long[]{arraylist.get(realSongPosition).id});
                } else if (itemId == R.id.popup_song_delete) {
                    long[] deleteIds = {arraylist.get(realSongPosition).id};
                    switch (action) {
                        case Constants.NAVIGATE_PLAYLIST_FAVORITE:
                            ListenerUtil.showDeleteFromFavorite(mContext, deleteIds);
                            break;
                        case Constants.NAVIGATE_PLAYLIST_RECENTPLAY:
                            ListenerUtil.showDeleteFromRecentlyPlay(mContext, deleteIds);
                            break;
                        default:
                            ListenerUtil.showDeleteDialog(mContext, arraylist.get(realSongPosition).title, deleteIds,
                                    (dialog, which) -> {
                                        arraylist.remove(realSongPosition);
                                        songIDs = getSongIds();
                                        notifyItemRemoved(position);
                                    });
                            break;
                    }
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

    @SuppressLint("NotifyDataSetChanged")
    public void setSongList(List<Song> arraylist) {
        this.arraylist = arraylist;
        this.songIDs = getSongIds();
        if (!arraylist.isEmpty()) {
            this.topPlayScore = arraylist.get(0).getPlayCountScore();
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        if (arraylist == null || arraylist.isEmpty() || (withHeader && position == 0))
            return "";

        if (withHeader) {
            position = position - 1;
        }
        char ch = arraylist.get(position).title.charAt(0);
        if (Character.isDigit(ch)) {
            return "#";
        } else
            return Character.toString(ch);
    }

    public static class Type {
        public static final int TYPE_PLAY_SHUFFLE = 0;
        public static final int TYPE_SONG = 1;
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView title;
        private final TextView artist;
        private final TextView album;
        private final ImageView albumArt;
        private final ImageView popupMenu;
        private final View playscore;

        public ItemHolder(View view) {
            super(view);
            this.title = view.findViewById(R.id.text_item_title);
            this.artist = view.findViewById(R.id.text_item_subtitle);
            this.album = view.findViewById(R.id.text_item_subtitle_2);
            this.albumArt = view.findViewById(R.id.image);
            this.popupMenu = view.findViewById(R.id.popup_menu);
            this.playscore = view.findViewById(R.id.playscore);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Log.d("SongsListAdapter", "onClick at position " + pos);
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> MusicPlayer.playAll(mContext, songIDs, pos - 1, -1, ListenerUtil.IdType.NA, false), 100);
        }
    }

    public class PlayShuffleViewHoler extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ImageView playShuffle;
        private final TextView textView;

        public PlayShuffleViewHoler(View view) {
            super(view);
            this.playShuffle = view.findViewById(R.id.play_shuffle);
            this.textView = view.findViewById(R.id.textView);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            final Handler handler = new Handler(Looper.getMainLooper());
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            handler.postDelayed(() -> {
                MusicPlayer.playAll(mContext, songIDs, -1, -1, ListenerUtil.IdType.NA, true);
                handler.postDelayed(() -> {
                    notifyItemChanged(currentlyPlayingPosition);
                    notifyItemChanged(pos);
                    currentlyPlayingPosition = pos;
                }, 50);
            }, 100);
        }
    }

}


