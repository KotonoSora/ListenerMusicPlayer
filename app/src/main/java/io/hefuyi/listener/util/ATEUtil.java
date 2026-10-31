package io.hefuyi.listener.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import com.afollestad.appthemeengine.Config;

import io.hefuyi.listener.R;

/**
 * Created by hefuyi on 2017/1/23.
 */
@SuppressWarnings({"deprecation", "unused"})
public class ATEUtil {

    public static String getATEKey(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dark_theme", false) ?
                "dark_theme" : "light_theme";
    }

    public static boolean isDarkTheme(Context context) {
        return "dark_theme".equals(getATEKey(context));
    }

    public static int getThemePrimaryColor(Context context) {
        return Config.primaryColor(context, getATEKey(context));
    }

    public static int getThemePrimaryColorDark(Context context) {
        return Config.primaryColorDark(context, getATEKey(context));
    }

    public static int getThemeAccentColor(Context context) {
        return Config.accentColor(context, getATEKey(context));
    }

    public static int getThemeTextColorPrimary(Context context) {
        if (isDarkTheme(context)) {
            return ContextCompat.getColor(context, R.color.colorPrimaryTextWhite);
        } else {
            return ContextCompat.getColor(context, R.color.colorPrimaryTextBlack);
        }
    }

    public static int getThemeTextColorSecondly(Context context) {
        if (isDarkTheme(context)) {
            return ContextCompat.getColor(context, R.color.colorSubTextWhite);
        } else {
            return ContextCompat.getColor(context, R.color.colorSubTextBlack);
        }
    }

    public static Drawable getDefaultAlbumDrawable(Context context) {
        TypedValue defaultAlbum = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.default_album_drawable, defaultAlbum, true);
        return ContextCompat.getDrawable(context, defaultAlbum.resourceId);
    }

    public static Drawable getDefaultSingerDrawable(Context context) {
        TypedValue defaultSinger = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.default_singer_drawable, defaultSinger, true);
        return ContextCompat.getDrawable(context, defaultSinger.resourceId);
    }

    public static int getThemeAlbumDefaultPaletteColor(Context context) {
        TypedValue paletteColor = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.album_default_palette_color, paletteColor, true);
        return paletteColor.resourceId != 0 ? ContextCompat.getColor(context, paletteColor.resourceId) : paletteColor.data;
    }

}
