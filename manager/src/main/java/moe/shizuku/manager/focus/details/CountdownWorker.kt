package moe.shizuku.manager.focus.details

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay

class CountdownWorker(appContext: Context, parameters: WorkerParameters) :
    CoroutineWorker(appContext, parameters) {
    companion object {
        const val WORK_TAG = "CountdownWorker"
        const val TOTAL_TIME_MILLIS = "TOTAL_TIME_MILLIS"
        const val REMAINING_TIME_MILLIS = "REMAINING_TIME_MILLIS"
    }

    override suspend fun doWork(): Result {
        val totalTimeMillis = inputData.getLong(TOTAL_TIME_MILLIS, 0L)
        var remainingTimeMillis = totalTimeMillis
        return try {
            while (remainingTimeMillis > 0) {
                if (isStopped) {
                    return Result.failure()
                }
                delay(1000L)
                remainingTimeMillis -= 1000
                val outputData = workDataOf(REMAINING_TIME_MILLIS to remainingTimeMillis)
                setProgressAsync(outputData)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}