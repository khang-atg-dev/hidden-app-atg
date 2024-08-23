package moe.shizuku.manager;

import static moe.shizuku.manager.utils.ExtensionsKt.hasBatteryOptimizationExemption;
import static moe.shizuku.manager.utils.ExtensionsKt.hasNotificationPermission;
import static moe.shizuku.manager.utils.ExtensionsKt.isDialogFragmentShowing;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import moe.shizuku.manager.account.AccountFragment;
import moe.shizuku.manager.app.AppBarActivity;
import moe.shizuku.manager.databinding.ActivityMainBinding;
import moe.shizuku.manager.focus.FocusFragment;
import moe.shizuku.manager.focus.details.CountdownService;
import moe.shizuku.manager.focus.details.FocusDetailsActivity;
import moe.shizuku.manager.hidden.HiddenFragment;
import moe.shizuku.manager.hidden.RequiredPermissionDialogFragment;
import moe.shizuku.manager.lock.LockDialogFragment;
import moe.shizuku.manager.model.CurrentFocus;
import moe.shizuku.manager.settings.SettingsActivity;
import moe.shizuku.manager.statistics.StatisticsFragment;
import moe.shizuku.manager.utils.AutoStartPermissionHelper;
import moe.shizuku.manager.utils.ExtensionsKt;

public class MainActivity extends AppBarActivity {
    private final DialogFragment lockFragment = new LockDialogFragment();
    private final RequiredPermissionDialogFragment requiredPermissionDialogFragment = new RequiredPermissionDialogFragment();
    private final AutoStartPermissionHelper autoStartPermissionHelper = AutoStartPermissionHelper.Companion.getInstance();
    private final HiddenFragment hiddenFragment = new HiddenFragment();
    private final StatisticsFragment statisticsFragment = new StatisticsFragment();
    private final AccountFragment accountFragment = new AccountFragment();
    private final FocusFragment focusFragment = new FocusFragment();
    private final FragmentManager fm = getSupportFragmentManager();
    private ActivityMainBinding binding;
    private Fragment activeFragment = focusFragment;
    private boolean showMenu = true;

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        BottomNavigationView bottomNavigationView = binding.bottomNavigation;
        fm.beginTransaction().add(R.id.fragment_container_view, accountFragment, "4").hide(accountFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container_view, hiddenFragment, "3").hide(hiddenFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container_view, statisticsFragment, "2").hide(statisticsFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container_view, focusFragment, "1").commit();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(item.getTitle());
                showMenu = item.getItemId() == R.id.hiddenFragment || item.getItemId() == R.id.focusFragment;
                invalidateOptionsMenu();
            }
            boolean backward = true;
            FragmentTransaction transaction = fm.beginTransaction();
            @SuppressLint("NonConstantResourceId")
            Fragment nextFragment = switch (item.getItemId()) {
                case R.id.accountFragment -> {
                    backward = false;
                    yield accountFragment;
                }
                case R.id.hiddenFragment -> {
                    backward = activeFragment == accountFragment;
                    yield hiddenFragment;
                }
                case R.id.statisticsFragment -> {
                    backward = activeFragment == hiddenFragment;
                    yield statisticsFragment;
                }
                case R.id.focusFragment -> focusFragment;
                default -> activeFragment;
            };
            transaction.setCustomAnimations(
                    backward ? R.anim.slide_in_left : R.anim.slide_in_right,
                    backward ? R.anim.slide_out_right : R.anim.slide_out_left,
                    backward ? R.anim.slide_in_right : R.anim.slide_in_left,
                    backward ? R.anim.slide_out_left : R.anim.slide_out_right
            );
            transaction.hide(activeFragment).show(nextFragment);
            transaction.commit();
            activeFragment = nextFragment;
            return true;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Focus");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (showMenu) {
            if (activeFragment == hiddenFragment)
                getMenuInflater().inflate(R.menu.hidden_menu, menu);
            else
                getMenuInflater().inflate(R.menu.focus_menu, menu);
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.add_group) {
            hiddenFragment.onClickAddGroup();
            return true;
        } else if (item.getItemId() == R.id.add_focus) {
            focusFragment.onAddFocusTask();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        CurrentFocus currentFocus = ShizukuSettings.getCurrentFocusTask();
        if (currentFocus != null) {
            this.stopService(new Intent(this, CountdownService.class));
            Intent intent = new Intent(this, FocusDetailsActivity.class);
            startActivity(intent);
        }
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
