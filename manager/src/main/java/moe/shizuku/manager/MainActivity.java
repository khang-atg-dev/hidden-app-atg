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
        if (
                !hasNotificationPermission(this) ||
                        !hasBatteryOptimizationExemption(this) ||
                        !autoStartPermissionHelper.getAutoStartPermission(this, false, false)
        ) {
            if (!isDialogFragmentShowing(requiredPermissionDialogFragment)) {
                requiredPermissionDialogFragment.show(getSupportFragmentManager(), "RequiredPermission");
            }
        }
        if (ShizukuSettings.getEnablePassword() && ShizukuSettings.getIsLocked() && !ShizukuSettings.getIsOpenOtherActivity()) {
            if (!isDialogFragmentShowing(lockFragment)) {
                lockFragment.show(getSupportFragmentManager(), "my_dialog");
            }
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
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
