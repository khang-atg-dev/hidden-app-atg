package moe.shizuku.manager.focus.details

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.ColorsBottomSheetLayoutBinding

class ColorsBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: ColorsBottomSheetLayoutBinding

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

    private var colorSelected = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ColorsBottomSheetLayoutBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.groupName.check(binding.all.id)
        binding.all.isChecked = true
        updateChip(binding.all.id)

        binding.groupName.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds[0].equals(binding.custom.id)) {
                binding.customPicker.visibility = View.VISIBLE
                binding.groupColor.visibility = View.GONE
            } else {
                binding.customPicker.visibility = View.GONE
                binding.groupColor.visibility = View.VISIBLE
                checkedIds.forEach {
                    updateChip(it)
                }
            }
        }
    }

    private fun updateChip(chipId: Int) {
        binding.groupColor.removeAllViews()
        binding.groupColor.isSingleSelection = true
        mapColor[chipId]?.forEach { hexColor ->
            val customChip = (layoutInflater.inflate(
                R.layout.custom_chip_layout,
                binding.groupColor,
                false
            ) as Chip).apply {
                text = hexColor.uppercase()
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(hexColor))
                setOnClickListener {
                    colorSelected = hexColor
                    binding.groupColor.children.forEach { c ->
                        if (c is Chip) {
                            c.isChecked = c.text == hexColor.uppercase()
                        }
                    }
                }
            }
            binding.groupColor.addView(customChip)
        }
    }
}