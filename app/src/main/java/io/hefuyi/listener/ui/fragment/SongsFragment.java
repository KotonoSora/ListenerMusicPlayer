package io.hefuyi.listener.ui.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

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
import io.hefuyi.listener.event.MetaChangedEvent;
import io.hefuyi.listener.event.RecentlyPlayEvent;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.DaggerSongsComponent;
import io.hefuyi.listener.injector.component.SongsComponent;
import io.hefuyi.listener.injector.module.ActivityModule;
import io.hefuyi.listener.injector.module.SongsModule;
import io.hefuyi.listener.mvp.contract.SongsContract;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.ui.adapter.SongsListAdapter;
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
public class SongsFragment extends Fragment implements SongsContract.View {

    @Inject
    SongsContract.Presenter mPresenter;
    FastScrollRecyclerView recyclerView;
    ViewStub emptyView;
    private SongsListAdapter mAdapter;
    private PreferencesUtility mPreferences;
    private String action;

    public static SongsFragment newInstance(String action) {

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
        SongsFragment fragment = new SongsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies();
        mPresenter.attachView(this);
        mPreferences = PreferencesUtility.getInstance(requireActivity());

        if (getArguments() != null) {
            action = getArguments().getString(Constants.PLAYLIST_TYPE);
        }

        mAdapter = new SongsListAdapter((AppCompatActivity) requireActivity(), null, action, true);
    }

    public void injectDependencies() {
        ApplicationComponent applicationComponent = ((ListenerApp) requireActivity().getApplication()).getApplicationComponent();
        SongsComponent songsComponent = DaggerSongsComponent.builder()
                .applicationComponent(applicationComponent)
                .activityModule(new ActivityModule(requireActivity()))
                .songsModule(new SongsModule())
                .build();
        songsComponent.inject(this);
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

        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL_LIST, true));

        ListenerUtil.applyBottomInsetWithPlayer(recyclerView);

        setupMenu();

        mPresenter.loadSongs(action);

        if (Constants.NAVIGATE_PLAYLIST_FAVORITE.equals(action)) {
            subscribeFavoriteSongEvent();
        } else if (Constants.NAVIGATE_PLAYLIST_RECENTPLAY.equals(action)) {
            subscribeRecentlyPlayEvent();
        } else {
            subscribeMediaUpdateEvent();
        }
        subscribeMetaChangedEvent();
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.song_sort_by, menu);
                if (!Constants.NAVIGATE_ALLSONG.equals(action)) {
                    MenuItem sortItem = menu.findItem(R.id.menu_sort_by);
                    if (sortItem != null) {
                        sortItem.setVisible(false);
                    }
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.menu_sort_by_az) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_A_Z);
                    mPresenter.loadSongs(action);
                    return true;
                } else if (itemId == R.id.menu_sort_by_za) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_Z_A);
                    mPresenter.loadSongs(action);
                    return true;
                } else if (itemId == R.id.menu_sort_by_add) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_DATE);
                    mPresenter.loadSongs(action);
                    return true;
                } else if (itemId == R.id.menu_sort_by_artist) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ARTIST);
                    mPresenter.loadSongs(action);
                    return true;
                } else if (itemId == R.id.menu_sort_by_album) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ALBUM);
                    mPresenter.loadSongs(action);
                    return true;
                } else if (itemId == R.id.menu_sort_by_duration) {
                    mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_DURATION);
                    mPresenter.loadSongs(action);
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
    public void showSongs(List<Song> songList) {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        mAdapter.setSongList(songList);
    }

    @Override
    public void showEmptyView() {
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);
    }

    private void subscribeFavoriteSongEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(FavoriteSongEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> mPresenter.loadSongs(action), throwable -> {
                });
        RxBus.getInstance().addSubscription(this, subscription);
    }

    private void subscribeRecentlyPlayEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(RecentlyPlayEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> mPresenter.loadSongs(action), throwable -> {
                });
        RxBus.getInstance().addSubscription(this, subscription);
    }

    private void subscribeMediaUpdateEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(MediaUpdateEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .debounce(1, TimeUnit.SECONDS)
                .subscribe(event -> mPresenter.loadSongs(action), throwable -> {
                });
        RxBus.getInstance().addSubscription(this, subscription);
    }

}
