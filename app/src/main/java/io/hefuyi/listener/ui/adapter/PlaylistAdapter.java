package io.hefuyi.listener.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.ArrayList;
import java.util.List;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.dataloader.PlaylistLoader;
import io.hefuyi.listener.dataloader.PlaylistSongLoader;
import io.hefuyi.listener.mvp.model.Playlist;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ColorUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.NavigationUtil;
import io.hefuyi.listener.util.PreferencesUtility;
import io.hefuyi.listener.widget.fastscroller.FastScrollRecyclerView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ItemHolder> implements FastScrollRecyclerView.SectionedAdapter {

    private final List<Playlist> arraylist;
    private final Fragment mFragment;
    private final Context mContext;
    private final boolean isGrid;

    public PlaylistAdapter(Fragment fragment, List<Playlist> arraylist) {
        this.arraylist = (arraylist != null) ? arraylist : new ArrayList<>();
        this.mFragment = fragment;
        this.mContext = fragment.getContext();
        this.isGrid = PreferencesUtility.getInstance(mFragment.getContext()).getPlaylistView() == Constants.PLAYLIST_VIEW_GRID;
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
        final Playlist localItem = arraylist.get(position);

        itemHolder.title.setText(localItem.name);
        itemHolder.songCount.setText(ListenerUtil.makeLabel(mContext, R.plurals.n_songs, localItem.songCount));
        itemHolder.subtitle1.setVisibility(View.GONE);
        itemHolder.divider.setVisibility(View.GONE);

        PlaylistSongLoader.getSongsInPlaylist(mContext, localItem.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(playlistSongs -> {
                    final Uri uri;
                    final long firstAlbumID;
                    if (!playlistSongs.isEmpty()) {
                        firstAlbumID = playlistSongs.get(0).albumId;
                        uri = ListenerUtil.getAlbumArtUri(firstAlbumID);
                    } else {
                        firstAlbumID = -1;
                        uri = null;
                    }
                    itemHolder.playlistArt.setTag(R.string.playlistArt, firstAlbumID);

                    Glide.with(itemHolder.itemView.getContext())
                            .load(uri)
                            .asBitmap()
                            .placeholder(ATEUtil.getDefaultAlbumDrawable(mContext))
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                    if (isGrid) {
                                        itemHolder.footer.setBackgroundColor(ATEUtil.getThemeAlbumDefaultPaletteColor(mContext));
                                    }
                                    itemHolder.playlistArt.setImageDrawable(ATEUtil.getDefaultAlbumDrawable(mContext));
                                    itemHolder.title.setTextColor(ATEUtil.getThemeTextColorPrimary(mContext));
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
                                                itemHolder.playlistArt.setImageBitmap(resource);
                                                itemHolder.title.setTextColor(ColorUtil.getOpaqueColor(detailColor));
                                                itemHolder.songCount.setTextColor(detailColor);
                                                itemHolder.popupMenu.setColorFilter(detailColor);
                                            }
                                        });
                                    } else {
                                        itemHolder.playlistArt.setImageBitmap(resource);
                                    }
                                }
                            });
                });

        itemHolder.playlistArt.setTransitionName("transition_album_art" + itemHolder.getLayoutPosition());

        setOnPopupMenuListener(itemHolder);
    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setPlaylist(List<Playlist> playlists) {
        this.arraylist.clear();
        this.arraylist.addAll(playlists);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        if (arraylist == null || arraylist.isEmpty())
            return "";
        return Character.toString(arraylist.get(position).name.charAt(0));
    }

    private void setOnPopupMenuListener(final ItemHolder itemHolder) {
        itemHolder.popupMenu.setOnClickListener(v -> {

            final PopupMenu menu = new PopupMenu(mContext, v);
            menu.setOnMenuItemClickListener(item -> {
                int adapterPos = itemHolder.getBindingAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION) return false;
                final Playlist playlist = arraylist.get(adapterPos);
                int itemId = item.getItemId();
                if (itemId == R.id.popup_playlist_rename) {
                    new MaterialDialog.Builder(mContext)
                            .title(R.string.rename_playlist)
                            .positiveText(R.string.sure)
                            .negativeText(R.string.cancel)
                            .input(null, playlist.name, false, (dialog, input) -> {
                                MusicPlayer.renamePlaylist(mContext, playlist.id, input.toString());
                                itemHolder.title.setText(input.toString());
                                Toast.makeText(mContext, R.string.rename_playlist_success, Toast.LENGTH_SHORT).show();
                            })
                            .show();
                } else if (itemId == R.id.popup_playlist_addto_playlist) {
                    getSongListIdByPlaylist(playlist.id)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(ids -> ListenerUtil.showAddPlaylistDialog(mFragment.getActivity(), ids));
                } else if (itemId == R.id.popup_playlist_addto_queue) {
                    getSongListIdByPlaylist(playlist.id)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(ids -> MusicPlayer.addToQueue(mContext, ids, -1, ListenerUtil.IdType.Playlist));
                } else if (itemId == R.id.popup_playlist_delete) {
                    new MaterialDialog.Builder(mContext)
                            .title(R.string.delete_playlist)
                            .positiveText(R.string.delete)
                            .negativeText(R.string.cancel)
                            .onPositive((dialog, which) -> {
                                int delPos = itemHolder.getBindingAdapterPosition();
                                if (delPos != RecyclerView.NO_POSITION) {
                                    PlaylistLoader.deletePlaylists(mContext, playlist.id);
                                    arraylist.remove(delPos);
                                    notifyItemRemoved(delPos);
                                    Toast.makeText(mContext, R.string.delete_playlist_success, Toast.LENGTH_SHORT).show();
                                }
                            })
                            .onNegative((dialog, which) -> dialog.dismiss())
                            .show();
                }
                return false;
            });
            menu.inflate(R.menu.popup_playlist);
            menu.show();
        });
    }

    private Observable<long[]> getSongListIdByPlaylist(long playlistId) {
        return PlaylistSongLoader.getSongsInPlaylist(mContext, playlistId)
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
        private final TextView subtitle1;
        private final View divider;
        private final TextView songCount;
        private final ImageView playlistArt;
        private final View footer;
        private final ImageView popupMenu;

        public ItemHolder(View view) {
            super(view);
            this.title = view.findViewById(R.id.text_item_title);
            this.subtitle1 = view.findViewById(R.id.text_item_subtitle);
            this.divider = view.findViewById(R.id.divider_subtitle);
            this.songCount = view.findViewById(R.id.text_item_subtitle_2);
            this.playlistArt = view.findViewById(R.id.image);
            this.footer = view.findViewById(R.id.footer);
            this.popupMenu = view.findViewById(R.id.popup_menu);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Playlist playlist = arraylist.get(pos);
            Object tag = playlistArt.getTag(R.string.playlistArt);
            long firstAlbumId = (tag instanceof Long) ? (Long) tag : -1L;
            NavigationUtil.navigateToPlaylistDetail(mFragment.getActivity(), playlist.id,
                    playlist.name, firstAlbumId,
                    new Pair<>(playlistArt, "transition_album_art" + pos));
        }

    }
}