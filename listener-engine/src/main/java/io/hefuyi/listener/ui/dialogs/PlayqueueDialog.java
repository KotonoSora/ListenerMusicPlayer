package io.hefuyi.listener.ui.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import javax.inject.Inject;

import io.hefuyi.listener.ListenerApp;
import io.hefuyi.listener.MusicPlayer;
import io.hefuyi.listener.MusicService;
import io.hefuyi.listener.R;
import io.hefuyi.listener.injector.component.ApplicationComponent;
import io.hefuyi.listener.injector.component.DaggerPlayqueueSongComponent;
import io.hefuyi.listener.injector.component.PlayqueueSongComponent;
import io.hefuyi.listener.injector.module.PlayqueueSongModule;
import io.hefuyi.listener.mvp.contract.PlayqueueSongContract;
import io.hefuyi.listener.mvp.model.Song;
import io.hefuyi.listener.ui.adapter.PlayqueueSongsAdapter;
import io.hefuyi.listener.util.ColorUtil;
import io.hefuyi.listener.util.ListenerUtil;
import io.hefuyi.listener.widget.DividerItemDecoration;

/**
 * Created by hefuyi on 2016/12/27.
 */
@SuppressWarnings("SpellCheckingInspection")
public class PlayqueueDialog extends DialogFragment implements PlayqueueSongContract.View {

    @Inject
    PlayqueueSongContract.Presenter mPresenter;
    TextView tvPlayMode;
    ImageView ivPlayMode;
    ImageView clearAll;
    RecyclerView recyclerView;
    LinearLayout root;

    private PlayqueueSongsAdapter mAdapter;
    private Palette.Swatch mSwatch;

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(true);
            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams params = window.getAttributes();
                if (params != null) {
                    params.gravity = Gravity.BOTTOM;
                    params.width = WindowManager.LayoutParams.MATCH_PARENT;
                    params.windowAnimations = R.style.DialogAnimation;
                    window.setAttributes(params);
                }
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies();
        mPresenter.attachView(this);
        mAdapter = new PlayqueueSongsAdapter((AppCompatActivity) requireActivity(), null);
    }

    private void injectDependencies() {
        ApplicationComponent applicationComponent = ((ListenerApp) requireActivity().getApplication()).getApplicationComponent();
        PlayqueueSongComponent playqueueSongComponent = DaggerPlayqueueSongComponent.builder()
                .applicationComponent(applicationComponent)
                .playqueueSongModule(new PlayqueueSongModule())
                .build();
        playqueueSongComponent.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_playqueue, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        root = (LinearLayout) view;
        tvPlayMode = view.findViewById(R.id.tv_play_mode);
        ivPlayMode = view.findViewById(R.id.iv_play_mode);
        clearAll = view.findViewById(R.id.clear_all);
        recyclerView = view.findViewById(R.id.recycler_view_songs);

        ListenerUtil.applySystemBarPadding(view, false, true);

        if (mSwatch != null) {
            root.setBackgroundColor(mSwatch.getRgb());
            mAdapter.setPaletteSwatch(mSwatch);
            int blackWhiteColor = ColorUtil.getBlackWhiteColor(mSwatch.getRgb());
            tvPlayMode.setTextColor(blackWhiteColor);
            ivPlayMode.setColorFilter(blackWhiteColor);
            clearAll.setColorFilter(blackWhiteColor);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL_LIST, false));

        int shuffleMode = MusicPlayer.getShuffleMode();
        int repeatMode = MusicPlayer.getRepeatMode();
        if (shuffleMode == MusicService.SHUFFLE_NONE) {
            if (repeatMode == MusicService.REPEAT_CURRENT) {
                //单曲播放模式
                ivPlayMode.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_one_shot));
                tvPlayMode.setText(R.string.repeat_current);
            } else {
                //顺序播放模式
                ivPlayMode.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_list_repeat));
                tvPlayMode.setText(R.string.repeat_all);
            }
        } else if (shuffleMode == MusicService.SHUFFLE_NORMAL || shuffleMode == MusicService.SHUFFLE_AUTO) {
            //随机播放模式
            ivPlayMode.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_list_shuffle));
            tvPlayMode.setText(R.string.shuffle_all);
        }

        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                if (mAdapter.getItemCount() == 0) {
                    dismiss();
                }
            }
        });

        mPresenter.subscribe();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPresenter.unsubscribe();
    }

    @Override
    public void showSongs(List<Song> songs) {
        mAdapter.setSongList(songs);
    }

    public void setPaletteSwatch(Palette.Swatch swatch) {
        if (swatch == null) {
            return;
        }
        mSwatch = swatch;
        if (root != null) {
            root.setBackgroundColor(mSwatch.getRgb());
            int blackWhiteColor = ColorUtil.getBlackWhiteColor(mSwatch.getRgb());
            tvPlayMode.setTextColor(blackWhiteColor);
            ivPlayMode.setColorFilter(blackWhiteColor);
            clearAll.setColorFilter(blackWhiteColor);
            mAdapter.setPaletteSwatch(mSwatch);
        }
    }

    @Override
    public void dismiss() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.dismiss();
        } else {
            super.dismiss();
        }
    }
}
