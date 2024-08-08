package moe.shizuku.manager.home

import android.content.pm.PackageInfo
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
import moe.shizuku.manager.utils.BaseAdapter
import moe.shizuku.manager.utils.BaseHolder
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView

class CreateGroupBottomSheetDialogFragment : BottomSheetDialogFragment(),
    ItemGroupBottomSheetCallback {
    private var data: List<AppGroupBottomSheet> = emptyList()
    private lateinit var adapter: AppsGroupBottomAdapter
    private var selectedPkgs: Set<String> = emptySet()
    private var callback: GroupBottomSheetCallback? = null

    private lateinit var edtName: TextInputEditText
    private lateinit var edtLayout: TextInputLayout


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
                when {
                    edtName.text.toString().isEmpty() -> edtLayout.error = "Name cannot be empty"
                    ShizukuSettings.getPksByGroupName(edtName.text.toString()) != null -> edtLayout.error = "Group name already exists! Please use another name."
                    selectedPkgs.isEmpty() -> Toast.makeText(
                        context,
                        "Please select at least one app!",
                        Toast.LENGTH_SHORT
                    ).show()

                    else -> callback?.onDone(edtName.text.toString(), selectedPkgs)
                }
            }
        }
        edtName = view.findViewById<TextInputEditText>(R.id.edt_name).apply {
            addTextChangedListener {
                edtLayout.error = null
            }
        }
        edtLayout = view.findViewById(R.id.edit_layout)
    }

    fun updateData(data: List<PackageInfo>) {
        val dataSet = data.map {
            AppGroupBottomSheet(it, false)
        }
        this.data = dataSet
    }

    override fun onSelected(pk: String, position: Int) {
        val updateData = data.map {
            if (it.appInfo.applicationInfo.packageName == pk) {
                it.copy(isChecked = true)
            } else {
                it
            }
        }
        this.data = updateData
        this.selectedPkgs = this.selectedPkgs.plus(pk)
        adapter.updateItem(position, data.find { it.appInfo.applicationInfo.packageName == pk })
    }

    override fun onUnselected(pk: String, position: Int) {
        val updateData = data.map {
            if (it.appInfo.applicationInfo.packageName == pk) {
                it.copy(isChecked = false)
            } else {
                it
            }
        }
        this.data = updateData
        this.selectedPkgs = this.selectedPkgs.minus(pk)
        adapter.updateItem(position, data.find { it.appInfo.applicationInfo.packageName == pk })
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
            val pm = itemView.context.packageManager
            val ai = data.appInfo.applicationInfo
            icon.setImageDrawable(ai.loadIcon(pm))
            name.text = ai.loadLabel(pm)
            pkg.text = ai.packageName
            checkBox.isChecked = data.isChecked
            checkBox.setOnClickListener {
                if (checkBox.isChecked)
                    listener.onSelected(data.appInfo.applicationInfo.packageName, position)
                else
                    listener.onUnselected(data.appInfo.applicationInfo.packageName, position)
            }
        }
    }
}

interface ItemGroupBottomSheetCallback {
    fun onSelected(pk: String, position: Int)
    fun onUnselected(pk: String, position: Int)
}

data class AppGroupBottomSheet(
    val appInfo: PackageInfo,
    var isChecked: Boolean
)

interface GroupBottomSheetCallback {
    fun onDone(groupName: String, pks: Set<String>)
}