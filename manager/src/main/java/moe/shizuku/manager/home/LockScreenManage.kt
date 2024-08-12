package moe.shizuku.manager.home

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import moe.shizuku.manager.AppConstants.PASSWORD_LENGTH
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings

class LockScreenManage {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var skipEvent: Boolean = false
    private var debounceTime = 0L
    private var currentPkg = ""

    private var password = ""
    private lateinit var pin1: TextView
    private lateinit var pin2: TextView
    private lateinit var pin3: TextView
    private lateinit var pin4: TextView
    private lateinit var txtTitle: TextView
    private lateinit var txtError: TextView
    private lateinit var btnBack: ImageView

    @RequiresApi(Build.VERSION_CODES.O)
    fun showLockScreen(context: Context, packageName: String) {
        if (overlayView != null || windowManager != null || skipEvent) {
            skipEvent = false
            return // Already showing
        }
        this.currentPkg = packageName
        ShizukuSettings.findTimeoutOfPkg(packageName).let {
            if (ShizukuSettings.getUnlockStatus(packageName) && debounceTime != 0L && System.currentTimeMillis() - debounceTime < it) {
                debounceTime = System.currentTimeMillis()
                return
            }
        }

        ShizukuSettings.saveUnlockStatus(packageName, false)

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Inflate the fragment's layout
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.lock_dialog_layout, null)

        overlayView?.let {
            btnBack = it.findViewById<ImageView?>(R.id.back_btn).apply {
                visibility = View.GONE
            }
            txtError = it.findViewById(R.id.txt_error)
            txtTitle = it.findViewById<TextView?>(R.id.tvEnterCode).apply {
                text = "Enter your password"
            }
            pin1 = it.findViewById(R.id.pin1)
            pin2 = it.findViewById(R.id.pin2)
            pin3 = it.findViewById(R.id.pin3)
            pin4 = it.findViewById(R.id.pin4)

            it.findViewById<TextView>(R.id.pin_code_button_1).setOnClickListener {
                updatePin("1")
            }
            it.findViewById<TextView>(R.id.pin_code_button_2).setOnClickListener {
                updatePin("2")
            }
            it.findViewById<TextView>(R.id.pin_code_button_3).setOnClickListener {
                updatePin("3")
            }
            it.findViewById<TextView>(R.id.pin_code_button_4).setOnClickListener {
                updatePin("4")
            }
            it.findViewById<TextView>(R.id.pin_code_button_5).setOnClickListener {
                updatePin("5")
            }
            it.findViewById<TextView>(R.id.pin_code_button_6).setOnClickListener {
                updatePin("6")
            }
            it.findViewById<TextView>(R.id.pin_code_button_7).setOnClickListener {
                updatePin("7")
            }
            it.findViewById<TextView>(R.id.pin_code_button_8).setOnClickListener {
                updatePin("8")
            }
            it.findViewById<TextView>(R.id.pin_code_button_9).setOnClickListener {
                updatePin("9")
            }
            it.findViewById<TextView>(R.id.pin_code_button_0).setOnClickListener {
                updatePin("0")
            }
            it.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
                resetUI()
            }
        }

        // Set layout parameters for the overlay view
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        // Add the view to the WindowManager
        windowManager?.addView(overlayView, params)
    }

    fun hideLockScreen() {
        if (overlayView != null) {
            debounceTime = System.currentTimeMillis()
            windowManager?.removeView(overlayView)
            overlayView = null
            windowManager = null
            resetUI()
        }
    }

    private fun updatePin(value: String) {
        if (password.length >= PASSWORD_LENGTH) return
        txtError.text = ""
        password += value
        when (password.length) {
            1 -> pin1.text = "*"
            2 -> pin2.text = "*"
            3 -> pin3.text = "*"
            4 -> pin4.text = "*"
            else -> {}
        }
        if (password.length == PASSWORD_LENGTH) {
            if (password == ShizukuSettings.getLockPassword()) {
                ShizukuSettings.saveUnlockStatus(currentPkg, true)
                hideLockScreen()
                skipEvent = true
            } else {
                txtError.text = "Your password incorrect!"
            }
        }
    }

    fun resetSkipEvent() {
        skipEvent = false
    }

    fun checkDebounceTime(): Boolean {
        return System.currentTimeMillis() - debounceTime > 300
    }

    private fun resetUI() {
        txtError.text = ""
        password = ""
        txtTitle.text = "Enter your password"
        pin1.text = ""
        pin2.text = ""
        pin3.text = ""
        pin4.text = ""
    }

}
