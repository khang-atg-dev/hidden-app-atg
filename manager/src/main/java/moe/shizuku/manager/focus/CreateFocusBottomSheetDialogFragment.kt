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

class CreateFocusBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var imm: InputMethodManager? = null
    private var callback: FocusBottomSheetCallback? = null

    private var edtText: TextInputEditText? = null

    fun setCallback(callback: FocusBottomSheetCallback) {
        this.callback = callback
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
            imm?.let {
                requestFocus()
                this.postDelayed({
                    it.showSoftInput(this@apply, InputMethodManager.SHOW_IMPLICIT)
                }, 200)
            }

            setOnEditorActionListener { _, actionId, _ ->
                val text = (edtText?.text ?: "").trim()
                if (text.isEmpty()) {
                    return@setOnEditorActionListener true
                }
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    this.clearFocus()
                    callback?.onDone((edtText?.text ?: "").trim().toString())
                    dismiss()
                }
                false
            }
        }
    }
}

interface FocusBottomSheetCallback {
    fun onDone(name: String)
}