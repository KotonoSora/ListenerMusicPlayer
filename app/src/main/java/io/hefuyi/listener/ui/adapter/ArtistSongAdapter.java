package io.hefuyi.listener.ui.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Rect;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.dataloader.ArtistAlbumLoader;
import io.hefuyi.listener.mvp.model.Album;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ColorUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.NavigationUtil;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by hefuyi on 2016/11/24.
 */

public class ArtistSongAdapter extends RecyclerView.Adapter<ArtistSongAdapter.ItemHolder> {

    private final Activity mContext;
    private final long artistID;
    private List<Song> arraylist;
    private long[] songIDs;

    public ArtistSongAdapter(Activity context, List<Song> arraylist, long artistID) {
        this.arraylist = arraylist;
        this.mContext = context;
        this.artistID = artistID;
        if (arraylist != null) {
            this.songIDs = getSongIds();
        }
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        if (viewType == 0) {
            View v0 = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.artist_detail_albums_header, viewGroup, false);
            return new ItemHolder(v0);
        } else {
            View v2 = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list_linear_layout_item, viewGroup, false);
            return new ItemHolder(v2);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ItemHolder itemHolder, int i) {

        if (getItemViewType(i) == 0) {
            //nothing
            setUpAlbums(itemHolder.albumsRecyclerView);
        } else {
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

            setOnPopupMenuListener(itemHolder, i - 1);
        }

    }

    @Override
    public void onViewRecycled(ItemHolder itemHolder) {

        if (itemHolder.getItemViewType() == 0)
            clearExtraSpacingBetweenCards(itemHolder.albumsRecyclerView);

    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    @Override
    public int getItemViewType(int position) {
        int viewType;
        if (position == 0) {
            viewType = 0;
        } else viewType = 1;
        return viewType;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSongList(List<Song> songList) {
        this.arraylist = songList;
        this.songIDs = getSongIds();
        notifyDataSetChanged();
    }

    private void setUpAlbums(final RecyclerView albumsRecyclerview) {

        albumsRecyclerview.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));

        //to add spacing between cards
        int spacingInPixels = mContext.getResources().getDimensionPixelSize(R.dimen.spacing_card);
        albumsRecyclerview.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
        albumsRecyclerview.setNestedScrollingEnabled(false);

        ArtistAlbumLoader.getAlbumsForArtist(mContext, artistID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(albumList -> {
                    ArtistAlbumAdapter mAlbumAdapter = new ArtistAlbumAdapter(mContext, albumList);
                    albumsRecyclerview.setAdapter(mAlbumAdapter);
                });

    }

    private void setOnPopupMenuListener(ItemHolder itemHolder, final int position) {

        itemHolder.popupMenu.setOnClickListener(v -> {
            final PopupMenu menu = new PopupMenu(mContext, v);
            menu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.popup_song_play_next) {
                    long[] ids = new long[1];
                    ids[0] = arraylist.get(position + 1).id;
                    MusicPlayer.playNext(mContext, ids, -1, ListenerUtil.IdType.NA);
                } else if (itemId == R.id.popup_song_addto_playlist) {
                    ListenerUtil.showAddPlaylistDialog(mContext, new long[]{arraylist.get(position + 1).id});
                } else if (itemId == R.id.popup_song_addto_queue) {
                    long[] id = new long[1];
                    id[0] = arraylist.get(position + 1).id;
                    MusicPlayer.addToQueue(mContext, id, -1, ListenerUtil.IdType.NA);
                } else if (itemId == R.id.popup_song_goto_album) {
                    NavigationUtil.goToAlbum(mContext, arraylist.get(position + 1).albumId,
                            arraylist.get(position + 1).title);
                } else if (itemId == R.id.popup_song_delete) {
                    long[] deleteIds = {arraylist.get(position + 1).id};
                    ListenerUtil.showDeleteDialog(mContext, arraylist.get(position + 1).title, deleteIds,
                            (dialog, which) -> {
                                arraylist.remove(position + 1);
                                songIDs = getSongIds();
                                notifyItemRemoved(position + 1);
                            });
                }
                return false;
            });
            menu.inflate(R.menu.popup_song);
            menu.getMenu().findItem(R.id.popup_song_goto_artist).setVisible(false);
            menu.show();
        });
    }

    private void clearExtraSpacingBetweenCards(RecyclerView albumsRecyclerview) {
        //to clear any extra spacing between cards
        int spacingInPixelstoClear = -(mContext.getResources().getDimensionPixelSize(R.dimen.spacing_card));
        albumsRecyclerview.addItemDecoration(new SpacesItemDecoration(spacingInPixelstoClear));

    }

    public long[] getSongIds() {
        List<Song> actualArraylist = new ArrayList<>(arraylist);
        actualArraylist.remove(0);
        long[] ret = new long[actualArraylist.size()];
        for (int i = 0; i < actualArraylist.size(); i++) {
            ret[i] = actualArraylist.get(i).id;
        }
        return ret;
    }

    public static class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

            //the padding from left
            outRect.left = space;


        }
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title;
        TextView artist;
        TextView album;
        ImageView albumArt;
        ImageView popupMenu;
        RecyclerView albumsRecyclerView;

        public ItemHolder(View view) {
            super(view);

            this.albumsRecyclerView = view.findViewById(R.id.recycler_view_album);

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
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            handler.postDelayed(() -> MusicPlayer.playAll(mContext, songIDs, pos - 1, artistID, ListenerUtil.IdType.Artist, false), 100);
        }

    }
}
