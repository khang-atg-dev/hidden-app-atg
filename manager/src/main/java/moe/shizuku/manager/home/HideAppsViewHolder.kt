package moe.shizuku.manager.home

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.google.android.material.chip.Chip
import moe.shizuku.manager.AppConstants.GROUP_PKG_PREFIX
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.databinding.HideAppsLayoutBinding
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.management.AppsViewModel
import rikka.core.content.asActivity
import rikka.lifecycle.Resource
import rikka.lifecycle.Status
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

class HideAppsViewHolder(binding: HideAppsLayoutBinding, root: View) : BaseViewHolder<Any?>(root) {
    companion object {
        val CREATOR = Creator<Any> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = HideAppsLayoutBinding.inflate(inflater, outer.root, true)
            HideAppsViewHolder(inner, outer.root)
        }
    }

    private val appsViewModel = AppsViewModel(context)
    private val chipGroup = binding.chipGroup
    private val emptyTxt = binding.emptyTxt
    private var apps: List<PackageInfo> = emptyList()

    private var isBottomSheetShowing = false

    private val packagesObserver = Observer<Resource<List<PackageInfo>>> {
        if (it.status == Status.SUCCESS) {
            it.data?.let { d ->
                apps = d
                updateData(d)
            }
        }
    }

    init {
        binding.actionBtn.let {
            if (!ShizukuSettings.isHideEnabled()) {
                it.text = "Show"
                it.icon = context.getDrawable(R.drawable.baseline_remove_red_eye_24)
            } else {
                it.text = "Hide"
                it.icon = context.getDrawable(R.drawable.baseline_visibility_off_24)
            }
            binding.actionBtn.setOnClickListener { _ ->
                ShizukuSettings.setHideApp(!ShizukuSettings.isHideEnabled())
                if (!ShizukuSettings.isHideEnabled()) {
                    it.text = "Show"
                    it.icon = context.getDrawable(R.drawable.baseline_remove_red_eye_24)
                } else {
                    it.text = "Hide"
                    it.icon = context.getDrawable(R.drawable.baseline_visibility_off_24)
                }
            }
        }

        emptyTxt.setOnClickListener {
            showAppsBottomSheet(it.context)
        }

        appsViewModel.packages.observeForever(packagesObserver)
        if (appsViewModel.packages.value == null) {
            appsViewModel.loadApps(context)
        }
    }


    override fun onViewDetachedFromWindow() {
        appsViewModel.packages.removeObserver(packagesObserver)
        super.onViewDetachedFromWindow()
    }

    private fun updateData(data: List<PackageInfo>) {
        val lockedPkgs = ShizukuSettings.getListHiddenAppsAsSet()
        chipGroup.removeAllViews()
        if (data.isEmpty() || lockedPkgs.isEmpty()) {
            emptyTxt.visibility = View.VISIBLE
            chipGroup.visibility = View.GONE
            return
        } else {
            emptyTxt.visibility = View.GONE
            chipGroup.visibility = View.VISIBLE
        }
        val pm = context.packageManager
        data.forEach {
            if (lockedPkgs.contains(it.applicationInfo.packageName)) {
                chipGroup.addView(
                    Chip(context).apply {
                        setCloseIconVisible(true)
                        text = it.applicationInfo.loadLabel(pm)
                        chipIcon = it.applicationInfo.loadIcon(pm)
                        setOnCloseIconClickListener { _ ->
                            chipGroup.removeView(this)
                            if (chipGroup.childCount == 1) {
                                emptyTxt.visibility = View.VISIBLE
                                chipGroup.visibility = View.GONE
                            }
                            ShizukuSettings.removeHiddenApp(it.applicationInfo.packageName)
                        }
                    }
                )
            }
        }
        lockedPkgs.filter { it.startsWith(GROUP_PKG_PREFIX) }.forEach {
            chipGroup.addView(
                Chip(context).apply {
                    setCloseIconVisible(true)
                    text = it.substringAfterLast(".")
                    chipIcon = context.getDrawable(R.drawable.baseline_apps_24)
                    setOnCloseIconClickListener { _ ->
                        chipGroup.removeView(this)
                        if (chipGroup.childCount == 1) {
                            emptyTxt.visibility = View.VISIBLE
                            chipGroup.visibility = View.GONE
                        }
                        ShizukuSettings.removeHiddenApp(it)
                    }
                }
            )
        }
        chipGroup.addView(
            Chip(context).apply {
                text = "Add"
                chipIcon = context.getDrawable(android.R.drawable.ic_input_add)
                chipIcon?.setTint(Color.DKGRAY)
                setOnClickListener { v ->
                    showAppsBottomSheet(v.context)
                }
            }
        )
    }

    private fun showAppsGroupBottomSheet(context: Context) {
        CreateGroupBottomSheetDialogFragment().apply {
            this.updateData(apps)
            this.setCallback(object : GroupBottomSheetCallback {
                override fun onDone(groupName: String, pks: Set<String>) {
                    ShizukuSettings.savePksByGroupName(groupName, pks)
                    ShizukuSettings.saveGroupLockedApps(groupName)
                    this@apply.dismiss()
                    showAppsBottomSheet(context)
                }
            })
            this.show(
                context.asActivity<FragmentActivity>().supportFragmentManager,
                "GroupAppsBottomSheet"
            )
        }
    }

    private fun showAppsBottomSheet(context: Context) {
        if (isBottomSheetShowing) return
        isBottomSheetShowing = true
        AppsBottomSheetDialogFragment().apply {
            this.clearData()
            this.updateSelectedApps(ShizukuSettings.getListHiddenAppsAsSet())
            this.updateGroupData(ShizukuSettings.getGroupLockedAppsAsSet())
            this.updateData(context, apps)
            this.setCallback(object : BottomSheetCallback {
                override fun onDone(pks: Set<String>) {
                    if (pks.isNotEmpty()) {
                        ShizukuSettings.saveHiddenApp(pks)
                        this@HideAppsViewHolder.updateData(apps)
                    }
                    this@apply.dismiss()
                }

                override fun onCreateGroup() {
                    this@apply.dismiss()
                    showAppsGroupBottomSheet(context)
                }

                override fun onClosed() {
                    isBottomSheetShowing = false
                }
            })
            this.show(
                context.asActivity<FragmentActivity>().supportFragmentManager,
                "AppsBottomSheet"
            )
        }
    }
}