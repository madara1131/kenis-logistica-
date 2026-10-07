package com.kenisshop.logistica.notif

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kenisshop.logistica.data.AppDatabase
import com.kenisshop.logistica.data.midia.MiDiaPrefs
import com.kenisshop.logistica.data.midia.avisosTemporadas
import com.kenisshop.logistica.data.midia.calcularTemporadas
import java.time.LocalDate

/** Revisa las temporadas y envía los avisos que tocan hoy. Todo local, sin internet. */
object RevisorTemporadas {
    suspend fun revisar(context: Context) {
        val prefs = MiDiaPrefs(context)
        val ajustes = prefs.ajustes()
        val hoy = LocalDate.now()
        val temporadas = AppDatabase.get(context).miDiaDao().temporadas()
        val avisos = avisosTemporadas(calcularTemporadas(temporadas, hoy, ajustes), prefs.avisosEnviados(), ajustes)
        if (avisos.isEmpty()) return
        avisos.forEach { a ->
            // un id por aviso para que no se pisen entre sí
            Notificaciones.miDia(context, 50_000 + (a.claves.first().hashCode() and 0x7FFF), a.titulo, a.texto, a.urgente)
        }
        prefs.marcarEnviados(avisos.flatMap { it.claves }, hoy.year)
    }
}

/** WorkManager: revisión diaria aunque la app esté cerrada. */
class TemporadasWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            RevisorTemporadas.revisar(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
