package moe.shizuku.manager.focus.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.AppConstants.FOCUS_ID
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.databinding.FocusDetailsFragmentBinding
import moe.shizuku.manager.model.Focus

class FocusDetailsFragment : Fragment() {
    private lateinit var binding: FocusDetailsFragmentBinding
    private lateinit var circleProgressView: CircleProgressView

    private var isPaused = false
    private var focusTask: Focus? = null

    private lateinit var workRequest: OneTimeWorkRequest
    private var workManager: WorkManager? = null
    private var workInfoLiveData: LiveData<WorkInfo>? = null
    private var observer: Observer<WorkInfo?> = Observer { workInfo ->
        workInfo?.let {
            when (it.state) {
                WorkInfo.State.RUNNING -> {
                    val remainingTimeMillis = workInfo.progress.getLong(
                        CountdownWorker.REMAINING_TIME_MILLIS,
                        -1L
                    )
                    if (remainingTimeMillis >= 0) {
                        circleProgressView.updateProgress(remainingTimeMillis)
                    }
                }
                WorkInfo.State.SUCCEEDED -> {
                    endTimer()
                    this@FocusDetailsFragment.activity?.finish()
                }
                else -> {}
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        context?.let {
            workManager = WorkManager.getInstance(it)
        }
        arguments?.let {
            ShizukuSettings.getFocusTaskById(it.getString(FOCUS_ID, ""))?.let { f ->
                focusTask = f
                workRequest = createCountdownWorkRequest(f.time)
            }
        }
        binding = FocusDetailsFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        focusTask?.let { f ->
            binding.name.text = f.name
            circleProgressView = binding.circleProgress.apply {
                setTotalTimeMillis(f.time)
                startTimer()
            }
            binding.btnPauseResume.let { v ->
                v.text = if (isPaused) "Resume" else "Pause"
                v.setOnClickListener {
                    if (isPaused) {
                        resumeTimer()
                        v.text = "Pause"
                    } else {
                        pauseTimer()
                        v.text = "Resume"
                    }
                }
            }
            binding.btnEnd.setOnClickListener {
                context?.let {
                    MaterialAlertDialogBuilder(it)
                        .setTitle("Are you sure to end this task now?")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            endTimer()
                            this.activity?.finish()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show()
                }
            }
        }
    }

    override fun onDestroy() {
        workRequest.let {
            workManager?.cancelWorkById(it.id)
            workInfoLiveData?.removeObserver(observer)
        }
        super.onDestroy()
    }

    private fun createCountdownWorkRequest(totalTimeMillis: Long): OneTimeWorkRequest {
        return OneTimeWorkRequest.Builder(CountdownWorker::class.java)
            .addTag(CountdownWorker.WORK_TAG)
            .setInputData(
                workDataOf(
                    CountdownWorker.TOTAL_TIME_MILLIS to totalTimeMillis,
                    CountdownWorker.FOCUS_ID to focusTask?.id
                )
            )
            .build()
    }

    private fun startTimer() {
        workManager?.let {
            it.beginUniqueWork(
                CountdownWorker.WORK_TAG, ExistingWorkPolicy.REPLACE, workRequest
            ).enqueue()
            observeWorkProgress(it, workRequest)
        }
    }

    private fun observeWorkProgress(workManager: WorkManager, workRequest: WorkRequest) {
        workInfoLiveData = workManager.getWorkInfoByIdLiveData(workRequest.id)
        workInfoLiveData?.observe(viewLifecycleOwner, observer)
    }

    private fun pauseTimer() {
        isPaused = true
        workRequest.let {
            workManager?.cancelWorkById(it.id)
            workInfoLiveData?.removeObserver(observer)
        }
    }

    private fun resumeTimer() {
        val remainingTimeMillis = ShizukuSettings.getRemainingTime(focusTask?.id ?: "")
        if (remainingTimeMillis > 0) {
            workRequest = createCountdownWorkRequest(remainingTimeMillis)
            startTimer()
            isPaused = false
        }
    }


    private fun endTimer() {
        isPaused = false
        workRequest.let {
            workManager?.cancelWorkById(it.id)
            workInfoLiveData?.removeObserver(observer)
        }
        ShizukuSettings.removeRemainingTime(focusTask?.id)
        ShizukuSettings.removeCurrentFocusTask()
    }

}