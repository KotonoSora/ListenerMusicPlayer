package io.hefuyi.listener.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import io.hefuyi.listener.R;
import io.hefuyi.listener.util.ATEUtil;


@SuppressWarnings("unused")
public class PlayPauseView extends FrameLayout {

    private static final long PLAY_PAUSE_ANIMATION_DURATION = 200;

    private final PlayPauseDrawable mDrawable;
    private final Paint mPaint = new Paint();
    public boolean isDrawCircle;
    public int circleAlpha;
    private int mDrawableColor;
    private AnimatorSet mAnimatorSet;
    private int mBackgroundColor;
    private int mWidth;
    private int mHeight;
    private boolean mIsPlay;

    public PlayPauseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);

        try (TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PlayPauseView)) {
            isDrawCircle = typedArray.getBoolean(R.styleable.PlayPauseView_isCircleDraw, true);
            circleAlpha = typedArray.getInt(R.styleable.PlayPauseView_circleAlpha, 255);
        }
        mBackgroundColor = ATEUtil.getThemeAccentColor(context);
        mDrawableColor = ATEUtil.getThemeAccentColor(context);

        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAlpha(circleAlpha);
        mPaint.setColor(mBackgroundColor);
        mDrawable = new PlayPauseDrawable(context, mDrawableColor);
        mDrawable.setCallback(this);

    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldWidth, int oldHeight) {
        super.onSizeChanged(w, h, oldWidth, oldHeight);
        mDrawable.setBounds(0, 0, w, h);
        mWidth = w;
        mHeight = h;

        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        setClipToOutline(true);
    }

    public void setCircleAlpha(int alpha) {
        circleAlpha = alpha;
        invalidate();
    }

    @SuppressWarnings("SpellCheckingInspection")
    public void setCircleAlpah(int alpah) {
        setCircleAlpha(alpah);
    }

    private int getCircleColor() {
        return mBackgroundColor;
    }

    public void setCircleColor(@ColorInt int color) {
        mBackgroundColor = color;
        invalidate();
    }

    public int getDrawableColor() {
        return mDrawableColor;
    }

    public void setDrawableColor(@ColorInt int color) {
        mDrawableColor = color;
        mDrawable.setDrawableColor(color);
        invalidate();
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == mDrawable || super.verifyDrawable(who);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(mBackgroundColor);
        final float radius = Math.min(mWidth, mHeight) / 2f;
        if (isDrawCircle) {
            mPaint.setColor(mBackgroundColor);
            mPaint.setAlpha(circleAlpha);
            canvas.drawCircle(mWidth / 2f, mHeight / 2f, radius, mPaint);
        }
        mDrawable.draw(canvas);
    }

    public boolean isPlay() {
        return mIsPlay;
    }

    //此时为待暂停标识
    public void Play() {
        if (mIsPlay) {
            return;
        }
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
        }
        mAnimatorSet = new AnimatorSet();
        mIsPlay = true;
        mDrawable.setIsPlay(true);
        final Animator pausePlayAnim = mDrawable.getPausePlayAnimator();
        mAnimatorSet.setInterpolator(new DecelerateInterpolator());
        mAnimatorSet.setDuration(PLAY_PAUSE_ANIMATION_DURATION);
        pausePlayAnim.start();
    }

    //此时为为待播放标识
    public void Pause() {
        if (!mIsPlay) {
            return;
        }
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
        }

        mAnimatorSet = new AnimatorSet();
        mIsPlay = false;
        mDrawable.setIsPlay(false);
        final Animator pausePlayAnim = mDrawable.getPausePlayAnimator();
        mAnimatorSet.setInterpolator(new DecelerateInterpolator());
        mAnimatorSet.setDuration(PLAY_PAUSE_ANIMATION_DURATION);
        pausePlayAnim.start();
    }

}
