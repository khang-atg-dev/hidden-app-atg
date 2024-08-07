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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import moe.shizuku.manager.utils.EmptySharedPreferencesImpl;
import moe.shizuku.manager.utils.EnvironmentUtils;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class ShizukuSettings {

    public static final String NAME = "settings";
    public static final String NIGHT_MODE = "night_mode";
    public static final String LANGUAGE = "language";
    public static final String KEEP_START_ON_BOOT = "start_on_boot";
    public static final String LOCK_APPS = "lock_apps";
    public static final String GROUP_LOCK_APPS = "group_lock_apps";

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

    public static void saveGroupLockedApp(Set<String> pkgs) {
        String pkgsStr;
        if (pkgs.size() == 1) {
            pkgsStr = pkgs.iterator().next();
        } else {
            pkgsStr = String.join(",", pkgs);
        }
        getPreferences().edit().putString(GROUP_LOCK_APPS, pkgsStr).apply();
    }

    public static void removeGroupLockedApp(String pkg) {
        Set<String> packages = getGroupLockedAppsAsSet();
        packages.remove(pkg);
        String pkgsStr = String.join(",", packages);
        getPreferences().edit().putString(LOCK_APPS, pkgsStr).apply();
    }

    public static Set<String> getPksByGroupName(String name) {
        return getPreferences().getStringSet(name, Collections.emptySet());
    }

    public static void savePksByGroupName(String name, Set<String> pkgs) {
        getPreferences().edit().putStringSet(name, pkgs).apply();
    }
}
