package com.juanka.uberrental

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Al prender el teléfono, vuelve a levantar el servicio en primer plano.
 * (El servicio de accesibilidad se reactiva solo si sigue habilitado.)
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            KeepAliveService.start(context)
        }
    }
}
