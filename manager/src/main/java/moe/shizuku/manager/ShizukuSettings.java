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

import moe.shizuku.manager.utils.EmptySharedPreferencesImpl;
import moe.shizuku.manager.utils.EnvironmentUtils;

import static java.lang.annotation.RetentionPolicy.SOURCE;
import static moe.shizuku.manager.AppConstants.GROUP_PKG_PREFIX;

public class ShizukuSettings {

    public static final String NAME = "settings";
    public static final String NIGHT_MODE = "night_mode";
    public static final String LANGUAGE = "language";
    public static final String KEEP_START_ON_BOOT = "start_on_boot";
    public static final String ENABLE_HIDE_APP = "enable_hide_app";
    public static final String ENABLE_LOCK_APP = "enable_lock_app";
    public static final String HIDE_APPS = "hide_apps";
    public static final String LOCK_APPS = "lock_apps";
    public static final String GROUP_LOCK_APPS = "group_lock_apps";
    public static final String ENABLE_PASSWORD = "enable_password";
    public static final String AUTO_LOCK_TIMEOUT = "auto_lock_timeout";
    public static final String LOCK_PASSWORD = "LOCK_PASSWORD";
    public static final String IS_LOCKED = "IS_LOCKED";
    public static final String IS_CHANNING_PASSWORD = "IS_CHANNING_PASSWORD";
    public static final String IS_OPEN_OTHER_ACTIVITY = "IS_OPEN_OTHER_ACTIVITY";


    private static SharedPreferences sPreferences;

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

    public static Boolean isHideEnabled() {
        return getPreferences().getBoolean(ENABLE_HIDE_APP, false);
    }

    public static void setHideApp(boolean value) {
        getPreferences().edit().putBoolean(ENABLE_HIDE_APP, value).apply();
    }

    public static Boolean isLockEnabled() {
        return getPreferences().getBoolean(ENABLE_LOCK_APP, false);
    }

    public static void setLockApp(boolean value) {
        getPreferences().edit().putBoolean(ENABLE_LOCK_APP, value).apply();
    }

    public static Set<String> getListHiddenAppsAsSet() {
        String pkgStr = getPreferences().getString(HIDE_APPS, null);
        if (pkgStr == null || pkgStr.isEmpty()) return Collections.emptySet();
        return new HashSet<>(Arrays.asList(pkgStr.split(",")));
    }

    public static void saveHiddenApp(Set<String> pkgs) {
        String pkgsStr;
        if (pkgs.size() == 1) {
            pkgsStr = pkgs.iterator().next();
        } else {
            pkgsStr = String.join(",", pkgs);
        }
        getPreferences().edit().putString(HIDE_APPS, pkgsStr).apply();
    }

    public static void removeHiddenApp(String pkg) {
        Set<String> packages = getListHiddenAppsAsSet();
        packages.remove(pkg);
        String pkgsStr = String.join(",", packages);
        getPreferences().edit().putString(HIDE_APPS, pkgsStr).apply();
    }

    public static Set<String> getListLockedAppsAsSet() {
        String pkgStr = getPreferences().getString(LOCK_APPS, null);
        if (pkgStr == null || pkgStr.isEmpty()) return Collections.emptySet();
        return new HashSet<>(Arrays.asList(pkgStr.split(",")));
    }

    public static void saveLockedApp(Set<String> pkgs) {
        String pkgsStr;
        if (pkgs.size() == 1) {
            pkgsStr = pkgs.iterator().next();
        } else {
            pkgsStr = String.join(",", pkgs);
        }
        getPreferences().edit().putString(LOCK_APPS, pkgsStr).apply();
    }

    public static void removeLockedApp(String pkg) {
        Set<String> packages = getListLockedAppsAsSet();
        packages.remove(pkg);
        String pkgsStr = String.join(",", packages);
        getPreferences().edit().putString(LOCK_APPS, pkgsStr).apply();
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

    public static Set<String> getPksByGroupName(String name) {
        return getPreferences().getStringSet(name, Collections.emptySet());
    }

    public static void savePksByGroupName(String name, Set<String> pkgs) {
        getPreferences().edit().putStringSet(GROUP_PKG_PREFIX + name, pkgs).apply();
    }

    public static boolean getEnablePassword() {
        return getPreferences().getBoolean(ENABLE_PASSWORD, true);
    }

    public static void setEnablePassword(boolean value) {
        getPreferences().edit().putBoolean(ENABLE_PASSWORD, value).apply();
    }

    public static int getAutoLockTimeout() {
        return getPreferences().getInt(AUTO_LOCK_TIMEOUT, 0);
    }

    public static void setAutoLockTimeout(int value) {
        getPreferences().edit().putInt(AUTO_LOCK_TIMEOUT, value).apply();
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
