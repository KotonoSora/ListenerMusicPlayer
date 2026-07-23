package io.hefuyi.listener.ui.activity;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.afollestad.appthemeengine.customizers.ATEActivityThemeCustomizer;
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.HashMap;
import java.util.Map;

import io.hefuyi.listener.Constants;
import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.event.MetaChangedEvent;
import io.hefuyi.listener.listener.PanelSlideListener;
import io.hefuyi.listener.permission.PermissionCallback;
import io.hefuyi.listener.permission.PermissionManager;
import io.hefuyi.listener.ui.fragment.AlbumDetailFragment;
import io.hefuyi.listener.ui.fragment.ArtistDetailFragment;
import io.hefuyi.listener.ui.fragment.FoldersFragment;
import io.hefuyi.listener.ui.fragment.MainFragment;
import io.hefuyi.listener.ui.fragment.PlayRankingFragment;
import io.hefuyi.listener.ui.fragment.PlaylistFragment;
import io.hefuyi.listener.ui.fragment.SearchFragment;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ListenerUtil;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends BaseActivity implements ATEActivityThemeCustomizer {

    private final Map<String, Runnable> navigationMap = new HashMap<String, Runnable>();
    private final Handler navDrawerRunnable = new Handler();
    private final Runnable navigateSearch = new Runnable() {
        public void run() {
            Fragment fragment = new SearchFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.hide(getSupportFragmentManager().findFragmentById(R.id.fragment_container));
            transaction.add(R.id.fragment_container, fragment);
            transaction.addToBackStack(null).commit();
        }
    };
    private final Runnable navigateSetting = new Runnable() {
        public void run() {
            final Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            MainActivity.this.startActivity(intent);
        }
    };
    private final Runnable navigateAlbum = new Runnable() {
        public void run() {
            long albumID = getIntent().getExtras().getLong(Constants.ALBUM_ID);
            String albumName = getIntent().getExtras().getString(Constants.ALBUM_NAME);
            Fragment fragment = AlbumDetailFragment.newInstance(albumID, albumName, false, null);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment).commit();
        }
    };
    private final Runnable navigateArtist = new Runnable() {
        public void run() {
            long artistID = getIntent().getExtras().getLong(Constants.ARTIST_ID);
            String artistName = getIntent().getExtras().getString(Constants.ARTIST_NAME);
            Fragment fragment = ArtistDetailFragment.newInstance(artistID, artistName, false, null);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment).commit();
        }
    };
    SlidingUpPanelLayout panelLayout;
    NavigationView navigationView;
    private final Runnable navigateLibrary = new Runnable() {
        public void run() {
            navigationView.getMenu().findItem(R.id.nav_library).setChecked(true);
            Fragment fragment = MainFragment.newInstance(Constants.NAVIGATE_ALLSONG);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment).commitAllowingStateLoss();

        }
    };
    private final Runnable navigatePlaylist = new Runnable() {
        public void run() {
            navigationView.getMenu().findItem(R.id.nav_playlists).setChecked(true);
            Fragment fragment = new PlaylistFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.hide(getSupportFragmentManager().findFragmentById(R.id.fragment_container));
            transaction.replace(R.id.fragment_container, fragment).commit();

        }
    };
    private final Runnable navigateFavorite = new Runnable() {
        public void run() {
            navigationView.getMenu().findItem(R.id.nav_favorite).setChecked(true);
            Fragment fragment = MainFragment.newInstance(Constants.NAVIGATE_PLAYLIST_FAVORITE);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment).commit();
        }
    };
    private final Runnable navigateFolders = new Runnable() {
        public void run() {
            navigationView.getMenu().findItem(R.id.nav_folders).setChecked(true);
            Fragment fragment = new FoldersFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.hide(getSupportFragmentManager().findFragmentById(R.id.fragment_container));
            transaction.replace(R.id.fragment_container, fragment).commit();

        }
    };
    private final Runnable navigateRecentPlay = new Runnable() {
        public void run() {
            navigationView.getMenu().findItem(R.id.nav_recent_play).setChecked(true);
            Fragment fragment = MainFragment.newInstance(Constants.NAVIGATE_PLAYLIST_RECENTPLAY);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment).commit();
        }
    };
    private final Runnable navigateRecentAdd = new Runnable() {
        public void run() {
            navigationView.getMenu().findItem(R.id.nav_recent_add).setChecked(true);
            Fragment fragment = MainFragment.newInstance(Constants.NAVIGATE_PLAYLIST_RECENTADD);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment).commit();
        }
    };
    private final Runnable navigatePlayRanking = new Runnable() {
        public void run() {
            navigationView.getMenu().findItem(R.id.nav_play_ranking).setChecked(true);
            Fragment fragment = new PlayRankingFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment).commit();
        }
    };
    DrawerLayout mDrawerLayout;
    private TextView songtitle;
    private TextView songartist;
    private ImageView albumart;
    private String action;
    private final PermissionCallback permissionReadstorageCallback = new PermissionCallback() {
        @Override
        public void permissionGranted() {
            loadEverything();
        }

        @Override
        public void permissionRefused() {
            finish();
        }
    };
    private Runnable runnable;
    private PanelSlideListener mPanelSlideListener;
    private boolean listenerSeted = false;
    private boolean isDarkTheme;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        action = getIntent().getAction();

        isDarkTheme = ATEUtil.getATEKey(this).equals("dark_theme");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        panelLayout = findViewById(R.id.sliding_layout);
        navigationView = findViewById(R.id.nav_view);

        navigationMap.put(Constants.NAVIGATE_LIBRARY, navigateLibrary);
        navigationMap.put(Constants.NAVIGATE_ALBUM, navigateAlbum);
        navigationMap.put(Constants.NAVIGATE_ARTIST, navigateArtist);
        navigationMap.put(Constants.NAVIGATE_PLAYLIST_FAVORITE, navigateFavorite);

        View header = navigationView.inflateHeaderView(R.layout.nav_header);
        albumart = header.findViewById(R.id.album_art);
        songtitle = header.findViewById(R.id.song_title);
        songartist = header.findViewById(R.id.song_artist);

        ViewCompat.setOnApplyWindowInsetsListener(mDrawerLayout, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            // Header: increase height and pad internal text if needed,
            // but alignParentBottom handles it if height is increased.
            ViewGroup.LayoutParams lp = header.getLayoutParams();
            if (lp != null) {
                // The base height of the header is 208dp
                lp.height = (int) (getResources().getDisplayMetrics().density * 208) + top;
                header.setLayoutParams(lp);
            }
            header.setPadding(0, top, 0, 0);

            int basePanelHeight = getResources().getDimensionPixelSize(R.dimen.sliding_up_header);
            panelLayout.setPanelHeight(basePanelHeight + bottom);

            navigationView.setPadding(0, 0, 0, bottom);

            return insets;
        });

        navDrawerRunnable.postDelayed(new Runnable() {
            @Override
            public void run() {
                setupDrawerContent(navigationView);
                setupNavigationIcons(navigationView);
            }
        }, 700);


        checkPermissionAndThenLoad();

        addBackstackListener();

        if (Intent.ACTION_VIEW.equals(action)) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    MusicPlayer.clearQueue();
                    MusicPlayer.openFile(getIntent().getData().getPath());
                    MusicPlayer.playOrPause();
                }
            }, 350);
        }
        subscribeMetaChangedEvent();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (panelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    panelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                } else if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                } else if (!isNavigatingMain()) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                } else {
                    finish();
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navDrawerRunnable.removeCallbacksAndMessages(null);
        RxBus.getInstance().unSubscribe(this);
        if (mPanelSlideListener != null) {
            RxBus.getInstance().unSubscribe(mPanelSlideListener);
        }
    }

    private void loadEverything() {
        Runnable navigation = navigationMap.get(action);
        if (navigation != null) {
            navigation.run();
        } else {
            navigateLibrary.run();
        }

        initializeQuickControls();
    }

    private void checkPermissionAndThenLoad() {
        //check for permission
        String[] requiredPermissions = ListenerUtil.getRequiredPermissions();
        if (PermissionManager.hasPermission(this, requiredPermissions)) {
            loadEverything();
        } else {
            String storagePermission = ListenerUtil.getStoragePermission();
            String rationale = getString(R.string.permission_rationale_storage);
            if (ListenerUtil.isTiramisu()) {
                rationale = getString(R.string.permission_rationale_tiramisu);
            }

            if (PermissionManager.shouldShowRequestPermissionRationale(this, storagePermission)) {
                Snackbar.make(panelLayout, rationale,
                                Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                PermissionManager.askForPermission(MainActivity.this, requiredPermissions, permissionReadstorageCallback);
                            }
                        }).show();
            } else {
                PermissionManager.askForPermission(this, requiredPermissions, permissionReadstorageCallback);
            }
        }
    }

    /**
     * 监听menu点击
     *
     * @param navigationView
     */
    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(final MenuItem menuItem) {
                        updatePosition(menuItem);
                        return true;

                    }
                });
    }

    /**
     * 设置图标
     *
     * @param navigationView
     */
    private void setupNavigationIcons(NavigationView navigationView) {

        //material-icon-lib currently doesn't work with navigationview of design support library 22.2.0+
        //set icons manually for now
        //https://github.com/code-mc/material-icon-lib/issues/15

        if (!isDarkTheme) {
            navigationView.getMenu().findItem(R.id.nav_library).setIcon(R.drawable.ic_music_note_black_48dp);
            navigationView.getMenu().findItem(R.id.nav_playlists).setIcon(R.drawable.ic_queue_music_black_48dp);
            navigationView.getMenu().findItem(R.id.nav_folders).setIcon(R.drawable.ic_folder_black_48dp);
            navigationView.getMenu().findItem(R.id.nav_favorite).setIcon(R.drawable.ic_favorite_black_48dp);
            navigationView.getMenu().findItem(R.id.nav_recent_play).setIcon(R.drawable.ic_watch_later_black_48dp);
            navigationView.getMenu().findItem(R.id.nav_recent_add).setIcon(R.drawable.ic_add_box_black_48dp);
            navigationView.getMenu().findItem(R.id.nav_play_ranking).setIcon(R.drawable.ic_sort_black_48dp);
            navigationView.getMenu().findItem(R.id.nav_settings).setIcon(R.drawable.ic_settings_black_48dp);
            navigationView.getMenu().findItem(R.id.nav_exit).setIcon(R.drawable.ic_exit_to_app_black_48dp);
        } else {
            navigationView.getMenu().findItem(R.id.nav_library).setIcon(R.drawable.ic_music_note_white_48dp);
            navigationView.getMenu().findItem(R.id.nav_playlists).setIcon(R.drawable.ic_queue_music_white_48dp);
            navigationView.getMenu().findItem(R.id.nav_folders).setIcon(R.drawable.ic_folder_white_48dp);
            navigationView.getMenu().findItem(R.id.nav_favorite).setIcon(R.drawable.ic_favorite_white_48dp);
            navigationView.getMenu().findItem(R.id.nav_recent_play).setIcon(R.drawable.ic_watch_later_white_48dp);
            navigationView.getMenu().findItem(R.id.nav_recent_add).setIcon(R.drawable.ic_add_box_white_48dp);
            navigationView.getMenu().findItem(R.id.nav_play_ranking).setIcon(R.drawable.ic_sort_white_48dp);
            navigationView.getMenu().findItem(R.id.nav_settings).setIcon(R.drawable.ic_settings_white_48dp);
            navigationView.getMenu().findItem(R.id.nav_exit).setIcon(R.drawable.ic_exit_to_app_white_48dp);
        }

    }

    /**
     * 导航
     *
     * @param menuItem
     */
    private void updatePosition(final MenuItem menuItem) {
        runnable = null;

        int itemId = menuItem.getItemId();
        if (itemId == R.id.nav_library) {
            runnable = navigateLibrary;
        } else if (itemId == R.id.nav_playlists) {
            runnable = navigatePlaylist;
        } else if (itemId == R.id.nav_folders) {
            runnable = navigateFolders;
        } else if (itemId == R.id.nav_favorite) {
            runnable = navigateFavorite;
        } else if (itemId == R.id.nav_recent_play) {
            runnable = navigateRecentPlay;
        } else if (itemId == R.id.nav_recent_add) {
            runnable = navigateRecentAdd;
        } else if (itemId == R.id.nav_play_ranking) {
            runnable = navigatePlayRanking;
        } else if (itemId == R.id.nav_settings) {
            runnable = navigateSetting;
        } else if (itemId == R.id.nav_exit) {
            this.finish();
        }

        if (runnable != null) {
            mDrawerLayout.closeDrawers();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }, 350);
        }
    }

    /**
     * 设置导航栏header部分信息
     */
    private void setDetailsToHeader() {
        String name = MusicPlayer.getTrackName();
        String artist = MusicPlayer.getArtistName();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(artist)) {
            songtitle.setText(R.string.app_name);
            songartist.setText("");
            Drawable defaultHeader = ContextCompat.getDrawable(this, R.drawable.icon_drawer_theme_bg);
            defaultHeader.setColorFilter(ATEUtil.getThemePrimaryColor(this), PorterDuff.Mode.DARKEN);
            albumart.setImageDrawable(defaultHeader);
            return;
        }

        songtitle.setText(name);
        songartist.setText(artist);

        Drawable defaultHeader = ContextCompat.getDrawable(this, R.drawable.icon_drawer_theme_bg);
        defaultHeader.setColorFilter(ATEUtil.getThemePrimaryColor(this), PorterDuff.Mode.DARKEN);

        Glide.with(this).load(ListenerUtil.getAlbumArtUri(MusicPlayer.getCurrentAlbumId()))
                .error(defaultHeader)
                .centerCrop()
                .into(albumart);
    }

    private boolean isNavigatingMain() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        return (currentFragment instanceof MainFragment || currentFragment instanceof PlaylistFragment
                || currentFragment instanceof PlayRankingFragment || currentFragment instanceof FoldersFragment);
    }

    private void addBackstackListener() {
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                getSupportFragmentManager().findFragmentById(R.id.fragment_container).onResume();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            if (panelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                panelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            } else if (isNavigatingMain()) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            } else {
                getOnBackPressedDispatcher().onBackPressed();
            }
            return true;
        } else if (itemId == R.id.action_search) {
            navigateSearch.run();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!listenerSeted && panelLayout.findViewById(R.id.topContainer) != null) {
            mPanelSlideListener = new PanelSlideListener(panelLayout);
            panelLayout.addPanelSlideListener(mPanelSlideListener);
            listenerSeted = true;
        }
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
                        setDetailsToHeader();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                    }
                });
        RxBus.getInstance().addSubscription(this, subscription);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        PermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        setDetailsToHeader();
    }

    @Override
    public int getActivityTheme() {
        return isDarkTheme ? R.style.AppThemeDark : R.style.AppThemeLight;
    }

}
