package moe.shizuku.manager.home

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.AppConstants.GROUP_PKG_PREFIX
import moe.shizuku.manager.AppConstants.RELOAD_PACKAGES_FOR_LOCK
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.databinding.HomeActivityBinding
import moe.shizuku.manager.management.appsViewModel
import moe.shizuku.manager.service.MainForegroundService
import moe.shizuku.manager.settings.SettingsActivity
import moe.shizuku.manager.shizuku.ShizukuActivity
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.utils.isAccessibilityServiceEnabled
import moe.shizuku.manager.utils.isCanDrawOverlays
import rikka.core.ktx.unsafeLazy
import rikka.lifecycle.Status
import rikka.lifecycle.viewModels
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect

abstract class HomeActivity : AppBarActivity(), HomeCallback {
    private val homeModel by viewModels { HomeViewModel(this) }
    private val appsModel by appsViewModel()
    private val adapter by unsafeLazy { HomeAdapter() }
    private val apps = mutableListOf<PackageInfo>()
    private val lockPermissionDialogFragment = LockPermissionDialogFragment()
    private val createGroupBottomSheet = CreateGroupBottomSheetDialogFragment()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            ShizukuSettings.setIsOpenOtherActivity(false)
        }
        if (isGranted) {
            val serviceIntent = Intent(this, MainForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        homeModel.reloadGroupApps()
        homeModel.reloadServiceStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        writeStarterFiles()

        val binding = HomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createGroupBottomSheet.setCallback(homeModel)

        lifecycleScope.launch {
            homeModel.events.flowWithLifecycle(
                this@HomeActivity.lifecycle,
                Lifecycle.State.STARTED
            ).collectLatest {
                when (it) {
                    is HomeEvents.ShowShirukuAlert -> {
                        MaterialAlertDialogBuilder(this@HomeActivity)
                            .setTitle("Shiruku inactive")
                            .setMessage(it.message)
                            .setPositiveButton("Go to settings") { _, _ ->
                                this@HomeActivity.startActivity(
                                    Intent(
                                        this@HomeActivity,
                                        ShizukuActivity::class.java
                                    )
                                )
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()
                            .show()
                    }

                    HomeEvents.ShowAutoStartNotice -> {
                        MaterialAlertDialogBuilder(this@HomeActivity)
                            .setTitle("Auto-Start Permission")
                            .setMessage("Please enable auto-start for this app to ensure it functions correctly.")
                            .setCancelable(false)
                            .setPositiveButton("Ok") { _, _ ->
                                if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) {
                                    ShizukuSettings.setIsShowAutoStartNotice(true)
                                    val intent = Intent("miui.intent.action.OP_AUTO_START").apply {
                                        addCategory(Intent.CATEGORY_DEFAULT)
                                        putExtra("package_name", packageName)
                                    }
                                    startActivity(intent)
                                }
                            }
                            .create()
                            .show()
                    }

                    HomeEvents.RequestIgnoreBatteryOptimizations -> {
                        MaterialAlertDialogBuilder(this@HomeActivity)
                            .setTitle("Battery optimization")
                            .setMessage("Please allow Shiruku to ignore battery optimization")
                            .setPositiveButton("Allow") { _, _ ->
                                val intent = Intent().apply {
                                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                    data = Uri.parse("package:$packageName")
                                }
                                this@HomeActivity.startActivity(intent)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()
                            .show()
                    }

                    HomeEvents.RefreshLock -> {
                        this@HomeActivity.sendBroadcast(
                            Intent(RELOAD_PACKAGES_FOR_LOCK).setPackage(
                                this@HomeActivity.packageName
                            )
                        )
                    }
                }
            }
        }

        homeModel.serviceStatus.observe(this) {
            if (it.status == Status.SUCCESS) {
                homeModel.serviceStatus.value?.data ?: return@observe
            }
        }

        homeModel.groupApps.observe(this) {
            if (it.status == Status.SUCCESS) {
                adapter.updateData(it.data)
            }
        }

        appsModel.packages.observe(this) {
            if (it.status == Status.SUCCESS && !it.data.isNullOrEmpty()) {
                apps.addAll(it.data ?: emptyList())
            }
        }

        if (appsModel.packages.value == null) {
            appsModel.loadApps(this)
        }

        if (homeModel.groupApps.value == null) {
            homeModel.reloadGroupApps()
        }

        val recyclerView = binding.list
        recyclerView.adapter = adapter
        recyclerView.fixEdgeEffect()
        recyclerView.addItemSpacing(top = 4f, bottom = 4f, unit = TypedValue.COMPLEX_UNIT_DIP)
        recyclerView.addEdgeSpacing(
            top = 4f,
            bottom = 4f,
            left = 16f,
            right = 16f,
            unit = TypedValue.COMPLEX_UNIT_DIP
        )

        adapter.listener = this
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClickAddGroup() {
        CreateGroupBottomSheetDialogFragment().apply {
            this.updateData(this@HomeActivity, apps)
            this.setCallback(homeModel)
            this.show(
                supportFragmentManager,
                "GroupAppsBottomSheet"
            )
        }
    }

    override fun onClickGroup(groupName: String) {
        createGroupBottomSheet.let {
            it.updateData(
                this@HomeActivity,
                apps,
                ShizukuSettings.getPksByGroupName(groupName)
            )
            it.show(
                supportFragmentManager,
                "GroupAppsBottomSheet"
            )
        }
    }

    override fun onDeleteGroup(groupName: String) {
        MaterialAlertDialogBuilder(this@HomeActivity)
            .setTitle("Do you want to delete this group?")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                homeModel.onDeleteGroup(groupName)
                ShizukuSettings.removeGroupLockedApp(GROUP_PKG_PREFIX + groupName)
                ShizukuSettings.removeDataByGroupName(groupName)
                homeModel.reloadPkgLock()
                homeModel.reloadGroupApps()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    override fun onActionHide(groupName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.areNotificationsEnabled()) {
                val serviceIntent = Intent(this, MainForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                if (homeModel.checkBatteryOptimizationRemoval(this)) {
                    homeModel.checkAutoStart(groupName)
                }
            } else {
                ShizukuSettings.setIsOpenOtherActivity(true)
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            return
        }
        if (homeModel.checkBatteryOptimizationRemoval(this)) {
            homeModel.checkAutoStart(groupName)
        }
    }


    override fun onActionLock(groupName: String) {
        if (this.isCanDrawOverlays() && this.isAccessibilityServiceEnabled()) {
            homeModel.actionLockGroup(groupName)
        } else {
            lockPermissionDialogFragment.show(supportFragmentManager, "LockPermission")
        }
    }

    override fun onEditTimeout(groupName: String) {
        val data = ShizukuSettings.getPksByGroupName(groupName)
        data?.let {
            val indexSelected = resources.getStringArray(R.array.auto_lock_timeout_values)
                .indexOf(it.timeOut.toString())
            MaterialAlertDialogBuilder(this@HomeActivity)
                .setTitle("Auto-Lock Timeout")
                .setSingleChoiceItems(
                    resources.getStringArray(R.array.auto_lock_timeout_entries),
                    indexSelected
                ) { d, which ->
                    val timeout =
                        resources.getStringArray(R.array.auto_lock_timeout_values)[which].toLong()
                    homeModel.changeTimeout(groupName, timeout)
                    d.dismiss()
                }
                .create()
                .show()
        }
    }

    private fun writeStarterFiles() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Starter.writeSdcardFiles(applicationContext)
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(this@HomeActivity)
                        .setTitle("Cannot write files")
                        .setMessage(Log.getStackTraceString(e))
                        .setPositiveButton(android.R.string.ok, null)
                        .create()
                        .apply {
                            setOnShowListener {
                                this.findViewById<TextView>(android.R.id.message)!!.apply {
                                    typeface = Typeface.MONOSPACE
                                    setTextIsSelectable(true)
                                }
                            }
                        }
                        .show()
                }
            }
        }
    }
}
