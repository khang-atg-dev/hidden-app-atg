package moe.shizuku.manager.focus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import moe.shizuku.manager.R
import moe.shizuku.manager.widget.wheelpicker.TextWheelAdapter
import moe.shizuku.manager.widget.wheelpicker.TextWheelPickerView

class WheelPickerBottomSheet: BottomSheetDialogFragment() {

    private val textWheelAdapter = TextWheelAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        textWheelAdapter.values = (0 until 20).map { TextWheelPickerView.Item("$it", "index-$it") }
        return inflater.inflate(R.layout.wheel_picker_bottom_sheet_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextWheelPickerView>(R.id.picker_view)?.setAdapter(textWheelAdapter)
    }
}