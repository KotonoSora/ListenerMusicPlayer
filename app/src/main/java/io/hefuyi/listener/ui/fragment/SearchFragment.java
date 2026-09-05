package io.hefuyi.listener.ui.fragment;


import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.appthemeengine.ATE;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.hefuyi.listener.ListenerApp;
import io.hefuyi.listener.R;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.event.MediaUpdateEvent;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.DaggerSearchComponent;
import io.hefuyi.listener.injector.component.SearchComponent;
import io.hefuyi.listener.injector.module.SearchModule;
import io.hefuyi.listener.mvp.contract.SearchContract;
import io.hefuyi.listener.provider.SearchHistory;
import io.hefuyi.listener.ui.adapter.SearchAdapter;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ListenerUtil;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 */
public class SearchFragment extends Fragment implements SearchView.OnQueryTextListener, View.OnTouchListener, SearchContract.View {
    private final List<Object> searchResults = Collections.emptyList();
    @Inject
    SearchContract.Presenter mPresenter;
    Toolbar toolbar;
    RecyclerView recyclerView;
    ViewStub emptyView;
    private SearchView mSearchView;
    private InputMethodManager mImm;
    private String queryString;
    private SearchAdapter adapter;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies();
        mPresenter.attachView(this);

        mImm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        adapter = new SearchAdapter(requireActivity());
    }

    private void injectDependencies() {
        ApplicationComponent applicationComponent = ((ListenerApp) requireActivity().getApplication()).getApplicationComponent();
        SearchComponent searchComponent = DaggerSearchComponent.builder()
                .applicationComponent(applicationComponent)
                .searchModule(new SearchModule())
                .build();
        searchComponent.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_layout, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbar = view.findViewById(R.id.toolbar);
        recyclerView = view.findViewById(R.id.recyclerview);
        emptyView = view.findViewById(R.id.view_empty);

        ATE.apply(this, ATEUtil.getATEKey(requireActivity()));

        ListenerUtil.applySystemBarPaddingAndHeight(toolbar, true, false);

        ListenerUtil.applyBottomInsetWithPlayerAndIme(recyclerView);

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(toolbar);
        final ActionBar ab = activity.getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        requireActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        recyclerView.setOnTouchListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        setupMenu();
        subscribeMediaUpdateEvent();
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.menu_search, menu);
                MenuItem searchItem = menu.findItem(R.id.menu_search);
                if (searchItem != null) {
                    mSearchView = (SearchView) searchItem.getActionView();

                    if (mSearchView != null) {
                        mSearchView.setOnQueryTextListener(SearchFragment.this);
                        mSearchView.setQueryHint(getString(R.string.search_library));
                        mSearchView.setIconifiedByDefault(false);
                        mSearchView.setIconified(false);
                    }

                    searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                        @Override
                        public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                            return true;
                        }

                        @Override
                        public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            return true;
                        }
                    });

                    searchItem.expandActionView();
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        }, getViewLifecycleOwner());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideInputManager();
        mPresenter.unsubscribe();
        RxBus.getInstance().unSubscribe(this);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        onQueryTextChange(query);
        hideInputManager();

        return true;
    }

    @SuppressWarnings("notifyDataSetChanged")
    @Override
    public boolean onQueryTextChange(final String newText) {

        if (newText.equals(queryString)) {
            return true;
        }

        queryString = newText;

        if (queryString.trim().isEmpty()) {
            searchResults.clear();
            adapter.updateSearchResults(searchResults);
            adapter.notifyDataSetChanged();
        } else {
            mPresenter.search(newText);
        }

        return true;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            v.performClick();
            if (mImm != null && mSearchView != null) {
                mImm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
                mSearchView.clearFocus();
            }
        }
        return false;
    }

    private void hideInputManager() {
        if (mSearchView != null) {
            if (mImm != null) {
                mImm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            }
            mSearchView.clearFocus();

            SearchHistory.getInstance(getContext()).addSearchString(queryString);
        }
    }

    @Override
    public void showSearchResult(List<Object> list) {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.updateSearchResults(list);
    }

    @Override
    public void showEmptyView() {
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.INVISIBLE);
    }

    private void subscribeMediaUpdateEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(MediaUpdateEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .debounce(1, TimeUnit.SECONDS)
                .subscribe(event -> mPresenter.search(queryString), throwable -> {
                });

        RxBus.getInstance().addSubscription(this, subscription);
    }
}
