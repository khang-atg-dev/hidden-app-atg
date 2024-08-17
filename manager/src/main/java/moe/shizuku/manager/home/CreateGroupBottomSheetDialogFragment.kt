package moe.shizuku.manager.home

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
    private lateinit var txtEmpty: TextView

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
        txtEmpty = view.findViewById(R.id.empty_txt)
        view.findViewById<TextView>(R.id.cancel_btn)?.apply {
            setOnClickListener {
                dismiss()
            }
        }
        view.findViewById<BorderRecyclerView>(R.id.list_apps)?.apply {
            adapter = this@CreateGroupBottomSheetDialogFragment.adapter
            fixEdgeEffect()
            addEdgeSpacing(top = 8f, bottom = 8f, unit = TypedValue.COMPLEX_UNIT_DIP)
            addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    when (e.action) {
                        MotionEvent.ACTION_DOWN -> {
                            val imm =
                                context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(rv.windowToken, 0)
                        }
                    }
                    return false
                }

                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

            })
        }
        adapter.dataSource = data
        view.findViewById<MaterialButton>(R.id.action_btn)?.apply {
            setOnClickListener {
                val editText = edtName.text?.toString()?.trim() ?: ""
                val newName =
                    context.getString(R.string.default_group_name).takeIf { editText.isEmpty() }
                        ?: editText
                when {
                    newName.isEmpty() -> edtLayout.error =
                        context.getString(R.string.name_cannot_be_empty)

                    selectedPkgs.isEmpty() -> Toast.makeText(
                        context,
                        context.getString(R.string.please_select_one_app),
                        Toast.LENGTH_SHORT
                    ).show()

                    else -> {
                        editGroup?.let {
                            callback?.onEditDone(
                                it.id,
                                newName,
                                selectedPkgs
                            )
                            dismiss()
                            return@setOnClickListener
                        }
                        callback?.onDone(newName, selectedPkgs)
                        dismiss()
                    }
                }
            }
        }
        edtLayout = view.findViewById<TextInputLayout?>(R.id.edit_layout).apply {
            if (editGroup == null || editGroup?.isDefaultGroup == true) {
                this.hint = context.getString(R.string.default_group_name)
            }
        }
        edtName = view.findViewById<TextInputEditText>(R.id.edt_name).apply {
            editGroup?.let {
                this.setText(it.groupName)
            }
            addTextChangedListener {
                edtLayout.error = null
            }
            this.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    edtLayout.hint = context.getString(R.string.name)
                } else {
                    if (editGroup == null || editGroup?.isDefaultGroup == true) {
                        edtLayout.hint = context.getString(R.string.default_group_name)
                    }
                }
            }
        }
        view.findViewById<TextInputEditText?>(R.id.edt_search).apply {
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val bottomSheet =
                        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout
                    val bottomSheetBehavior = bottomSheet?.let { BottomSheetBehavior.from(it) }
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    bottomSheetBehavior?.skipCollapsed = true
                }
            }
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    this.clearFocus()
                }
                false
            }
            addTextChangedListener {
                doAfterTextChanged { text ->
                    text?.let {
                        if (it.isEmpty()) {
                            adapter.dataSource = data
                        } else {
                            searchApp(it.toString().trim())
                        }
                    }
                }
            }
        }
    }

    fun updateData(
        context: Context,
        data: List<AppGroupBottomSheet>,
        editGroup: GroupApps? = null
    ) {
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

    private fun searchApp(searchText: String) {
        val temp = this.data.filter { it.name.contains(searchText, true) }
        txtEmpty.visibility = View.GONE.takeIf { temp.isNotEmpty() } ?: View.VISIBLE
        this.adapter.dataSource = temp
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
    fun onDone(name: String ,pks: Set<String>)
    fun onEditDone(id: String, newGroupName: String, pkgs: Set<String>)
}