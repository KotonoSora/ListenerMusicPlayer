package io.hefuyi.listener.ui.fragment;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.appthemeengine.ATE;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import javax.inject.Inject;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.ListenerApp;
import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.dataloader.PlaylistSongLoader;
import io.hefuyi.listener.event.MetaChangedEvent;
import io.hefuyi.listener.event.PlaylistUpdateEvent;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.DaggerPlaylistSongComponent;
import io.hefuyi.listener.injector.component.PlaylistSongComponent;
import io.hefuyi.listener.injector.module.PlaylistSongModule;
import io.hefuyi.listener.mvp.contract.PlaylistDetailContract;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.ui.adapter.PlaylistSongAdapter;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ColorUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.widget.DividerItemDecoration;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * A simple {@link Fragment} subclass.
 */
public class PlaylistDetailFragment extends Fragment implements PlaylistDetailContract.View {

    @Inject
    PlaylistDetailContract.Presenter mPresenter;
    Toolbar toolbar;
    CollapsingToolbarLayout collapsingToolbarLayout;
    AppBarLayout appBarLayout;
    FloatingActionButton fabPlay;
    ImageView playlistArt;
    RecyclerView recyclerView;

    private Context mContext;
    private int primaryColor = -1;
    private PlaylistSongAdapter mAdapter;
    private long playlistID = -1;
    private String playlistName;
    private long firstAlbumID = -1;

    public static PlaylistDetailFragment newInstance(long playlistID, String playlistName, long firstAlbumID, boolean useTransition, String transitionName) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putLong(Constants.PLAYLIST_ID, playlistID);
        args.putString(Constants.PLAYLIST_NAME, playlistName);
        args.putLong(Constants.FIRST_ALBUM_ID, firstAlbumID);
        args.putBoolean("transition", useTransition);
        if (useTransition)
            args.putString("transition_name", transitionName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies();
        mPresenter.attachView(this);

        Bundle args = getArguments();
        if (args != null) {
            playlistName = args.getString(Constants.PLAYLIST_NAME);
            firstAlbumID = args.getLong(Constants.FIRST_ALBUM_ID);
            playlistID = args.getLong(Constants.PLAYLIST_ID);
        }
        mContext = requireActivity();
        mAdapter = new PlaylistSongAdapter(requireContext(), playlistID, null);
    }

    private void injectDependencies() {
        ApplicationComponent applicationComponent = ((ListenerApp) requireActivity().getApplication()).getApplicationComponent();
        PlaylistSongComponent playlistSongComponent = DaggerPlaylistSongComponent.builder()
                .applicationComponent(applicationComponent)
                .playlistSongModule(new PlaylistSongModule())
                .build();
        playlistSongComponent.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_album_detail, container, false);
        toolbar = root.findViewById(R.id.toolbar);
        ListenerUtil.applySystemBarPaddingAndHeight(toolbar, true, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playlistArt = view.findViewById(R.id.album_art);
        toolbar = view.findViewById(R.id.toolbar);
        collapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar);
        appBarLayout = view.findViewById(R.id.app_bar);
        fabPlay = view.findViewById(R.id.fab_play);
        recyclerView = view.findViewById(R.id.recyclerview);

        ATE.apply(this, ATEUtil.getATEKey(requireActivity()));

        Bundle args = getArguments();
        if (args != null && args.getBoolean("transition")) {
            playlistArt.setTransitionName(args.getString("transition_name"));
        }

        fabPlay.setOnClickListener(v -> onFabPlayClick());

        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL_LIST, false));

        ListenerUtil.applyBottomInsetWithPlayer(recyclerView);

        setupToolbar();
        setupMenu();

        mPresenter.loadPlaylistSongs(playlistID);
        mPresenter.loadPlaylistArt(firstAlbumID);
        //监听歌曲删除事件,修改歌单封面
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                RxBus.getInstance().post(new PlaylistUpdateEvent());
                if (positionStart == 0) {
                    List<Song> songs = mAdapter.getSongList();
                    if (songs.isEmpty()) {
                        firstAlbumID = -1;
                    } else {
                        firstAlbumID = songs.get(0).albumId;
                    }
                    mPresenter.loadPlaylistArt(firstAlbumID);
                }
            }
        });
        subscribeMetaChangedEvent();
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.menu_playlist_detail, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.action_playlist_detail_rename) {
                    showRenamePlaylistDialog(playlistName);
                    return true;
                } else if (itemId == R.id.action_playlist_detail_addto_playlist) {
                    ListenerUtil.showAddPlaylistDialog(requireActivity(), mAdapter.getSongIds());
                    return true;
                } else if (itemId == R.id.action_playlist_detail_addto_queue) {
                    MusicPlayer.addToQueue(mContext, mAdapter.getSongIds(), -1, ListenerUtil.IdType.Playlist);
                    return true;
                } else if (itemId == R.id.action_playlist_detail_delete) {
                    showDeletePlaylistDialog();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPresenter.unsubscribe();
        RxBus.getInstance().unSubscribe(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        toolbar.setBackgroundColor(Color.TRANSPARENT);
        if (primaryColor != -1 && getActivity() != null) {
            collapsingToolbarLayout.setContentScrimColor(primaryColor);
            collapsingToolbarLayout.setStatusBarScrimColor(ColorUtil.getStatusBarColor(primaryColor));
        }
    }

    private void setupToolbar() {
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(toolbar);
        final ActionBar ab = activity.getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        collapsingToolbarLayout.setTitle(playlistName);
    }

    public void onFabPlayClick() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> MusicPlayer.playAll(requireActivity(), mAdapter.getSongIds(), 0, playlistID, ListenerUtil.IdType.Playlist, false), 150);
    }

    @SuppressWarnings("notifyDataSetChanged")
    private void subscribeMetaChangedEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(MetaChangedEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .subscribe(event -> mAdapter.notifyDataSetChanged(), throwable -> {
                });
        RxBus.getInstance().addSubscription(this, subscription);
    }

    @Override
    public void showPlaylistSongs(List<Song> songList) {
        mAdapter.setSongList(songList);
    }

    @Override
    public void showPlaylistArt(Drawable drawable) {
        if (getActivity() != null) {
            playlistArt.setImageDrawable(drawable);
        }
    }

    @Override
    public void showPlaylistArt(Bitmap bitmap) {
        playlistArt.setImageBitmap(bitmap);
        if (ATEUtil.isDarkTheme(mContext)) {
            return;
        }
        new Palette.Builder(bitmap).generate(palette -> {
            if (palette != null) {
                Palette.Swatch swatch = ColorUtil.getMostPopulousSwatch(palette);
                if (swatch != null) {
                    int color = swatch.getRgb();
                    collapsingToolbarLayout.setContentScrimColor(color);
                    collapsingToolbarLayout.setStatusBarScrimColor(ColorUtil.getStatusBarColor(color));
                    primaryColor = color;
                }
            }
        });
    }

    private void showDeletePlaylistDialog() {
        new MaterialDialog.Builder(requireActivity())
                .title(R.string.delete_playlist_song)
                .positiveText(R.string.delete)
                .negativeText(R.string.cancel)
                .onPositive((dialog, which) -> {
                    PlaylistSongLoader.removeFromPlaylist(mContext, mAdapter.getSongIds(), playlistID);
                    mPresenter.loadPlaylistSongs(playlistID);
                    showPlaylistArt(ATEUtil.getDefaultAlbumDrawable(mContext));
                    primaryColor = ATEUtil.getThemePrimaryColor(mContext);
                    collapsingToolbarLayout.setContentScrimColor(primaryColor);
                    collapsingToolbarLayout.setStatusBarScrimColor(ColorUtil.getStatusBarColor(primaryColor));
                    RxBus.getInstance().post(new PlaylistUpdateEvent());
                })
                .onNegative((dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showRenamePlaylistDialog(String oldName) {
        new MaterialDialog.Builder(requireActivity())
                .title(R.string.rename_playlist)
                .positiveText(R.string.sure)
                .negativeText(R.string.cancel)
                .input(null, oldName, false, (dialog, input) -> {
                    MusicPlayer.renamePlaylist(requireActivity(), playlistID, input.toString());
                    collapsingToolbarLayout.setTitle(input.toString());
                    playlistName = input.toString();
                    RxBus.getInstance().post(new PlaylistUpdateEvent());
                    Toast.makeText(requireActivity(), R.string.rename_playlist_success, Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
