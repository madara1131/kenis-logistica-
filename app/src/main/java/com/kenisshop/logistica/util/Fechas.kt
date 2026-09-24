package com.kenisshop.logistica.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Fechas {
    private val formato: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val zona: ZoneId get() = ZoneId.systemDefault()

    fun hoy(): Long = desdeLocalDate(LocalDate.now())

    fun aLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()

    fun desdeLocalDate(fecha: LocalDate): Long = fecha.atStartOfDay(zona).toInstant().toEpochMilli()

    fun formatear(millis: Long?): String = millis?.let { aLocalDate(it).format(formato) } ?: "—"

    /** Días completos entre dos fechas (por calendario, no por horas). */
    fun diasEntre(desde: Long, hasta: Long = System.currentTimeMillis()): Long =
        ChronoUnit.DAYS.between(aLocalDate(desde), aLocalDate(hasta)).coerceAtLeast(0)
}
