package moe.shizuku.manager.app

import android.content.Context
import androidx.fragment.app.Fragment

abstract class BaseFragment: Fragment() {
    abstract fun getTitle(context: Context): String
}