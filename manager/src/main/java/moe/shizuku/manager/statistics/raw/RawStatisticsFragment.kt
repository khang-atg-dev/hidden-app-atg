package moe.shizuku.manager.statistics.raw

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
import moe.shizuku.manager.databinding.RawStatisticsFragmentBinding
import moe.shizuku.manager.statistics.SegmentTime
import moe.shizuku.manager.utils.toDate
import rikka.core.ktx.unsafeLazy
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import java.util.Calendar

class RawStatisticsFragment : Fragment() {
    private lateinit var binding: RawStatisticsFragmentBinding
    private val adapter by unsafeLazy { RawStatisticAdapter() }
    private val viewModel by unsafeLazy { RawStatisticsViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    adapter.updateData(state.statisticsData)
                }
            }
        }
        arguments?.let {
            val id = it.getString("RAW_STATISTICS_ID") ?: ""
            val segment = SegmentTime.fromId(it.getInt("SEGMENT_ENUM_ID", SegmentTime.DAY.id))
            val date = it.getString("DATE_INDICATOR")?.toDate() ?: Calendar.getInstance().time
            viewModel.initData(id, segment, date)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = RawStatisticsFragmentBinding.inflate(inflater)
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
    }
}