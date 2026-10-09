package com.neytron.ethereal.data

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neytron.ethereal.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Фоновый воркер для периодической проверки обновлений через GitHub API.
 *
 * Настраивается на запуск раз в сутки (см. schedule).
 *
 * Если найдено обновление — просто сохраняет его в кэш (UpdateRepository).
 * UI сам подхватит свежие данные при следующем открытии About.
 *
 * Воркер работает офлайн-безопасно: если сети нет — просто ничего не делает.
 */
class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val current = BuildConfig.VERSION_NAME
            Log.d(TAG, "Periodic update check started. Current=$current")

            val info = UpdateRepository.getUpdateInfo(
                context = applicationContext,
                currentVersion = current,
                forceRefresh = true
            )

            if (info != null) {
                Log.d(
                    TAG,
                    "Update check done. latest=${info.latestVersion}, hasUpdate=${info.hasUpdate}"
                )
            } else {
                Log.w(TAG, "Update check returned null (network or parse error)")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed: ${e.message}", e)
            // Retry — если сеть отвалилась, Android сам попробует позже
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UpdateWorker"

        private const val WORK_NAME = "ethereal_update_check"

        /** Интервал между проверками. */
        private const val INTERVAL_HOURS = 24L

        /**
         * Ставит периодическую задачу в очередь WorkManager.
         *
         * KEEP — если задача уже стоит, не заменяем (сохраняем исходное расписание).
         * UPDATE — заменяем на новое расписание.
         *
         * Здесь используем KEEP, чтобы не сбивать расписание при каждом запуске приложения.
         */
        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                    repeatInterval = INTERVAL_HOURS,
                    repeatIntervalTimeUnit = TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        request
                    )

                Log.d(TAG, "Periodic work scheduled (every ${INTERVAL_HOURS}h)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule periodic work: ${e.message}", e)
            }
        }

        /**
         * Отменяет фоновую проверку.
         * Например, если пользователь отключил аналитику или проверку обновлений.
         */
        fun cancel(context: Context) {
            try {
                WorkManager.getInstance(context.applicationContext)
                    .cancelUniqueWork(WORK_NAME)
                Log.d(TAG, "Periodic work cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel periodic work: ${e.message}", e)
            }
        }
    }
}