package io.hefuyi.listener.ui.fragment;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.CompoundButtonCompat;
import androidx.fragment.app.Fragment;

import com.afollestad.appthemeengine.ATE;
import com.afollestad.appthemeengine.Config;
import com.afollestad.appthemeengine.prefs.ATEColorPreference;
import com.afollestad.materialdialogs.color.ColorChooserDialog;

import io.hefuyi.listener.R;
import io.hefuyi.listener.ui.activity.SettingActivity;
import io.hefuyi.listener.util.ATEUtil;
import io.hefuyi.listener.util.PreferencesUtility;

/**
 * A simple {@link Fragment} subclass.
 */
@SuppressWarnings("deprecation")
public class SettingFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private String mAteKey;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        if (getActivity() != null) {
            PreferencesUtility.getInstance(getActivity()).setOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        invalidateSettings();
        ATE.apply(view, mAteKey);
        setupPreferenceListView(view);
    }

    public void invalidateSettings() {
        if (getActivity() == null) {
            return;
        }
        mAteKey = ((SettingActivity) getActivity()).getATEKey();

        ATEColorPreference primaryColorPref = (ATEColorPreference) findPreference("primary_color");
        if (primaryColorPref != null) {
            primaryColorPref.setColor(Config.primaryColor(getActivity(), mAteKey), Color.BLACK);
            primaryColorPref.setOnPreferenceClickListener(preference -> {
                if (getActivity() != null) {
                    new ColorChooserDialog.Builder((SettingActivity) getActivity(), R.string.primary_color)
                            .preselect(Config.primaryColor(getActivity(), mAteKey))
                            .show();
                }
                return true;
            });
        }

        Preference darkThemePref = findPreference("dark_theme");
        if (darkThemePref != null) {
            darkThemePref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (getActivity() != null) {
                    // Marks both theme configs as changed so MainActivity restarts itself on return
                    Config.markChanged(getActivity(), "light_theme");
                    Config.markChanged(getActivity(), "dark_theme");
                    // The dark_theme preference value gets saved by Android in the default PreferenceManager.
                    // It's used in getATEKey() of both the Activities.
                    getActivity().recreate();
                }
                return true;
            });
        }

    }

    private void setupPreferenceListView(View rootView) {
        ListView listView = rootView.findViewById(android.R.id.list);
        if (listView == null) {
            return;
        }

        listView.setDivider(null);
        listView.setCacheColorHint(Color.TRANSPARENT);

        final ListAdapter adapter = listView.getAdapter();
        if (adapter != null) {
            listView.setAdapter(new ListAdapter() {
                @Override
                public boolean areAllItemsEnabled() {
                    return adapter.areAllItemsEnabled();
                }

                @Override
                public boolean isEnabled(int position) {
                    return adapter.isEnabled(position);
                }

                @Override
                public void registerDataSetObserver(DataSetObserver observer) {
                    adapter.registerDataSetObserver(observer);
                }

                @Override
                public void unregisterDataSetObserver(DataSetObserver observer) {
                    adapter.unregisterDataSetObserver(observer);
                }

                @Override
                public int getCount() {
                    return adapter.getCount();
                }

                @Override
                public Object getItem(int position) {
                    return adapter.getItem(position);
                }

                @Override
                public long getItemId(int position) {
                    return adapter.getItemId(position);
                }

                @Override
                public boolean hasStableIds() {
                    return adapter.hasStableIds();
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = adapter.getView(position, convertView, parent);
                    stylePreferenceView(view);
                    return view;
                }

                @Override
                public int getItemViewType(int position) {
                    return adapter.getItemViewType(position);
                }

                @Override
                public int getViewTypeCount() {
                    return adapter.getViewTypeCount();
                }

                @Override
                public boolean isEmpty() {
                    return adapter.isEmpty();
                }
            });
        }
    }

    private void stylePreferenceView(View view) {
        if (view == null || getActivity() == null) {
            return;
        }

        Context context = getActivity();
        int textColorPrimary = ATEUtil.getThemeTextColorPrimary(context);
        int textColorSecondary = ATEUtil.getThemeTextColorSecondly(context);
        int primaryThemeColor = ATEUtil.getThemePrimaryColor(context);

        TextView titleView = view.findViewById(android.R.id.title);
        if (titleView != null) {
            titleView.setTextColor(textColorPrimary);
        }

        TextView summaryView = view.findViewById(android.R.id.summary);
        if (summaryView != null) {
            summaryView.setTextColor(textColorSecondary);
        }

        CompoundButton checkBox = view.findViewById(android.R.id.checkbox);
        if (checkBox == null) {
            checkBox = findCompoundButton(view);
        }
        if (checkBox != null) {
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
            };
            int[] colors = new int[]{
                    primaryThemeColor,
                    textColorSecondary
            };
            ColorStateList tintList = new ColorStateList(states, colors);
            CompoundButtonCompat.setButtonTintList(checkBox, tintList);
        }
    }

    private CompoundButton findCompoundButton(View view) {
        if (view instanceof CompoundButton) {
            return (CompoundButton) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                CompoundButton cb = findCompoundButton(group.getChildAt(i));
                if (cb != null) {
                    return cb;
                }
            }
        }
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }
}
