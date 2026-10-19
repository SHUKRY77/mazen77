package com.maxtunnel.vip;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

// مكتبة MMKV ضرورية جداً لمحرك V2Ray

public class KINGVpnApp extends Application {

    public static SharedPreferences sharedPreferences;
    public static SharedPreferences defaultPreferences;
    private static KINGVpnApp instance;
    public static boolean isFirstRun = false;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // 1. تهيئة الذاكرة الافتراضية
        sharedPreferences = getSharedPreferences("vpn_settings_data", Context.MODE_PRIVATE);
        defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // 2. التحقق مما إذا كان التطبيق يفتح لأول مرة (لإجراء التحديثات التلقائية لاحقاً)
        int currentVersionCode = 1; 
        if (defaultPreferences.getInt("app_version_code", 0) != currentVersionCode) {
            isFirstRun = true;
            defaultPreferences.edit().putInt("app_version_code", currentVersionCode).apply();
        }

        // 3. تهيئة مكتبة MMKV لتسريع قراءة سيرفرات V2Ray
        
    }

    public static KINGVpnApp getInstance() {
        return instance;
    }

    public static SharedPreferences getAppPreferences() {
        return defaultPreferences;
    }

    public static SharedPreferences getVpnPreferences() {
        return sharedPreferences;
    }
}
