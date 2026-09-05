package io.hefuyi.listener.widget;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.afollestad.appthemeengine.Config;

import io.hefuyi.listener.R;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.ColorUtil;

/**
 * Created by naman on 31/12/15.
 */
@SuppressWarnings({"deprecation", "unused", "SpellCheckingInspection"})
public class ThemedPreferenceCategory extends PreferenceCategory {

    private final Context context;

    public ThemedPreferenceCategory(Context context) {
        super(context);
        this.context = context;
    }

    public ThemedPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public ThemedPreferenceCategory(Context context, AttributeSet attrs,
                                    int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView titleView = view.findViewById(android.R.id.title);
        if (titleView != null) {
            int categoryColor = Config.primaryColor(context, ATEUtil.getATEKey(context));
            boolean darkTheme = ATEUtil.isDarkTheme(context);
            int bg = ContextCompat.getColor(context, darkTheme ? R.color.window_background_dark : R.color.window_background);
            titleView.setTextColor(ColorUtil.ensureContrastRatio(categoryColor, bg, 4.5));
        }
    }
}
