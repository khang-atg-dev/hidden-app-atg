package moe.shizuku.manager.focus.details

import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.app.AppBarFragmentActivity

class FocusDetailsActivity : AppBarFragmentActivity() {
    override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
        super.onApplyUserThemeResource(theme, isDecorView)
        theme.applyStyle(R.style.ThemeOverlay_Rikka_Material3_Preference, true)
    }

    override fun onStart() {
        super.onStart()
//        ShizukuSettings.setIsOpenOtherActivity(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideToolbar()
        val currentFocus = ShizukuSettings.getCurrentFocusTask()
        if (currentFocus == null) this.finish()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FocusDetailsFragment())
                .commit()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {

            }
        }
    }

    override fun onStop() {
        ShizukuSettings.setIsOpenOtherActivity(false)
        super.onStop()
    }

    override fun onBackPressed() {

    }
}