package com.kenisshop.logistica.notif

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kenisshop.logistica.MainActivity
import com.kenisshop.logistica.R
import com.kenisshop.logistica.data.Pedido

object Notificaciones {
    const val CANAL_ATRASO = "alertas_atraso"
    const val CANAL_RECORDATORIO = "recordatorio_diario"
    const val CANAL_TRAKER = "traker_cambios"
    private const val ID_RECORDATORIO = 1

    fun crearCanales(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_ATRASO, "Alertas de atraso", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Aviso cuando un pedido aéreo pasa 15 días o uno marítimo 25 días sin ingreso"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_RECORDATORIO, "Recordatorio diario", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Pregunta diaria: ¿Se registró mercadería hoy?"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_TRAKER, "Traker de gastos", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos del Traker: presupuestos excedidos, saldo negativo y cambios al importar el Excel"
            }
        )
    }

    /** Aviso del Traker. [id] distinto por tipo de aviso para que no se pisen entre sí. */
    @SuppressLint("MissingPermission")
    fun traker(context: Context, id: Int, titulo: String, lineas: List<String>, urgente: Boolean = false) {
        if (!puedeNotificar(context)) return
        val estilo = NotificationCompat.InboxStyle().setBigContentTitle(titulo)
        lineas.take(7).forEach { estilo.addLine(it) }
        if (lineas.size > 7) estilo.setSummaryText("y ${lineas.size - 7} más")
        val n = NotificationCompat.Builder(context, CANAL_TRAKER)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(if (urgente) 0xFFD93B30.toInt() else 0xFFD9468F.toInt())
            .setContentTitle(titulo)
            .setContentText(lineas.firstOrNull() ?: "")
            .setStyle(estilo)
            .setPriority(if (urgente) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(abrirApp(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }

    private fun abrirApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun puedeNotificar(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun alertaAtraso(context: Context, p: Pedido) {
        if (!puedeNotificar(context)) return
        val dias = p.diasTranscurridos()
        val detalle = "Código: ${p.codigo}\n" +
            "Tipo: Mercadería ${p.tipo.etiqueta}\n" +
            "Empresa a casillero Miami: ${p.empresaEnvio}\n" +
            "Estado actual: ${p.estado().etiqueta}\n" +
            "⚠ Lleva $dias días sin registrar ingreso (límite ${p.tipo.diasLimite} días)."
        val n = NotificationCompat.Builder(context, CANAL_ATRASO)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(0xFFD93B30.toInt())
            .setContentTitle("¡Pedido ${p.tipo.etiqueta.lowercase()} retrasado! #${p.codigo}")
            .setContentText("${p.empresaEnvio} · ${p.estado().etiqueta} · $dias días sin ingreso")
            .setStyle(NotificationCompat.BigTextStyle().bigText(detalle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrirApp(context))
            .setAutoCancel(true)
            .build()
        // Un ID por pedido: si ya había aviso de ese pedido, se actualiza en vez de duplicarse.
        NotificationManagerCompat.from(context).notify(1000 + p.id.toInt(), n)
    }

    @SuppressLint("MissingPermission")
    fun recordatorioDiario(context: Context, sinIngreso: List<Pedido>) {
        if (!puedeNotificar(context)) return
        val resumen = if (sinIngreso.isEmpty()) {
            "No hay pedidos esperando ingreso."
        } else {
            "${sinIngreso.size} pedido(s) esperan ingreso. Toca para registrar."
        }
        val estilo = NotificationCompat.InboxStyle().setBigContentTitle("¿Se registró mercadería hoy?")
        if (sinIngreso.isEmpty()) {
            estilo.addLine(resumen)
        } else {
            sinIngreso.take(6).forEach { p ->
                estilo.addLine("#${p.codigo} · ${p.tipo.etiqueta} · ${p.empresaEnvio} · ${p.estado().etiqueta}")
            }
            if (sinIngreso.size > 6) estilo.setSummaryText("y ${sinIngreso.size - 6} más")
        }
        val n = NotificationCompat.Builder(context, CANAL_RECORDATORIO)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(0xFF1E4E9A.toInt())
            .setContentTitle("¿Se registró mercadería hoy?")
            .setContentText(resumen)
            .setStyle(estilo)
            .setContentIntent(abrirApp(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_RECORDATORIO, n)
    }
}
