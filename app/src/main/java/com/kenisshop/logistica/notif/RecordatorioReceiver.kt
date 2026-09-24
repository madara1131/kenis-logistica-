package com.kenisshop.logistica.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kenisshop.logistica.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Lo dispara AlarmManager una vez al día. */
class RecordatorioReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val resultado = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sinIngreso = AppDatabase.get(context).pedidoDao().sinIngreso()
                Notificaciones.recordatorioDiario(context, sinIngreso)
            } finally {
                resultado.finish()
            }
        }
    }
}
