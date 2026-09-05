package io.hefuyi.listener.util;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;

/**
 * Created by hefuyi on 2016/11/23.
 */

public class ColorUtil {

    public static int getStatusBarColor(int color) {
        float[] arrayOfFloat = new float[3];
        Color.colorToHSV(color, arrayOfFloat);
        arrayOfFloat[2] *= 0.9F;
        return Color.HSVToColor(arrayOfFloat);
    }

    public static int getBlackWhiteColor(int color) { //根据颜色的亮度转换为黑白色
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        if (darkness >= 0.5) {
            return Color.WHITE;
        } else return Color.BLACK;
    }

    public static int getOpaqueColor(@ColorInt int paramInt) {
        return 0xFF000000 | paramInt;
    }

    public static @Nullable Palette.Swatch getMostPopulousSwatch(Palette palette) {
        Palette.Swatch mostPopulous = null;
        if (palette != null) {
            for (Palette.Swatch swatch : palette.getSwatches()) {
                if (mostPopulous == null || swatch.getPopulation() > mostPopulous.getPopulation()) {
                    mostPopulous = swatch;
                }
            }
        }
        return mostPopulous;
    }

    public static int ensureContrastRatio(int foregroundColor, int backgroundColor, double minContrast) {
        if (ColorUtils.calculateContrast(foregroundColor, backgroundColor) >= minContrast) {
            return foregroundColor;
        }

        float[] hsv = new float[3];
        Color.colorToHSV(foregroundColor, hsv);

        boolean isBackgroundDark = ColorUtils.calculateLuminance(backgroundColor) < 0.5;

        for (int i = 0; i < 100; i++) {
            if (isBackgroundDark) {
                hsv[2] = Math.min(1.0f, hsv[2] + 0.01f);
                if (hsv[2] >= 0.95f) {
                    hsv[1] = Math.max(0.0f, hsv[1] - 0.02f);
                }
            } else {
                hsv[2] = Math.max(0.0f, hsv[2] - 0.01f);
            }

            int adjustedColor = Color.HSVToColor(hsv);
            if (ColorUtils.calculateContrast(adjustedColor, backgroundColor) >= minContrast) {
                return adjustedColor;
            }
        }

        return isBackgroundDark ? Color.WHITE : Color.BLACK;
    }
}
