package moe.shizuku.manager.statistics.raw

import android.content.res.Resources
import android.os.Bundle
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppBarFragmentActivity
import moe.shizuku.manager.statistics.SegmentTime
import moe.shizuku.manager.utils.getTimeAsString
import java.util.Calendar

class RawStatisticsActivity: AppBarFragmentActivity() {
    override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
        super.onApplyUserThemeResource(theme, isDecorView)
        theme.applyStyle(R.style.ThemeOverlay_Rikka_Material3_Preference, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = this.getString(R.string.data_details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val fragment = RawStatisticsFragment().apply {
                arguments = Bundle().apply {
                    putString("RAW_STATISTICS_ID", intent.getStringExtra("RAW_STATISTICS_ID") ?: "")
                    putSerializable("SEGMENT_ENUM_ID", intent.getIntExtra("SEGMENT_ENUM_ID", SegmentTime.DAY.id))
                    putString("DATE_INDICATOR", intent.getStringExtra("DATE_INDICATOR") ?: Calendar.getInstance().time.getTimeAsString())
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
}