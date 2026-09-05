package io.hefuyi.listener.ui.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.List;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.dataloader.AlbumSongLoader;
import io.hefuyi.listener.mvp.model.Album;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ColorUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.NavigationUtil;
import io.hefuyi.listener.util.PreferencesUtility;
import io.hefuyi.listener.widget.fastscroller.FastScrollRecyclerView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ItemHolder> implements FastScrollRecyclerView.SectionedAdapter {

    private final Activity mContext;
    private final boolean isGrid;
    private final String action;
    private List<Album> arraylist;

    public AlbumAdapter(Activity context, String action) {
        this.mContext = context;
        this.isGrid = PreferencesUtility.getInstance(mContext).isAlbumsInGrid();
        this.action = action;
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        int layoutRes = isGrid ? R.layout.item_list_grid_layout_item : R.layout.item_list_linear_layout_item;
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(layoutRes, viewGroup, false);
        return new ItemHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ItemHolder itemHolder, final int position) {
        Album localItem = arraylist.get(position);

        itemHolder.title.setText(localItem.title);
        itemHolder.artist.setText(localItem.artistName);
        itemHolder.songCount.setText(ListenerUtil.makeLabel(mContext, R.plurals.n_songs, localItem.songCount));

        Glide.with(itemHolder.itemView.getContext())
                .load(ListenerUtil.getAlbumArtUri(localItem.id))
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        if (isGrid) {
                            itemHolder.footer.setBackgroundColor(ATEUtil.getThemeAlbumDefaultPaletteColor(mContext));
                        }
                        itemHolder.albumArt.setImageDrawable(ATEUtil.getDefaultAlbumDrawable(mContext));
                        itemHolder.title.setTextColor(ATEUtil.getThemeTextColorPrimary(mContext));
                        itemHolder.artist.setTextColor(ATEUtil.getThemeTextColorSecondly(mContext));
                        itemHolder.songCount.setTextColor(ATEUtil.getThemeTextColorSecondly(mContext));
                        itemHolder.popupMenu.setColorFilter(ATEUtil.getThemeTextColorSecondly(mContext));
                    }

                    @Override
                    public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        if (isGrid) {
                            new Palette.Builder(resource).generate(palette -> {
                                Palette.Swatch swatch = ColorUtil.getMostPopulousSwatch(palette);
                                if (swatch != null) {
                                    int color = swatch.getRgb();
                                    itemHolder.footer.setBackgroundColor(color);

                                    int detailColor = swatch.getTitleTextColor();
                                    itemHolder.albumArt.setImageBitmap(resource);
                                    itemHolder.title.setTextColor(ColorUtil.getOpaqueColor(detailColor));
                                    itemHolder.artist.setTextColor(detailColor);
                                    itemHolder.songCount.setTextColor(detailColor);
                                    itemHolder.popupMenu.setColorFilter(detailColor);
                                }
                            });
                        } else {
                            itemHolder.albumArt.setImageBitmap(resource);
                        }
                    }
                });

        itemHolder.albumArt.setTransitionName("transition_album_art" + itemHolder.getLayoutPosition());

        setOnPopupMenuListener(itemHolder);

    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setAlbumsList(List<Album> arraylist) {
        this.arraylist = arraylist;
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

    private void setOnPopupMenuListener(final ItemHolder itemHolder) {
        itemHolder.popupMenu.setOnClickListener(v -> {

            final PopupMenu menu = new PopupMenu(mContext, v);
            int adapterPosition = itemHolder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;
            final Album album = arraylist.get(adapterPosition);
            menu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.popup_album_addto_queue) {
                    getSongListIdByAlbum(album.id)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(ids -> MusicPlayer.addToQueue(mContext, ids, -1, ListenerUtil.IdType.NA));
                } else if (itemId == R.id.popup_album_addto_playlist) {
                    getSongListIdByAlbum(album.id)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(ids -> ListenerUtil.showAddPlaylistDialog(mContext, ids));
                } else if (itemId == R.id.popup_album_goto_artist) {
                    NavigationUtil.goToArtist(mContext, album.artistId, album.artistName);
                } else if (itemId == R.id.popup_artist_delete) {
                    switch (action) {
                        case Constants.NAVIGATE_PLAYLIST_FAVORITE:
                            getSongListIdByAlbum(album.id)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(ids -> ListenerUtil.showDeleteFromFavorite(mContext, ids));
                            break;
                        case Constants.NAVIGATE_PLAYLIST_RECENTPLAY:
                            getSongListIdByAlbum(album.id)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(ids -> ListenerUtil.showDeleteFromRecentlyPlay(mContext, ids));
                            break;
                        default:
                            AlbumSongLoader.getSongsForAlbum(mContext, album.id)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(songs -> {
                                        long[] ids = new long[songs.size()];
                                        for (int i = 0; i < songs.size(); i++) {
                                            ids[i] = songs.get(i).id;
                                        }
                                        int currentPos = itemHolder.getBindingAdapterPosition();
                                        if (currentPos == RecyclerView.NO_POSITION) return;
                                        if (ids.length == 1) {
                                            ListenerUtil.showDeleteDialog(mContext, songs.get(0).title, ids,
                                                    (dialog, which) -> {
                                                        int delPos = itemHolder.getBindingAdapterPosition();
                                                        if (delPos != RecyclerView.NO_POSITION) {
                                                            arraylist.remove(delPos);
                                                            notifyItemRemoved(delPos);
                                                        }
                                                    });
                                        } else {
                                            String label = ListenerUtil.makeLabel(mContext,
                                                    R.plurals.n_songs, album.songCount);
                                            ListenerUtil.showDeleteDialog(mContext, label, ids,
                                                    (dialog, which) -> {
                                                        int delPos = itemHolder.getBindingAdapterPosition();
                                                        if (delPos != RecyclerView.NO_POSITION) {
                                                            arraylist.remove(delPos);
                                                            notifyItemRemoved(delPos);
                                                        }
                                                    });
                                        }
                                    });
                            break;
                    }
                }
                return false;
            });
            menu.inflate(R.menu.popup_album);
            menu.show();
        });
    }

    private Observable<long[]> getSongListIdByAlbum(long albumId) {
        return AlbumSongLoader.getSongsForAlbum(mContext, albumId)
                .map(songs -> {
                    long[] ids = new long[songs.size()];
                    for (int i = 0; i < songs.size(); i++) {
                        ids[i] = songs.get(i).id;
                    }
                    return ids;
                });
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView title;
        private final TextView artist;
        private final TextView songCount;
        private final ImageView albumArt;
        private final ImageView popupMenu;
        private final View footer;

        public ItemHolder(View view) {
            super(view);
            this.title = view.findViewById(R.id.text_item_title);
            this.artist = view.findViewById(R.id.text_item_subtitle);
            this.songCount = view.findViewById(R.id.text_item_subtitle_2);
            this.albumArt = view.findViewById(R.id.image);
            this.popupMenu = view.findViewById(R.id.popup_menu);
            this.footer = view.findViewById(R.id.footer);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Album album = arraylist.get(pos);
            NavigationUtil.navigateToAlbum(mContext, album.id, album.title,
                    new Pair<>(albumArt, "transition_album_art" + pos));
        }

    }
}