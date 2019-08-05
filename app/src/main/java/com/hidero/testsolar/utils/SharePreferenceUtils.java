package com.hidero.testsolar.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharePreferenceUtils {

    private static SharedPreferences preferences;
    private SharedPreferences.Editor editor;


    public SharePreferenceUtils(Context context) {
        preferences = context.getSharedPreferences(Const.SHARE_PREFERENCE, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }
    public static int getIntData(Context context, String key) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int r = preferences.getInt(key, 1);
        return r;
    }

    public static void insertIntData(Context context, String key, int value) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }
    public static void insertLongData(Context context, String key, long value) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static long getLongData(Context context, String key) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        long r = preferences.getLong(key, -1L);
        return r;
    }

    public static void removeData(Context context, String key) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        editor.apply();
    }
    public static void insertStringData(Context context, String key, String value) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void removeStringData(Context context, String key) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        editor.apply();
    }

    public static boolean getBooleanData(Context context, String key) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean r = preferences.getBoolean(key, false);
        return r;
    }

    public static void insertBooleanData(Context context, String key, boolean value) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static String getStringData(Context context, String key) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String r = preferences.getString(key, "");
        return r;
    }


}
