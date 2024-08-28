package moe.shizuku.manager.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.databinding.StatisticsFragmentBinding

class StatisticsFragment: Fragment() {
    private lateinit var binding: StatisticsFragmentBinding

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
        binding.test.text = ShizukuSettings.getAllStatistics().toString()
    }

    override fun onResume() {
        super.onResume()
        binding.test.text = ShizukuSettings.getAllStatistics().toString()
    }
}