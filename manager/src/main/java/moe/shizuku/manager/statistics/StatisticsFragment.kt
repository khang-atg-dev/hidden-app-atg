package moe.shizuku.manager.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import moe.shizuku.manager.databinding.StatisticsFragmentBinding
import rikka.lifecycle.viewModels

class StatisticsFragment: Fragment() {
    private lateinit var binding: StatisticsFragmentBinding
    private val viewModel by viewModels { StatisticsViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    binding.segmentTab.check(state.segmentSelected.id)
                    binding.dateIndicator.text = state.segmentSelected.getFormatTime(state.dateIndicator)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = StatisticsFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.segmentTab.addOnButtonCheckedListener { _group, checkedId, isChecked ->
            if (isChecked) {
                viewModel.onChangeSegment(checkedId)
            }
        }
        binding.forward.setOnClickListener { viewModel.onChangeDateIndicator(true) }
        binding.backward.setOnClickListener { viewModel.onChangeDateIndicator(false) }
    }

    override fun onResume() {
        super.onResume()

    }
}