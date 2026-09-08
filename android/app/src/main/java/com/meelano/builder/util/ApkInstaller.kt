package com.meelano.builder.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.meelano.builder.data.Repository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ApkInstaller {
    fun destFile(ctx: Context, filename: String): File =
        File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename)

    /** Download (skip if already exists) then return the file. */
    suspend fun download(
        ctx: Context,
        repo: Repository,
        url: String,
        filename: String,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dest = destFile(ctx, filename)
        if (!dest.exists() || dest.length() == 0L) {
            repo.download(url, dest, onProgress)
        } else {
            onProgress(100)
        }
        dest
    }

    /** Direct install of an APK (fires the system installer). */
    fun installApk(ctx: Context, apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!ctx.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(ctx, "Allow “Install unknown apps”, then tap Install again.",
                    Toast.LENGTH_LONG).show()
                val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${ctx.packageName}"))
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(i)
                return
            }
        }
        val uri: Uri = FileProvider.getUriForFile(
            ctx, "${ctx.packageName}.fileprovider", apk)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(i)
    }
}
