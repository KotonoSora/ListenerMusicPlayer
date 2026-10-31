package io.hefuyi.listener.ui.adapter;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ColorUtil;
import io.hefuyi.listener.util.ListenerUtil;

/**
 * Created by hefuyi on 2016/12/26.
 */

public class PlayqueueSongsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final AppCompatActivity mContext;
    private int currentlyPlayingPosition;
    private List<Song> arraylist;
    private long[] songIDs;
    private Palette.Swatch mSwatch;

    public PlayqueueSongsAdapter(AppCompatActivity context, List<Song> arraylist) {
        this.arraylist = Objects.requireNonNullElseGet(arraylist, ArrayList::new);
        this.mContext = context;
        this.songIDs = getSongIds();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View song = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list_linear_layout_item, viewGroup, false);
        return new ItemHolder(song);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemHolder itemHolder = (ItemHolder) holder;
        Song localItem;
        localItem = arraylist.get(position);

        itemHolder.title.setText(localItem.title);
        itemHolder.artist.setText(localItem.artistName);
        itemHolder.album.setText(localItem.albumName);

        if (mSwatch != null) {
            itemHolder.title.setTextColor(mSwatch.getBodyTextColor());
            itemHolder.artist.setTextColor(mSwatch.getTitleTextColor());
            itemHolder.album.setTextColor(mSwatch.getTitleTextColor());

            if (MusicPlayer.getQueuePosition() == position) {
                itemHolder.playIndicator.setVisibility(View.VISIBLE);
                itemHolder.playIndicator.setBackgroundColor(ColorUtil.getBlackWhiteColor(mSwatch.getRgb()));
            } else {
                itemHolder.playIndicator.setVisibility(View.GONE);
            }
        } else {
            itemHolder.title.setTextColor(ATEUtil.getThemeTextColorPrimary(mContext));
            itemHolder.artist.setTextColor(ATEUtil.getThemeTextColorSecondly(mContext));
            itemHolder.album.setTextColor(ATEUtil.getThemeTextColorSecondly(mContext));

            if (MusicPlayer.getQueuePosition() == position) {
                itemHolder.playIndicator.setVisibility(View.VISIBLE);
                itemHolder.playIndicator.setBackgroundColor(ATEUtil.getThemePrimaryColor(mContext));
            } else {
                itemHolder.playIndicator.setVisibility(View.GONE);
            }
        }

        Glide.with(holder.itemView.getContext()).load(ListenerUtil.getAlbumArtUri(localItem.albumId))
                .error(ATEUtil.getDefaultAlbumDrawable(mContext))
                .placeholder(ATEUtil.getDefaultAlbumDrawable(mContext))
                .centerCrop()
                .into(itemHolder.albumArt);
    }

    @Override
    public int getItemCount() {
        return arraylist.size();
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
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setPaletteSwatch(Palette.Swatch swatch) {
        mSwatch = swatch;
        notifyDataSetChanged();
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView title;
        private final TextView artist;
        private final TextView album;
        private final ImageView albumArt;
        private final ImageView popupMenu;
        private final View playIndicator;

        public ItemHolder(View view) {
            super(view);
            this.title = view.findViewById(R.id.text_item_title);
            this.artist = view.findViewById(R.id.text_item_subtitle);
            this.album = view.findViewById(R.id.text_item_subtitle_2);
            this.albumArt = view.findViewById(R.id.image);
            this.popupMenu = view.findViewById(R.id.popup_menu);
            this.playIndicator = view.findViewById(R.id.now_playing_indicator);

            popupMenu.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_clear_white_36dp));
            popupMenu.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                MusicPlayer.removeFromQueue(pos);
                arraylist.remove(pos);
                songIDs = getSongIds();
                notifyItemRemoved(pos);
            });
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            final Handler handler = new Handler(Looper.getMainLooper());
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            handler.postDelayed(() -> {
                MusicPlayer.playAll(mContext, songIDs, pos, -1, ListenerUtil.IdType.NA, false);
                handler.postDelayed(() -> {
                    notifyItemChanged(currentlyPlayingPosition);
                    notifyItemChanged(pos);
                    currentlyPlayingPosition = pos;
                }, 50);
            }, 100);
        }
    }
}
