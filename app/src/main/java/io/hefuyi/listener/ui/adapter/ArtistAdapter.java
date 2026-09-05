package io.hefuyi.listener.ui.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
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
import com.google.gson.Gson;

import java.util.List;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.ListenerApp;
import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.dataloader.ArtistSongLoader;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.ArtistInfoComponent;
import io.hefuyi.listener.injector.component.DaggerArtistInfoComponent;
import io.hefuyi.listener.injector.module.ArtistInfoModule;
import io.hefuyi.listener.mvp.model.Artist;
import io.hefuyi.listener.mvp.model.ArtistArt;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ColorUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.NavigationUtil;
import io.hefuyi.listener.util.PreferencesUtility;
import io.hefuyi.listener.widget.fastscroller.FastScrollRecyclerView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ItemHolder> implements FastScrollRecyclerView.SectionedAdapter {

    private final Activity mContext;
    private final boolean isGrid;
    private List<Artist> arraylist;
    private String action;

    @SuppressWarnings("unused")
    public ArtistAdapter(Activity context, List<Artist> arraylist) {
        this.arraylist = arraylist;
        this.mContext = context;
        this.isGrid = PreferencesUtility.getInstance(mContext).isArtistsInGrid();
        injectDependencies(context);
    }

    public ArtistAdapter(Activity context, String action) {
        this.mContext = context;
        this.isGrid = PreferencesUtility.getInstance(mContext).isArtistsInGrid();
        this.action = action;
        injectDependencies(context);
    }

    private void injectDependencies(Activity context) {
        ApplicationComponent applicationComponent = ((ListenerApp) context.getApplication()).getApplicationComponent();
        ArtistInfoComponent artistInfoComponent = DaggerArtistInfoComponent.builder()
                .applicationComponent(applicationComponent)
                .artistInfoModule(new ArtistInfoModule())
                .build();
        artistInfoComponent.injectForAdapter(this);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setArtistList(List<Artist> arraylist) {
        this.arraylist = arraylist;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        int layoutRes = isGrid ? R.layout.item_list_grid_layout_item : R.layout.item_list_linear_layout_item;
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(layoutRes, viewGroup, false);
        return new ItemHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ItemHolder itemHolder, int position) {
        final Artist localItem = arraylist.get(position);

        itemHolder.name.setText(localItem.name);
        itemHolder.albumCount.setText(ListenerUtil.makeLabel(mContext, R.plurals.n_albums, localItem.albumCount));
        itemHolder.songCount.setText(ListenerUtil.makeLabel(mContext, R.plurals.n_songs, localItem.songCount));

        String artistArtJson = PreferencesUtility.getInstance(mContext).getArtistArt(localItem.id);
        if (TextUtils.isEmpty(artistArtJson)) {
            loadArtistArt(null, itemHolder);
        } else {
            ArtistArt artistArt = new Gson().fromJson(artistArtJson, ArtistArt.class);
            loadArtistArt(artistArt, itemHolder);
        }

        itemHolder.artistImage.setTransitionName("transition_artist_art" + itemHolder.getLayoutPosition());

        setOnPopupMenuListener(itemHolder);

    }

    private void loadArtistArt(ArtistArt artistArt, final ItemHolder itemHolder) {
        if (artistArt == null) {
            itemHolder.artistImage.setImageDrawable(ATEUtil.getDefaultSingerDrawable(mContext));
            itemHolder.name.setTextColor(ATEUtil.getThemeTextColorPrimary(mContext));
            itemHolder.albumCount.setTextColor(ATEUtil.getThemeTextColorSecondly(mContext));
            itemHolder.songCount.setTextColor(ATEUtil.getThemeTextColorSecondly(mContext));
            itemHolder.popupMenu.setColorFilter(ATEUtil.getThemeTextColorSecondly(mContext));
            itemHolder.footer.setBackgroundColor(ATEUtil.getThemeAlbumDefaultPaletteColor(mContext));
            return;
        }
        if (isGrid) {
            Glide.with(mContext)
                    .load(artistArt.getExtralarge())
                    .asBitmap()
                    .placeholder(ATEUtil.getDefaultSingerDrawable(mContext))
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            itemHolder.artistImage.setImageDrawable(ATEUtil.getDefaultSingerDrawable(mContext));
                            itemHolder.name.setTextColor(ATEUtil.getThemeTextColorPrimary(mContext));
                            itemHolder.albumCount.setTextColor(ATEUtil.getThemeTextColorSecondly(mContext));
                            itemHolder.songCount.setTextColor(ATEUtil.getThemeTextColorSecondly(mContext));
                            itemHolder.popupMenu.setColorFilter(ATEUtil.getThemeTextColorSecondly(mContext));
                            itemHolder.footer.setBackgroundColor(ATEUtil.getThemeAlbumDefaultPaletteColor(mContext));
                        }

                        @Override
                        public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            new Palette.Builder(resource).generate(palette -> {
                                Palette.Swatch swatch = ColorUtil.getMostPopulousSwatch(palette);
                                if (swatch != null) {
                                    int color = swatch.getRgb();
                                    itemHolder.footer.setBackgroundColor(color);

                                    int detailColor = swatch.getTitleTextColor();
                                    itemHolder.artistImage.setImageBitmap(resource);
                                    itemHolder.name.setTextColor(ColorUtil.getOpaqueColor(detailColor));
                                    itemHolder.albumCount.setTextColor(detailColor);
                                    itemHolder.songCount.setTextColor(detailColor);
                                    itemHolder.popupMenu.setColorFilter(detailColor);
                                }
                            });
                        }
                    });
        } else {
            Glide.with(mContext)
                    .load(artistArt.getLarge())
                    .placeholder(ATEUtil.getDefaultSingerDrawable(mContext))
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .error(ATEUtil.getDefaultSingerDrawable(mContext))
                    .into(itemHolder.artistImage);
        }
    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    @SuppressWarnings("unused")
    public void updateDataSet(List<Artist> arrayList) {
        this.arraylist = arrayList;
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
            int adapterPosition = itemHolder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;
            final Artist artist = arraylist.get(adapterPosition);
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
                    switch (action) {
                        case Constants.NAVIGATE_PLAYLIST_FAVORITE:
                            getSongListIdByArtist(artist.id)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(ids -> ListenerUtil.showDeleteFromFavorite(mContext, ids));
                            break;
                        case Constants.NAVIGATE_PLAYLIST_RECENTPLAY:
                            getSongListIdByArtist(artist.id)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(ids -> ListenerUtil.showDeleteFromRecentlyPlay(mContext, ids));
                            break;
                        default:
                            ArtistSongLoader.getSongsForArtist(mContext, artist.id)
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
                                                    R.plurals.n_songs, artist.songCount);
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
            menu.inflate(R.menu.popup_artist);
            menu.show();
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

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView name;
        private final TextView albumCount;
        private final TextView songCount;
        private final ImageView artistImage;
        private final ImageView popupMenu;
        private final View footer;

        public ItemHolder(View view) {
            super(view);
            this.name = view.findViewById(R.id.text_item_title);
            this.albumCount = view.findViewById(R.id.text_item_subtitle);
            this.songCount = view.findViewById(R.id.text_item_subtitle_2);
            this.artistImage = view.findViewById(R.id.image);
            this.popupMenu = view.findViewById(R.id.popup_menu);
            this.footer = view.findViewById(R.id.footer);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Artist artist = arraylist.get(pos);
            NavigationUtil.navigateToArtist(mContext, artist.id, artist.name,
                    new Pair<>(artistImage, "transition_artist_art" + pos));
        }

    }
}