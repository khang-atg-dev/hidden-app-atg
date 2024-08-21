package moe.shizuku.manager.focus

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import moe.shizuku.manager.databinding.FocusFragmentBinding
import rikka.core.ktx.unsafeLazy
import rikka.lifecycle.viewModels
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect

class FocusFragment : Fragment(), FocusCallback {
    private lateinit var binding: FocusFragmentBinding
    private val viewModel by viewModels { FocusViewModel() }
    private val adapter by unsafeLazy { FocusAdapter() }

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
            setItemTouchHelper(it, recyclerView, adapter)
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

    override fun onOpenTimePicker(time: Long) {
        this.activity?.supportFragmentManager?.let { s ->
            WheelPickerBottomSheet().show(s, "WheelPickerBottomSheet")
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
}