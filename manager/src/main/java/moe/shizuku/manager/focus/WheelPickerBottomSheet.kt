package moe.shizuku.manager.focus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import moe.shizuku.manager.AppConstants.DEFAULT_TIME_FOCUS
import moe.shizuku.manager.R
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

    private var dataPhase1 = emptyList<TextWheelPickerView.Item>()
    private val specialCase = listOf(90, 120, 150, 180, 210)
    private var dataPhase2 = emptyList<TextWheelPickerView.Item>()
    private var dataPhase3 = emptyList<TextWheelPickerView.Item>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        context?.let { context ->
            val minFormat = context.getString(R.string.min_time_bottom_sheet)
            val minsFormat = context.getString(R.string.mins_time_bottom_sheet)
            val hoursFormat = context.getString(R.string.hours_time_bottom_sheet)
            dataPhase1 = (1 until 81).map { value ->
                TextWheelPickerView.Item(
                    "$value",
                    String.format(if (value == 1) minFormat else minsFormat, value)
                )
            }
            dataPhase2 = (85 until 241 step 5).map { value ->
                var text = String.format(minsFormat, value)
                if (specialCase.contains(value)) {
                    text += String.format(hoursFormat, value.toFloat() / 60)
                }
                TextWheelPickerView.Item(
                    "$value",
                    text
                )
            }
            dataPhase3 = (270 until 481 step 30).map { value ->
                TextWheelPickerView.Item(
                    "$value",
                    "${String.format(minsFormat, value)} ${
                        String.format(
                            hoursFormat,
                            value.toFloat() / 60
                        )
                    }"
                )
            }
            textWheelAdapter.values = dataPhase1 + dataPhase2 + dataPhase3
            selectedIndex = textWheelAdapter.values.indexOfFirst {
                it.id == selectedValue.toString()
            }
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