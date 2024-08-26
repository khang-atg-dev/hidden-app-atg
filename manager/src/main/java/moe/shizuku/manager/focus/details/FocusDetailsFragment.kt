package moe.shizuku.manager.focus.details

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
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
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.databinding.FocusDetailsFragmentBinding
import moe.shizuku.manager.model.CurrentFocus


class FocusDetailsFragment : Fragment() {
    private lateinit var binding: FocusDetailsFragmentBinding
    private lateinit var circleProgressView: CircleProgressView

    private var countdownServiceIntent: Intent? = null
    private var isPaused = false
    private var focusTask: CurrentFocus? = null
    private var isLandScape: Boolean = false

    private var workRequest: OneTimeWorkRequest? = null
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
                    view?.postDelayed({
                        activity?.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }, 300)
                    activity?.finish()
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
        ShizukuSettings.getCurrentFocusTask()?.let { f ->
            focusTask = f
            isPaused = f.isPaused
            workRequest = createCountdownWorkRequest(f.remainingTime)
            context?.let { c ->
                countdownServiceIntent = Intent(c, CountdownService::class.java)
                if (!isPaused) startCountdownService()
            }
        }
        binding = FocusDetailsFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        focusTask?.let { f ->
            binding.name.text = f.name
            circleProgressView = binding.circleProgress.apply {
                initTimeMillis(f.time, f.remainingTime)
                if (!isPaused) startTimer()
            }
            binding.btnPauseResume.let { v ->
                v.text = if (isPaused) "Resume" else "Pause"
                v.icon = if (isPaused) {
                    context?.getDrawable(R.drawable.ic_outline_play_arrow_24)
                } else {
                    context?.getDrawable(R.drawable.baseline_pause_24)
                }
                v.setOnClickListener {
                    if (isPaused) {
                        resumeTimer()
                        v.text = "Pause"
                        v.icon = context?.getDrawable(R.drawable.baseline_pause_24)
                    } else {
                        pauseTimer()
                        v.text = "Resume"
                        v.icon = context?.getDrawable(R.drawable.ic_outline_play_arrow_24)
                    }
                }
            }
            ShizukuSettings.getColorCurrentTask()?.let {
                updateColorView(it)
            }
            binding.colorPicker.setOnClickListener {
                this.activity?.supportFragmentManager?.let {
                    ColorsBottomSheet(object : ColorPickerCallback {
                        override fun onColorSelected(hexColor: String) {
                            ShizukuSettings.saveColorCurrentTask(hexColor)
                            updateColorView(hexColor)
                        }
                    }).show(it, "ColorPicker")
                }
            }
            binding.btnEnd.setOnClickListener {
                context?.let {
                    MaterialAlertDialogBuilder(it)
                        .setTitle("Are you sure to end this task now?")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            endTimer()
                            view.postDelayed({
                                activity?.requestedOrientation =
                                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }, 300)
                            activity?.finish()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show()
                }
            }
            binding.rotation.setOnClickListener {
                activity?.let {
                    if (isLandScape) {
                        it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    } else {
                        it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                    isLandScape = !isLandScape
                }
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroy() {
        workRequest?.let {
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
                )
            )
            .build()
    }

    private fun startTimer() {
        workManager?.let {
            workRequest?.let { wr ->
                it.beginUniqueWork(
                    CountdownWorker.WORK_TAG, ExistingWorkPolicy.REPLACE, wr
                ).enqueue()
                observeWorkProgress(it, wr)
            }
        }
    }

    private fun observeWorkProgress(workManager: WorkManager, workRequest: WorkRequest) {
        workInfoLiveData = workManager.getWorkInfoByIdLiveData(workRequest.id)
        workInfoLiveData?.observe(viewLifecycleOwner, observer)
    }

    private fun pauseTimer() {
        stopCountdownService()
        isPaused = true
        workRequest?.let {
            workManager?.cancelWorkById(it.id)
            workInfoLiveData?.removeObserver(observer)
        }
        ShizukuSettings.updateIsPausedCurrentFocusTask(true)
    }

    private fun resumeTimer() {
        val remainingTimeMillis = ShizukuSettings.getCurrentFocusTask()?.remainingTime ?: -1
        if (remainingTimeMillis > 0) {
            startCountdownService()
            workRequest = createCountdownWorkRequest(remainingTimeMillis)
            startTimer()
            isPaused = false
            ShizukuSettings.updateIsPausedCurrentFocusTask(false)
        }
    }

    private fun endTimer() {
        stopCountdownService()
        isPaused = false
        workRequest?.let {
            workManager?.cancelWorkById(it.id)
            workInfoLiveData?.removeObserver(observer)
        }
        ShizukuSettings.removeCurrentFocusTask()
    }

    private fun startCountdownService() {
        context?.let {
            countdownServiceIntent?.let { intent ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.startForegroundService(intent)
                } else {
                    it.startService(intent)
                }
            }
        }
    }

    private fun stopCountdownService() {
        context?.let {
            countdownServiceIntent?.let { intent ->
                it.stopService(intent)
            }
        }
    }

    private fun updateColorView(hexColor: String) {
        val color = Color.parseColor(hexColor)
        circleProgressView.updateColor(hexColor)
        binding.name.setTextColor(color)
        binding.btnPauseResume.setBackgroundColor(color)
        binding.btnEnd.setBackgroundColor(color)
        binding.rotation.setColorFilter(color)
        binding.colorPicker.setColorFilter(color)
        binding.brightDecrease.setColorFilter(color)
        binding.brightIncrease.setColorFilter(color)
        binding.keepScreen.setColorFilter(color)
    }
}