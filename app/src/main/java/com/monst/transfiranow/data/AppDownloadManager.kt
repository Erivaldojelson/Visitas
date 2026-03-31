package com.monst.transfiranow.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.getSystemService
import java.net.URLConnection
import java.util.concurrent.ConcurrentHashMap

object AppDownloadManager {
    private data class Snapshot(
        val bytes: Long,
        val timestamp: Long
    )

    private val trackedIds = linkedSetOf<Long>()
    private val lastSnapshots = ConcurrentHashMap<Long, Snapshot>()

    fun enqueue(context: Context, url: String): Result<Long> {
        val manager = context.getSystemService<DownloadManager>()
            ?: return Result.failure(IllegalStateException("DownloadManager indisponível"))
        val uri = runCatching { Uri.parse(url) }.getOrNull()
            ?: return Result.failure(IllegalArgumentException("URL inválida"))
        if (uri.scheme !in setOf("http", "https")) {
            return Result.failure(IllegalArgumentException("Use um link http ou https"))
        }

        val fileName = guessFileName(url)
        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription("Download iniciado pelo Transfira-now")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        return runCatching {
            manager.enqueue(request).also { id ->
                synchronized(trackedIds) { trackedIds += id }
                TransferRepository.setHelperMessage("Download iniciado pelo app. Esse é o fluxo com mais chance de aparecer na Now Bar.")
            }
        }
    }

    fun poll(context: Context) {
        val manager = context.getSystemService<DownloadManager>() ?: return
        val ids = synchronized(trackedIds) { trackedIds.toList() }
        if (ids.isEmpty()) return

        val query = DownloadManager.Query().setFilterById(*ids.toLongArray())
        manager.query(query)?.use { cursor ->
            val seen = mutableSetOf<Long>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                seen += id
                val title = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)).orEmpty()
                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                val timestamp = System.currentTimeMillis()
                val previous = lastSnapshots[id]
                val speed = previous?.let {
                    val elapsed = (timestamp - it.timestamp).coerceAtLeast(1L)
                    ((downloaded - it.bytes).coerceAtLeast(0L) * 1000L) / elapsed
                }
                lastSnapshots[id] = Snapshot(downloaded, timestamp)

                val progress = when {
                    total > 0L -> ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                    else -> null
                }

                val state = when (status) {
                    DownloadManager.STATUS_RUNNING -> TransferState.Active
                    DownloadManager.STATUS_PENDING, DownloadManager.STATUS_PAUSED -> TransferState.Waiting
                    DownloadManager.STATUS_SUCCESSFUL -> TransferState.Completed
                    DownloadManager.STATUS_FAILED -> TransferState.Failed
                    else -> TransferState.Waiting
                }

                TransferRepository.upsert(
                    TransferEntry(
                        id = "dm:$id",
                        sourceApp = "Transfira-now",
                        title = title.ifBlank { "Download do app" },
                        progress = progress,
                        state = state,
                        origin = TransferOrigin.AppManaged,
                        detail = detailForStatus(status, reason),
                        downloadedBytes = downloaded.takeIf { it >= 0L },
                        totalBytes = total.takeIf { it > 0L },
                        speedBytesPerSecond = speed,
                        updatedAt = timestamp
                    )
                )

                if (state == TransferState.Completed || state == TransferState.Failed) {
                    synchronized(trackedIds) { trackedIds.remove(id) }
                    lastSnapshots.remove(id)
                }
            }

            val missingIds = ids.filterNot { it in seen }
            if (missingIds.isNotEmpty()) {
                synchronized(trackedIds) { trackedIds.removeAll(missingIds.toSet()) }
                missingIds.forEach { lastSnapshots.remove(it) }
            }
        }
    }

    private fun guessFileName(url: String): String {
        val candidate = URLConnection.guessContentTypeFromName(url)
        val lastSegment = Uri.parse(url).lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        return lastSegment ?: "download-${System.currentTimeMillis()}${extensionForMime(candidate)}"
    }

    private fun extensionForMime(mime: String?): String {
        return when (mime) {
            "application/vnd.android.package-archive" -> ".apk"
            "application/zip" -> ".zip"
            else -> ""
        }
    }

    private fun detailForStatus(status: Int, reason: Int): String {
        return when (status) {
            DownloadManager.STATUS_RUNNING -> "Download iniciado pelo Transfira-now."
            DownloadManager.STATUS_PENDING -> "Aguardando fila do sistema."
            DownloadManager.STATUS_PAUSED -> "Download pausado pelo sistema. Código: $reason"
            DownloadManager.STATUS_SUCCESSFUL -> "Download concluído com sucesso."
            DownloadManager.STATUS_FAILED -> "Download falhou. Código: $reason"
            else -> "Status desconhecido."
        }
    }
}
