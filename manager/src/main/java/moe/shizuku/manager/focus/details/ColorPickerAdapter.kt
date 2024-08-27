package moe.shizuku.manager.focus.details

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import moe.shizuku.manager.R
import moe.shizuku.manager.utils.BaseAdapter
import moe.shizuku.manager.utils.BaseHolder

class ColorPickerAdapter(
    inflater: LayoutInflater,
    private val callback: ColorCallback,
) : BaseAdapter<ItemColor, BaseHolder<ItemColor>>(inflater) {

    inner class ColorPickerViewHolder(view: View) : BaseHolder<ItemColor>(view) {
        override fun bind(data: ItemColor, position: Int) {
            super.bind(data, position)
            data.let { d ->
                itemView.findViewById<TextView>(R.id.text_title)?.apply {
                    text = d.text.uppercase()
                    val hexColor =
                        if (d.hexColor.isNotEmpty()) Color.parseColor(d.hexColor) else Color.LTGRAY
                    background = GradientDrawable().apply {
                        if (d.isSelected) {
                            setStroke(4, Color.BLACK)
                        }
                        shape = GradientDrawable.RECTANGLE  // Shape of the background
                        cornerRadius = 16f  // Rounded corner radius
                        color = ColorStateList.valueOf(hexColor)  // Background color
                    }
                    setOnClickListener { _ ->
                        callback.onSelectedColor(d.text)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseHolder<ItemColor> {
        return ColorPickerViewHolder(inflater.inflate(R.layout.item_color_layout, parent, false))
    }
}

interface ColorCallback {
    fun onSelectedColor(text: String)
}

data class ItemColor(
    val text: String,
    val hexColor: String,
    val isSelected: Boolean = false
)