package io.hefuyi.listener.ui.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.List;

import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.dataloader.AlbumSongLoader;
import io.hefuyi.listener.dataloader.ArtistSongLoader;
import io.hefuyi.listener.mvp.model.Album;
import io.hefuyi.listener.mvp.model.Artist;
import io.hefuyi.listener.mvp.model.ArtistArt;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.NavigationUtil;
import io.hefuyi.listener.util.PreferencesUtility;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by hefuyi on 2017/1/3.
 */

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ItemHolder> {

    private final Activity mContext;
    private List<?> searchResults = Collections.emptyList();

    public SearchAdapter(Activity context) {
        this.mContext = context;
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        switch (viewType) {
            case 10:
                View v10 = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.search_section_header, viewGroup, false);
                return new ItemHolder(v10);
            case 0:
            case 1:
            case 2:
            default:
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list_linear_layout_item, viewGroup, false);
                return new ItemHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ItemHolder itemHolder, int position) {
        switch (getItemViewType(position)) {
            case 0:
                Song song = (Song) searchResults.get(position);
                itemHolder.title.setText(song.title);
                itemHolder.subtitle1.setText(song.artistName);
                itemHolder.subtitle2.setText(song.albumName);

                Glide.with(itemHolder.itemView.getContext()).load(ListenerUtil.getAlbumArtUri(song.albumId))
                        .error(ATEUtil.getDefaultAlbumDrawable(mContext))
                        .placeholder(ATEUtil.getDefaultAlbumDrawable(mContext))
                        .centerCrop()
                        .into(itemHolder.image);

                setOnPopupMenuListener(itemHolder, 0);
                break;
            case 1:
                Album album = (Album) searchResults.get(position);
                itemHolder.title.setText(album.title);
                itemHolder.subtitle1.setText(album.artistName);
                itemHolder.subtitle2.setText(ListenerUtil.makeLabel(mContext, R.plurals.n_songs, album.songCount));

                Glide.with(itemHolder.itemView.getContext())
                        .load(ListenerUtil.getAlbumArtUri(album.id))
                        .asBitmap()
                        .placeholder(ATEUtil.getDefaultAlbumDrawable(mContext))
                        .error(ATEUtil.getDefaultAlbumDrawable(mContext))
                        .centerCrop()
                        .into(itemHolder.image);

                itemHolder.image.setTransitionName("transition_album_art" + position);

                setOnPopupMenuListener(itemHolder, 1);
                break;
            case 2:
                Artist artist = (Artist) searchResults.get(position);
                itemHolder.title.setText(artist.name);
                itemHolder.subtitle1.setText(ListenerUtil.makeLabel(mContext, R.plurals.n_albums, artist.albumCount));
                itemHolder.subtitle2.setText(ListenerUtil.makeLabel(mContext, R.plurals.n_songs, artist.songCount));

                String artistArtJson = PreferencesUtility.getInstance(mContext).getArtistArt(artist.id);
                if (!TextUtils.isEmpty(artistArtJson)) {
                    ArtistArt artistArt = new Gson().fromJson(artistArtJson, ArtistArt.class);
                    Glide.with(mContext)
                            .load(artistArt.getLarge())
                            .asBitmap()
                            .placeholder(ATEUtil.getDefaultSingerDrawable(mContext))
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .error(ATEUtil.getDefaultSingerDrawable(mContext))
                            .into(itemHolder.image);
                }

                itemHolder.image.setTransitionName("transition_artist_art" + position);

                setOnPopupMenuListener(itemHolder, 2);
                break;
            case 10:
                itemHolder.sectionHeader.setText((String) searchResults.get(position));
                break;
            case 3:
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    private void setOnPopupMenuListener(final ItemHolder itemHolder, final int type) {
        switch (type) {
            case 0:
                setSongPopupMenu(itemHolder);
                break;
            case 1:
                setAlbumPopupMenu(itemHolder);
                break;
            case 2:
                setArtistPopupMenu(itemHolder);
                break;
        }
    }

    private void setSongPopupMenu(ItemHolder itemHolder) {
        itemHolder.popupMenu.setOnClickListener(v -> {
            int pos = itemHolder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            final Song song = (Song) searchResults.get(pos);
            final PopupMenu menu = new PopupMenu(mContext, v);
            menu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.popup_song_play_next) {
                    MusicPlayer.playNext(mContext, new long[]{song.id}, -1, ListenerUtil.IdType.NA);
                } else if (itemId == R.id.popup_song_goto_album) {
                    NavigationUtil.goToAlbum(mContext, song.albumId, song.title);
                } else if (itemId == R.id.popup_song_goto_artist) {
                    NavigationUtil.goToArtist(mContext, song.artistId, song.artistName);
                } else if (itemId == R.id.popup_song_addto_queue) {
                    MusicPlayer.addToQueue(mContext, new long[]{song.id}, -1, ListenerUtil.IdType.NA);
                } else if (itemId == R.id.popup_song_addto_playlist) {
                    ListenerUtil.showAddPlaylistDialog(mContext, new long[]{song.id});
                } else if (itemId == R.id.popup_song_delete) {
                    ListenerUtil.showDeleteDialog(mContext, song.title, new long[]{song.id},
                            (dialog, which) -> {
                                int delPos = itemHolder.getBindingAdapterPosition();
                                if (delPos != RecyclerView.NO_POSITION) {
                                    searchResults.remove(delPos);
                                    notifyItemRemoved(delPos);
                                }
                            });
                }
                return false;
            });
            menu.inflate(R.menu.popup_song);
            menu.show();
        });
    }

    private void setAlbumPopupMenu(ItemHolder itemHolder) {
        itemHolder.popupMenu.setOnClickListener(v -> {
            int pos = itemHolder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            final Album album = (Album) searchResults.get(pos);
            final PopupMenu menu = new PopupMenu(mContext, v);
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
                    AlbumSongLoader.getSongsForAlbum(mContext, album.id)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> {
                                long[] ids = new long[songs.size()];
                                for (int i = 0; i < songs.size(); i++) {
                                    ids[i] = songs.get(i).id;
                                }
                                if (ids.length == 1) {
                                    ListenerUtil.showDeleteDialog(mContext, songs.get(0).title, ids,
                                            (dialog, which) -> {
                                                int delPos = itemHolder.getBindingAdapterPosition();
                                                if (delPos != RecyclerView.NO_POSITION) {
                                                    searchResults.remove(delPos);
                                                    notifyItemRemoved(delPos);
                                                }
                                            });
                                } else {
                                    String songCount = ListenerUtil.makeLabel(mContext,
                                            R.plurals.n_songs, album.songCount);
                                    ListenerUtil.showDeleteDialog(mContext, songCount, ids,
                                            (dialog, which) -> {
                                                int delPos = itemHolder.getBindingAdapterPosition();
                                                if (delPos != RecyclerView.NO_POSITION) {
                                                    searchResults.remove(delPos);
                                                    notifyItemRemoved(delPos);
                                                }
                                            });
                                }
                            });
                }
                return false;
            });
            menu.inflate(R.menu.popup_album);
            menu.show();
        });
    }

    private void setArtistPopupMenu(ItemHolder itemHolder) {
        itemHolder.popupMenu.setOnClickListener(v -> {
            int pos = itemHolder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            final Artist artist = (Artist) searchResults.get(pos);
            final PopupMenu menu = new PopupMenu(mContext, v);
            menu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.popup_artist_addto_queue) {
                    getSongListIdByArtist(artist.id)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(ids -> MusicPlayer.addToQueue(mContext, ids, -1, ListenerUtil.IdType.NA));
                } else if (itemId == R.id.popup_artist_addto_playlist) {
                    getSongListIdByArtist(artist.id)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(ids -> ListenerUtil.showAddPlaylistDialog(mContext, ids));
                } else if (itemId == R.id.popup_artist_delete) {
                    ArtistSongLoader.getSongsForArtist(mContext, artist.id)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> {
                                long[] ids = new long[songs.size()];
                                for (int i = 0; i < songs.size(); i++) {
                                    ids[i] = songs.get(i).id;
                                }
                                if (ids.length == 1) {
                                    ListenerUtil.showDeleteDialog(mContext, songs.get(0).title, ids,
                                            (dialog, which) -> {
                                                int delPos = itemHolder.getBindingAdapterPosition();
                                                if (delPos != RecyclerView.NO_POSITION) {
                                                    searchResults.remove(delPos);
                                                    notifyItemRemoved(delPos);
                                                }
                                            });
                                } else {
                                    String songCount = ListenerUtil.makeLabel(mContext,
                                            R.plurals.n_songs, artist.songCount);
                                    ListenerUtil.showDeleteDialog(mContext, songCount, ids,
                                            (dialog, which) -> {
                                                int delPos = itemHolder.getBindingAdapterPosition();
                                                if (delPos != RecyclerView.NO_POSITION) {
                                                    searchResults.remove(delPos);
                                                    notifyItemRemoved(delPos);
                                                }
                                            });
                                }
                            });
                }
                return false;
            });
            menu.inflate(R.menu.popup_artist);
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

    private Observable<long[]> getSongListIdByArtist(long id) {
        return ArtistSongLoader.getSongsForArtist(mContext, id)
                .map(songs -> {
                    long[] ids = new long[songs.size()];
                    for (int i = 0; i < songs.size(); i++) {
                        ids[i] = songs.get(i).id;
                    }
                    return ids;
                });
    }

    @Override
    public int getItemViewType(int position) {
        if (searchResults.get(position) instanceof Song)
            return 0;
        if (searchResults.get(position) instanceof Album)
            return 1;
        if (searchResults.get(position) instanceof Artist)
            return 2;
        if (searchResults.get(position) instanceof String)
            return 10;
        return 3;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateSearchResults(List<?> searchResults) {
        this.searchResults = searchResults;
        notifyDataSetChanged();
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ImageView image;
        private final TextView title;
        private final TextView subtitle1;
        private final TextView subtitle2;
        private final ImageView popupMenu;

        private final TextView sectionHeader;

        public ItemHolder(View view) {
            super(view);

            this.image = view.findViewById(R.id.image);
            this.title = view.findViewById(R.id.text_item_title);
            this.subtitle1 = view.findViewById(R.id.text_item_subtitle);
            this.subtitle2 = view.findViewById(R.id.text_item_subtitle_2);
            this.popupMenu = view.findViewById(R.id.popup_menu);

            this.sectionHeader = view.findViewById(R.id.section_header);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            switch (getItemViewType()) {
                case 0:
                    Song song = (Song) searchResults.get(pos);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        long[] ret = new long[]{song.id};
                        MusicPlayer.playAll(mContext, ret, 0, -1, ListenerUtil.IdType.NA, false);
                    }, 100);
                    break;
                case 1:
                    Album album = (Album) searchResults.get(pos);
                    NavigationUtil.navigateToAlbum(mContext, album.id, album.title,
                            new Pair<>(image, "transition_album_art" + pos));
                    break;
                case 2:
                    Artist artist = (Artist) searchResults.get(pos);
                    NavigationUtil.navigateToArtist(mContext, artist.id, artist.name,
                            new Pair<>(image, "transition_artist_art" + pos));
                    break;
                case 3:
                case 10:
                default:
                    break;
            }
        }

    }
}