package com.kenisshop.logistica.notif

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kenisshop.logistica.data.AppDatabase
import com.kenisshop.logistica.data.EstadoPedido

/** Revisa (aunque la app esté cerrada) los pedidos que pasaron su límite sin ingreso. */
class AtrasoWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val pendientes = AppDatabase.get(applicationContext).pedidoDao().sinIngreso()
        pendientes
            .filter { it.estado() == EstadoPedido.VENCIDO }
            .forEach { Notificaciones.alertaAtraso(applicationContext, it) }
        return Result.success()
    }
}
