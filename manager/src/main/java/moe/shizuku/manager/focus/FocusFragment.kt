package moe.shizuku.manager.focus

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.databinding.FocusFragmentBinding
import moe.shizuku.manager.focus.details.FocusDetailsActivity
import moe.shizuku.manager.model.CurrentFocus
import rikka.core.ktx.unsafeLazy
import rikka.lifecycle.viewModels
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import java.util.concurrent.TimeUnit

class FocusFragment : Fragment(), FocusCallback {
    private lateinit var binding: FocusFragmentBinding
    private val viewModel by viewModels { FocusViewModel() }
    private val adapter by unsafeLazy { FocusAdapter() }
    private var itemTouchHelper: SwipeCallback<FocusAdapter>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter.listener = this
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    adapter.updateData(state.focusList)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FocusFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = binding.list
        recyclerView.adapter = adapter
        recyclerView.fixEdgeEffect()
        recyclerView.addItemSpacing(top = 4f, bottom = 4f, unit = TypedValue.COMPLEX_UNIT_DIP)
        recyclerView.addEdgeSpacing(
            top = 4f,
            bottom = 4f,
            left = 16f,
            right = 16f,
            unit = TypedValue.COMPLEX_UNIT_DIP
        )
        context?.let {
            itemTouchHelper = SwipeCallback(it, adapter).let { c ->
                val itemTouchHelper = ItemTouchHelper(c)
                itemTouchHelper.attachToRecyclerView(recyclerView)
                c
            }
        }
        adapter.listener = this
    }

    override fun onAddFocusTask() {
        this.activity?.supportFragmentManager?.let { s ->
            CreateFocusBottomSheetDialogFragment().let {
                it.setCallback(viewModel)
                it.show(s, "FocusBottomSheet")
            }
        }
    }

    override fun onOpenTimePicker(id: String, time: Long) {
        this.activity?.supportFragmentManager?.let { s ->
            WheelPickerBottomSheet(
                id = id,
                selectedValue = TimeUnit.MILLISECONDS.toMinutes(time).toInt(),
                viewModel
            ).show(s, "WheelPickerBottomSheet")
        }
    }

    override fun onEditName(id: String) {
        this.activity?.supportFragmentManager?.let { s ->
            CreateFocusBottomSheetDialogFragment().let {
                it.setEditName(id)
                it.setCallback(viewModel)
                it.show(s, "FocusBottomSheet")
            }
        }
    }

    override fun onDelete(id: String) {
        context?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(it.getString(R.string.do_u_want_delete_focus))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    viewModel.deleteFocusTask(id)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show()
        }
    }

    override fun onStart(id: String, time: Long, name: String) {
        context?.let {
            ShizukuSettings.saveCurrentFocusTask(
                CurrentFocus(
                    id = id,
                    name = name,
                    time = time,
                    remainingTime = time,
                    isPaused = false
                )
            )
            startActivity(Intent(it, FocusDetailsActivity::class.java))
        }
    }
}