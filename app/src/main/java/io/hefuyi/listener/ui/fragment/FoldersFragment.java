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

import io.hefuyi.listener.ListenerApp;
import io.hefuyi.listener.R;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.event.MetaChangedEvent;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.DaggerFoldersComponent;
import io.hefuyi.listener.injector.component.FoldersComponent;
import io.hefuyi.listener.injector.module.FolderModule;
import io.hefuyi.listener.mvp.contract.FoldersContract;
import io.hefuyi.listener.mvp.model.FolderInfo;
import io.hefuyi.listener.ui.adapter.FolderAdapter;
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
public class FoldersFragment extends Fragment implements FoldersContract.View {

    @Inject
    FoldersContract.Presenter mPresenter;
    FastScrollRecyclerView recyclerView;
    View emptyView;
    Toolbar toolbar;
    private FolderAdapter mAdapter;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies();
        mPresenter.attachView(this);

        mAdapter = new FolderAdapter((AppCompatActivity) requireActivity(), null);
    }

    private void injectDependencies() {
        ApplicationComponent applicationComponent = ((ListenerApp) requireActivity().getApplication()).getApplicationComponent();
        FoldersComponent foldersComponent = DaggerFoldersComponent.builder()
                .applicationComponent(applicationComponent)
                .folderModule(new FolderModule())
                .build();
        foldersComponent.inject(this);
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
            ab.setTitle(R.string.folders);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL_LIST, false));

        ListenerUtil.applyBottomInsetWithPlayer(recyclerView);

        mPresenter.subscribe();
        subscribeMetaChangedEvent();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPresenter.unsubscribe();
        RxBus.getInstance().unSubscribe(this);
    }

    @Override
    public void showEmptyView() {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    @Override
    public void showFolders(List<FolderInfo> folderInfos) {
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        mAdapter.setFolderList(folderInfos);
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
