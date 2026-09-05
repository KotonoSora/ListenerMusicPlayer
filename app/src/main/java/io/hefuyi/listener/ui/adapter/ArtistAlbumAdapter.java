package io.hefuyi.listener.ui.adapter;

import android.app.Activity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

import io.hefuyi.listener.R;
import io.hefuyi.listener.mvp.model.Album;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.NavigationUtil;

/**
 * Created by hefuyi on 2016/11/24.
 */

public class ArtistAlbumAdapter extends RecyclerView.Adapter<ArtistAlbumAdapter.ItemHolder> {

    private final List<Album> arraylist;
    private final Activity mContext;

    public ArtistAlbumAdapter(Activity context, List<Album> arraylist) {
        this.arraylist = arraylist;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_artist_album, viewGroup, false);
        return new ItemHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemHolder itemHolder, int position) {

        Album localItem = arraylist.get(position);

        itemHolder.title.setText(localItem.title);
        String songCount = ListenerUtil.makeLabel(mContext, R.plurals.n_songs, localItem.songCount);
        itemHolder.details.setText(songCount);

        Glide.with(mContext).load(ListenerUtil.getAlbumArtUri(localItem.id))
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .error(ATEUtil.getDefaultAlbumDrawable(mContext))
                .centerCrop()
                .into(itemHolder.albumArt);

        itemHolder.albumArt.setTransitionName("transition_album_art" + itemHolder.getLayoutPosition());
    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title;
        TextView details;
        ImageView albumArt;

        public ItemHolder(View view) {
            super(view);
            this.title = view.findViewById(R.id.album_title);
            this.details = view.findViewById(R.id.album_details);
            this.albumArt = view.findViewById(R.id.album_art);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Album album = arraylist.get(pos);
            NavigationUtil.navigateToAlbum(mContext, album.id,
                    album.title,
                    new Pair<>(albumArt, "transition_album_art" + pos));
        }

    }
}