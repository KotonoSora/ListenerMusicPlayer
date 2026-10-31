package io.hefuyi.listener.widget;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

public class SquareImageView extends AppCompatImageView {


    public SquareImageView(Context context) {
        super(context);
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int size;
        if (heightMode != MeasureSpec.UNSPECIFIED && height > 0 && (height < width || width == 0)) {
            size = height;
        } else if (width > 0) {
            size = width;
        } else {
            size = height;
        }

        setMeasuredDimension(size, size);
    }
}