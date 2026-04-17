package com.example.pomodorotimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, TimerService::class.java)
        when (intent.action) {
            "NOTIF_PAUSE" -> {
                serviceIntent.action = TimerService.ACTION_PAUSE
                context.startService(serviceIntent)
            }
            "NOTIF_RESUME" -> {
                serviceIntent.action = TimerService.ACTION_RESUME
                context.startService(serviceIntent)
            }
            "NOTIF_RESET" -> {
                serviceIntent.action = TimerService.ACTION_RESET
                context.startService(serviceIntent)
            }
            "NOTIF_SKIP" -> {
                serviceIntent.action = TimerService.ACTION_RESET
                context.startService(serviceIntent)
                // Send skip broadcast to fragment
                context.sendBroadcast(Intent("ACTION_SKIP_FROM_NOTIF").apply {
                    setPackage(context.packageName)
                })
            }
        }
    }
}
