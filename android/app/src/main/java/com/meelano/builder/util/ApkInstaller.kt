package com.meelano.builder.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object ApkInstaller {
    fun destFile(ctx: Context, filename: String): File =
        File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename)

    fun canInstall(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            ctx.packageManager.canRequestPackageInstalls()

    /**
     * System download (resumable, background-friendly, with a status-bar
     * notification). Progress callback runs on a background thread.
     */
    suspend fun download(
        ctx: Context,
        url: String,
        filename: String,
        token: String,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dest = destFile(ctx, filename)
        if (dest.exists() && dest.length() > 0) {
            onProgress(100)
            return@withContext dest
        }
        dest.parentFile?.mkdirs()
        if (dest.exists()) dest.delete()
        val dm = ctx.getSystemService(DownloadManager::class.java)
        val req = DownloadManager.Request(Uri.parse(url))
            .setTitle(filename)
            .setDescription("MeeLano Builder")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(dest))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        if (token.isNotEmpty()) req.addRequestHeader("X-Builder-Token", token)
        val id = dm.enqueue(req)
        while (true) {
            val q = DownloadManager.Query().setFilterById(id)
            var done = false
            dm.query(q)?.use { c ->
                if (c.moveToFirst()) {
                    val status = c.getInt(c.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_STATUS))
                    val soFar = c.getLong(c.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = c.getLong(c.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            onProgress(100)
                            done = true
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val reason = c.getInt(c.getColumnIndexOrThrow(
                                DownloadManager.COLUMN_REASON))
                            throw Exception("download failed (reason $reason)")
                        }
                        else -> onProgress(
                            if (total > 0) (soFar * 100 / total).toInt() else -1)
                    }
                }
            }
            if (done) break
            delay(400)
        }
        dest
    }

    /**
     * Direct install of an APK (fires the system installer).
     * @return true if the installer was launched, false if the user must
     * first grant the "unknown apps" permission (we open Settings for them).
     */
    fun installApk(ctx: Context, apk: File): Boolean {
        if (!canInstall(ctx)) {
            Toast.makeText(ctx, "Allow “Install unknown apps”, then tap Install again.",
                Toast.LENGTH_LONG).show()
            val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${ctx.packageName}"))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
            return false
        }
        val uri: Uri = FileProvider.getUriForFile(
            ctx, "${ctx.packageName}.fileprovider", apk)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(i)
        return true
    }
}
