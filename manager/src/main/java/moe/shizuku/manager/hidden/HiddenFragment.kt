package moe.shizuku.manager.hidden

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.AppConstants.GROUP_PKG_PREFIX
import moe.shizuku.manager.AppConstants.RELOAD_PACKAGES_FOR_LOCK
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.databinding.HiddenFragmentBinding
import moe.shizuku.manager.management.appsViewModel
import moe.shizuku.manager.shizuku.ShizukuActivity
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.utils.getApplicationIcon
import moe.shizuku.manager.utils.isAccessibilityServiceEnabled
import moe.shizuku.manager.utils.isCanDrawOverlays
import rikka.core.ktx.unsafeLazy
import rikka.lifecycle.Status
import rikka.lifecycle.viewModels
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect

class HiddenFragment : Fragment(), HiddenCallback {
    private lateinit var binding: HiddenFragmentBinding
    private val homeModel by viewModels { HiddenViewModel() }
    private val appsModel by appsViewModel()
    private val adapter by unsafeLazy { HiddenAdapter() }
    private val apps = mutableListOf<AppGroupBottomSheet>()
    private val lockPermissionDialogFragment = LockPermissionDialogFragment()

    override fun onResume() {
        super.onResume()
        homeModel.reloadGroupApps()
        homeModel.reloadServiceStatus()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HiddenFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        writeStarterFiles()
        context?.let { c ->
            homeModel.initAppHider(c)
        }
        lifecycleScope.launch {
            homeModel.events.flowWithLifecycle(
                this@HiddenFragment.lifecycle,
                Lifecycle.State.STARTED
            ).collectLatest {
                context?.let { c ->
                    when (it) {
                        is HomeEvents.ShowShirukuAlert -> {
                            MaterialAlertDialogBuilder(c)
                                .setTitle(this@HiddenFragment.getString(R.string.shizuku_inactive))
                                .setMessage(it.message)
                                .setPositiveButton(this@HiddenFragment.getString(R.string.go_to_settings)) { _, _ ->
                                    this@HiddenFragment.startActivity(
                                        Intent(c, ShizukuActivity::class.java)
                                    )
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .create()
                                .show()
                        }

                        HomeEvents.RefreshLock -> {
                            c.sendBroadcast(Intent(RELOAD_PACKAGES_FOR_LOCK).setPackage(c.packageName))
                        }
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
                context?.let { c ->
                    val pm = c.packageManager
                    it.data?.forEach { d ->
                        apps.add(
                            AppGroupBottomSheet(
                                pkName = d.packageName,
                                name = d.applicationInfo.loadLabel(pm).toString(),
                                icon = c.getApplicationIcon(d.packageName),
                                isChecked = false
                            )
                        )
                    }
                }
            }
        }

        if (appsModel.packages.value == null) {
            context?.let {
                appsModel.loadApps(it)
            }
        }

        if (homeModel.groupApps.value == null) {
            homeModel.reloadGroupApps()
        }
    }

    override fun onClickAddGroup() {
        context?.let { c ->
            this.activity?.supportFragmentManager?.let { s ->
                CreateGroupBottomSheetDialogFragment().let {
                    it.updateData(c, apps)
                    it.setCallback(homeModel)
                    it.show(
                        s,
                        "GroupAppsBottomSheet"
                    )
                }
            }
        }
    }

    override fun onClickGroup(id: String) {
        context?.let { c ->
            this.activity?.supportFragmentManager?.let { s ->
                CreateGroupBottomSheetDialogFragment().let {
                    it.setCallback(homeModel)
                    it.updateData(
                        c,
                        apps,
                        ShizukuSettings.getPksById(id)
                    )
                    it.show(
                        s,
                        "GroupAppsBottomSheet"
                    )
                }
            }
        }
    }

    override fun onDeleteGroup(id: String) {
        context?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(this@HiddenFragment.getString(R.string.delete_group_msg))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    homeModel.onDeleteGroup(id)
                    ShizukuSettings.removeGroupLockedApp(GROUP_PKG_PREFIX + id)
                    ShizukuSettings.removeDataById(id)
                    homeModel.reloadPkgLock()
                    homeModel.reloadGroupApps()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show()
        }
    }

    override fun onActionHide(id: String) {
        context?.let {
            homeModel.actionHideGroup(id, it)
        }
    }


    override fun onActionLock(id: String) {
        context?.let {
            if (it.isCanDrawOverlays() && it.isAccessibilityServiceEnabled()) {
                homeModel.actionLockGroup(id)
            } else {
                this.activity?.supportFragmentManager?.let { s ->
                    lockPermissionDialogFragment.show(s, "LockPermission")
                }
            }
        }
    }

    override fun onEditTimeout(id: String) {
        context?.let { c ->
            val data = ShizukuSettings.getPksById(id)
            data?.let {
                val indexSelected = resources.getStringArray(R.array.auto_lock_timeout_values)
                    .indexOf(it.timeOut.toString())
                MaterialAlertDialogBuilder(c)
                    .setTitle("Auto-Lock Timeout")
                    .setSingleChoiceItems(
                        resources.getStringArray(R.array.auto_lock_timeout_entries),
                        indexSelected
                    ) { d, which ->
                        val timeout =
                            resources.getStringArray(R.array.auto_lock_timeout_values)[which].toLong()
                        homeModel.changeTimeout(id, timeout)
                        d.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }

    private fun writeStarterFiles() {
        lifecycleScope.launch(Dispatchers.IO) {
            context?.let {
                try {
                    Starter.writeSdcardFiles(it)
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        MaterialAlertDialogBuilder(it)
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
}
