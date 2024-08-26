package moe.shizuku.manager.focus.details

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
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
    private var currentBrightness: Int = 0
    private var isKeepScreenOn: Boolean = false

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
            currentBrightness = getCurrentBrightness()
            isKeepScreenOn = ShizukuSettings.getKeepScreenOnCurrentTask()
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
                v.text =
                    context?.getString(if (isPaused) R.string.resume_forcus_task else R.string.pause_focus_task)
                v.icon = if (isPaused) {
                    context?.getDrawable(R.drawable.ic_outline_play_arrow_24)
                } else {
                    context?.getDrawable(R.drawable.baseline_pause_24)
                }
                v.setOnClickListener {
                    if (isPaused) {
                        resumeTimer()
                        v.text = context?.getString(R.string.pause_focus_task)
                        v.icon = context?.getDrawable(R.drawable.baseline_pause_24)
                    } else {
                        pauseTimer()
                        v.text = context?.getString(R.string.resume_forcus_task)
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
                        .setTitle(it.getString(R.string.are_u_sure_end_task))
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
            binding.brightDecrease.setOnClickListener {
                if (!checkWriteSettingsPermission()) {
                    showPermissionExplanationDialog()
                } else {
                    decreaseBrightness()
                }
            }
            binding.brightIncrease.setOnClickListener {
                if (!checkWriteSettingsPermission()) {
                    showPermissionExplanationDialog()
                } else {
                    increaseBrightness()
                }
            }
            binding.keepScreen.setImageDrawable(
                if (isKeepScreenOn) {
                    context?.getDrawable(R.drawable.light_bulb)
                } else {
                    context?.getDrawable(R.drawable.light_bulb_slash)
                }
            )
            binding.keepScreen.setOnClickListener {
                activity?.let {
                    if (!isKeepScreenOn) {
                        binding.keepScreen.setImageDrawable(it.getDrawable(R.drawable.light_bulb))
                        it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        binding.keepScreen.setImageDrawable(it.getDrawable(R.drawable.light_bulb_slash))
                        it.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
                isKeepScreenOn = !isKeepScreenOn
                ShizukuSettings.setKeepScreenOnCurrentTask(isKeepScreenOn)
                Toast.makeText(
                    context,
                    context?.getString(R.string.keep_screen) + " " +
                            if (isKeepScreenOn) context?.getString(R.string.on)
                            else context?.getString(R.string.off),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroy() {
        workRequest?.let {
            workManager?.cancelWorkById(it.id)
            workInfoLiveData?.removeObserver(observer)
        }
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

    private fun checkWriteSettingsPermission(): Boolean {
        return Settings.System.canWrite(requireContext())
    }

    private fun requestWriteSettingsPermission() {
        if (!checkWriteSettingsPermission()) {
            context?.let {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:" + it.packageName)
                startActivity(intent)
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        context?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(it.getString(R.string.permissions_required))
                .setMessage(it.getString(R.string.write_setting_perrmission_msg))
                .setPositiveButton(it.getString(R.string.grant_permission)) { _, _ ->
                    requestWriteSettingsPermission()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun getCurrentBrightness(): Int {
        return context?.let {
            Settings.System.getInt(
                it.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                0
            ) / 13 // Convert from 0-255 to 0-20
        } ?: 0
    }

    private fun setBrightnessLevel(level: Int) {
        if (level < 0 || level > 20) return
        val brightnessValue = level * 13 // Convert from 0-20 to 0-255
        val layoutParams = activity?.window?.attributes
        layoutParams?.screenBrightness = brightnessValue / 255f
        activity?.window?.attributes = layoutParams
    }

    private fun increaseBrightness() {
        if (currentBrightness < 20) {
            currentBrightness++
            setBrightnessLevel(currentBrightness)
        }
    }

    private fun decreaseBrightness() {
        if (currentBrightness > 0) {
            currentBrightness--
            setBrightnessLevel(currentBrightness)
        }
    }
}