package io.hefuyi.listener.ui.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.afollestad.appthemeengine.ATE;

import java.util.List;

import javax.inject.Inject;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.ListenerApp;
import io.hefuyi.listener.R;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.event.MetaChangedEvent;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.DaggerPlayRankingComponent;
import io.hefuyi.listener.injector.component.PlayRankingComponent;
import io.hefuyi.listener.injector.module.PlayRankingModule;
import io.hefuyi.listener.mvp.contract.PlayRankingContract;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.ui.adapter.SongsListAdapter;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.widget.fastscroller.FastScrollRecyclerView;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlayRankingFragment extends Fragment implements PlayRankingContract.View {

    @Inject
    PlayRankingContract.Presenter mPresenter;
    FastScrollRecyclerView recyclerView;
    View emptyView;
    Toolbar toolbar;
    private SongsListAdapter mAdapter;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependences();
        mPresenter.attachView(this);

        mAdapter = new SongsListAdapter((AppCompatActivity) getActivity(), null, Constants.NAVIGATE_ALLSONG, true);
    }

    private void injectDependences() {
        ApplicationComponent applicationComponent = ((ListenerApp) getActivity().getApplication()).getApplicationComponent();
        PlayRankingComponent playRankingComponent = DaggerPlayRankingComponent.builder()
                .applicationComponent(applicationComponent)
                .playRankingModule(new PlayRankingModule())
                .build();
        playRankingComponent.inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list_layout, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerview);
        emptyView = view.findViewById(R.id.view_empty);
        toolbar = view.findViewById(R.id.toolbar);

        ATE.apply(this, ATEUtil.getATEKey(getActivity()));

        io.hefuyi.listener.util.ListenerUtil.applySystemBarPaddingAndHeight(toolbar, true, false);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        final ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(R.string.play_ranking);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mAdapter);

        io.hefuyi.listener.util.ListenerUtil.applyBottomInsetWithPlayer(recyclerView);

        mPresenter.subscribe();
        subscribeMetaChangedEvent();
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPresenter.unsubscribe();
        RxBus.getInstance().unSubscribe(this);
    }

    @Override
    public void showRanking(List<Song> songList) {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        mAdapter.setSongList(songList);
    }

    @Override
    public void showEmptyView() {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    private void subscribeMetaChangedEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(MetaChangedEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .subscribe(new Action1<MetaChangedEvent>() {
                    @Override
                    public void call(MetaChangedEvent event) {
                        mAdapter.notifyDataSetChanged();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                    }
                });
        RxBus.getInstance().addSubscription(this, subscription);
    }
}
