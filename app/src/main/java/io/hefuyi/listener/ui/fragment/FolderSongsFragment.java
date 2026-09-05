package io.hefuyi.listener.ui.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
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
import io.hefuyi.listener.injector.component.DaggerFolderSongsComponent;
import io.hefuyi.listener.injector.component.FolderSongsComponent;
import io.hefuyi.listener.injector.module.FolderSongsModule;
import io.hefuyi.listener.mvp.contract.FolderSongsContract;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.ui.adapter.SongsListAdapter;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.widget.DividerItemDecoration;
import io.hefuyi.listener.widget.fastscroller.FastScrollRecyclerView;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 */
public class FolderSongsFragment extends Fragment implements FolderSongsContract.View {

    @Inject
    FolderSongsContract.Presenter mPresenter;
    FastScrollRecyclerView recyclerView;
    Toolbar toolbar;
    private SongsListAdapter mAdapter;
    private String path;

    public static FolderSongsFragment newInstance(String path) {

        Bundle args = new Bundle();
        args.putString(Constants.FOLDER_PATH, path);

        FolderSongsFragment fragment = new FolderSongsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies();
        mPresenter.attachView(this);

        if (getArguments() != null) {
            path = getArguments().getString(Constants.FOLDER_PATH);
        }

        mAdapter = new SongsListAdapter((AppCompatActivity) requireActivity(), null, Constants.NAVIGATE_ALLSONG, true);
    }

    private void injectDependencies() {
        ApplicationComponent applicationComponent = ((ListenerApp) requireActivity().getApplication()).getApplicationComponent();
        FolderSongsComponent folderSongsComponent = DaggerFolderSongsComponent.builder()
                .applicationComponent(applicationComponent)
                .folderSongsModule(new FolderSongsModule())
                .build();
        folderSongsComponent.inject(this);
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
        toolbar = view.findViewById(R.id.toolbar);

        ATE.apply(this, ATEUtil.getATEKey(requireActivity()));

        ListenerUtil.applySystemBarPaddingAndHeight(toolbar, true, false);

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(toolbar);

        final ActionBar ab = activity.getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(R.string.folders);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL_LIST, true));

        ListenerUtil.applyBottomInsetWithPlayer(recyclerView);

        mPresenter.loadSongs(path);
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
        mAdapter.setSongList(songList);
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
