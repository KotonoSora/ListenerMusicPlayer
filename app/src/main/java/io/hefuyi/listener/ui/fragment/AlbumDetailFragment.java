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
import io.hefuyi.listener.event.MetaChangedEvent;
import io.hefuyi.listener.injector.component.AlbumSongsComponent;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.DaggerAlbumSongsComponent;
import io.hefuyi.listener.injector.module.AlbumSongsModel;
import io.hefuyi.listener.mvp.contract.AlbumDetailContract;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.ui.adapter.AlbumSongsAdapter;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ColorUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.PreferencesUtility;
import io.hefuyi.listener.util.SortOrder;
import io.hefuyi.listener.widget.DividerItemDecoration;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 */
public class AlbumDetailFragment extends Fragment implements AlbumDetailContract.View {

    @Inject
    AlbumDetailContract.Presenter mPresenter;
    Toolbar toolbar;
    CollapsingToolbarLayout collapsingToolbarLayout;
    AppBarLayout appBarLayout;
    FloatingActionButton fabPlay;
    ImageView albumArt;
    RecyclerView recyclerView;

    private PreferencesUtility mPreferences;
    private Context context;
    private AlbumSongsAdapter mAdapter;
    private long albumID = -1;
    private String albumName;
    private int primaryColor = -1;

    public static AlbumDetailFragment newInstance(long id, String name, boolean useTransition, String transitionName) {
        AlbumDetailFragment fragment = new AlbumDetailFragment();
        Bundle args = new Bundle();
        args.putLong(Constants.ALBUM_ID, id);
        args.putString(Constants.ALBUM_NAME, name);
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
            albumID = args.getLong(Constants.ALBUM_ID);
            albumName = args.getString(Constants.ALBUM_NAME);
        }
        context = requireActivity();
        mPreferences = PreferencesUtility.getInstance(context);
        mAdapter = new AlbumSongsAdapter(requireActivity(), albumID);
    }

    private void injectDependencies() {
        ApplicationComponent applicationComponent = ((ListenerApp) requireActivity().getApplication()).getApplicationComponent();
        AlbumSongsComponent albumSongsComponent = DaggerAlbumSongsComponent.builder()
                .applicationComponent(applicationComponent)
                .albumSongsModel(new AlbumSongsModel())
                .build();
        albumSongsComponent.inject(this);
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

        albumArt = view.findViewById(R.id.album_art);
        toolbar = view.findViewById(R.id.toolbar);
        collapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar);
        appBarLayout = view.findViewById(R.id.app_bar);
        fabPlay = view.findViewById(R.id.fab_play);
        recyclerView = view.findViewById(R.id.recyclerview);

        ATE.apply(this, ATEUtil.getATEKey(context));

        Bundle args = getArguments();
        if (args != null && args.getBoolean("transition")) {
            albumArt.setTransitionName(args.getString("transition_name"));
        }

        fabPlay.setOnClickListener(v -> onFabPlayClick());

        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL_LIST, false));

        ListenerUtil.applyBottomInsetWithPlayer(recyclerView);

        setupToolbar();
        setupMenu();

        mPresenter.subscribe(albumID);
        subscribeMetaChangedEvent();
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.album_song_sort_by, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.menu_sort_by_az) {
                    mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_A_Z);
                    mPresenter.loadAlbumSongs(albumID);
                    return true;
                } else if (itemId == R.id.menu_sort_by_za) {
                    mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_Z_A);
                    mPresenter.loadAlbumSongs(albumID);
                    return true;
                } else if (itemId == R.id.menu_sort_by_year) {
                    mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_YEAR);
                    mPresenter.loadAlbumSongs(albumID);
                    return true;
                } else if (itemId == R.id.menu_sort_by_duration) {
                    mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_DURATION);
                    mPresenter.loadAlbumSongs(albumID);
                    return true;
                } else if (itemId == R.id.menu_sort_by_track_number) {
                    mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
                    mPresenter.loadAlbumSongs(albumID);
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
        ActionBar ab = activity.getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        collapsingToolbarLayout.setTitle(albumName);
    }

    @Override
    public void showAlbumSongs(List<Song> songList) {
        mAdapter.setSongList(songList);
    }

    @Override
    public void showAlbumArt(Bitmap bitmap) {
        albumArt.setImageBitmap(bitmap);
        if (ATEUtil.isDarkTheme(getActivity())) {
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

    @Override
    public void showAlbumArt(Drawable drawable) {
        albumArt.setImageDrawable(drawable);
        primaryColor = ATEUtil.getThemePrimaryColor(getContext());
        collapsingToolbarLayout.setContentScrimColor(primaryColor);
        collapsingToolbarLayout.setStatusBarScrimColor(ColorUtil.getStatusBarColor(primaryColor));
    }

    public void onFabPlayClick() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            AlbumSongsAdapter adapter = (AlbumSongsAdapter) recyclerView.getAdapter();
            if (adapter != null) {
                MusicPlayer.playAll(requireActivity(), adapter.getSongIds(), 0, albumID, ListenerUtil.IdType.Album, false);
            }
        }, 150);
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
}
