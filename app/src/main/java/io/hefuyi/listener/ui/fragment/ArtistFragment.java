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
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.appthemeengine.ATE;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.ListenerApp;
import io.hefuyi.listener.R;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.event.FavoriteSongEvent;
import io.hefuyi.listener.event.MediaUpdateEvent;
import io.hefuyi.listener.event.RecentlyPlayEvent;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.ArtistComponent;
import io.hefuyi.listener.injector.component.DaggerArtistComponent;
import io.hefuyi.listener.injector.module.ActivityModule;
import io.hefuyi.listener.injector.module.ArtistsModule;
import io.hefuyi.listener.mvp.contract.ArtistContract;
import io.hefuyi.listener.mvp.model.Artist;
import io.hefuyi.listener.ui.adapter.ArtistAdapter;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.PreferencesUtility;
import io.hefuyi.listener.util.SortOrder;
import io.hefuyi.listener.widget.DividerItemDecoration;
import io.hefuyi.listener.widget.fastscroller.FastScrollRecyclerView;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 */
public class ArtistFragment extends Fragment implements ArtistContract.View {

    @Inject
    ArtistContract.Presenter mPresenter;
    FastScrollRecyclerView recyclerView;
    View emptyView;
    private ArtistAdapter mAdapter;
    private GridLayoutManager layoutManager;
    private RecyclerView.ItemDecoration itemDecoration;
    private PreferencesUtility mPreferences;
    private boolean isGrid;
    private String action;

    public static ArtistFragment newInstance(String action) {

        Bundle args = new Bundle();
        switch (action) {
            case Constants.NAVIGATE_ALLSONG:
            case Constants.NAVIGATE_PLAYLIST_RECENTADD:
            case Constants.NAVIGATE_PLAYLIST_RECENTPLAY:
            case Constants.NAVIGATE_PLAYLIST_FAVORITE:
                args.putString(Constants.PLAYLIST_TYPE, action);
                break;
            default:
                throw new RuntimeException("wrong action type");
        }
        ArtistFragment fragment = new ArtistFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies();
        mPresenter.attachView(this);
        mPreferences = PreferencesUtility.getInstance(requireActivity());
        isGrid = mPreferences.isArtistsInGrid();
        if (isGrid) {
            layoutManager = new GridLayoutManager(requireActivity(), 2);
        } else {
            layoutManager = new GridLayoutManager(requireActivity(), 1);
        }

        if (getArguments() != null) {
            action = getArguments().getString(Constants.PLAYLIST_TYPE);
        }
        mAdapter = new ArtistAdapter(requireActivity(), action);
    }

    private void injectDependencies() {
        ApplicationComponent applicationComponent = ((ListenerApp) requireActivity().getApplication()).getApplicationComponent();
        ArtistComponent artistComponent = DaggerArtistComponent.builder()
                .applicationComponent(applicationComponent)
                .activityModule(new ActivityModule(requireActivity()))
                .artistsModule(new ArtistsModule())
                .build();
        artistComponent.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recyclerview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerview);
        emptyView = view.findViewById(R.id.view_empty);

        ATE.apply(this, ATEUtil.getATEKey(requireActivity()));

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);
        setItemDecoration();

        ListenerUtil.applyBottomInsetWithPlayer(recyclerView);

        mPresenter.loadArtists(action);
        setupMenu();

        if (Constants.NAVIGATE_PLAYLIST_FAVORITE.equals(action)) {
            subscribeFavoriteSongEvent();
        } else if (Constants.NAVIGATE_PLAYLIST_RECENTPLAY.equals(action)) {
            subscribeRecentlyPlayEvent();
        } else {
            subscribeMediaUpdateEvent();
        }
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_show_as, menu);
                if (Constants.NAVIGATE_ALLSONG.equals(action)) {
                    menuInflater.inflate(R.menu.artist_sort_by, menu);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.menu_sort_by_az) {
                    mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_A_Z);
                    mPresenter.loadArtists(action);
                    return true;
                } else if (itemId == R.id.menu_sort_by_za) {
                    mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_Z_A);
                    mPresenter.loadArtists(action);
                    return true;
                } else if (itemId == R.id.menu_sort_by_number_of_songs) {
                    mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS);
                    mPresenter.loadArtists(action);
                    return true;
                } else if (itemId == R.id.menu_sort_by_number_of_albums) {
                    mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS);
                    mPresenter.loadArtists(action);
                    return true;
                } else if (itemId == R.id.menu_show_as_list) {
                    if (isGrid) {
                        mPreferences.setArtistsInGrid(false);
                        isGrid = false;
                        updateLayoutManager(1);
                    }
                    return true;
                } else if (itemId == R.id.menu_show_as_grid) {
                    if (!isGrid) {
                        mPreferences.setArtistsInGrid(true);
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
    public void showArtists(List<Artist> artists) {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        mAdapter.setArtistList(artists);
    }

    @Override
    public void showEmptyView() {
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);
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
        mAdapter = new ArtistAdapter(requireActivity(), action);
        recyclerView.setAdapter(mAdapter);
        layoutManager.setSpanCount(column);
        layoutManager.requestLayout();
        setItemDecoration();
        mPresenter.loadArtists(action);
    }

    private void subscribeMediaUpdateEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(MediaUpdateEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .debounce(1, TimeUnit.SECONDS)
                .subscribe(event -> mPresenter.loadArtists(action), throwable -> {
                });
        RxBus.getInstance().addSubscription(this, subscription);
    }

    private void subscribeFavoriteSongEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(FavoriteSongEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> mPresenter.loadArtists(action), throwable -> {
                });
        RxBus.getInstance().addSubscription(this, subscription);
    }

    private void subscribeRecentlyPlayEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(RecentlyPlayEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> mPresenter.loadArtists(action), throwable -> {
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
