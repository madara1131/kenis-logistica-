@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kenisshop.logistica.ui.midia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kenisshop.logistica.data.midia.Actividad
import com.kenisshop.logistica.data.midia.AvancePremio
import com.kenisshop.logistica.data.midia.Cumplimiento
import com.kenisshop.logistica.data.midia.EstadoActividad
import com.kenisshop.logistica.data.midia.EstadoPremio
import com.kenisshop.logistica.data.midia.InfoTemporada
import com.kenisshop.logistica.data.midia.calcularAvance
import com.kenisshop.logistica.data.midia.calcularTemporadas
import com.kenisshop.logistica.data.midia.estadoDe
import com.kenisshop.logistica.data.midia.fechaLarga
import com.kenisshop.logistica.data.midia.racha
import com.kenisshop.logistica.data.midia.textoCuentaRegresiva
import com.kenisshop.logistica.data.midia.textoUnidad
import com.kenisshop.logistica.ui.theme.EstadoRojo
import com.kenisshop.logistica.ui.theme.EstadoVerde
import com.kenisshop.logistica.ui.theme.VerdeGuardar
import com.kenisshop.logistica.ui.traker.BarraProgreso
import java.time.LocalDate

/** Colores para elegir en una actividad (verde gym, morado personal, azul negocio…). */
val COLORES_ACTIVIDAD = listOf(
    Color(0xFF2E9E44), Color(0xFF7B4FD6), Color(0xFF1E4E9A), Color(0xFFD9468F), Color(0xFFEF8A17), Color(0xFF16A6B6)
)

private val DIAS_SEMANA = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")

/** Pestaña "Mi Día": Hoy · Temporadas · Premios. */
@Composable
fun MiDiaScreen(vm: MiDiaViewModel = viewModel()) {
    val temporadas by vm.temporadas.collectAsStateWithLifecycle()
    val actividades by vm.actividades.collectAsStateWithLifecycle()
    val cumplimientos by vm.cumplimientos.collectAsStateWithLifecycle()
    val premios by vm.premios.collectAsStateWithLifecycle()
    var vista by rememberSaveable { mutableStateOf(0) }

    // La fecha sale del teléfono
    val hoy = LocalDate.now()
    val infos = remember(temporadas, vm.ajustes, hoy) { calcularTemporadas(temporadas, hoy, vm.ajustes) }
    val avances = remember(premios, cumplimientos, hoy) { premios.map { calcularAvance(it, cumplimientos, hoy.toEpochDay()) } }

    Column(Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            val opciones = listOf("Hoy", "Temporadas", "Premios")
            opciones.forEachIndexed { i, texto ->
                SegmentedButton(
                    selected = vista == i,
                    onClick = { vista = i },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = opciones.size),
                    icon = {}
                ) { Text(texto, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
            }
        }
        when (vista) {
            0 -> VistaHoy(hoy, infos, actividades, cumplimientos, avances, vm, onVerTemporadas = { vista = 1 }, onVerPremios = { vista = 2 })
            1 -> VistaTemporadas(infos, hoy, vm)
            else -> VistaPremios(avances, actividades, hoy, vm)
        }
    }

    vm.celebracion?.let { ganado ->
        DialogoCelebracion(ganado, actividades.firstOrNull { it.id == ganado.premio.actividadId }, onCerrar = { vm.cerrarCelebracion() })
    }
}

// ====================================================================== HOY

@Composable
private fun VistaHoy(
    hoy: LocalDate,
    infos: List<InfoTemporada>,
    actividades: List<Actividad>,
    cumplimientos: List<Cumplimiento>,
    avances: List<AvancePremio>,
    vm: MiDiaViewModel,
    onVerTemporadas: () -> Unit,
    onVerPremios: () -> Unit
) {
    var editar by remember { mutableStateOf<Actividad?>(null) }
    var nueva by remember { mutableStateOf(false) }
    val dia = hoy.toEpochDay()
    val activas = actividades.filter { it.activa }
    val premiosActivos = avances.filter { it.estado == EstadoPremio.ACTIVO }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("Hoy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    "${DIAS_SEMANA[hoy.dayOfWeek.value - 1]} ${fechaLarga(hoy, hoy)} de ${hoy.year}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        infos.firstOrNull()?.let { prox ->
            item {
                Surface(
                    onClick = onVerTemporadas,
                    shape = RoundedCornerShape(16.dp),
                    color = colorDe(prox.estado).copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(prox.temporada.emoji, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Próxima temporada", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(prox.temporada.nombre, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (prox.esHoy) "🎉 ¡Es hoy!" else "${textoCuentaRegresiva(prox.diasRestantes)} · ${fechaLarga(prox.fecha, hoy)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        ChipEstadoTemporada(prox.estado)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Actividades de hoy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                TextButton(onClick = { nueva = true }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Actividad")
                }
            }
        }
        if (activas.isEmpty()) {
            item { Text("Todavía no hay actividades. Agrega una (por ejemplo, Gym).", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(activas, key = { it.id }) { a ->
            TarjetaActividad(
                a = a,
                estado = estadoDe(a.id, dia, cumplimientos),
                racha = racha(a.id, dia, cumplimientos),
                onMarcar = { vm.marcar(a, dia, it) },
                onEditar = { editar = a }
            )
        }
        val ocultas = actividades.filter { !it.activa }
        if (ocultas.isNotEmpty()) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ocultas.forEach { a ->
                        OutlinedButton(onClick = { editar = a }) { Text("${a.emoji} ${a.nombre} (inactiva)") }
                    }
                }
            }
        }
        if (premiosActivos.isNotEmpty()) {
            item {
                Card(
                    onClick = onVerPremios,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🏆 Premios activos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        premiosActivos.forEach { av ->
                            Column {
                                Row {
                                    Text(av.premio.nombre, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${av.progreso} / ${av.meta} ${av.premio.textoUnidad(av.meta)}", fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(4.dp))
                                BarraProgreso(av.fraccion, colorActividad(av.premio.actividadId, actividades))
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (nueva) DialogoActividad(null, onGuardar = { vm.guardarActividad(it) }, onCerrar = { nueva = false })
    editar?.let { a -> DialogoActividad(a, onGuardar = { vm.guardarActividad(it) }, onCerrar = { editar = null }) }
}

fun colorActividad(actividadId: Long?, actividades: List<Actividad>): Color =
    actividades.firstOrNull { it.id == actividadId }?.let { Color(it.color) } ?: Color(0xFF1E4E9A)

@Composable
private fun TarjetaActividad(
    a: Actividad,
    estado: EstadoActividad,
    racha: Int,
    onMarcar: (EstadoActividad) -> Unit,
    onEditar: () -> Unit
) {
    val color = Color(a.color)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (estado == EstadoActividad.COMPLETADA) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Text(a.emoji, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        when (estado) {
                            EstadoActividad.COMPLETADA -> "✅ Completada"
                            EstadoActividad.CANCELADA -> "✖️ Cancelada"
                            EstadoActividad.PENDIENTE -> "Pendiente"
                        } + if (racha > 0) " · 🔥 Racha de $racha ${if (racha == 1) "día" else "días"}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (estado) {
                            EstadoActividad.COMPLETADA -> EstadoVerde
                            EstadoActividad.CANCELADA -> EstadoRojo
                            EstadoActividad.PENDIENTE -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, "Editar actividad", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val hecha = estado == EstadoActividad.COMPLETADA
                Button(
                    onClick = { onMarcar(if (hecha) EstadoActividad.PENDIENTE else EstadoActividad.COMPLETADA) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hecha) VerdeGuardar else color,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (hecha) "Completada (deshacer)" else "Marcar completada", maxLines = 1)
                }
                val cancelada = estado == EstadoActividad.CANCELADA
                OutlinedButton(
                    onClick = { onMarcar(if (cancelada) EstadoActividad.PENDIENTE else EstadoActividad.CANCELADA) },
                    modifier = Modifier.height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (cancelada) EstadoRojo else MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.Cancel, if (cancelada) "Quitar cancelación" else "Cancelar hoy", Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun DialogoActividad(existente: Actividad?, onGuardar: (Actividad) -> Unit, onCerrar: () -> Unit) {
    var nombre by remember { mutableStateOf(existente?.nombre ?: "") }
    var emoji by remember { mutableStateOf(existente?.emoji ?: "✅") }
    var color by remember { mutableStateOf(existente?.color ?: COLORES_ACTIVIDAD[0].toArgb()) }
    var activa by remember { mutableStateOf(existente?.activa ?: true) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (existente == null) "Nueva actividad" else "Editar actividad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it; error = false },
                    label = { Text("Nombre (ej: Gym)") }, isError = error, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                OutlinedTextField(value = emoji, onValueChange = { emoji = it.take(4) }, label = { Text("Emoji") }, singleLine = true)
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    COLORES_ACTIVIDAD.forEach { c ->
                        val sel = color == c.toArgb()
                        Box(
                            Modifier.size(if (sel) 36.dp else 30.dp).clip(CircleShape).background(c).clickable { color = c.toArgb() },
                            contentAlignment = Alignment.Center
                        ) { if (sel) Text("✓", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
                if (existente != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Activa (aparece en Hoy)", Modifier.weight(1f))
                        Switch(checked = activa, onCheckedChange = { activa = it })
                    }
                    Text(
                        "Desactivarla no borra su historial ni el avance de los premios.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isBlank()) error = true else {
                    val base = existente ?: Actividad(nombre = nombre, emoji = emoji, color = color)
                    onGuardar(base.copy(nombre = nombre.trim(), emoji = emoji.trim().ifEmpty { "✅" }, color = color, activa = activa))
                    onCerrar()
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
