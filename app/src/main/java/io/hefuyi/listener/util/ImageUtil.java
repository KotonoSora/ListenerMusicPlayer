package io.hefuyi.listener.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ImageUtil {

    public static Drawable createBlurredImageFromBitmap(Bitmap bitmap, Context context, int inSampleSize) {
        // 1. Create a scaled bitmap for efficiency
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,
                bitmap.getWidth() / inSampleSize,
                bitmap.getHeight() / inSampleSize, true);

        // 2. Prepare the blurred bitmap
        Bitmap blurredBitmap = Bitmap.createBitmap(scaledBitmap.getWidth(), scaledBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(blurredBitmap);

        // 3. Configure Paint with BlurMaskFilter
        Paint paint = new Paint();
        paint.setMaskFilter(new BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL));

        // 4. Draw the bitmap onto the canvas with the blur filter
        canvas.drawBitmap(scaledBitmap, 0, 0, paint);

        return new BitmapDrawable(context.getResources(), blurredBitmap);
    }
}