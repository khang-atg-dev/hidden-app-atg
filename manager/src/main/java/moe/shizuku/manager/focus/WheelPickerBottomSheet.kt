package moe.shizuku.manager.focus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import moe.shizuku.manager.AppConstants.DEFAULT_TIME_FOCUS
import moe.shizuku.manager.databinding.WheelPickerBottomSheetLayoutBinding
import moe.shizuku.manager.widget.wheelpicker.BaseWheelPickerView
import moe.shizuku.manager.widget.wheelpicker.TextWheelAdapter
import moe.shizuku.manager.widget.wheelpicker.TextWheelPickerView
import java.util.concurrent.TimeUnit

class WheelPickerBottomSheet(
    private val id: String,
    private val selectedValue: Int = TimeUnit.MILLISECONDS.toMinutes(DEFAULT_TIME_FOCUS).toInt(),
    private val callback: WheelPickerCallback,
) : BottomSheetDialogFragment() {

    private lateinit var binding: WheelPickerBottomSheetLayoutBinding

    private val textWheelAdapter = TextWheelAdapter()
    private var selectedIndex = 0

    private val dataPhase1 = (1 until 81).map { value ->
        TextWheelPickerView.Item(
            "$value",
            "$value " + if (value == 1) "minute" else "minutes"
        )
    }

    private val specialCase = listOf(90, 120, 150, 180, 210)
    private val dataPhase2 = (85 until 241 step 5).map { value ->
        var text = "$value minutes"
        if (specialCase.contains(value)) {
            text += " (${value.toFloat() / 60} hours)"
        }
        TextWheelPickerView.Item(
            "$value",
            text
        )
    }

    private val dataPhase3 = (270 until 481 step 30).map { value ->
        TextWheelPickerView.Item(
            "$value",
            "$value minutes" + " (${value.toFloat() / 60} hours)"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        textWheelAdapter.values = dataPhase1 + dataPhase2 + dataPhase3
        selectedIndex = textWheelAdapter.values.indexOfFirst {
            it.id == selectedValue.toString()
        }
        binding = WheelPickerBottomSheetLayoutBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.pickerView.let {
            it.setAdapter(adapter = textWheelAdapter)
            if (selectedIndex != -1) {
                it.setSelectedIndex(selectedIndex, false)
            }
            it.setWheelListener(object : BaseWheelPickerView.WheelPickerViewListener {
                override fun didSelectItem(picker: BaseWheelPickerView, index: Int) {
                }
            })
        }

        binding.confirmButton.setOnClickListener {
            callback.onConfirm(
                id,
                TimeUnit.MINUTES.toMillis(
                    textWheelAdapter.values[binding.pickerView.selectedIndex].id.toLong()
                )
            )
            this@WheelPickerBottomSheet.dismiss()
        }
    }
}

interface WheelPickerCallback {
    fun onConfirm(id: String, time: Long)
}