package moe.shizuku.manager.focus

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings

class CreateFocusBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var imm: InputMethodManager? = null
    private var callback: FocusBottomSheetCallback? = null
    private var edtText: TextInputEditText? = null
    private var editName: String? = null
    private var editID: String? = null

    fun setCallback(callback: FocusBottomSheetCallback) {
        this.callback = callback
    }

    fun setEditName(id: String) {
        ShizukuSettings.getFocusTaskById(id)?.let {
            editName = it.name
            editID = it.id
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.focus_bottom_sheet_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        edtText = view.findViewById<TextInputEditText>(R.id.edt_name_focus)?.apply {
            setText(context.getString(R.string.default_focus_task_name).takeIf { editID.isNullOrEmpty() } ?: editName)
            imm?.let {
                this.postDelayed({
                    requestFocus()
                    it.showSoftInput(this@apply, InputMethodManager.SHOW_IMPLICIT)
                }, 500)
            }

            setOnEditorActionListener { _, actionId, _ ->
                var text = (edtText?.text ?: "").trim().toString()
                if (text.isEmpty()) {
                    text = context.getString(R.string.default_focus_task_name)
                }
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (editID.isNullOrEmpty()) {
                        callback?.onDone(text)
                    } else {
                        callback?.onDoneEdit(editID ?: "", text)
                    }
                    this.postDelayed({
                        this@CreateFocusBottomSheetDialogFragment.dismiss()
                    }, 200)
                }
                false
            }
        }
    }
}

interface FocusBottomSheetCallback {
    fun onDone(name: String)
    fun onDoneEdit(id: String, name: String)
}