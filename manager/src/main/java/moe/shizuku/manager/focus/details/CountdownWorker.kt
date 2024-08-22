package moe.shizuku.manager.focus.details

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import moe.shizuku.manager.ShizukuSettings
import kotlin.coroutines.cancellation.CancellationException

class CountdownWorker(appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {
    companion object {
        const val WORK_TAG = "CountdownWorker"
        const val TOTAL_TIME_MILLIS = "TOTAL_TIME_MILLIS"
        const val REMAINING_TIME_MILLIS = "REMAINING_TIME_MILLIS"
        const val FOCUS_ID = "FOCUS_ID"
    }

    override suspend fun doWork(): Result {
        val totalTimeMillis = inputData.getLong(TOTAL_TIME_MILLIS, 0L)
        val focusId = inputData.getString(FOCUS_ID) ?: ""
        var remainingTimeMillis = totalTimeMillis
        return try {
            while (remainingTimeMillis > 0) {
                if (isStopped) {
                    // Save the remaining time when the work is stopped
                    saveRemainingTime(focusId, remainingTimeMillis)
                    return Result.success()
                }

                // Simulate work being done (1-second intervals)
                delay(1000L)

                remainingTimeMillis -= 1000

                // Update progress
                val outputData = workDataOf(REMAINING_TIME_MILLIS to remainingTimeMillis)
                setProgress(outputData)
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) {
                saveRemainingTime(focusId, remainingTimeMillis)
                Result.failure() // Or retry if appropriate
            } else {
                saveRemainingTime(focusId, remainingTimeMillis)
                Result.failure()
            }
        }
    }

    private fun saveRemainingTime(focusId: String, remainingTimeMillis: Long) {
        ShizukuSettings.saveRemainingTime(focusId, remainingTimeMillis)
    }
}