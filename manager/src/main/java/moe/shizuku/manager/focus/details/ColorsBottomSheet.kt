package moe.shizuku.manager.focus.details

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.databinding.ColorsBottomSheetLayoutBinding
import top.defaults.colorpicker.ColorPickerPopup

class ColorsBottomSheet(
    private val callback: ColorPickerCallback,
) : BottomSheetDialogFragment(), ColorCallback, View.OnClickListener {
    private lateinit var binding: ColorsBottomSheetLayoutBinding
    private lateinit var colorPicker: ColorPickerPopup
    private lateinit var adapter: ColorPickerAdapter

    private var redArray: Array<String> = emptyArray()
    private var pinkArray: Array<String> = emptyArray()
    private var blueArray: Array<String> = emptyArray()
    private var greenArray: Array<String> = emptyArray()
    private var yellowArray: Array<String> = emptyArray()
    private var orangeArray: Array<String> = emptyArray()
    private var greyArray: Array<String> = emptyArray()
    private var purpleArray: Array<String> = emptyArray()
    private var deepPurpleArray: Array<String> = emptyArray()
    private var indigoArray: Array<String> = emptyArray()
    private var lightBlueArray: Array<String> = emptyArray()
    private var cyanArray: Array<String> = emptyArray()
    private var tealArray: Array<String> = emptyArray()
    private var lightGreenArray: Array<String> = emptyArray()
    private var limeArray: Array<String> = emptyArray()
    private var amberArray: Array<String> = emptyArray()
    private var deepOrangeArray: Array<String> = emptyArray()
    private var brownArray: Array<String> = emptyArray()
    private var blueGreyArray: Array<String> = emptyArray()
    private var mapColor = emptyMap<Int, Array<String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.resources?.let {
            redArray = it.getStringArray(R.array.red)
            pinkArray = it.getStringArray(R.array.pink)
            blueArray = it.getStringArray(R.array.blue)
            greenArray = it.getStringArray(R.array.green)
            yellowArray = it.getStringArray(R.array.yellow)
            orangeArray = it.getStringArray(R.array.orange)
            greyArray = it.getStringArray(R.array.grey)
            purpleArray = it.getStringArray(R.array.purple)
            deepPurpleArray = it.getStringArray(R.array.deep_purple)
            indigoArray = it.getStringArray(R.array.indigo)
            lightBlueArray = it.getStringArray(R.array.light_blue)
            cyanArray = it.getStringArray(R.array.cyan)
            tealArray = it.getStringArray(R.array.teal)
            lightGreenArray = it.getStringArray(R.array.light_green)
            limeArray = it.getStringArray(R.array.lime)
            amberArray = it.getStringArray(R.array.amber)
            deepOrangeArray = it.getStringArray(R.array.deep_orange)
            brownArray = it.getStringArray(R.array.brown)
            blueGreyArray = it.getStringArray(R.array.blue_grey)
            mapColor = mapOf(
                R.id.all to redArray + pinkArray + blueArray + greenArray + yellowArray + orangeArray + greyArray + purpleArray + deepPurpleArray + indigoArray + lightBlueArray + cyanArray + tealArray + lightGreenArray + limeArray + amberArray + deepOrangeArray + brownArray + blueGreyArray,
                R.id.red to redArray,
                R.id.pink to pinkArray,
                R.id.blue to blueArray,
                R.id.green to greenArray,
                R.id.yellow to yellowArray,
                R.id.orange to orangeArray,
                R.id.grey to greyArray,
            )
        }
    }

    private var colorSelected = ShizukuSettings.getColorCurrentTask()
    private var colorGroupSelected: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        adapter = ColorPickerAdapter(inflater, this)
        binding = ColorsBottomSheetLayoutBinding.inflate(inflater)
        updateChip(binding.all.id)
        colorPicker = ColorPickerPopup.Builder(context)
            .initialColor(colorSelected?.let { Color.parseColor(it) } ?: Color.WHITE)
            .enableBrightness(true)
            .enableAlpha(true)
            .okTitle("Choose")
            .cancelTitle("Cancel")
            .showIndicator(true)
            .build()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.confirmButton.setOnClickListener {
            colorSelected?.let {
                callback.onColorSelected(it)
                dismiss()
            }
        }
        binding.groupColor.adapter = adapter
        setBackgroundForTextView(binding.all.id)
        colorGroupSelected = binding.all.id
        binding.all.setOnClickListener(this)
        binding.red.setOnClickListener(this)
        binding.pink.setOnClickListener(this)
        binding.blue.setOnClickListener(this)
        binding.green.setOnClickListener(this)
        binding.yellow.setOnClickListener(this)
        binding.orange.setOnClickListener(this)
        binding.grey.setOnClickListener(this)
        binding.custom.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (v.id != R.id.custom) {
            colorGroupSelected = v.id
            setBackgroundForTextView(v.id)
            updateChip(v.id)
        } else {
            colorPicker.show(v, object : ColorPickerPopup.ColorPickerObserver() {
                override fun onColorPicked(color: Int) {
                    callback.onColorSelected(String.format("#%08X", color))
                    dismiss()
                }
            })
        }
    }

    override fun onSelectedColor(text: String) {
        colorSelected = text
        updateChip(colorGroupSelected)
    }

    private fun updateChip(id: Int) {
        adapter.dataSource = mapColor[id]?.map { hexColor ->
            ItemColor(
                text = hexColor,
                hexColor = hexColor,
                isSelected = hexColor == colorSelected
            )
        }
    }

    private fun getGradientColor(colorInt: Int, isSelected: Boolean = false) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            color = ColorStateList.valueOf(colorInt)
            if (isSelected) {
                setStroke(4, Color.BLACK)
            }
        }

    private fun setBackgroundForTextView(id: Int) {
        binding.custom.background = getGradientColor(Color.LTGRAY, binding.custom.id == id)
        binding.all.background = getGradientColor(Color.LTGRAY, binding.all.id == id)
        binding.red.background = getGradientColor(Color.RED, binding.red.id == id)
        binding.pink.background =
            getGradientColor(Color.parseColor("#FFC0CB"), binding.pink.id == id)
        binding.blue.background = getGradientColor(Color.BLUE, binding.blue.id == id)
        binding.green.background = getGradientColor(Color.GREEN, binding.green.id == id)
        binding.yellow.background = getGradientColor(Color.YELLOW, binding.yellow.id == id)
        binding.orange.background =
            getGradientColor(Color.parseColor("#FFA500"), binding.orange.id == id)
        binding.grey.background =
            getGradientColor(Color.parseColor("#808080"), binding.grey.id == id)
    }
}

interface ColorPickerCallback {
    fun onColorSelected(hexColor: String)
}