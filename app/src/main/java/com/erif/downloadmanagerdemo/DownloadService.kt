package com.erif.downloadmanagerdemo

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class DownloadService: Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("Start service")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        log("Destroy service")
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun log(message: String?) {
        Log.d("DownloadManagerDemo", message ?: "")
    }


    /*startService(Intent(this, DownloadService::class.java))
        Handler(mainLooper).postDelayed({
            stopService(Intent(this, DownloadService::class.java))
        }, 2000)*/
    /*private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }*/

}