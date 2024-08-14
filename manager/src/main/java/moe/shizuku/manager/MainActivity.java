package moe.shizuku.manager;

import static moe.shizuku.manager.utils.ExtensionsKt.hasBatteryOptimizationExemption;
import static moe.shizuku.manager.utils.ExtensionsKt.hasNotificationPermission;
import static moe.shizuku.manager.utils.ExtensionsKt.isDialogFragmentShowing;

import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.DialogFragment;

import moe.shizuku.manager.home.HomeActivity;
import moe.shizuku.manager.home.RequiredPermissionDialogFragment;
import moe.shizuku.manager.lock.LockDialogFragment;
import moe.shizuku.manager.utils.AutoStartPermissionHelper;
import moe.shizuku.manager.utils.ExtensionsKt;

public class MainActivity extends HomeActivity {

    private final DialogFragment lockFragment = new LockDialogFragment();
    private final RequiredPermissionDialogFragment requiredPermissionDialogFragment = new RequiredPermissionDialogFragment();
    private final AutoStartPermissionHelper autoStartPermissionHelper = AutoStartPermissionHelper.Companion.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        ExtensionsKt.checkLockAppsPermission(this);
        ExtensionsKt.checkHideAppsPermission();
        if (!hasNotificationPermission(this) ||
                !hasBatteryOptimizationExemption(this) ||
                !(!autoStartPermissionHelper.isAutoStartPermissionAvailable(this, false) || autoStartPermissionHelper.getAutoStartPermission(this, false, false))
        ) {
            if (!isDialogFragmentShowing(requiredPermissionDialogFragment)) {
                requiredPermissionDialogFragment.show(getSupportFragmentManager(), "RequiredPermission");
            }
        }
        if ((ShizukuSettings.getLockPassword().isEmpty() || ShizukuSettings.getEnablePassword()) && ShizukuSettings.getIsLocked() && !ShizukuSettings.getIsOpenOtherActivity()) {
            long lastTime = ShizukuSettings.getTimeoutLandmark();
            long timeout = ShizukuSettings.getTimeoutPassword();
            if (lastTime != 0L && System.currentTimeMillis() - lastTime <= timeout) {
                super.onResume();
                return;
            }
            if (!isDialogFragmentShowing(lockFragment)) {
                lockFragment.show(getSupportFragmentManager(), "LockDialogFragment");
            }
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (ShizukuSettings.getEnablePassword()) {
            ShizukuSettings.saveTimeoutLandmark(System.currentTimeMillis());
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        ShizukuSettings.setIsLocked(true);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        ShizukuSettings.setIsOpenOtherActivity(false);
        super.onDestroy();
    }
}
