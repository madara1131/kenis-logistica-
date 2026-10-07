package com.kenisshop.logistica.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kenisshop.logistica.data.traker.TipoLista
import com.kenisshop.logistica.data.traker.TrakerDatabase
import com.kenisshop.logistica.data.traker.listaEtiquetas
import com.kenisshop.logistica.data.traker.paginaNombre
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Recordatorios de la libreta: suenan aunque la app esté cerrada. */
object RecordatoriosNotas {
    private const val EXTRA_ID = "nota_id"

    private fun intento(context: Context, id: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            (20_000 + id).toInt(),
            Intent(context, NotaReceiver::class.java).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    /** Programa el aviso a la hora exacta (aunque el teléfono esté en reposo). */
    fun programar(context: Context, id: Long, cuando: Long) {
        if (cuando <= System.currentTimeMillis()) return
        val alarmas = context.getSystemService(AlarmManager::class.java)
        val exacta = Build.VERSION.SDK_INT < 31 || alarmas.canScheduleExactAlarms()
        if (exacta) {
            alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, intento(context, id))
        } else {
            alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, intento(context, id))
        }
    }

    fun cancelar(context: Context, id: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(intento(context, id))
    }

    /** Después de reiniciar el teléfono se vuelven a programar. */
    suspend fun reprogramarTodos(context: Context) {
        val ahora = System.currentTimeMillis()
        TrakerDatabase.get(context).dao().listas()
            .filter { it.lista == TipoLista.NOTA && (it.recordatorio ?: 0L) > ahora }
            .forEach { programar(context, it.id, it.recordatorio!!) }
    }

    fun idDe(intent: Intent): Long = intent.getLongExtra(EXTRA_ID, -1L)
}

class NotaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = RecordatoriosNotas.idDe(intent)
        if (id < 0) return
        val resultado = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nota = TrakerDatabase.get(context).dao().item(id)
                if (nota != null) {
                    val detalle = buildString {
                        append("📒 ${nota.paginaNombre}")
                        if (nota.listaEtiquetas.isNotEmpty()) append(" · " + nota.listaEtiquetas.joinToString(" ") { "#$it" })
                    }
                    Notificaciones.recordatorioNota(context, (40_000 + id).toInt(), nota.nombre, detalle)
                }
            } finally {
                resultado.finish()
            }
        }
    }
}
