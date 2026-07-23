package io.hefuyi.listener.ui.activity;

import static io.hefuyi.listener.MusicPlayer.mService;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.afollestad.appthemeengine.ATEActivity;

import java.lang.ref.WeakReference;

import io.hefuyi.listener.IListenerService;
import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.MusicService;
import io.hefuyi.listener.R;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.event.MetaChangedEvent;
import io.hefuyi.listener.ui.fragment.QuickControlsFragment;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.NavigationUtil;

/**
 * Created by hefuyi on 2016/11/7.
 */

public class BaseActivity extends ATEActivity implements ServiceConnection {

    private MusicPlayer.ServiceToken mToken;
    private PlaybackStatus mPlaybackStatus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        getWindow().setNavigationBarContrastEnforced(false);

        mToken = MusicPlayer.bindToService(this, this);
        mPlaybackStatus = new PlaybackStatus(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicService.PLAYSTATE_CHANGED);
        // Track changes
        filter.addAction(MusicService.META_CHANGED);
        // Update a list, probably the playlist fragment's
        filter.addAction(MusicService.REFRESH);
        // If a playlist has changed, notify us
        filter.addAction(MusicService.PLAYLIST_CHANGED);
        // If there is an error playing a track
        filter.addAction(MusicService.TRACK_ERROR);

        ContextCompat.registerReceiver(this, mPlaybackStatus, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IListenerService.Stub.asInterface(service);
        onServiceConnected();
    }

    public void onServiceConnected() {
        MetaChangedEvent metaChangedEvent = new MetaChangedEvent(MusicPlayer.getCurrentAudioId(),
                MusicPlayer.getTrackName(), MusicPlayer.getArtistName());
        RxBus.getInstance().post(metaChangedEvent);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        if (mToken != null) {
            MusicPlayer.unbindFromService(mToken);
            mToken = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (!ListenerUtil.hasEffectsPanel(BaseActivity.this)) {
            menu.removeItem(R.id.action_equalizer);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (itemId == R.id.action_settings) {
            NavigationUtil.navigateToSettings(this);
            return true;
        } else if (itemId == R.id.action_equalizer) {
            NavigationUtil.navigateToEqualizer(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public String getATEKey() {
        return ATEUtil.getATEKey(this);
    }

    public void initializeQuickControls() {
        QuickControlsFragment fragment1 = new QuickControlsFragment();
        FragmentManager fragmentManager1 = getSupportFragmentManager();
        fragmentManager1.beginTransaction()
                .replace(R.id.quickcontrols_container, fragment1).commitAllowingStateLoss();
    }

    public void applySystemBarPadding(View view, boolean top, boolean bottom) {
        ListenerUtil.applySystemBarPadding(view, top, bottom);
    }

    public void applyBottomInsetWithPlayer(View view) {
        ListenerUtil.applyBottomInsetWithPlayer(view);
    }

    public void applyBottomInsetWithPlayerAndIme(View view) {
        ListenerUtil.applyBottomInsetWithPlayerAndIme(view);
    }

    public void applySystemBarPaddingAndHeight(View view, boolean top, boolean bottom) {
        ListenerUtil.applySystemBarPaddingAndHeight(view, top, bottom);
    }

    private final static class PlaybackStatus extends BroadcastReceiver {

        private final WeakReference<BaseActivity> mReference;


        public PlaybackStatus(final BaseActivity activity) {
            mReference = new WeakReference<BaseActivity>(activity);
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            BaseActivity baseActivity = mReference.get();
            if (baseActivity != null) {
                if (action.equals(MusicService.TRACK_ERROR)) {
                    final String errorMsg = context.getString(R.string.error_playing_track);
                    Toast.makeText(baseActivity, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
