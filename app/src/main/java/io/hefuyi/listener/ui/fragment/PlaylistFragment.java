package io.hefuyi.listener.ui.fragment;


import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.appthemeengine.ATE;

import java.util.List;

import javax.inject.Inject;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.ListenerApp;
import io.hefuyi.listener.R;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.event.PlaylistUpdateEvent;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.DaggerPlaylistComponent;
import io.hefuyi.listener.injector.component.PlaylistComponent;
import io.hefuyi.listener.injector.module.PlaylistModule;
import io.hefuyi.listener.mvp.contract.PlaylistContract;
import io.hefuyi.listener.mvp.model.Playlist;
import io.hefuyi.listener.ui.adapter.PlaylistAdapter;
import io.hefuyi.listener.ui.dialogs.CreatePlaylistDialog;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.PreferencesUtility;
import io.hefuyi.listener.widget.DividerItemDecoration;
import io.hefuyi.listener.widget.fastscroller.FastScrollRecyclerView;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class PlaylistFragment extends Fragment implements PlaylistContract.View {

    @Inject
    PlaylistContract.Presenter mPresenter;
    FastScrollRecyclerView recyclerView;
    View emptyView;
    Toolbar toolbar;
    private PlaylistAdapter mAdapter;
    private GridLayoutManager layoutManager;
    private RecyclerView.ItemDecoration itemDecoration;
    private PreferencesUtility mPreferences;
    private boolean isGrid;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies();
        mPresenter.attachView(this);

        mPreferences = PreferencesUtility.getInstance(requireActivity());
        isGrid = mPreferences.getPlaylistView() == Constants.PLAYLIST_VIEW_GRID;
        mAdapter = new PlaylistAdapter(this, null);
        if (isGrid) {
            layoutManager = new GridLayoutManager(requireActivity(), 2);
        } else {
            layoutManager = new GridLayoutManager(requireActivity(), 1);
        }
    }

    private void injectDependencies() {
        ApplicationComponent applicationComponent = ((ListenerApp) requireActivity().getApplication()).getApplicationComponent();
        PlaylistComponent playlistComponent = DaggerPlaylistComponent.builder()
                .applicationComponent(applicationComponent)
                .playlistModule(new PlaylistModule())
                .build();
        playlistComponent.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerview);
        emptyView = view.findViewById(R.id.view_empty);
        toolbar = view.findViewById(R.id.toolbar);

        ATE.apply(this, ATEUtil.getATEKey(requireActivity()));

        ListenerUtil.applySystemBarPaddingAndHeight(toolbar, true, false);

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(toolbar);

        final ActionBar ab = activity.getSupportActionBar();
        if (ab != null) {
            ab.setHomeAsUpIndicator(R.drawable.ic_menu);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(R.string.playlists);
        }

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);
        setItemDecoration();

        ListenerUtil.applyBottomInsetWithPlayer(recyclerView);

        setupMenu();

        mPresenter.subscribe();
        subscribePlaylistUpdateEvent();
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_playlist, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.action_new_playlist) {
                    CreatePlaylistDialog.newInstance().show(getChildFragmentManager(), "CREATE_PLAYLIST");
                    return true;
                } else if (itemId == R.id.menu_show_as_list) {
                    if (isGrid) {
                        mPreferences.setPlaylistView(Constants.PLAYLIST_VIEW_LIST);
                        isGrid = false;
                        updateLayoutManager(1);
                    }
                    return true;
                } else if (itemId == R.id.menu_show_as_grid) {
                    if (!isGrid) {
                        mPreferences.setPlaylistView(Constants.PLAYLIST_VIEW_GRID);
                        isGrid = true;
                        updateLayoutManager(2);
                    }
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
    public void showPlaylist(List<Playlist> playlists) {
        emptyView.setVisibility(View.GONE);
        mAdapter.setPlaylist(playlists);
    }

    @Override
    public void showEmptyView() {
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void setItemDecoration() {
        if (isGrid) {
            int spacingInPixels = requireContext().getResources().getDimensionPixelSize(R.dimen.spacing_card_album_grid);
            itemDecoration = new SpacesItemDecoration(spacingInPixels);
        } else {
            itemDecoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL_LIST, false);
        }
        recyclerView.addItemDecoration(itemDecoration);
    }

    private void updateLayoutManager(int column) {
        recyclerView.removeItemDecoration(itemDecoration);
        mAdapter = new PlaylistAdapter(this, null);
        recyclerView.setAdapter(mAdapter);
        layoutManager.setSpanCount(column);
        layoutManager.requestLayout();
        setItemDecoration();
        mPresenter.loadPlaylist();
    }

    private void subscribePlaylistUpdateEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(PlaylistUpdateEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> mPresenter.loadPlaylist(), throwable -> {
                });
        RxBus.getInstance().addSubscription(this, subscription);
    }

    public static class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position % 2 == 0) {
                outRect.left = 0;
                outRect.top = space;
                outRect.right = space / 2;
            } else {
                outRect.left = space / 2;
                outRect.top = space;
                outRect.right = 0;
            }
        }
    }
}
