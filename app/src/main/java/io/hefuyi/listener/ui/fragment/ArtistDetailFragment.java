package io.hefuyi.listener.ui.fragment;


import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;

import com.afollestad.appthemeengine.ATE;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import javax.inject.Inject;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.ListenerApp;
import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.ArtistInfoComponent;
import io.hefuyi.listener.injector.component.DaggerArtistInfoComponent;
import io.hefuyi.listener.injector.module.ArtistInfoModule;
import io.hefuyi.listener.mvp.contract.ArtistDetailContract;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ColorUtil;
import io.hefuyi.listener.util.ListenerUtil;

/**
 * A simple {@link Fragment} subclass.
 */
public class ArtistDetailFragment extends Fragment implements ArtistDetailContract.View {

    @Inject
    ArtistDetailContract.Presenter mPresenter;
    ImageView artistArt;
    Toolbar toolbar;
    CollapsingToolbarLayout collapsingToolbarLayout;
    AppBarLayout appBarLayout;
    FloatingActionButton fabPlay;

    private ArtistMusicFragment mArtistMusicFragment;
    private long artistID = -1;
    private String artistName = "";
    private int primaryColor;

    public static ArtistDetailFragment newInstance(long id, String name, boolean useTransition, String transitionName) {
        ArtistDetailFragment fragment = new ArtistDetailFragment();
        Bundle args = new Bundle();
        args.putLong(Constants.ARTIST_ID, id);
        args.putString(Constants.ARTIST_NAME, name);
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
            artistID = args.getLong(Constants.ARTIST_ID);
            artistName = args.getString(Constants.ARTIST_NAME);
        }

    }

    private void injectDependencies() {
        ApplicationComponent applicationComponent = ((ListenerApp) requireActivity().getApplication()).getApplicationComponent();
        ArtistInfoComponent artistInfoComponent = DaggerArtistInfoComponent.builder()
                .applicationComponent(applicationComponent)
                .artistInfoModule(new ArtistInfoModule())
                .build();
        artistInfoComponent.injectForFragment(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_artist_detail, container, false);
        toolbar = root.findViewById(R.id.toolbar);
        ListenerUtil.applySystemBarPaddingAndHeight(toolbar, true, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        artistArt = view.findViewById(R.id.artist_art);
        toolbar = view.findViewById(R.id.toolbar);
        collapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar);
        appBarLayout = view.findViewById(R.id.app_bar);
        fabPlay = view.findViewById(R.id.fab_play);

        ATE.apply(this, ATEUtil.getATEKey(requireActivity()));

        Bundle args = getArguments();
        if (args != null && args.getBoolean("transition")) {
            artistArt.setTransitionName(args.getString("transition_name"));
        }

        fabPlay.setOnClickListener(v -> onFabPlayClick());

        setupToolbar();

        mArtistMusicFragment = ArtistMusicFragment.newInstance(artistID);
        getChildFragmentManager().beginTransaction().replace(R.id.container, mArtistMusicFragment).commit();
        mPresenter.subscribe(artistID);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPresenter.unsubscribe();
    }

    private void setupToolbar() {
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(toolbar);
        ActionBar ab = activity.getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        collapsingToolbarLayout.setTitle(artistName);
    }

    @Override
    public void showArtistArt(Bitmap bitmap) {
        artistArt.setImageBitmap(bitmap);
        if (ATEUtil.isDarkTheme(requireActivity())) {
            primaryColor = ATEUtil.getThemePrimaryColor(requireContext());
            collapsingToolbarLayout.setContentScrimColor(primaryColor);
            collapsingToolbarLayout.setStatusBarScrimColor(ColorUtil.getStatusBarColor(primaryColor));
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
    public void showArtistArt(Drawable drawable) {
        artistArt.setImageDrawable(drawable);
        primaryColor = ATEUtil.getThemePrimaryColor(requireContext());
        collapsingToolbarLayout.setContentScrimColor(primaryColor);
        collapsingToolbarLayout.setStatusBarScrimColor(ColorUtil.getStatusBarColor(primaryColor));
    }

    public void onFabPlayClick() {
        if (mArtistMusicFragment != null && mArtistMusicFragment.mSongAdapter != null) {
            MusicPlayer.playAll(requireActivity(), mArtistMusicFragment.mSongAdapter.getSongIds(), 0, artistID, ListenerUtil.IdType.Artist, false);
        }
    }
}
