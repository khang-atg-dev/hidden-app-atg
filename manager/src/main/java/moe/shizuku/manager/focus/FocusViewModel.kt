package moe.shizuku.manager.focus

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import moe.shizuku.manager.AppConstants.DEFAULT_TIME_FOCUS
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.model.Focus
import java.util.UUID

class FocusViewModel : ViewModel(), FocusBottomSheetCallback, WheelPickerCallback {
    private val _state = MutableStateFlow(FocusState())
    val state = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                focusList = ShizukuSettings.getFocusTasks()
            )
        }
    }

    override fun onDone(name: String) {
        _state.update {
            val uuid = UUID.randomUUID().toString()
            val currentList = it.focusList
            val newFocus = Focus(
                id = uuid,
                name = name,
                time = DEFAULT_TIME_FOCUS
            )
            ShizukuSettings.saveFocusTask(newFocus)
            it.copy(
                focusList = currentList.plus(newFocus)
            )
        }
    }

    override fun onDoneEdit(id: String, name: String) {
        _state.update {
            ShizukuSettings.getFocusTaskById(id)?.let { focus ->
                val currentList = it.focusList
                val newFocus = focus.copy(name = name)
                ShizukuSettings.updateFocusTask(newFocus)
                return@update it.copy(
                    focusList = currentList.map { i -> if (i.id == id) newFocus else i }
                )
            }
            it
        }
    }

    override fun onConfirm(id: String, time: Long) {
        _state.update {
            val currentList = it.focusList
            currentList.find { i -> i.id == id }?.let { focus ->
                ShizukuSettings.updateFocusTask(focus.copy(time = time))
            }
            it.copy(
                focusList = currentList.map { i ->
                    if (i.id == id) i.copy(time = time) else i
                }
            )
        }
    }

    fun deleteFocusTask(id: String) {
        _state.update {
            ShizukuSettings.removeFocusTask(id)
            val currentList = it.focusList
            currentList.find { i -> i.id == id }?.let { d ->
                return@update it.copy(
                    focusList = currentList.minus(d)
                )
            }
            return@update it
        }
    }
}

data class FocusState(
    val focusList: List<Focus> = emptyList()
)

interface FocusCallback {
    fun onDelete(id: String)
    fun onEditName(id: String)
    fun onStart(id: String, time: Long, name: String)
    fun onAddFocusTask()
    fun onOpenTimePicker(id: String, time: Long) {}
}