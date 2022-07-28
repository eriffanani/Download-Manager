package com.erif.downloadmanagerdemo

import android.Manifest
import android.animation.ValueAnimator
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import java.io.File

class MainActivity : AppCompatActivity(), DownloadUtils.Callback, DownloadUtils.CallbackFinished {

    companion object {
        private const val REQUEST_CODE: Int = 1
    }

    private var manager: DownloadManager? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var parentView: RelativeLayout? = null
    private var button: Button? = null
    private var progressBar: CircularProgressIndicator? = null
    private var txtPercentage: TextView? = null
    private var downloadChecker: Thread? = null
    private var btnWidth = 0
    private var currentProgress = 0

    /*private val arrUrl = arrayOf(
        "https://file-examples.com/storage/fe52cb0c4862dc676a1b341/2017/11/file_example_MP3_5MG.mp3",
        "https://media.neliti.com/media/publications/249244-none-837c3dfb.pdf",
        "https://file-examples.com/storage/fe52cb0c4862dc676a1b341/2017/11/file_example_MP3_2MG.mp3",
        "https://wirasetiawan29.files.wordpress.com/2016/01/tentang-material-design1.png"
    )*/
    private val url = "https://jsoncompare.org/LearningContainer/SampleFiles/Video/MP4/Sample-MP4-Video-File-Download.mp4"
    //private val url = "https://media.neliti.com/media/publications/249244-none-837c3dfb.pdf"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        broadcastReceiver = DownloadUtils.broadcastReceiver(manager, this)

        parentView = findViewById(R.id.act_main_parentView)
        button = findViewById(R.id.act_main_btnDownload)
        progressBar = findViewById(R.id.act_main_progressBar)
        txtPercentage = findViewById(R.id.act_main_txtPercentage)

        registerReceiver(
            broadcastReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )

        button?.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                downloadFile()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE
                )
            }
        }

        val id = 6434L
        val query = DownloadManager.Query()
        query.setFilterById(id)
        val cursor: Cursor? = manager?.query(query)
        if (cursor?.moveToFirst() == true) {
            if (cursor.getInt(getStatus(cursor)) != DownloadManager.STATUS_SUCCESSFUL) {
                log("Resume current download")
                button?.visibility = View.INVISIBLE
                button?.let { it.post { button?.text = "" } }
                progressBar?.visibility = View.VISIBLE
                txtPercentage?.visibility = View.VISIBLE
                val thread = DownloadUtils.downloadChecker(manager, id, this)
                thread.start()
            }
        }
        button?.let { it.post { btnWidth = it.width } }

    }

    private fun getStatus(cursor: Cursor?): Int {
        return cursor?.getColumnIndex(DownloadManager.COLUMN_STATUS) ?: 0
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            downloadFile()
        }
    }

    private fun downloadFile() {
        currentProgress = 0
        val downloadId: Long = manager?.enqueue(DownloadUtils.request(url)) ?: 0L
        log("Download Id: $downloadId")
        downloadChecker = DownloadUtils.downloadChecker(manager, downloadId, this)
        DownloadAnimationUtils.hideButton(button, progressBar, txtPercentage)
        progressBar?.isIndeterminate = true
        Handler(mainLooper).postDelayed({
            downloadChecker?.start()
        }, 300L)
    }

    override fun onDownloadProgress(progress: Int) {
        runOnUiThread {
            if (progress > 0)
                progressBar?.isIndeterminate = false
            changeProgress(progress)
        }
    }

    private fun changeProgress(progress: Int) {
        if (progress > currentProgress) {
            log("Progress: $progress")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressBar?.setProgress(progress, true)
            } else {
                progressBar?.progress = progress
            }
            animateProgressText(currentProgress, progress)
            currentProgress = progress
        }
    }

    private fun animateProgressText(start: Int, end: Int) {
        val anim = ValueAnimator.ofInt(start, end)
        anim?.duration = 250
        anim?.addUpdateListener {
            val new = it.animatedValue as Int
            val percentage = "$new%"
            txtPercentage?.text = percentage
        }
        Handler(mainLooper).postDelayed({anim.start()}, 350)
    }

    override fun onDownloadFailed() {
        log("Download failed")
    }

    override fun onDownloadPause() {
        log("Download paused")
    }

    override fun onDownloadPending() {
        log("Download pending")
    }

    override fun onDownloadSuccess() {
        log("Download completed from receiver")
        if (progressBar?.progress == 100) {
            Handler(mainLooper).postDelayed({
                DownloadAnimationUtils.showButton(button, btnWidth, progressBar, txtPercentage)
                showSnackBar()
            }, 1500)
        }
    }

    private fun openDocument2() {
        val externalStorage = DownloadUtils.getExternalStorage(this)
        val xabiruDir = "Xabiru/Xabiru Documents"
        val fileName = getFileName(url)
        val destinationPath = "$externalStorage/$xabiruDir/$fileName"
        val file = File(destinationPath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                this, "${applicationContext.packageName}.provider", file
            )
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.clipData = ClipData.newRawUri("", uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val getExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            val mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(getExtension)
            val undefinedExtension = getExtension.equals("", ignoreCase = true) || mimetype == null
            val extension = if (undefinedExtension) "text/*" else mimetype
            intent.setDataAndType(uri, extension)
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                log("Open pdf failed: ${e.message}")
            }
        }
    }

    private fun showSnackBar() {
        parentView?.let {
            val message = "Document downloaded successfully"
            Snackbar.make(it, message, 3000)
                .setAction("OPEN") {
                    openDocument2()
                }
                .show()
        }
    }

    private fun getFileName(filePath: String?): String? {
        val strLength = filePath?.lastIndexOf("/") ?: 0
        return if (strLength > 0)
            "XBR-"+filePath?.substring(strLength + 1)
        else
            null
    }

    private fun log(message: String?) {
        Log.d("DownloadManagerDemo", message ?: "")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        log("Destroy receiver")
    }

}