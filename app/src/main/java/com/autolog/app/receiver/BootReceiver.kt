package com.autolog.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.autolog.app.AutoLogApplication

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Приложение готово к работе после перезагрузки
            val app = context.applicationContext as? AutoLogApplication
            app?.checkAlreadyConnectedDevices()
        }
    }
}