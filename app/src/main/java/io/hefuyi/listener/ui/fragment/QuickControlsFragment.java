package io.hefuyi.listener.ui.fragment;


import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;

import com.afollestad.appthemeengine.ATE;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import net.steamcrafted.materialiconlib.MaterialIconView;

import java.io.File;
import java.security.InvalidParameterException;

import javax.inject.Inject;

import io.hefuyi.listener.ListenerApp;
import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.R;
import io.hefuyi.listener.RxBus;
import io.hefuyi.listener.databinding.FragmentPlaybackControlsBinding;
import io.hefuyi.listener.event.FavoriteSongEvent;
import io.hefuyi.listener.event.MetaChangedEvent;
import io.hefuyi.listener.event.PlayStateChangedEvent;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.DaggerQuickControlsComponent;
import io.hefuyi.listener.injector.component.QuickControlsComponent;
import io.hefuyi.listener.injector.module.ActivityModule;
import io.hefuyi.listener.injector.module.QuickControlsModule;
import io.hefuyi.listener.listener.PaletteColorChangeListener;
import io.hefuyi.listener.mvp.contract.QuickControlsContract;
import io.hefuyi.listener.provider.FavoriteSong;
import io.hefuyi.listener.ui.dialogs.PlayqueueDialog;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ColorUtil;
import io.hefuyi.listener.util.DensityUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.util.NavigationUtil;
import io.hefuyi.listener.util.ScrimUtil;
import io.hefuyi.listener.viewmodel.MusicViewModel;
import io.hefuyi.listener.widget.ForegroundImageView;
import io.hefuyi.listener.widget.LyricView;
import io.hefuyi.listener.widget.PlayPauseView;
import io.hefuyi.listener.widget.timely.TimelyView;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * A simple {@link Fragment} subclass.
 */
public class QuickControlsFragment extends Fragment implements QuickControlsContract.View {

    private static final String TAG = "QuickControlsFragment";
    private static PaletteColorChangeListener sListener;
    private final int[] timeArr = new int[]{0, 0, 0, 0, 0};
    public View topContainer;
    @Inject
    QuickControlsContract.Presenter mPresenter;
    ProgressBar mProgress;
    PlayPauseView mPlayPauseView;
    ForegroundImageView mAlbumArt;
    MaterialIconView previous;
    MaterialIconView next;
    MaterialIconView favorite;
    MaterialIconView iconPlayQueue;
    LyricView mLyricView;
    ImageView popupMenu;
    SeekBar mSeekBar;
    TimelyView timelyView11;
    TimelyView timelyView12;
    TimelyView timelyView13;
    TimelyView timelyView14;
    TimelyView timelyView15;
    TextView hourColon;
    TextView minuteColon;
    LinearLayout songElapsedTime;
    private FragmentPlaybackControlsBinding binding;
    private MusicViewModel musicViewModel;
    private int blackWhiteColor;
    private Handler mElapsedTimeHandler;
    private final Runnable mUpdateElapsedTime = new Runnable() {
        @Override
        public void run() {
            if (getActivity() != null) {
                String time = ListenerUtil.makeShortTimeString(getActivity(), mSeekBar.getProgress() / 1000);
                if (time.length() < 5) {
                    timelyView11.setVisibility(View.GONE);
                    timelyView12.setVisibility(View.GONE);
                    hourColon.setVisibility(View.GONE);
                    tv13(time.charAt(0) - '0');
                    tv14(time.charAt(2) - '0');
                    tv15(time.charAt(3) - '0');
                } else if (time.length() == 5) {
                    timelyView12.setVisibility(View.VISIBLE);
                    tv12(time.charAt(0) - '0');
                    tv13(time.charAt(1) - '0');
                    tv14(time.charAt(3) - '0');
                    tv15(time.charAt(4) - '0');
                } else {
                    timelyView11.setVisibility(View.VISIBLE);
                    hourColon.setVisibility(View.VISIBLE);
                    tv11(time.charAt(0) - '0');
                    tv12(time.charAt(2) - '0');
                    tv13(time.charAt(3) - '0');
                    tv14(time.charAt(5) - '0');
                    tv15(time.charAt(6) - '0');
                }
                mElapsedTimeHandler.postDelayed(this, 600);
            }

        }
    };
    private PlayqueueDialog bottomDialogFragment;
    private SlidingUpPanelLayout mSlidingUpPanelLayout;
    private Palette.Swatch mSwatch;
    private boolean mIsFavorite = false;

    public static void setPaletteColorChangeListener(PaletteColorChangeListener paletteColorChangeListener) {
        sListener = paletteColorChangeListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies();
        mPresenter.attachView(this);
    }

    private void injectDependencies() {
        ApplicationComponent applicationComponent = ((ListenerApp) requireActivity().getApplication())
                .getApplicationComponent();
        QuickControlsComponent quickControlsComponent = DaggerQuickControlsComponent.builder()
                .applicationComponent(applicationComponent)
                .activityModule(new ActivityModule(requireActivity()))
                .quickControlsModule(new QuickControlsModule())
                .build();
        quickControlsComponent.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlaybackControlsBinding.inflate(inflater, container, false);
        musicViewModel = new ViewModelProvider(requireActivity()).get(MusicViewModel.class);
        binding.setVm(musicViewModel);
        binding.setLifecycleOwner(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        topContainer = binding.topContainer;
        View customToolbar = binding.customToolbar;
        ListenerUtil.applySystemBarPadding(customToolbar, true, false);
        ListenerUtil.applySystemBarPadding(view, false, true);

        mProgress = binding.songProgressNormal;
        mPlayPauseView = binding.playPause;
        mAlbumArt = binding.albumArt;
        previous = binding.previous;
        next = binding.next;
        favorite = binding.heart;
        iconPlayQueue = binding.icPlayQueue;
        mLyricView = binding.lyricView;
        popupMenu = binding.popupMenu;
        mSeekBar = binding.seekSongTouch;
        timelyView11 = binding.songElapsedTime.timelyView11;
        timelyView12 = binding.songElapsedTime.timelyView12;
        timelyView13 = binding.songElapsedTime.timelyView13;
        timelyView14 = binding.songElapsedTime.timelyView14;
        timelyView15 = binding.songElapsedTime.timelyView15;
        hourColon = binding.songElapsedTime.hourColon;
        minuteColon = binding.songElapsedTime.minuteColon;
        songElapsedTime = (LinearLayout) binding.songElapsedTime.getRoot();

        ATE.apply(this, ATEUtil.getATEKey(requireActivity()));

        mSlidingUpPanelLayout = (SlidingUpPanelLayout) binding.getRoot().getParent().getParent();

        setUpPopupMenu(popupMenu);

        mLyricView.setLineSpace(15.0f);
        mLyricView.setTextSize(17.0f);
        mLyricView.setPlayable(true);
        mLyricView.setTranslationY(DensityUtil.getScreenWidth(requireActivity()) + DensityUtil.dip2px(requireActivity(), 120));
        mLyricView.setOnPlayerClickListener((progress, content) -> {
            MusicPlayer.seek(progress);
            if (!MusicPlayer.isPlaying()) {
                mPresenter.onPlayPauseClick();
            }
        });

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mProgress.getLayoutParams();
        mProgress.measure(0, 0);
        layoutParams.setMargins(0, -(mProgress.getMeasuredHeight() / 2), 0, 0);
        mProgress.setLayoutParams(layoutParams);

        if (mProgress.getProgressDrawable() instanceof LayerDrawable) {
            ScaleDrawable scaleDrawable = (ScaleDrawable) ((LayerDrawable) mProgress.getProgressDrawable()).findDrawableByLayerId(R.id.progress);
            if (scaleDrawable != null && scaleDrawable.getDrawable() instanceof GradientDrawable) {
                GradientDrawable gradientDrawable = (GradientDrawable) scaleDrawable.getDrawable();
                int colorAccent = ATEUtil.getThemeAccentColor(requireActivity());
                gradientDrawable.setColors(new int[]{colorAccent, colorAccent, colorAccent});
            }
        }

        //清除默认的左右边距
        mSeekBar.setPadding(0, DensityUtil.dip2px(requireContext(), 36), 0, 0);
        mSeekBar.setSecondaryProgress(mSeekBar.getMax());

        songElapsedTime.setY((DensityUtil.getScreenWidth(requireContext()) - songElapsedTime.getHeight()) / 2.0f);

        setUpTimelyView();
        setSeekBarListener();

        mPlayPauseView.setOnClickListener(v -> onPlayPauseClick());
        previous.setOnClickListener(v -> onPreviousClick());
        next.setOnClickListener(v -> onNextClick());
        favorite.setOnClickListener(v -> onFavoriteClick());
        iconPlayQueue.setOnClickListener(v -> onPlayQueueClick());
        binding.upIndicator.setOnClickListener(v -> onUpIndicatorClick());

        if (mPlayPauseView != null) {
            if (MusicPlayer.isPlaying())
                mPlayPauseView.Play();
            else mPlayPauseView.Pause();
        }

        mPresenter.updateNowPlayingCard();

        subscribeFavoriteSongEvent();
        subscribeMetaChangedEvent();
        subscribePlayStateChangedEvent();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPresenter.unsubscribe();
        if (mElapsedTimeHandler != null) {
            mElapsedTimeHandler.removeCallbacks(mUpdateElapsedTime);
        }
        if (mProgress != null) {
            mProgress.removeCallbacks(mUpdateProgress);
        }
        sListener = null;
        RxBus.getInstance().unSubscribe(this);
    }

    @Override
    public void showLyric(File file) {
        if (file == null) {
            mLyricView.reset(getString(R.string.no_lyrics));
        } else {
            mLyricView.setLyricFile(file, "UTF-8");
        }
    }

    @Override
    public void setPlayPauseButton(boolean isPlaying) {
        if (musicViewModel != null) {
            musicViewModel.setIsPlaying(isPlaying);
        }
        if (isPlaying) {
            mPlayPauseView.Play();
        } else {
            mPlayPauseView.Pause();
        }
    }

    private void setUpTimelyView() {
        if (timelyView11 != null) {
            String time = ListenerUtil.makeShortTimeString(requireActivity(), MusicPlayer.position() / 1000);
            if (time.length() < 5) {
                timelyView11.setVisibility(View.GONE);
                timelyView12.setVisibility(View.GONE);
                hourColon.setVisibility(View.GONE);

                changeDigit(timelyView13, time.charAt(0) - '0');
                changeDigit(timelyView14, time.charAt(2) - '0');
                changeDigit(timelyView15, time.charAt(3) - '0');

            } else if (time.length() == 5) {
                timelyView12.setVisibility(View.VISIBLE);
                changeDigit(timelyView12, time.charAt(0) - '0');
                changeDigit(timelyView13, time.charAt(1) - '0');
                changeDigit(timelyView14, time.charAt(3) - '0');
                changeDigit(timelyView15, time.charAt(4) - '0');
            } else {
                timelyView11.setVisibility(View.VISIBLE);
                hourColon.setVisibility(View.VISIBLE);
                changeDigit(timelyView11, time.charAt(0) - '0');
                changeDigit(timelyView12, time.charAt(2) - '0');
                changeDigit(timelyView13, time.charAt(3) - '0');
                changeDigit(timelyView14, time.charAt(5) - '0');
                changeDigit(timelyView15, time.charAt(6) - '0');
            }
        }

        if (timelyView11 != null) {
            mElapsedTimeHandler = new Handler(Looper.getMainLooper());
            mElapsedTimeHandler.postDelayed(mUpdateElapsedTime, 600);
        }

    }

    private void setUpPopupMenu(ImageView popupMenu) {
        popupMenu.setOnClickListener(v -> {
            final PopupMenu menu = new PopupMenu(requireContext(), v);
            menu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.popup_song_goto_album) {
                    if (mSlidingUpPanelLayout != null) {
                        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                        NavigationUtil.navigateToAlbum(requireActivity(), MusicPlayer.getCurrentAlbumId(),
                                MusicPlayer.getAlbumName(), null);
                    }
                } else if (itemId == R.id.popup_song_goto_artist) {
                    if (mSlidingUpPanelLayout != null) {
                        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                        NavigationUtil.navigateToArtist(requireActivity(), MusicPlayer.getCurrentArtistId(),
                                MusicPlayer.getArtistName(), null);
                    }
                } else if (itemId == R.id.popup_song_addto_playlist) {
                    ListenerUtil.showAddPlaylistDialog(requireActivity(), new long[]{MusicPlayer.getCurrentAudioId()});
                } else if (itemId == R.id.popup_song_delete) {
                    long[] deleteIds = {MusicPlayer.getCurrentAudioId()};
                    ListenerUtil.showDeleteDialog(requireContext(), MusicPlayer.getTrackName(), deleteIds,
                            (dialog, which) -> {
                            });
                }
                return false;
            });
            menu.inflate(R.menu.menu_now_playing);
            menu.show();
        });
    }

    private void setSeekBarListener() {
        if (mSeekBar != null)
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        if (songElapsedTime.getVisibility() == View.GONE) {
                            songElapsedTime.setVisibility(View.VISIBLE);
                        }
                        mProgress.removeCallbacks(mUpdateProgress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    songElapsedTime.setVisibility(View.GONE);
                    MusicPlayer.seek(seekBar.getProgress());
                    mProgress.postDelayed(mUpdateProgress, 10);
                }
            });
    }

    /**
     * 返回暂停播放按钮的状态
     *
     * @return true表示按钮为待暂定状态, false表示按钮为待播放状态
     */
    @Override
    public boolean getPlayPauseStatus() {
        return mPlayPauseView.isPlay();
    }

    @Override
    public void startUpdateProgress() {
        mProgress.postDelayed(mUpdateProgress, 10);
    }

    @Override
    public void setProgressMax(int max) {
        mProgress.setMax(max);
        mSeekBar.setMax(max);
    }

    @Override
    public void setAlbumArt(Bitmap albumArt) {
        mAlbumArt.setImageBitmap(albumArt);
    }

    @Override
    public void setAlbumArt(Drawable albumArt) {
        mAlbumArt.setImageDrawable(albumArt);
        if (TextUtils.isEmpty(MusicPlayer.getTrackName()) && TextUtils.isEmpty(MusicPlayer.getArtistName())) {
            mAlbumArt.setForeground(null);
            if (getContext() == null) {
                return;
            }
            TypedValue paletteColor = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.album_default_palette_color, paletteColor, true);
            topContainer.setBackgroundColor(paletteColor.data);
            mPlayPauseView.setDrawableColor(ATEUtil.getThemeAccentColor(getContext()));
            mPlayPauseView.setEnabled(false);
            next.setEnabled(false);
            next.setColor(ATEUtil.getThemeAccentColor(getContext()));
            if (sListener != null) {
                sListener.onPaletteColorChange(paletteColor.data, ATEUtil.getThemeAccentColor(getContext()));
            }
        }
    }

    @Override
    public void setTitle(String title) {
        if (musicViewModel != null) {
            musicViewModel.setSongTitle(title);
        }
    }

    @Override
    public void setArtist(String artist) {
        if (musicViewModel != null) {
            musicViewModel.setSongArtist(artist);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setPalette(Palette palette) {
        if (getContext() == null) {
            return;
        }
        if (palette != null) {
            mSwatch = ColorUtil.getMostPopulousSwatch(palette);
        } else {
            mSwatch = null;
        }

        int paletteColor;
        if (mSwatch != null) {
            paletteColor = mSwatch.getRgb();
            int artistColor = mSwatch.getTitleTextColor();
            binding.title.setTextColor(ColorUtil.getOpaqueColor(artistColor));
            binding.artist.setTextColor(artistColor);
        } else {
            if (palette != null) {
                mSwatch = palette.getMutedSwatch() == null ? palette.getVibrantSwatch() : palette.getMutedSwatch();
            }
            if (mSwatch != null) {
                paletteColor = mSwatch.getRgb();
                int artistColor = mSwatch.getTitleTextColor();
                binding.title.setTextColor(ColorUtil.getOpaqueColor(artistColor));
                binding.artist.setTextColor(artistColor);
            } else {
                paletteColor = ATEUtil.getThemeAlbumDefaultPaletteColor(getContext());
                if (getContext() != null) {
                    binding.title.setTextColor(ATEUtil.getThemeTextColorPrimary(getContext()));
                    binding.artist.setTextColor(ATEUtil.getThemeTextColorSecondly(getContext()));
                }
            }

        }
        //set icon color
        blackWhiteColor = ColorUtil.getBlackWhiteColor(paletteColor);
        topContainer.setBackgroundColor(paletteColor);
        if (bottomDialogFragment != null && mSwatch != null) {
            bottomDialogFragment.setPaletteSwatch(mSwatch);
        }
        mLyricView.setHighLightTextColor(blackWhiteColor);
        mLyricView.setDefaultColor(blackWhiteColor);
        mLyricView.setTouchable(false);
        mLyricView.setHintColor(blackWhiteColor);
        mPlayPauseView.setDrawableColor(blackWhiteColor);
        mPlayPauseView.setCircleColor(blackWhiteColor);
        mPlayPauseView.setCircleAlpah(0);
        mPlayPauseView.setEnabled(true);
        next.setEnabled(true);
        next.setColor(blackWhiteColor);
        previous.setColor(blackWhiteColor);
        next.setColor(blackWhiteColor);
        iconPlayQueue.setColor(blackWhiteColor);

        //set timely color
        setTimelyColor(blackWhiteColor);

        //set seekbar progressdrawable
        if (mSeekBar.getProgressDrawable() instanceof LayerDrawable) {
            ScaleDrawable scaleDrawable = (ScaleDrawable) ((LayerDrawable) mSeekBar.getProgressDrawable()).findDrawableByLayerId(R.id.progress);
            if (scaleDrawable != null && scaleDrawable.getDrawable() instanceof GradientDrawable) {
                GradientDrawable gradientDrawable = (GradientDrawable) scaleDrawable.getDrawable();
                gradientDrawable.setColors(new int[]{blackWhiteColor, blackWhiteColor, blackWhiteColor});
            }
        }

        mIsFavorite = FavoriteSong.getInstance(getContext()).isFavorite(MusicPlayer.getCurrentAudioId());
        if (mIsFavorite) {
            favorite.setColor(Color.parseColor("#E97767"));
        } else {
            favorite.setColor(blackWhiteColor);
        }
        //set albumart foreground
        mAlbumArt.setForeground(
                ScrimUtil.makeCubicGradientScrimDrawable(
                        paletteColor, //颜色
                        8, //渐变层数
                        Gravity.CENTER_HORIZONTAL)); //起始方向

        if (sListener != null) {
            sListener.onPaletteColorChange(paletteColor, blackWhiteColor);
        }

    }    private final Runnable mUpdateProgress = new Runnable() {

        @Override
        public void run() {

            long position = MusicPlayer.position();
            mProgress.setProgress((int) position);
            mSeekBar.setProgress((int) position);
            mLyricView.setCurrentTimeMillis(position);
            if (MusicPlayer.isPlaying()) {
                mProgress.postDelayed(mUpdateProgress, 50);
            } else mProgress.removeCallbacks(this);

        }
    };

    public void onUpIndicatorClick() {
        if (mSlidingUpPanelLayout != null) {
            mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
    }

    public void onPlayPauseClick() {
        mPresenter.onPlayPauseClick();
    }

    public void onNextClick() {
        mPresenter.onNextClick();
    }

    public void onPreviousClick() {
        mPresenter.onPreviousClick();
    }

    public void onPlayQueueClick() {
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        if (bottomDialogFragment == null) {
            bottomDialogFragment = new PlayqueueDialog();
        }
        bottomDialogFragment.show(fm, "fragment_bottom_dialog");
        if (mSwatch != null) {
            bottomDialogFragment.setPaletteSwatch(mSwatch);

        }
    }

    public void onFavoriteClick() {
        if (mIsFavorite) {
            int num = FavoriteSong.getInstance(requireContext()).removeFavoriteSong(new long[]{MusicPlayer.getCurrentAudioId()});
            if (num == 1) {
                favorite.setColor(blackWhiteColor);
                mIsFavorite = false;
                RxBus.getInstance().post(new FavoriteSongEvent());
                Toast.makeText(requireContext(), R.string.remove_favorite_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), R.string.remove_favorite_fail, Toast.LENGTH_SHORT).show();
            }
        } else {
            int num = FavoriteSong.getInstance(requireContext()).addFavoriteSong(new long[]{MusicPlayer.getCurrentAudioId()});
            if (num == 1) {
                favorite.setColor(Color.parseColor("#E97767"));
                mIsFavorite = true;
                RxBus.getInstance().post(new FavoriteSongEvent());
                Toast.makeText(requireContext(), R.string.add_favorite_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), R.string.add_favorite_fail, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void subscribeFavoriteSongEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(FavoriteSongEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    if (getContext() == null) {
                        return;
                    }
                    mIsFavorite = FavoriteSong.getInstance(getContext()).isFavorite(MusicPlayer.getCurrentAudioId());
                    if (mIsFavorite) {
                        favorite.setColor(Color.parseColor("#E97767"));
                    } else {
                        favorite.setColor(blackWhiteColor);
                    }
                }, throwable -> {
                });
        RxBus.getInstance().addSubscription(this, subscription);
    }

    private void subscribeMetaChangedEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(MetaChangedEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    mPresenter.updateNowPlayingCard();
                    mPresenter.loadLyric();
                }, throwable -> {
                });
        RxBus.getInstance().addSubscription(this, subscription);
    }

    private void subscribePlayStateChangedEvent() {
        Subscription subscription = RxBus.getInstance()
                .toObservable(PlayStateChangedEvent.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> mPresenter.updateNowPlayingCard(), throwable -> {
                });
        RxBus.getInstance().addSubscription(this, subscription);
    }

    private void changeDigit(TimelyView tv, int end) {
        ObjectAnimator obja = tv.animate(end);
        obja.setDuration(400);
        obja.start();
    }

    private void setTimelyColor(@ColorInt int color) {
        hourColon.setTextColor(color);
        minuteColon.setTextColor(color);
        timelyView11.setTextColor(color);
        timelyView12.setTextColor(color);
        timelyView13.setTextColor(color);
        timelyView14.setTextColor(color);
        timelyView15.setTextColor(color);
    }

    private void changeDigit(TimelyView tv, int start, int end) {
        try {
            ObjectAnimator obja = tv.animate(start, end);
            obja.setDuration(400);
            obja.start();
        } catch (InvalidParameterException e) {
            Log.e(TAG, "Error animating timely view", e);
        }
    }

    private void tv11(int a) {
        if (a != timeArr[0]) {
            changeDigit(timelyView11, timeArr[0], a);
            timeArr[0] = a;
        }
    }

    private void tv12(int a) {
        if (a != timeArr[1]) {
            changeDigit(timelyView12, timeArr[1], a);
            timeArr[1] = a;
        }
    }

    private void tv13(int a) {
        if (a != timeArr[2]) {
            changeDigit(timelyView13, timeArr[2], a);
            timeArr[2] = a;
        }
    }

    private void tv14(int a) {
        if (a != timeArr[3]) {
            changeDigit(timelyView14, timeArr[3], a);
            timeArr[3] = a;
        }
    }

    private void tv15(int a) {
        if (a != timeArr[4]) {
            changeDigit(timelyView15, timeArr[4], a);
            timeArr[4] = a;
        }
    }




}
