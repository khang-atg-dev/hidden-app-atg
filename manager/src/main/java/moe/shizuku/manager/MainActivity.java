package moe.shizuku.manager;

import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.DialogFragment;

import moe.shizuku.manager.home.CreateGroupBottomSheetDialogFragment;
import moe.shizuku.manager.home.HomeActivity;
import moe.shizuku.manager.lock.LockFragment;

public class MainActivity extends HomeActivity {

    private final DialogFragment lockFragment = new LockFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        if (ShizukuSettings.getEnablePassword() && ShizukuSettings.getIsLocked() && !ShizukuSettings.getIsOpenOtherActivity()) {
            lockFragment.show(getSupportFragmentManager(), "my_dialog");
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
}
