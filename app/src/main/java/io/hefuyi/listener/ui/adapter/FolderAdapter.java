package io.hefuyi.listener.ui.adapter;

import android.annotation.SuppressLint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.dataloader.SongLoader;
import io.hefuyi.listener.event.MediaUpdateEvent;
import io.hefuyi.listener.mvp.model.FolderInfo;
import io.hefuyi.listener.util.DensityUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.NavigationUtil;
import io.hefuyi.listener.widget.fastscroller.FastScrollRecyclerView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class FolderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FastScrollRecyclerView.SectionedAdapter {

    private final AppCompatActivity mContext;
    private List<FolderInfo> arraylist;

    public FolderAdapter(AppCompatActivity context, List<FolderInfo> arraylist) {
        this.arraylist = (arraylist != null) ? arraylist : new ArrayList<>();
        this.mContext = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list_linear_layout_item, viewGroup, false);
        return new ItemHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemHolder itemHolder = (ItemHolder) holder;
        FolderInfo localItem = arraylist.get(position);
        Drawable image = ContextCompat.getDrawable(mContext, R.drawable.ic_folder_black_48dp);
        if (image != null) {
            image.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(mContext, R.color.folderTint), PorterDuff.Mode.SRC_IN));
            itemHolder.image.setImageDrawable(image);
        }
        itemHolder.folderName.setText(localItem.folderName);
        itemHolder.songCount.setText(ListenerUtil.makeLabel(mContext, R.plurals.n_songs, localItem.songCount));
        itemHolder.folderPath.setText(localItem.folderPath);
        itemHolder.folderPath.setMaxWidth(DensityUtil.dip2px(mContext, 240));
        setOnPopupMenuListener(itemHolder);
    }

    @Override
    public int getItemCount() {
        return (null != arraylist ? arraylist.size() : 0);
    }

    private void setOnPopupMenuListener(final ItemHolder itemHolder) {
        itemHolder.popupMenu.setOnClickListener(v -> {
            final PopupMenu menu = new PopupMenu(mContext, v);
            int adapterPosition = itemHolder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;
            final FolderInfo folderInfo = arraylist.get(adapterPosition);
            menu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.popup_folder_addto_queue) {
                    getSongListIdByFolder(folderInfo.folderPath)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(ids -> MusicPlayer.addToQueue(mContext, ids, -1, ListenerUtil.IdType.Folder));
                } else if (itemId == R.id.popup_folder_addto_playlist) {
                    getSongListIdByFolder(folderInfo.folderPath)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(ids -> ListenerUtil.showAddPlaylistDialog(mContext, ids));
                } else if (itemId == R.id.popup_folder_delete) {
                    new MaterialDialog.Builder(mContext)
                            .title(mContext.getResources().getString(R.string.delete_folder))
                            .content(mContext.getResources().getString(R.string.delete_folder_confirmation, folderInfo.songCount))
                            .positiveText(R.string.delete)
                            .negativeText(R.string.cancel)
                            .onPositive((dialog, which) -> getSongListIdByFolder(folderInfo.folderPath)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(ids -> {
                                        ListenerUtil.deleteTracks(mContext, ids);
                                        RxBus.getInstance().post(new MediaUpdateEvent());
                                    }))
                            .onNegative((dialog, which) -> dialog.dismiss())
                            .show();
                }
                return false;
            });
            menu.inflate(R.menu.popup_folder);
            menu.show();
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setFolderList(List<FolderInfo> arraylist) {
        this.arraylist = arraylist;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        char ch = arraylist.get(position).folderName.charAt(0);
        if (Character.isDigit(ch)) {
            return "#";
        } else
            return Character.toString(ch);
    }

    private Observable<long[]> getSongListIdByFolder(String path) {
        return SongLoader.getSongListInFolder(mContext, path)
                .map(songs -> {
                    long[] ids = new long[songs.size()];
                    for (int i = 0; i < songs.size(); i++) {
                        ids[i] = songs.get(i).id;
                    }
                    return ids;
                });
    }

    public class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ImageView image;
        private final TextView folderName;
        private final TextView songCount;
        private final TextView folderPath;
        private final ImageView popupMenu;

        public ItemHolder(View view) {
            super(view);
            this.image = view.findViewById(R.id.image);
            this.folderName = view.findViewById(R.id.text_item_title);
            this.songCount = view.findViewById(R.id.text_item_subtitle);
            this.folderPath = view.findViewById(R.id.text_item_subtitle_2);
            this.popupMenu = view.findViewById(R.id.popup_menu);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            NavigationUtil.navigateToFolderSongs(mContext, arraylist.get(pos).folderPath);
        }
    }
}