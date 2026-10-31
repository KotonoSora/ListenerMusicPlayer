package io.hefuyi.listener.receiver;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import androidx.annotation.NonNull;

import androidx.legacy.content.WakefulBroadcastReceiver;

import io.hefuyi.listener.MusicService;
import io.hefuyi.listener.ui.activity.MainActivity;


/**
 * Used to control headset playback.
 * Single press: pause/resume
 * Double press: next track
 * Triple press: previous track
 * Long press: voice search
 */
//在WakefulBroadcastReceiver中调用startWakefulService来启动MusicService在后台播放音乐,
// 这样service具有wake_lock权限,并且wake_lock由WakefulBroadcastReceiver自动管理
@SuppressWarnings("deprecation")
public class MediaButtonIntentReceiver extends WakefulBroadcastReceiver {
    private static final boolean DEBUG = false;
    private static final String TAG = "ButtonIntentReceiver";

    private static final int MSG_LONG_PRESS_TIMEOUT = 1;
    private static final int MSG_HEADSET_DOUBLE_CLICK_TIMEOUT = 2;

    private static final int LONG_PRESS_DELAY = 1000;
    private static final int DOUBLE_CLICK = 800;

    private static WakeLock mWakeLock = null;
    private static int mClickCounter = 0;
    private static long mLastClickTime = 0;
    private static boolean mDown = false;
    private static boolean mLaunched = false;

    /**
     * 启动MusicService,并拥有wake_lock权限
     *
     * @param context Context
     * @param command Command string
     */
    private static void startService(Context context, String command) {
        final Intent i = new Intent(context, MusicService.class);
        i.setAction(MusicService.SERVICECMD);
        i.putExtra(MusicService.CMDNAME, command);
        i.putExtra(MusicService.FROM_MEDIA_BUTTON, true);
        startWakefulService(context, i);
    }

    private static void acquireWakeLockAndSendMessage(Context context, Message msg, long delay) {
        if (mWakeLock == null) {
            Context appContext = context.getApplicationContext();
            PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "listener:headset_button");
            mWakeLock.setReferenceCounted(false); //设置无论请求多少次wake lock,都只需一次释放
        }
        if (DEBUG) Log.v(TAG, "Acquiring wake lock and sending " + msg.what);
        // Make sure we don't indefinitely hold the wake lock under any circumstances
        mWakeLock.acquire(10000); //防止无期限hold住wake lock

        mHandler.sendMessageDelayed(msg, delay);
    }

    /**
     * 如果handler的消息队列中没有待处理消息,就释放receiver hold住的wake lock
     */
    private static void releaseWakeLockIfHandlerIdle() {
        if (mHandler.hasMessages(MSG_LONG_PRESS_TIMEOUT)
                || mHandler.hasMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)) {
            if (DEBUG) Log.v(TAG, "Handler still has messages pending, not releasing wake lock");
            return;
        }

        if (mWakeLock != null) {
            if (DEBUG) Log.v(TAG, "Releasing wake lock");
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String intentAction = intent.getAction();
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) { //当耳机拔出时暂停播放
            startService(context, MusicService.CMDPAUSE);
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) { //耳机按钮事件
            final KeyEvent event;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.class);
            } else {
                event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            }
            if (event == null) {
                return;
            }

            final int keycode = event.getKeyCode();
            final int action = event.getAction();
            final long eventTime = event.getEventTime();

            String command = null;
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    command = MusicService.CMDSTOP;
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    command = MusicService.CMDTOGGLEPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    command = MusicService.CMDNEXT;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    command = MusicService.CMDPREVIOUS;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    command = MusicService.CMDPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    command = MusicService.CMDPLAY;
                    break;
            }
            if (command != null) {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mDown) {
                        if (MusicService.CMDTOGGLEPAUSE.equals(command)
                                || MusicService.CMDPLAY.equals(command)) {
                            if (mLastClickTime != 0
                                    && eventTime - mLastClickTime > LONG_PRESS_DELAY) {
                                acquireWakeLockAndSendMessage(context,
                                        mHandler.obtainMessage(MSG_LONG_PRESS_TIMEOUT, context), 0);
                            }
                        }
                    } else if (event.getRepeatCount() == 0) {

                        if (keycode == KeyEvent.KEYCODE_HEADSETHOOK) {
                            if (eventTime - mLastClickTime >= DOUBLE_CLICK) {
                                mClickCounter = 0;
                            }

                            mClickCounter++;
                            if (DEBUG) Log.v(TAG, "Got headset click, count = " + mClickCounter);
                            mHandler.removeMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT);

                            Message msg = mHandler.obtainMessage(
                                    MSG_HEADSET_DOUBLE_CLICK_TIMEOUT, mClickCounter, 0, context);

                            long delay = mClickCounter < 3 ? DOUBLE_CLICK : 0;
                            if (mClickCounter >= 3) {
                                mClickCounter = 0;
                            }
                            mLastClickTime = eventTime;
                            acquireWakeLockAndSendMessage(context, msg, delay);
                        } else {
                            startService(context, command);
                        }
                        mLaunched = false;
                        mDown = true;
                    }
                } else {
                    mHandler.removeMessages(MSG_LONG_PRESS_TIMEOUT);
                    mDown = false;
                }
                if (isOrderedBroadcast()) {
                    abortBroadcast();
                }
                releaseWakeLockIfHandlerIdle();
            }
        }
    }

    private static final Handler mHandler = new Handler(Looper.getMainLooper()) {

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleMessage(@NonNull final Message msg) {
            switch (msg.what) {
                case MSG_LONG_PRESS_TIMEOUT:
                    if (DEBUG) Log.v(TAG, "Handling long press timeout, launched " + mLaunched);
                    if (!mLaunched) {
                        final Context context = (Context) msg.obj;
                        final Intent i = new Intent();
                        i.setClass(context, MainActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(i);
                        mLaunched = true;
                    }
                    break;

                case MSG_HEADSET_DOUBLE_CLICK_TIMEOUT: //双击时间阈值内
                    final int clickCount = msg.arg1;
                    final String command;

                    if (DEBUG) Log.v(TAG, "Handling headset click, count = " + clickCount);
                    switch (clickCount) {
                        case 1:
                            command = MusicService.CMDTOGGLEPAUSE;
                            break;
                        case 2:
                            command = MusicService.CMDNEXT;
                            break;
                        case 3:
                            command = MusicService.CMDPREVIOUS;
                            break;
                        default:
                            command = null;
                            break;
                    }

                    if (command != null) {
                        final Context context = (Context) msg.obj;
                        startService(context, command);
                    }
                    break;
            }
            releaseWakeLockIfHandlerIdle();
        }
    };


}
