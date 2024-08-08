package moe.shizuku.manager;

import android.app.ActivityThread;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import moe.shizuku.manager.model.GroupApps;
import moe.shizuku.manager.utils.EmptySharedPreferencesImpl;
import moe.shizuku.manager.utils.EnvironmentUtils;

import static java.lang.annotation.RetentionPolicy.SOURCE;
import static moe.shizuku.manager.AppConstants.GROUP_PKG_PREFIX;

import com.google.gson.Gson;

public class ShizukuSettings {

    public static final String NAME = "settings";
    public static final String NIGHT_MODE = "night_mode";
    public static final String LANGUAGE = "language";
    public static final String KEEP_START_ON_BOOT = "start_on_boot";

    //
    public static final String GROUP_LOCK_APPS = "group_lock_apps";
    public static final String ENABLE_PASSWORD = "enable_password";
    public static final String LOCK_PASSWORD = "LOCK_PASSWORD";
    public static final String IS_LOCKED = "IS_LOCKED";
    public static final String IS_CHANNING_PASSWORD = "IS_CHANNING_PASSWORD";
    public static final String IS_OPEN_OTHER_ACTIVITY = "IS_OPEN_OTHER_ACTIVITY";


    private static SharedPreferences sPreferences;
    private static final Gson gson = new Gson();

    public static SharedPreferences getPreferences() {
        return sPreferences;
    }

    @NonNull
    private static Context getSettingsStorageContext(@NonNull Context context) {
        Context storageContext;
        storageContext = context.createDeviceProtectedStorageContext();

        storageContext = new ContextWrapper(storageContext) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                try {
                    return super.getSharedPreferences(name, mode);
                } catch (IllegalStateException e) {
                    // SharedPreferences in credential encrypted storage are not available until after user is unlocked
                    return new EmptySharedPreferencesImpl();
                }
            }
        };

        return storageContext;
    }

    public static void initialize(Context context) {
        if (sPreferences == null) {
            sPreferences = getSettingsStorageContext(context)
                    .getSharedPreferences(NAME, Context.MODE_PRIVATE);
        }
    }

    @IntDef({
            LaunchMethod.UNKNOWN,
            LaunchMethod.ROOT,
            LaunchMethod.ADB,
    })
    @Retention(SOURCE)
    public @interface LaunchMethod {
        int UNKNOWN = -1;
        int ROOT = 0;
        int ADB = 1;
    }

    @LaunchMethod
    public static int getLastLaunchMode() {
        return getPreferences().getInt("mode", LaunchMethod.UNKNOWN);
    }

    public static void setLastLaunchMode(@LaunchMethod int method) {
        getPreferences().edit().putInt("mode", method).apply();
    }

    @AppCompatDelegate.NightMode
    public static int getNightMode() {
        int defValue = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (EnvironmentUtils.isWatch(ActivityThread.currentActivityThread().getApplication())) {
            defValue = AppCompatDelegate.MODE_NIGHT_YES;
        }
        return getPreferences().getInt(NIGHT_MODE, defValue);
    }

    public static Locale getLocale() {
        String tag = getPreferences().getString(LANGUAGE, null);
        if (TextUtils.isEmpty(tag) || "SYSTEM".equals(tag)) {
            return Locale.getDefault();
        }
        return Locale.forLanguageTag(tag);
    }

    public static Set<String> getGroupLockedAppsAsSet() {
        String pkgStr = getPreferences().getString(GROUP_LOCK_APPS, null);
        if (pkgStr == null || pkgStr.isEmpty()) return Collections.emptySet();
        return new HashSet<>(Arrays.asList(pkgStr.split(",")));
    }

    public static void saveGroupLockedApps(String groupName) {
        List<String> pkgs = new ArrayList<>(getGroupLockedAppsAsSet());
        pkgs.add(GROUP_PKG_PREFIX + groupName);
        String pkgsStr = String.join(",", pkgs);
        getPreferences().edit().putString(GROUP_LOCK_APPS, pkgsStr).apply();
    }

    public static void removeGroupLockedApp(String pkg) {
        Set<String> packages = getGroupLockedAppsAsSet();
        packages.remove(pkg);
        String pkgsStr = String.join(",", packages);
        getPreferences().edit().putString(GROUP_LOCK_APPS, pkgsStr).apply();
    }

    @Nullable
    public static GroupApps getPksByGroupName(String name) {
        String objStr = getPreferences().getString(name, "");
        if (objStr.isEmpty()) return null;
        try {
            return gson.fromJson(objStr, GroupApps.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveDataByGroupName(String name, GroupApps data) {
        getPreferences().edit().putString(GROUP_PKG_PREFIX + name, gson.toJson(data)).apply();
    }

    public static void removeDataByGroupName(String name) {
        getPreferences().edit().remove(GROUP_PKG_PREFIX + name).apply();
    }

    public static boolean getEnablePassword() {
        return getPreferences().getBoolean(ENABLE_PASSWORD, true);
    }

    public static void setEnablePassword(boolean value) {
        getPreferences().edit().putBoolean(ENABLE_PASSWORD, value).apply();
    }

    public static String getLockPassword() {
        return getPreferences().getString(LOCK_PASSWORD, "");
    }

    public static void setLockPassword(String value) {
        getPreferences().edit().putString(LOCK_PASSWORD, value).apply();
    }

    public static boolean getIsLocked() {
        return getPreferences().getBoolean(IS_LOCKED, true);
    }

    public static void setIsLocked(boolean value) {
        getPreferences().edit().putBoolean(IS_LOCKED, value).apply();
    }

    public static void setIsChanningPassword(boolean value) {
        getPreferences().edit().putBoolean(IS_CHANNING_PASSWORD, value).apply();
    }

    public static boolean getIsChanningPassword() {
        return getPreferences().getBoolean(IS_CHANNING_PASSWORD, false);
    }

    public static boolean getIsOpenOtherActivity() {
        return getPreferences().getBoolean(IS_OPEN_OTHER_ACTIVITY, false);
    }

    public static void setIsOpenOtherActivity(boolean value) {
        getPreferences().edit().putBoolean(IS_OPEN_OTHER_ACTIVITY, value).apply();
    }
}
