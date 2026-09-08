package com.meelano.builder.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.meelano.builder.MainActivity
import com.meelano.builder.R
import com.meelano.builder.data.Repository
import com.meelano.builder.data.isTerminal
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

/**
 * Watches one build in the background and posts a notification when it
 * finishes — even if the user left the app. Tap opens the build screen.
 */
class NotificationWorker(ctx: Context, params: WorkerParameters) :
    CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getString("job_id") ?: return Result.failure()
        val server = inputData.getString("server") ?: return Result.failure()
        val token = inputData.getString("token") ?: ""
        val lang = inputData.getString("lang") ?: "en"
        val repo = Repository()
        val deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(45)
        var status = ""
        var name = ""
        var terminal = false
        while (System.currentTimeMillis() < deadline) {
            try {
                val j = repo.apiFor(server, token).job(jobId)
                status = j.status
                name = j.name.ifEmpty { j.idea }
                if (j.isTerminal()) {
                    terminal = true
                    break
                }
            } catch (_: Exception) { /* transient: keep polling */ }
            delay(20_000)
        }
        if (terminal) notify(status, name, jobId, lang)
        return Result.success()
    }

    private fun notify(status: String, name: String, jobId: String, lang: String) {
        if (ContextCompat.checkSelfPermission(applicationContext,
                Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return
        val fa = lang == "fa"
        val title = when (status) {
            "done" -> if (fa) "✅ برنامه‌ات آماده‌ست!" else "✅ Your app is ready!"
            "failed" -> if (fa) "❌ ساخت ناموفق بود" else "❌ Build failed"
            else -> if (fa) "📦 بیلد تمام شد" else "📦 Build finished"
        }
        val intent = Intent(applicationContext, MainActivity::class.java)
            .putExtra("job_id", jobId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(applicationContext, jobId.hashCode(),
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(
            "builds", if (fa) "پایان ساخت" else "Builds finished",
            NotificationManager.IMPORTANCE_DEFAULT))
        val n = NotificationCompat.Builder(applicationContext, "builds")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(name)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(jobId.hashCode(), n)
    }

    companion object {
        fun watch(ctx: Context, jobId: String, server: String, token: String, lang: String) {
            val req = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(Data.Builder()
                    .putString("job_id", jobId)
                    .putString("server", server)
                    .putString("token", token)
                    .putString("lang", lang)
                    .build())
                .addTag("build-$jobId")
                .build()
            WorkManager.getInstance(ctx).enqueue(req)
        }
    }
}
