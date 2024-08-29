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
import moe.shizuku.manager.utils.formatMillisecondsToSimple
import rikka.lifecycle.viewModels

class StatisticsFragment : Fragment() {
    private lateinit var binding: StatisticsFragmentBinding
    private lateinit var adapter: StatisticAdapter
    private val viewModel by viewModels { StatisticsViewModel(requireContext()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    binding.segmentTab.check(state.segmentSelected.id)
                    binding.dateIndicator.text =
                        state.segmentSelected.getFormatTime(state.dateIndicator)
                    binding.totalTime.text = state.totalTime.formatMillisecondsToSimple()
                    binding.totalFocus.text = state.numberOfFocuses.toString()
                    if (state.pieData.dataSetCount != 0) {
                        binding.pieChartContainer.visibility = View.VISIBLE
                        binding.pieChart.data = state.pieData
                        binding.pieChart.invalidate()
                        adapter.dataSource = state.listStatistics
                    } else {
                        binding.pieChartContainer.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        adapter = StatisticAdapter(inflater)
        binding = StatisticsFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.segmentTab.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.onChangeSegment(checkedId)
            }
        }
        binding.forward.setOnClickListener { viewModel.onChangeDateIndicator(true) }
        binding.backward.setOnClickListener { viewModel.onChangeDateIndicator(false) }
        binding.pieChart.let {
            it.extraTopOffset = 20f
            it.extraBottomOffset = 20f
            it.setDrawEntryLabels(true)
            it.setUsePercentValues(true)
            it.setEntryLabelColor(binding.totalTime.textColors.defaultColor)
            it.setEntryLabelTextSize(10f)
            it.isRotationEnabled = false
            it.legend.isEnabled = false
            it.description.isEnabled = false
            it.setHoleColor(android.R.color.transparent)
        }
        binding.listStatistics.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }
}