package io.hefuyi.listener.ui.activity;

import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.afollestad.appthemeengine.ATE;
import com.afollestad.appthemeengine.Config;
import com.afollestad.appthemeengine.customizers.ATEActivityThemeCustomizer;
import com.afollestad.materialdialogs.color.ColorChooserDialog;

import io.hefuyi.listener.R;
import io.hefuyi.listener.ui.fragment.SettingFragment;
import io.hefuyi.listener.util.ColorUtil;
import io.hefuyi.listener.util.ListenerUtil;

public class SettingActivity extends BaseActivity implements ColorChooserDialog.ColorCallback, ATEActivityThemeCustomizer {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        ListenerUtil.applySystemBarPaddingAndHeight(toolbar, true, false);

        View fragmentContainer = findViewById(R.id.fragment_container);
        ListenerUtil.applySystemBarPadding(fragmentContainer, false, true);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.settings);

        PreferenceFragment fragment = new SettingFragment();
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public int getActivityTheme() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dark_theme", false) ?
                R.style.AppThemeDark : R.style.AppThemeLight;
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        final Config configLight = ATE.config(this, "light_theme");
        configLight.primaryColor(selectedColor);
        configLight.primaryColorDark(ColorUtil.getStatusBarColor(selectedColor));
        configLight.accentColor(selectedColor);
        configLight.commit();

        final Config configDark = ATE.config(this, "dark_theme");
        configDark.primaryColor(selectedColor);
        configDark.primaryColorDark(ColorUtil.getStatusBarColor(selectedColor));
        configDark.accentColor(selectedColor);
        configDark.commit();

        Config.markChanged(this, "light_theme");
        Config.markChanged(this, "dark_theme");
        recreate(); // recreation needed to reach the checkboxes in the preferences layout
    }
}
