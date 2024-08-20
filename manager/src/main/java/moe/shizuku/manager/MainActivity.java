package moe.shizuku.manager;

import static moe.shizuku.manager.utils.ExtensionsKt.hasBatteryOptimizationExemption;
import static moe.shizuku.manager.utils.ExtensionsKt.hasNotificationPermission;
import static moe.shizuku.manager.utils.ExtensionsKt.isDialogFragmentShowing;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import moe.shizuku.manager.app.AppBarActivity;
import moe.shizuku.manager.databinding.ActivityMainBinding;
import moe.shizuku.manager.focus.FocusFragment;
import moe.shizuku.manager.hidden.HiddenFragment;
import moe.shizuku.manager.hidden.RequiredPermissionDialogFragment;
import moe.shizuku.manager.lock.LockDialogFragment;
import moe.shizuku.manager.utils.AutoStartPermissionHelper;
import moe.shizuku.manager.utils.ExtensionsKt;

public class MainActivity extends AppBarActivity {

    private final DialogFragment lockFragment = new LockDialogFragment();
    private final RequiredPermissionDialogFragment requiredPermissionDialogFragment = new RequiredPermissionDialogFragment();
    private final AutoStartPermissionHelper autoStartPermissionHelper = AutoStartPermissionHelper.Companion.getInstance();
    private ActivityMainBinding binding;

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        BottomNavigationView bottomNavigation = binding.bottomNavigation;
        NavController navController = Navigation.findNavController(this, R.id.fragment_container_view);
        navController.setGraph(R.navigation.nav_graph);
        NavigationUI.setupWithNavController(bottomNavigation, navController);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                return;
            }
            if (!isDialogFragmentShowing(lockFragment)) {
                lockFragment.show(getSupportFragmentManager(), "LockDialogFragment");
            }
        }
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
