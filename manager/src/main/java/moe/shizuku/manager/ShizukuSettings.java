package moe.shizuku.manager;

import static java.lang.annotation.RetentionPolicy.SOURCE;
import static moe.shizuku.manager.AppConstants.DEFAULT_AUTO_LOCK_TIMEOUT;
import static moe.shizuku.manager.AppConstants.GROUP_PKG_PREFIX;

import android.app.ActivityThread;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import moe.shizuku.manager.model.CurrentFocus;
import moe.shizuku.manager.model.Focus;
import moe.shizuku.manager.model.GroupApps;
import moe.shizuku.manager.model.StatisticFocus;
import moe.shizuku.manager.utils.EmptySharedPreferencesImpl;
import moe.shizuku.manager.utils.EnvironmentUtils;

public class ShizukuSettings {

    public static final String NAME = "settings";
    public static final String NIGHT_MODE = "night_mode";
    public static final String LANGUAGE = "language";
    public static final String KEEP_START_ON_BOOT = "start_on_boot";

    //
    public static final String GROUP_LOCK_APPS = "group_lock_apps";
    public static final String ENABLE_PASSWORD = "enable_password";
    public static final String TIME_OUT_PASSWORD = "time_out_password";
    public static final String TIME_OUT_LANDMARK = "time_out_landmark";
    public static final String LOCK_PASSWORD = "LOCK_PASSWORD";
    public static final String IS_LOCKED = "IS_LOCKED";
    public static final String IS_CHANNING_PASSWORD = "IS_CHANNING_PASSWORD";
    public static final String IS_OPEN_OTHER_ACTIVITY = "IS_OPEN_OTHER_ACTIVITY";
    public static final String GROUP_APPS_IS_HIDDEN = "GROUP_APPS_IS_HIDDEN";
    public static final String FOCUS_TASK_LIST = "FOCUS_TASK_LIST";
    public static final String CURRENT_FOCUS_TASK = "CURRENT_FOCUS_TASK";
    public static final String COLOR_CURRENT_TASK = "COLOR_CURRENT_TASK";
    public static final String KEEP_SCREEN_ON_CURRENT_TASK = "KEEP_SCREEN_ON_CURRENT_TASK";
    public static final String STATISTICS_OF_FOCUS = "STATISTICS_OF_FOCUS";
    public static final String STATISTICS_OF_CURRENT_FOCUS = "STATISTICS_OF_CURRENT_FOCUS";


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
        return new HashSet<>(Arrays.asList(pkgStr.split("/-/")));
    }

    public static void saveGroupLockedApps(String id) {
        List<String> pkgs = new ArrayList<>(getGroupLockedAppsAsSet());
        pkgs.add(GROUP_PKG_PREFIX + id);
        String pkgsStr = String.join("/-/", pkgs);
        getPreferences().edit().putString(GROUP_LOCK_APPS, pkgsStr).apply();
    }

    public static void removeGroupLockedApp(String name) {
        Set<String> names = getGroupLockedAppsAsSet();
        names.remove(GROUP_PKG_PREFIX + name);
        String namesStr = String.join("/-/", names);
        getPreferences().edit().putString(GROUP_LOCK_APPS, namesStr).apply();
    }

    @Nullable
    public static GroupApps getPksById(String id) {
        String objStr = getPreferences().getString(GROUP_PKG_PREFIX + id, "");
        if (objStr.isEmpty()) return null;
        try {
            return gson.fromJson(objStr, GroupApps.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static long findTimeoutOfPkg(String pkg) {
        Set<String> groups = getGroupLockedAppsAsSet();
        long minTimeout = -1L;
        for (String group : groups) {
            String groupName = group.substring(group.lastIndexOf(".") + 1);
            GroupApps groupApps = getPksById(groupName);
            if (groupApps != null && groupApps.getPkgs().contains(pkg)) {
                if (groupApps.isLocked()) {
                    long timeout = groupApps.getTimeOut();
                    if (minTimeout < 0) minTimeout = timeout;
                    else if (timeout < minTimeout) minTimeout = timeout;
                    if (minTimeout == 0) return minTimeout;
                }
            }
        }
        return minTimeout == -1L ? DEFAULT_AUTO_LOCK_TIMEOUT : minTimeout;
    }

    public static void saveDataById(String id, GroupApps data) {
        getPreferences().edit().putString(GROUP_PKG_PREFIX + id, gson.toJson(data)).apply();
    }

    public static void removeDataById(String id) {
        getPreferences().edit().remove(GROUP_PKG_PREFIX + id).apply();
    }

    public static boolean getEnablePassword() {
        return getPreferences().getBoolean(ENABLE_PASSWORD, false);
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

    public static void saveAppsIsHidden(Set<String> pkg) {
        List<String> existPkgs = new ArrayList<>(getAppsIsHidden());
        existPkgs.addAll(pkg);
        getPreferences().edit().putStringSet(GROUP_APPS_IS_HIDDEN, new HashSet<>(existPkgs)).apply();
    }

    public static Set<String> getAppsIsHidden() {
        return getPreferences().getStringSet(GROUP_APPS_IS_HIDDEN, Collections.emptySet());
    }

    public static void removeAppsIsHidden(Set<String> pkg) {
        List<String> existPkgs = new ArrayList<>(getAppsIsHidden());
        existPkgs.removeAll(pkg);
        getPreferences().edit().putStringSet(GROUP_APPS_IS_HIDDEN, new HashSet<>(existPkgs)).apply();
    }

    public static void saveUnlockStatus(String pkg, boolean isUnlock) {
        getPreferences().edit().putBoolean(pkg, isUnlock).apply();
    }

    public static boolean getUnlockStatus(String pkg) {
        return getPreferences().getBoolean(pkg, false);
    }

    public static long getTimeoutPassword() {
        return getPreferences().getLong(TIME_OUT_PASSWORD, 0L);
    }

    public static void saveTimeoutPassword(long value) {
        getPreferences().edit().putLong(TIME_OUT_PASSWORD, value).apply();
    }

    public static long getTimeoutLandmark() {
        return getPreferences().getLong(TIME_OUT_LANDMARK, 0L);
    }

    public static void saveTimeoutLandmark(long value) {
        getPreferences().edit().putLong(TIME_OUT_LANDMARK, value).apply();
    }

    public static void saveFocusTask(Focus focusTask) {
        List<Focus> focusList = getFocusTasks();
        if (focusList.isEmpty()) focusList = new ArrayList<>();
        focusList.add(focusTask);
        getPreferences().edit().putString(FOCUS_TASK_LIST, gson.toJson(focusList)).apply();
    }

    public static void updateFocusTask(Focus focusTask) {
        List<Focus> focusList = getFocusTasks();
        if (focusList.isEmpty()) return;
        List<Focus> updatedList = focusList.stream()
                .map(obj -> obj.getId().equals(focusTask.getId()) ? focusTask : obj)
                .collect(Collectors.toList());
        getPreferences().edit().putString(FOCUS_TASK_LIST, gson.toJson(updatedList)).apply();
    }

    public static void removeFocusTask(String id) {
        List<Focus> focusList = getFocusTasks();
        if (!focusList.isEmpty()) {
            focusList.removeIf(i -> i.getId().equals(id));
            getPreferences().edit().putString(FOCUS_TASK_LIST, gson.toJson(focusList)).apply();
        }
    }

    public static List<Focus> getFocusTasks() {
        String objStr = getPreferences().getString(FOCUS_TASK_LIST, "");
        if (objStr.isEmpty()) return Collections.emptyList();
        try {
            return gson.fromJson(objStr, new TypeToken<List<Focus>>() {
            }.getType());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Nullable
    public static Focus getFocusTaskById(String id) {
        String objStr = getPreferences().getString(FOCUS_TASK_LIST, "");
        if (objStr.isEmpty()) return null;
        try {
            List<Focus> list = gson.fromJson(
                    objStr,
                    new TypeToken<List<Focus>>() {
                    }.getType()
            );
            Optional<Focus> target = list.stream()
                    .filter(obj -> obj.getId().equals(id))
                    .findAny();
            return target.orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveCurrentFocusTask(CurrentFocus focus) {
        getPreferences().edit().putString(CURRENT_FOCUS_TASK, gson.toJson(focus)).apply();
    }

    public static void updateIsPausedCurrentFocusTask(boolean value) {
        CurrentFocus currentFocus = getCurrentFocusTask();
        if (currentFocus == null) return;
        saveCurrentFocusTask(
                currentFocus.copy(
                        currentFocus.getId(),
                        currentFocus.getStatisticFocusId(),
                        currentFocus.getName(),
                        currentFocus.getTime(),
                        currentFocus.getRemainingTime(),
                        value
                )
        );
    }

    @Nullable
    public static CurrentFocus getCurrentFocusTask() {
        String objStr = getPreferences().getString(CURRENT_FOCUS_TASK, null);
        if (objStr == null || objStr.isEmpty()) return null;
        try {
            return gson.fromJson(objStr, CurrentFocus.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static void removeCurrentFocusTask() {
        getPreferences().edit().remove(CURRENT_FOCUS_TASK).apply();
        StatisticFocus statisticFocus = getStatisticsOfCurrentFocus();
        if (statisticFocus != null && statisticFocus.getRunningTime() >= 60 * 1000) {
            saveStatistics(statisticFocus);
        }
        removeStatisticsOfCurrentFocus();
    }

    @Nullable
    public static String getColorCurrentTask() {
        return getPreferences().getString(COLOR_CURRENT_TASK, null);
    }

    public static void saveColorCurrentTask(String value) {
        getPreferences().edit().putString(COLOR_CURRENT_TASK, value).apply();
    }

    public static boolean getKeepScreenOnCurrentTask() {
        return getPreferences().getBoolean(KEEP_SCREEN_ON_CURRENT_TASK, false);
    }

    public static void setKeepScreenOnCurrentTask(boolean value) {
        getPreferences().edit().putBoolean(KEEP_SCREEN_ON_CURRENT_TASK, value).apply();
    }

    private static void saveStatistics(StatisticFocus focus) {
        List<StatisticFocus> allStatistics = getAllStatistics();
        if (allStatistics == null) {
            allStatistics = new ArrayList<>();
        }
        allStatistics.add(focus);
        getPreferences().edit().putString(STATISTICS_OF_FOCUS, gson.toJson(allStatistics)).apply();
    }

    @Nullable
    public static List<StatisticFocus> getAllStatistics() {
        try {
            String objStr = getPreferences().getString(STATISTICS_OF_FOCUS, "");
            if (objStr.isEmpty()) return null;
            return gson.fromJson(
                    objStr,
                    new TypeToken<List<StatisticFocus>>() {
                    }.getType()
            );
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveStatisticsOfCurrentFocus(StatisticFocus value) {
        try {
            String objStr = gson.toJson(value);
            getPreferences().edit().putString(STATISTICS_OF_CURRENT_FOCUS, objStr).apply();
        } catch (Exception ignored) {
        }
    }

    public static void updateRunningTimeStatisticCurrentFocus(Long time) {
        try {
            StatisticFocus current = getStatisticsOfCurrentFocus();
            if (current == null) return;
            StatisticFocus newStatistics = new StatisticFocus(
                    current.getId(),
                    current.getFocusId(),
                    current.getName(),
                    current.getTime(),
                    current.getRunningTime() + time,
                    current.getPauseTime(),
                    current.getStartTime(),
                    current.getEndTime()
            );
            saveStatisticsOfCurrentFocus(newStatistics);
        } catch (Exception ignored) {
        }
    }

    public static void updateEndTimeStatisticCurrentFocus(String endTime) {
        try {
            StatisticFocus current = getStatisticsOfCurrentFocus();
            if (current == null) return;
            StatisticFocus newStatistics = current.copy(
                    current.getId(),
                    current.getFocusId(),
                    current.getName(),
                    current.getTime(),
                    current.getRunningTime(),
                    current.getPauseTime(),
                    current.getStartTime(),
                    endTime
            );
            saveStatisticsOfCurrentFocus(newStatistics);
        } catch (Exception ignored) {
        }
    }

    public static void updatePauseTimeStatisticCurrentFocus() {
        try {
            StatisticFocus current = getStatisticsOfCurrentFocus();
            if (current == null) return;
            StatisticFocus newStatistics = current.copy(
                    current.getId(),
                    current.getFocusId(),
                    current.getName(),
                    current.getTime(),
                    current.getRunningTime(),
                    current.getPauseTime() + 1,
                    current.getStartTime(),
                    current.getEndTime()
            );
            saveStatisticsOfCurrentFocus(newStatistics);
        } catch (Exception ignored) {
        }
    }

    private static void removeStatisticsOfCurrentFocus() {
        getPreferences().edit().remove(STATISTICS_OF_CURRENT_FOCUS).apply();
    }

    @Nullable
    private static StatisticFocus getStatisticsOfCurrentFocus() {
        try {
            String objStr = getPreferences().getString(STATISTICS_OF_CURRENT_FOCUS, "");
            if (objStr.isEmpty()) return null;
            return gson.fromJson(objStr, StatisticFocus.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}
