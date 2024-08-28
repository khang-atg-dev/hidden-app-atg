package moe.shizuku.manager;

public class AppConstants {

    public static final String TAG = "ShizukuManager";

    public static final String NOTIFICATION_CHANNEL_STATUS = "starter";
    public static final String NOTIFICATION_CHANNEL_WORK = "work";
    public static final int NOTIFICATION_ID_STATUS = 1;
    public static final int NOTIFICATION_ID_WORK = 2;

    private static final String PACKAGE = "moe.shizuku.manager";
    public static final String EXTRA = PACKAGE + ".extra";
    public static final String GROUP_PKG_PREFIX = "hidden.group.";
    public static final int PASSWORD_LENGTH = 4;

    public static final String RELOAD_PACKAGES_FOR_LOCK = "RELOAD_PACKAGES_FOR_LOCK";
    public static final Long DEFAULT_AUTO_LOCK_TIMEOUT = 0L;
    public static final Long DEFAULT_TIME_FOCUS = 25 * 60 * 1000L;
    public static final String FORMAT_TIME = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String FORMAT_DATE_TIME = "yyyy-MM-dd";
    public static final String FORMAT_MONTH_DAY_TIME = "MM-dd";
    public static final String FORMAT_YEAR_MONTH_TIME = "yyyy-MM";
    public static final String FORMAT_YEAR_TIME = "yyyy";
}
