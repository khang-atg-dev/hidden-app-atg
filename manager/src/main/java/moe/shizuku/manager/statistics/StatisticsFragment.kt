package moe.shizuku.manager.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.XAxis
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
                    if (state.barData.dataSetCount != 0) {
                        binding.barChartContainer.visibility = View.VISIBLE
                        binding.barChart.let {
                            it.xAxis.labelCount = state.barData.entryCount / 2
                            it.axisLeft.axisMaximum =
                                (30.takeIf { state.barData.maxEntryCountSet.yMax < 30 * 60 * 1000f }
                                    ?: 60) * 60 * 1000f
                            it.data = state.barData
                            binding.barChart.invalidate()
                        }
                    } else {
                        binding.barChartContainer.visibility = View.GONE
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

        binding.barChart.setDrawValueAboveBar(false)
        binding.barChart.legend.isEnabled = false
        binding.barChart.description.isEnabled = false
        binding.barChart.renderer = RoundedBarChartRenderer(
            binding.barChart,
            binding.barChart.animator,
            binding.barChart.viewPortHandler
        ).apply {
            setRoundedPositiveDataSetRadius(20f)
            setRoundedShadowRadius(0f)
            setRoundedNegativeDataSetRadius(0f)
        }
        binding.barChart.setPinchZoom(false)
        binding.barChart.isDoubleTapToZoomEnabled = false
        binding.barChart.isScaleXEnabled = false
        binding.barChart.isScaleYEnabled = false
        binding.barChart.xAxis.let {
            it.position = XAxis.XAxisPosition.BOTTOM
            it.setDrawAxisLine(false)
            it.setDrawGridLines(false)
            it.axisMinimum = -1f
        }
        binding.barChart.axisRight.isEnabled = false
        binding.barChart.axisLeft.let {
            it.axisMinimum = 0f
            it.labelCount = 6
            it.axisMaximum = 60 * 60 * 1000f
            it.setDrawAxisLine(false)
            it.valueFormatter = CustomBarValueFormatter()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }
}