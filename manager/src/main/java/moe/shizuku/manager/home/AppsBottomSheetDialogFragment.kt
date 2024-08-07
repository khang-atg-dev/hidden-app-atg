package moe.shizuku.manager.home

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import moe.shizuku.manager.AppConstants.GROUP_PKG_PREFIX
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.utils.BaseAdapter
import moe.shizuku.manager.utils.BaseHolder
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView

class AppsBottomSheetDialogFragment : BottomSheetDialogFragment(), ItemBottomSheetCallback {
    private var data: List<AppBottomSheet> = emptyList()
    private lateinit var adapter: AppsBottomAdapter
    private var selectedPkgs: Set<String> = emptySet()
    private var callback: BottomSheetCallback? = null

    fun setCallback(callback: BottomSheetCallback) {
        this.callback = callback
    }

    override fun onDetach() {
        callback?.onClosed()
        super.onDetach()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        selectedPkgs = ShizukuSettings.getListLockedAppsAsSet()
        adapter = AppsBottomAdapter(inflater, this)
        return inflater.inflate(R.layout.bottom_sheet_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<BorderRecyclerView>(R.id.list_apps)?.apply {
            this.adapter = this@AppsBottomSheetDialogFragment.adapter
            fixEdgeEffect()
            addEdgeSpacing(top = 8f, bottom = 8f, unit = TypedValue.COMPLEX_UNIT_DIP)
        }
        adapter.dataSource = data
        view.findViewById<MaterialButton>(R.id.action_btn)?.apply {
            setOnClickListener {
                callback?.onDone(selectedPkgs)
            }
        }

        view.findViewById<TextView>(R.id.create_group)?.apply {
            setOnClickListener {
                callback?.onCreateGroup()
            }
        }
    }

    fun clearData() {
        this.data = emptyList()
    }

    fun updateData(context: Context, data: List<PackageInfo>) {
        selectedPkgs = ShizukuSettings.getListLockedAppsAsSet()
        context.packageManager?.let { pm ->
            val dataSet = data.map {
                val ai = it.applicationInfo
                AppBottomSheet(
                    icon = ai.loadIcon(pm),
                    pkg = ai.packageName,
                    name = ai.loadLabel(pm).toString(),
                    isChecked = selectedPkgs.contains(it.applicationInfo.packageName)
                )
            }

            this.data = this.data.plus(dataSet)
        }
    }

    fun updateGroupData(data: Set<String>) {
        selectedPkgs = ShizukuSettings.getListLockedAppsAsSet()
        val dataSet = data.map {
            val pksSet = ShizukuSettings.getPksByGroupName(it)
            val pksStr = if (pksSet.size == 1) {
                pksSet.iterator().next();
            } else {
                pksSet.joinToString(",")
            }
            AppBottomSheet(
                icon = null,
                pkg = pksStr.replace(",", ", "),
                name = it.substringAfterLast("."),
                isChecked = selectedPkgs.contains(it)
            )
        }

        this.data = this.data.plus(dataSet)
    }

    override fun onSelected(pk: String, position: Int) {
        val updateData = data.map {
            if (it.pkg == pk) {
                it.copy(isChecked = true)
            } else {
                it
            }
        }
        this.data = updateData
        this.selectedPkgs = this.selectedPkgs.plus(pk)
        adapter.updateItem(position, updateData.find { it.pkg == pk })
    }

    override fun onUnselected(pk: String, position: Int) {
        val updateData = data.map {
            if (it.pkg == pk) {
                it.copy(isChecked = false)
            } else {
                it
            }
        }
        this.data = updateData
        adapter.updateItem(position, updateData.find { it.pkg == pk })
        this.selectedPkgs = this.selectedPkgs.minus(pk)
    }
}

class AppsBottomAdapter(inflater: LayoutInflater, private val listener: ItemBottomSheetCallback) :
    BaseAdapter<AppBottomSheet, BaseHolder<AppBottomSheet>>(inflater) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseHolder<AppBottomSheet> {
        return AppBottomSheetViewHolder(inflater.inflate(R.layout.item_bottom_sheet, parent, false))
    }

    inner class AppBottomSheetViewHolder(view: View) : BaseHolder<AppBottomSheet>(view) {

        override fun bind(data: AppBottomSheet, position: Int) {
            super.bind(data, position)
            val icon = itemView.findViewById<ImageView>(R.id.app_icon)
            val name = itemView.findViewById<TextView>(R.id.app_name)
            val pkg = itemView.findViewById<TextView>(R.id.package_name)
            val checkBox = itemView.findViewById<MaterialCheckBox>(R.id.checkbox)
            icon.setImageDrawable(
                data.icon ?: itemView.context.getDrawable(R.drawable.baseline_apps_24)
            )
            name.text = data.name
            pkg.text = data.pkg
            checkBox.isChecked = data.isChecked
            checkBox.setOnClickListener {
                if (checkBox.isChecked)
                    listener.onSelected(data.pkg, position)
                else
                    listener.onUnselected(data.pkg, position)
            }
        }
    }
}

interface ItemBottomSheetCallback {
    fun onSelected(pk: String, position: Int)
    fun onUnselected(pk: String, position: Int)
}

data class AppBottomSheet(
    val icon: Drawable?,
    val pkg: String,
    val name: String,
    var isChecked: Boolean
)

interface BottomSheetCallback {
    fun onDone(pks: Set<String>)
    fun onCreateGroup()
    fun onClosed()
}