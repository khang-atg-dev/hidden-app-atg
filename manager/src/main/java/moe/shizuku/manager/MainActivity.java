package moe.shizuku.manager;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import moe.shizuku.manager.home.HomeActivity;
import moe.shizuku.manager.lock.LockDialogFragment;
import moe.shizuku.manager.utils.ExtensionsKt;

public class MainActivity extends HomeActivity {

    private final DialogFragment lockFragment = new LockDialogFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        ExtensionsKt.checkLockAppsPermission(this);
        ExtensionsKt.checkHideAppsPermission();
        if (ShizukuSettings.getEnablePassword() && ShizukuSettings.getIsLocked() && !ShizukuSettings.getIsOpenOtherActivity()) {
            if (!lockFragment.isVisible()) lockFragment.show(getSupportFragmentManager(), "my_dialog");
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
