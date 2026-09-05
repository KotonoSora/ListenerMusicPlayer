package io.hefuyi.listener.ui.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.appthemeengine.ATE;

import java.util.List;

import javax.inject.Inject;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.ListenerApp;
import io.hefuyi.listener.R;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.event.MetaChangedEvent;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.ArtistSongsComponent;
import io.hefuyi.listener.injector.component.DaggerArtistSongsComponent;
import io.hefuyi.listener.injector.module.ArtistSongModule;
import io.hefuyi.listener.mvp.contract.ArtistSongContract;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.ui.adapter.ArtistSongAdapter;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.widget.DividerItemDecoration;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 */
public class ArtistMusicFragment extends Fragment implements ArtistSongContract.View {

    @Inject
    ArtistSongContract.Presenter mPresenter;
    RecyclerView songsRecyclerview;
    ArtistSongAdapter mSongAdapter;
    private long artistID = -1;

    public static ArtistMusicFragment newInstance(long id) {
        ArtistMusicFragment fragment = new ArtistMusicFragment();
        Bundle args = new Bundle();
        args.putLong(Constants.ARTIST_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies();
        mPresenter.attachView(this);
        if (getArguments() != null) {
            artistID = getArguments().getLong(Constants.ARTIST_ID);
        }
    }

    private void injectDependencies() {
        ApplicationComponent applicationComponent = ((ListenerApp) requireActivity().getApplication()).getApplicationComponent();
        ArtistSongsComponent artistSongsComponent = DaggerArtistSongsComponent.builder()
                .applicationComponent(applicationComponent)
                .artistSongModule(new ArtistSongModule())
                .build();
        artistSongsComponent.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artist_music, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        songsRecyclerview = view.findViewById(R.id.recycler_view_songs);

        ATE.apply(this, ATEUtil.getATEKey(requireActivity()));

        songsRecyclerview.setLayoutManager(new LinearLayoutManager(requireActivity()));
        songsRecyclerview.addItemDecoration(new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL_LIST, true));

        ListenerUtil.applyBottomInsetWithPlayer(songsRecyclerview);

        mSongAdapter = new ArtistSongAdapter(requireActivity(), null, artistID);
        songsRecyclerview.setAdapter(mSongAdapter);

        mPresenter.subscribe(artistID);
        subscribeMetaChangedEvent();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPresenter.unsubscribe();
        RxBus.getInstance().unSubscribe(this);
    }


    @Override
    public void showSongs(List<Song> songList) {
//         adding one dummy song to top of arraylist
//        there will be albums header at this position in recyclerview
        songList.add(0, new Song(-1, -1, -1, "dummy", "dummy", "dummy", -1, -1));
        mSongAdapter.setSongList(songList);
    }

    @SuppressWarnings("notifyDataSetChanged")
    private void subscribeMetaChangedEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(MetaChangedEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .subscribe(event -> mSongAdapter.notifyDataSetChanged(), throwable -> {
                });
        RxBus.getInstance().addSubscription(this, subscription);
    }
}
