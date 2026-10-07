package com.kenisshop.logistica.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Al reiniciar el teléfono (o actualizar la app) se vuelve a programar el recordatorio diario. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Programador.programarTodo(context)
            val resultado = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RecordatoriosNotas.reprogramarTodos(context)
                } finally {
                    resultado.finish()
                }
            }
        }
    }
}
