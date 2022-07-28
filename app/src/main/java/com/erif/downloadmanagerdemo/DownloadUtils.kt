package com.erif.downloadmanagerdemo

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import java.io.File

class DownloadUtils {

    companion object {

        fun request(url: String): DownloadManager.Request {
            val paths: List<String> = url.split("/")
            val originalFileName = paths[paths.size - 1]
            val fileName = createFileName(originalFileName)
            return DownloadManager.Request(Uri.parse(url))
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE
                        or DownloadManager.Request.NETWORK_WIFI)
                .setTitle("Xabiru")
                .setDescription("Mengunduh $originalFileName")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        fun downloadChecker(manager: DownloadManager?, downloadId: Long, callback: Callback): Thread {
            var isDownloading = true
            return Thread {
                while (isDownloading) {
                    val query = DownloadManager.Query()
                    query.setFilterById(downloadId)
                    val cursor: Cursor? = manager?.query(query)
                    cursor?.moveToFirst()
                    val bytesDownloaded: Int = cursor?.getInt(getBytesDownloaded(cursor)) ?: 0
                    val bytesTotal = cursor?.getInt(getBytesTotal(cursor)) ?: 0
                    if (cursor?.getInt(getStatus(cursor)) == DownloadManager.STATUS_SUCCESSFUL) {
                        isDownloading = false
                    }
                    val currentProgress: Int = ((bytesDownloaded * 100L) / bytesTotal).toInt()
                    callback.onDownloadProgress(currentProgress)
                    statusCallback(cursor, callback)
                    cursor?.close()
                }
            }
        }

        fun getCurrentDownload(manager: DownloadManager?, downloadId: Long): Boolean {
            val query = DownloadManager.Query()
            query.setFilterById(downloadId)
            val cursor: Cursor? = manager?.query(query)
            return cursor?.moveToFirst() == true
        }

        fun getCurrentDownloadCursor(manager: DownloadManager?, downloadId: Long): Cursor? {
            val query = DownloadManager.Query()
            query.setFilterById(downloadId)
            return manager?.query(query)
        }

        private fun statusCallback(cursor: Cursor?, callback: Callback) {
            //DownloadManager.STATUS_RUNNING -> callback?.pro
            //DownloadManager.STATUS_SUCCESSFUL -> "Download complete!"
            when (cursor?.getColumnIndex(DownloadManager.COLUMN_STATUS)?.let { cursor.getInt(it) }) {
                DownloadManager.STATUS_FAILED -> callback.onDownloadFailed()
                DownloadManager.STATUS_PAUSED -> callback.onDownloadPause()
                DownloadManager.STATUS_PENDING -> callback.onDownloadPending()
            }
        }

        private fun getBytesDownloaded(cursor: Cursor?): Int {
            return cursor?.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR) ?: 0
        }

        private fun getBytesTotal(cursor: Cursor?): Int {
            return cursor?.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES) ?: 0
        }

        private fun getStatus(cursor: Cursor?): Int {
            return cursor?.getColumnIndex(DownloadManager.COLUMN_STATUS) ?: 0
        }

        fun broadcastReceiver(manager: DownloadManager?, callback: CallbackFinished?): BroadcastReceiver {
            return object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.action
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                        callback?.onDownloadSuccess()
                        val parentPath: String? = getExternalStorage(context)
                        parentPath?.let {
                            val path = "$it/Download"
                            val newPath = "$it/Xabiru/Xabiru Documents"
                            val file = File(path)
                            if (file.exists()) {
                                if (file.isDirectory) {
                                    file.listFiles()?.forEach { childFile ->
                                        val name = childFile.name
                                        if (name.contains("XBR-")) {
                                            val fileDestination = File("$newPath/$name")
                                            childFile.renameTo(fileDestination)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        fun getExternalStorage(context: Context?): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val storageManager = context?.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val storageVolume = storageManager.primaryStorageVolume
                storageVolume.directory?.path
            } else {
                Environment.getExternalStorageDirectory().absolutePath
            }
        }

        private fun createFileName(originalFileName: String?): String {
            val millis = System.currentTimeMillis()
            //val additional = "XBR-$millis"
            val additional = "XBR"
            return "$additional-$originalFileName"
        }

        private fun log(message: String?) {
            Log.d("DownloadManagerDemo", message ?: "")
        }

    }

    interface Callback {
        fun onDownloadProgress(progress: Int)
        fun onDownloadFailed()
        fun onDownloadPause()
        fun onDownloadPending()
    }

    interface CallbackFinished {
        fun onDownloadSuccess()
    }

}