package com.ultraflymodel.polarbear.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public final class Settings {

    private Settings() {}

    private static SharedPreferences mPrefs;

    private static int mTheme;

    public static void updatePreferences(Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

//        mTheme = Integer.parseInt(mPrefs.getString("preference_theme", Integer.toString(R.style.ThemeLight)));

        rootAccess();
    }

    public static boolean showThumbnail() {
//        return mPrefs.getBoolean("showpreview", true);
        return true;
    }

    public static boolean showHiddenFiles() {
//        return mPrefs.getBoolean("displayhiddenfiles", true);
        return false;
    }

    public static boolean rootAccess() {
//        return mPrefs.getBoolean("enablerootaccess", false) && RootTools.isAccessGiven();
        return false;
    }

    public static boolean reverseListView() {
        return mPrefs.getBoolean("reverseList", false);
    }

    public static String getDefaultDir() {
        return mPrefs.getString("defaultdir", Environment.getExternalStorageDirectory().getPath());
    }

    public static int getListAppearance() {
//        return Integer.parseInt(mPrefs.getString("viewmode", "1"));
        return 0;
    }

    public static int getSortType() {
        return Integer.parseInt(mPrefs.getString("sort", "1"));
    }

    public static int getDefaultTheme() {
        return mTheme;
    }

    public static void setDefaultTheme(int theme) {
        mTheme = theme;
    }
}
