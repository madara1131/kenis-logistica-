package com.kenisshop.logistica.data.midia

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// ====================================================================== TEMPORADAS

/** Fecha comercial que se repite cada año (Día de las Madres, Navidad…). */
@Entity(tableName = "temporadas")
data class Temporada(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val emoji: String,
    /** 1..12 */
    val mes: Int,
    /** 1..31 */
    val dia: Int,
    val orden: Int = 0
)

enum class EstadoTemporada(val etiqueta: String) {
    A_TIEMPO("A tiempo"),
    SE_ACERCA("Se acerca"),
    LIMITE_PASADO("Fecha límite pasada"),
    ACTUAL("Temporada actual")
}

/**
 * Tiempos de tránsito que usa el calendario. Empiezan iguales a las reglas de logística
 * (15 días aérea, 25 marítima) y se pueden cambiar sin tocar los pedidos existentes.
 */
data class AjustesTemporadas(
    val diasAerea: Int = 15,
    val diasMaritima: Int = 25,
    /** Días antes de una fecha límite en que el estado pasa a "Se acerca" */
    val diasAviso: Int = 7,
    /** Días antes de la temporada en que se considera "Temporada actual" */
    val diasActual: Int = 3
)

data class InfoTemporada(
    val temporada: Temporada,
    /** Próxima ocurrencia (hoy incluido) */
    val fecha: LocalDate,
    val diasRestantes: Long,
    val limiteAerea: LocalDate,
    val limiteMaritima: LocalDate,
    /** Días que faltan para cada fecha límite; negativo = ya pasó */
    val diasParaAerea: Long,
    val diasParaMaritima: Long,
    val estado: EstadoTemporada
) {
    val esHoy: Boolean get() = diasRestantes == 0L
}

/** Próxima vez que cae el día/mes (si ya pasó este año, el siguiente). El 29 de febrero se ajusta al 28. */
fun proximaFecha(mes: Int, dia: Int, hoy: LocalDate): LocalDate {
    fun enAnio(anio: Int): LocalDate {
        val ym = YearMonth.of(anio, mes.coerceIn(1, 12))
        return ym.atDay(dia.coerceIn(1, ym.lengthOfMonth()))
    }
    val este = enAnio(hoy.year)
    return if (este.isBefore(hoy)) enAnio(hoy.year + 1) else este
}

/** Calcula cuenta regresiva, fechas "pide antes de" y estado. Todo sale de la fecha del teléfono. */
fun calcularTemporada(t: Temporada, hoy: LocalDate, a: AjustesTemporadas = AjustesTemporadas()): InfoTemporada {
    val fecha = proximaFecha(t.mes, t.dia, hoy)
    val restantes = ChronoUnit.DAYS.between(hoy, fecha)
    val limAerea = fecha.minusDays(a.diasAerea.toLong())
    val limMaritima = fecha.minusDays(a.diasMaritima.toLong())
    val paraAerea = ChronoUnit.DAYS.between(hoy, limAerea)
    val paraMaritima = ChronoUnit.DAYS.between(hoy, limMaritima)
    val estado = when {
        restantes <= a.diasActual -> EstadoTemporada.ACTUAL
        paraAerea < 0 && paraMaritima < 0 -> EstadoTemporada.LIMITE_PASADO
        // ya solo queda una vía, o la próxima fecha límite está cerca
        paraAerea < 0 || paraMaritima < 0 -> EstadoTemporada.SE_ACERCA
        minOf(paraAerea, paraMaritima) <= a.diasAviso -> EstadoTemporada.SE_ACERCA
        else -> EstadoTemporada.A_TIEMPO
    }
    return InfoTemporada(t, fecha, restantes, limAerea, limMaritima, paraAerea, paraMaritima, estado)
}

fun calcularTemporadas(lista: List<Temporada>, hoy: LocalDate, a: AjustesTemporadas = AjustesTemporadas()): List<InfoTemporada> =
    lista.map { calcularTemporada(it, hoy, a) }.sortedWith(compareBy({ it.diasRestantes }, { it.temporada.orden }))

private val MESES_ES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)

/** "25 de diciembre" (agrega el año si no es el actual). */
fun fechaLarga(f: LocalDate, hoy: LocalDate = LocalDate.now()): String =
    "${f.dayOfMonth} de ${MESES_ES[f.monthValue - 1]}" + if (f.year != hoy.year) " de ${f.year}" else ""

fun textoCuentaRegresiva(dias: Long): String = when (dias) {
    0L -> "¡Hoy!"
    1L -> "Falta 1 día"
    else -> "Faltan $dias días"
}

/** Aviso local pendiente de enviar. [claves] se guardan para no repetirlo. */
data class AvisoMiDia(val claves: List<String>, val titulo: String, val texto: String, val urgente: Boolean)

private val UMBRALES_LIMITE = listOf(7L, 3L, 1L, 0L)

/**
 * Avisos de temporadas que tocan hoy y que todavía no se han enviado.
 * Funciona aunque la revisión no corra todos los días: usa "quedan N o menos".
 */
fun avisosTemporadas(infos: List<InfoTemporada>, enviados: Set<String>, a: AjustesTemporadas = AjustesTemporadas()): List<AvisoMiDia> {
    val salida = ArrayList<AvisoMiDia>()
    for (i in infos) {
        val t = i.temporada
        val base = "T${t.id}|${i.fecha}"
        val nombre = "${t.emoji} ${t.nombre}".trim()

        if (i.esHoy) {
            val k = "$base|HOY"
            if (k !in enviados) salida.add(AvisoMiDia(listOf(k), "🎉 ¡Hoy es ${t.nombre}!", "Llegó la temporada $nombre.", false))
            continue
        }
        // Temporada próxima: dos semanas antes de la primera fecha límite (marítima)
        val primera = minOf(i.diasParaAerea, i.diasParaMaritima)
        if (primera in (a.diasAviso + 1)..(a.diasAviso + 14L)) {
            val k = "$base|PROX"
            if (k !in enviados) salida.add(
                AvisoMiDia(listOf(k), "🔔 Temporada próxima", "${t.nombre} se acerca (${textoCuentaRegresiva(i.diasRestantes).lowercase()}). Recuerda realizar tus pedidos con anticipación.", false)
            )
        }
        fun limite(codigo: String, via: String, dias: Long, fecha: LocalDate) {
            if (dias < 0) return
            val umbral = UMBRALES_LIMITE.lastOrNull { dias <= it } ?: return
            val k = "$base|$codigo|$umbral"
            if (k in enviados) return
            // al enviar un umbral se dan por enviados los anteriores (más lejanos)
            val claves = UMBRALES_LIMITE.filter { it >= umbral }.map { "$base|$codigo|$it" }
            val texto = when (dias) {
                0L -> "Hoy es el último día recomendado para pedir por vía $via para ${t.nombre}."
                1L -> "Queda 1 día para pedir por vía $via para ${t.nombre} (hasta el ${fechaLarga(fecha, fecha)})."
                else -> "Quedan $dias días para pedir por vía $via para ${t.nombre} (hasta el ${fechaLarga(fecha, fecha)})."
            }
            salida.add(AvisoMiDia(claves, "⚠️ Fecha límite próxima", texto, dias <= 1))
        }
        limite("MAR", "marítima", i.diasParaMaritima, i.limiteMaritima)
        limite("AER", "aérea", i.diasParaAerea, i.limiteAerea)
    }
    return salida
}

// ====================================================================== ACTIVIDADES DE MI DÍA

/** Actividad o hábito que se marca cada día (Gym, rutina personal…). */
@Entity(tableName = "actividades")
data class Actividad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val emoji: String,
    /** Color ARGB */
    val color: Int,
    val activa: Boolean = true,
    val orden: Int = 0
)

enum class EstadoActividad(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    COMPLETADA("Completada"),
    CANCELADA("Cancelada")
}

/** Lo que pasó con una actividad en un día. Si no hay fila, está pendiente. */
@Entity(
    tableName = "cumplimientos",
    indices = [Index(value = ["actividadId", "dia"], unique = true)]
)
data class Cumplimiento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actividadId: Long,
    /** Día como LocalDate.toEpochDay() */
    val dia: Long,
    val estado: EstadoActividad,
    val creado: Long = System.currentTimeMillis()
)

fun estadoDe(actividadId: Long, dia: Long, cumplimientos: List<Cumplimiento>): EstadoActividad =
    cumplimientos.firstOrNull { it.actividadId == actividadId && it.dia == dia }?.estado ?: EstadoActividad.PENDIENTE

/** Días seguidos completados hasta hoy (si hoy aún está pendiente, cuenta hasta ayer). */
fun racha(actividadId: Long, hoy: Long, cumplimientos: List<Cumplimiento>): Int {
    val hechos = cumplimientos.filter { it.actividadId == actividadId && it.estado == EstadoActividad.COMPLETADA }.map { it.dia }.toHashSet()
    var dia = if (hoy in hechos) hoy else hoy - 1
    var n = 0
    while (dia in hechos) { n++; dia-- }
    return n
}

// ====================================================================== PREMIOS

enum class UnidadMeta(val singular: String, val plural: String) {
    DIAS("día", "días"),
    VECES("vez", "veces"),
    SESIONES("sesión", "sesiones"),
    PERSONALIZADA("", "")
}

enum class EstadoPremio(val etiqueta: String) {
    ACTIVO("Activo"),
    INACTIVO("Inactivo"),
    DESBLOQUEADO("Desbloqueado"),
    VENCIDO("Vencido")
}

@Entity(tableName = "premios")
data class Premio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val descripcion: String? = null,
    /** Actividad de Mi Día que lo alimenta; null = se suma a mano */
    val actividadId: Long? = null,
    val unidad: UnidadMeta = UnidadMeta.DIAS,
    /** Texto de la unidad cuando es personalizada (ej. "km") */
    val unidadTexto: String? = null,
    val meta: Int,
    /** Día (epochDay) desde el que cuenta: cada premio nuevo empieza de cero */
    val inicioDia: Long,
    /** Fecha límite opcional (epochDay) */
    val fechaLimite: Long? = null,
    val imagenPath: String? = null,
    val activo: Boolean = true,
    /** Avance sumado a mano (premios sin actividad, o extras) */
    val progresoManual: Int = 0,
    /** Día en que se desbloqueó; null = todavía no */
    val desbloqueadoDia: Long? = null,
    /** Progreso con el que se desbloqueó (queda guardado en el historial) */
    val progresoFinal: Int? = null,
    val creado: Long = System.currentTimeMillis()
)

fun Premio.textoUnidad(cantidad: Int): String = when (unidad) {
    UnidadMeta.PERSONALIZADA -> unidadTexto?.trim().orEmpty()
    else -> if (cantidad == 1) unidad.singular else unidad.plural
}

data class AvancePremio(
    val premio: Premio,
    val progreso: Int,
    val estado: EstadoPremio
) {
    val meta: Int get() = premio.meta
    val restante: Int get() = (meta - progreso).coerceAtLeast(0)
    val porcentaje: Int get() = if (meta <= 0) 100 else (progreso * 100 / meta).coerceIn(0, 100)
    val fraccion: Float get() = if (meta <= 0) 1f else (progreso.toFloat() / meta).coerceIn(0f, 1f)
    val cumplido: Boolean get() = progreso >= meta
}

/**
 * Progreso del premio: cumplimientos COMPLETADOS de su actividad desde que se creó
 * (hasta la fecha límite si tiene) + lo sumado a mano. Pendientes y canceladas no cuentan.
 */
fun calcularAvance(p: Premio, cumplimientos: List<Cumplimiento>, hoy: Long): AvancePremio {
    if (p.desbloqueadoDia != null) {
        return AvancePremio(p, p.progresoFinal ?: p.meta, EstadoPremio.DESBLOQUEADO)
    }
    val automatico = if (p.actividadId == null) 0 else cumplimientos.count {
        it.actividadId == p.actividadId && it.estado == EstadoActividad.COMPLETADA &&
            it.dia >= p.inicioDia && (p.fechaLimite == null || it.dia <= p.fechaLimite)
    }
    val progreso = automatico + p.progresoManual
    val estado = when {
        !p.activo -> EstadoPremio.INACTIVO
        p.fechaLimite != null && hoy > p.fechaLimite && progreso < p.meta -> EstadoPremio.VENCIDO
        else -> EstadoPremio.ACTIVO
    }
    return AvancePremio(p, progreso, estado)
}

/** Premios activos que acaban de llegar a su meta y hay que desbloquear. */
fun porDesbloquear(premios: List<Premio>, cumplimientos: List<Cumplimiento>, hoy: Long): List<AvancePremio> =
    premios.map { calcularAvance(it, cumplimientos, hoy) }
        .filter { it.estado == EstadoPremio.ACTIVO && it.cumplido }

fun textoDesbloqueo(a: AvancePremio, actividad: Actividad?): String {
    val p = a.premio
    val de = actividad?.let { " de ${it.nombre}" } ?: ""
    return "Has cumplido ${p.meta} ${p.textoUnidad(p.meta)}$de. ¡Te has ganado: ${p.nombre}!"
}

/** Aviso "¡Vas muy bien!" cuando faltan 1 o 2 para la meta (y la meta es de 5 o más). */
fun textoCasi(a: AvancePremio, actividad: Actividad?): String? {
    if (a.estado != EstadoPremio.ACTIVO || a.meta < 5 || a.restante !in 1..2) return null
    val p = a.premio
    val de = actividad?.let { " de ${it.nombre}" } ?: ""
    val faltan = if (a.restante == 1) "Solo falta 1 ${p.textoUnidad(1)}" else "Solo faltan ${a.restante} ${p.textoUnidad(a.restante)}"
    return "Llevas ${a.progreso} de ${p.meta} ${p.textoUnidad(p.meta)}$de. ¡$faltan para desbloquear: ${p.nombre}!"
}

// ====================================================================== DATOS INICIALES

/** Tablas nuevas de Mi Día (mismas columnas que crea Room para las entidades de arriba). */
val SQL_TABLAS_MI_DIA = listOf(
    "CREATE TABLE IF NOT EXISTS `temporadas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `emoji` TEXT NOT NULL, `mes` INTEGER NOT NULL, `dia` INTEGER NOT NULL, `orden` INTEGER NOT NULL)",
    "CREATE TABLE IF NOT EXISTS `actividades` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `emoji` TEXT NOT NULL, `color` INTEGER NOT NULL, `activa` INTEGER NOT NULL, `orden` INTEGER NOT NULL)",
    "CREATE TABLE IF NOT EXISTS `cumplimientos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `actividadId` INTEGER NOT NULL, `dia` INTEGER NOT NULL, `estado` TEXT NOT NULL, `creado` INTEGER NOT NULL)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_cumplimientos_actividadId_dia` ON `cumplimientos` (`actividadId`, `dia`)",
    "CREATE TABLE IF NOT EXISTS `premios` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `descripcion` TEXT, `actividadId` INTEGER, `unidad` TEXT NOT NULL, `unidadTexto` TEXT, `meta` INTEGER NOT NULL, `inicioDia` INTEGER NOT NULL, `fechaLimite` INTEGER, `imagenPath` TEXT, `activo` INTEGER NOT NULL, `progresoManual` INTEGER NOT NULL, `desbloqueadoDia` INTEGER, `progresoFinal` INTEGER, `creado` INTEGER NOT NULL)"
)

/** Temporadas de Nicaragua y actividades con las que empieza la app (todo se puede editar después). */
val SQL_SEMILLAS_MI_DIA = listOf(
    "INSERT INTO temporadas (nombre, emoji, mes, dia, orden) VALUES ('Regreso a clases', '🎒', 2, 1, 0)",
    "INSERT INTO temporadas (nombre, emoji, mes, dia, orden) VALUES ('Día de las Madres', '🇳🇮', 5, 30, 1)",
    "INSERT INTO temporadas (nombre, emoji, mes, dia, orden) VALUES ('Fiestas Patrias', '🇳🇮', 9, 14, 2)",
    "INSERT INTO temporadas (nombre, emoji, mes, dia, orden) VALUES ('La Purísima / Gritería', '💙', 12, 7, 3)",
    "INSERT INTO temporadas (nombre, emoji, mes, dia, orden) VALUES ('Navidad', '🎄', 12, 25, 4)",
    "INSERT INTO actividades (nombre, emoji, color, activa, orden) VALUES ('Gym', '🏋️', -13722044, 1, 0)",
    "INSERT INTO actividades (nombre, emoji, color, activa, orden) VALUES ('Rutina personal', '🟣', -8695850, 1, 1)"
)
