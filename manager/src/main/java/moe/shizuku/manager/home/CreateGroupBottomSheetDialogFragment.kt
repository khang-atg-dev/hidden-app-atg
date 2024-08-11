package moe.shizuku.manager.home

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.model.GroupApps
import moe.shizuku.manager.utils.BaseAdapter
import moe.shizuku.manager.utils.BaseHolder
import moe.shizuku.manager.utils.getAppLabel
import moe.shizuku.manager.utils.getApplicationIcon
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView

class CreateGroupBottomSheetDialogFragment : BottomSheetDialogFragment(),
    ItemGroupBottomSheetCallback {
    private lateinit var adapter: AppsGroupBottomAdapter
    private lateinit var edtName: TextInputEditText
    private lateinit var edtLayout: TextInputLayout

    private var data: List<AppGroupBottomSheet> = emptyList()
    private var selectedPkgs: Set<String> = emptySet()
    private var callback: GroupBottomSheetCallback? = null
    private var editGroup: GroupApps? = null

    fun setCallback(callback: GroupBottomSheetCallback) {
        this.callback = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        adapter = AppsGroupBottomAdapter(inflater, this)
        return inflater.inflate(R.layout.group_bottom_sheet_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<BorderRecyclerView>(R.id.list_apps)?.apply {
            adapter = this@CreateGroupBottomSheetDialogFragment.adapter
            fixEdgeEffect()
            addEdgeSpacing(top = 8f, bottom = 8f, unit = TypedValue.COMPLEX_UNIT_DIP)
        }
        adapter.dataSource = data
        view.findViewById<MaterialButton>(R.id.action_btn)?.apply {
            setOnClickListener {
                val newName = edtName.text.toString().trim()
                when {
                    newName.isEmpty() -> edtLayout.error = "Name cannot be empty"
                    editGroup != null &&
                            editGroup?.groupName != newName &&
                            ShizukuSettings.getPksByGroupName(newName) != null -> {
                        edtLayout.error = "Group name already exists! Please use another name."
                    }

                    editGroup == null && ShizukuSettings.getPksByGroupName(newName) != null -> {
                        edtLayout.error =
                            "Group name already exists! Please use another name."
                    }

                    selectedPkgs.isEmpty() -> Toast.makeText(
                        context,
                        "Please select at least one app!",
                        Toast.LENGTH_SHORT
                    ).show()

                    else -> {
                        editGroup?.let {
                            callback?.onEditDone(
                                it.groupName,
                                newName,
                                selectedPkgs
                            )
                        } ?: callback?.onDone(newName, selectedPkgs)
                        dismiss()
                    }
                }
            }
        }
        edtName = view.findViewById<TextInputEditText>(R.id.edt_name).apply {
            editGroup?.let { this.setText(it.groupName) }
            addTextChangedListener {
                edtLayout.error = null
            }
        }
        edtLayout = view.findViewById(R.id.edit_layout)
    }

    fun updateData(context: Context, data: List<AppGroupBottomSheet>, editGroup: GroupApps? = null) {
        editGroup?.let {
            this.editGroup = it
            this.selectedPkgs = it.pkgs
        }
        val dataSet = data.map {
            AppGroupBottomSheet(
                pkName = it.pkName,
                name = it.name,
                icon = it.icon,
                isChecked = this.selectedPkgs.contains(it.pkName)
            )
        }
        ShizukuSettings.getAppsIsHidden().forEach {
            dataSet.plus(
                AppGroupBottomSheet(
                    pkName = it,
                    isChecked = this.selectedPkgs.contains(it),
                    name = context.getAppLabel(it),
                    icon = context.getApplicationIcon(it)
                )
            )
        }
        this.data = dataSet
    }

    override fun onSelected(pk: String, position: Int) {
        val updateData = data.map {
            if (it.pkName == pk) {
                it.copy(isChecked = true)
            } else {
                it
            }
        }
        this.data = updateData
        this.selectedPkgs = this.selectedPkgs.plus(pk)
        adapter.updateItem(position, data.find { it.pkName == pk })
    }

    override fun onUnselected(pk: String, position: Int) {
        val updateData = data.map {
            if (it.pkName == pk) {
                it.copy(isChecked = false)
            } else {
                it
            }
        }
        this.data = updateData
        this.selectedPkgs = this.selectedPkgs.minus(pk)
        adapter.updateItem(position, data.find { it.pkName == pk })
    }
}

class AppsGroupBottomAdapter(
    inflater: LayoutInflater,
    private val listener: ItemGroupBottomSheetCallback
) :
    BaseAdapter<AppGroupBottomSheet, BaseHolder<AppGroupBottomSheet>>(inflater) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseHolder<AppGroupBottomSheet> {
        return AppBottomSheetViewHolder(inflater.inflate(R.layout.item_bottom_sheet, parent, false))
    }

    inner class AppBottomSheetViewHolder(view: View) : BaseHolder<AppGroupBottomSheet>(view) {

        override fun bind(data: AppGroupBottomSheet, position: Int) {
            super.bind(data, position)
            val icon = itemView.findViewById<ImageView>(R.id.app_icon)
            val name = itemView.findViewById<TextView>(R.id.app_name)
            val pkg = itemView.findViewById<TextView>(R.id.package_name)
            val checkBox = itemView.findViewById<MaterialCheckBox>(R.id.checkbox)
            icon.setImageDrawable(
                data.icon ?: itemView.context.getDrawable(R.drawable.ic_system_icon)
            )
            name.text = data.name
            pkg.text = data.pkName
            checkBox.isChecked = data.isChecked
            checkBox.setOnClickListener {
                if (checkBox.isChecked)
                    listener.onSelected(data.pkName, position)
                else
                    listener.onUnselected(data.pkName, position)
            }
        }
    }
}

interface ItemGroupBottomSheetCallback {
    fun onSelected(pk: String, position: Int)
    fun onUnselected(pk: String, position: Int)
}

data class AppGroupBottomSheet(
    val pkName: String,
    val name: String,
    val icon: Drawable?,
    val isChecked: Boolean,
)

interface GroupBottomSheetCallback {
    fun onDone(groupName: String, pks: Set<String>)
    fun onEditDone(editGroupName: String, newGroupName: String, pkgs: Set<String>)
}