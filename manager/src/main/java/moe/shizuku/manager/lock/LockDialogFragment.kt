package moe.shizuku.manager.lock

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import moe.shizuku.manager.AppConstants.PASSWORD_LENGTH
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings

class LockDialogFragment : DialogFragment() {

    private var passwordState = PasswordState.Idle
    private var password = ""
    private var verifiedPassword = ""
    private var isReenterPass = false

    private lateinit var pin1: TextView
    private lateinit var pin2: TextView
    private lateinit var pin3: TextView
    private lateinit var pin4: TextView
    private lateinit var txtTitle: TextView
    private lateinit var txtError: TextView
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        passwordState = when {
            ShizukuSettings.getLockPassword().isEmpty() -> PasswordState.Empty
            ShizukuSettings.getIsChanningPassword() -> PasswordState.Changing
            ShizukuSettings.getIsLocked() -> PasswordState.Locked
            !ShizukuSettings.getEnablePassword() -> PasswordState.Unlocked
            else -> PasswordState.Idle
        }
        isCancelable = passwordState == PasswordState.Changing
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.lock_dialog_layout, container, false)
    }

    override fun onDismiss(dialog: DialogInterface) {
        resetUI()
        ShizukuSettings.setIsChanningPassword(false)
        super.onDismiss(dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnBack = view.findViewById<ImageView?>(R.id.back_btn).apply {
            if (passwordState == PasswordState.Changing) {
                visibility = View.VISIBLE
                setOnClickListener {
                    this@LockDialogFragment.dismiss()
                }
            } else {
                visibility = View.GONE
            }
        }
        txtError = view.findViewById(R.id.txt_error)
        txtTitle = view.findViewById<TextView?>(R.id.tvEnterCode).apply {
            text = context.getString(R.string.enter_your_password)
        }
        pin1 = view.findViewById(R.id.pin1)
        pin2 = view.findViewById(R.id.pin2)
        pin3 = view.findViewById(R.id.pin3)
        pin4 = view.findViewById(R.id.pin4)

        view.findViewById<TextView>(R.id.pin_code_button_1).setOnClickListener {
            updatePin("1")
        }
        view.findViewById<TextView>(R.id.pin_code_button_2).setOnClickListener {
            updatePin("2")
        }
        view.findViewById<TextView>(R.id.pin_code_button_3).setOnClickListener {
            updatePin("3")
        }
        view.findViewById<TextView>(R.id.pin_code_button_4).setOnClickListener {
            updatePin("4")
        }
        view.findViewById<TextView>(R.id.pin_code_button_5).setOnClickListener {
            updatePin("5")
        }
        view.findViewById<TextView>(R.id.pin_code_button_6).setOnClickListener {
            updatePin("6")
        }
        view.findViewById<TextView>(R.id.pin_code_button_7).setOnClickListener {
            updatePin("7")
        }
        view.findViewById<TextView>(R.id.pin_code_button_8).setOnClickListener {
            updatePin("8")
        }
        view.findViewById<TextView>(R.id.pin_code_button_9).setOnClickListener {
            updatePin("9")
        }
        view.findViewById<TextView>(R.id.pin_code_button_0).setOnClickListener {
            updatePin("0")
        }
        view.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            resetUI()
        }
    }

    private fun updatePin(value: String) {
        if (isReenterPass) {
            updateVerifyPin(value)
            return
        }
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
            when (passwordState) {
                PasswordState.Locked -> {
                    if (password == ShizukuSettings.getLockPassword()) {
                        this@LockDialogFragment.dismiss()
                    } else {
                        txtError.text = context?.getString(R.string.password_incorrect) ?: "Your password incorrect!"
                    }
                }
                PasswordState.Changing,
                PasswordState.Empty -> {
                    if (verifiedPassword.isEmpty()) {
                        changeUI()
                    }
                }

                else -> {}
            }
        }
    }

    private fun updateVerifyPin(value: String) {
        if (verifiedPassword.length >= PASSWORD_LENGTH) return
        txtError.text = ""
        verifiedPassword += value
        when (verifiedPassword.length) {
            1 -> pin1.text = "*"
            2 -> pin2.text = "*"
            3 -> pin3.text = "*"
            4 -> pin4.text = "*"
            else -> {}
        }
        if (verifiedPassword.length == PASSWORD_LENGTH) {
            when (passwordState) {
                PasswordState.Changing,
                PasswordState.Empty -> {
                    if (verifiedPassword == password) {
                        ShizukuSettings.setLockPassword(password)
                        ShizukuSettings.setIsLocked(false)
                        this@LockDialogFragment.dismiss()
                    } else {
                        txtError.text = context?.getString(R.string.password_not_match) ?: "Your password does not match!"
                    }
                }
                else -> {}
            }
        }
    }

    private fun changeUI() {
        isReenterPass = true
        verifiedPassword = ""
        txtTitle.text = context?.getString(R.string.re_enter_password) ?: "Re-enter your password"
        pin1.text = ""
        pin2.text = ""
        pin3.text = ""
        pin4.text = ""
    }

    private fun resetUI() {
        txtError.text = ""
        isReenterPass = false
        password = ""
        verifiedPassword = ""
        txtTitle.text = context?.getString(R.string.enter_your_password) ?: "Enter your password"
        pin1.text = ""
        pin2.text = ""
        pin3.text = ""
        pin4.text = ""
    }
}

enum class PasswordState {
    Locked,
    Unlocked,
    Changing,
    Empty,
    Idle,
}
