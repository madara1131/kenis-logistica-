@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kenisshop.logistica.ui.midia

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kenisshop.logistica.data.midia.Actividad
import com.kenisshop.logistica.data.midia.AvancePremio
import com.kenisshop.logistica.data.midia.EstadoPremio
import com.kenisshop.logistica.data.midia.Premio
import com.kenisshop.logistica.data.midia.UnidadMeta
import com.kenisshop.logistica.data.midia.fechaLarga
import com.kenisshop.logistica.data.midia.textoDesbloqueo
import com.kenisshop.logistica.data.midia.textoUnidad
import com.kenisshop.logistica.ui.ChipFiltro
import com.kenisshop.logistica.ui.theme.EstadoRojo
import com.kenisshop.logistica.ui.theme.EstadoVerde
import com.kenisshop.logistica.ui.theme.VerdeGuardar
import com.kenisshop.logistica.ui.traker.BarraProgreso
import com.kenisshop.logistica.util.Fotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

private val DORADO = Color(0xFFE0A100)

@Composable
fun VistaPremios(avances: List<AvancePremio>, actividades: List<Actividad>, hoy: LocalDate, vm: MiDiaViewModel) {
    val esTablet = LocalConfiguration.current.screenWidthDp >= 600
    var nuevo by remember { mutableStateOf(false) }
    var editar by remember { mutableStateOf<Premio?>(null) }

    val activos = avances.filter { it.estado == EstadoPremio.ACTIVO }
    val ganados = avances.filter { it.estado == EstadoPremio.DESBLOQUEADO }.sortedByDescending { it.premio.desbloqueadoDia }
    val otros = avances.filter { it.estado == EstadoPremio.INACTIVO || it.estado == EstadoPremio.VENCIDO }
    val columnas = if (esTablet) 2 else 1

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = { nuevo = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Nuevo premio", fontWeight = FontWeight.Bold)
            }
        }
        if (avances.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", style = MaterialTheme.typography.displayMedium)
                    Text("Ponte un premio por cumplir tus metas", fontWeight = FontWeight.Bold)
                    Text(
                        "Por ejemplo: 20 días de Gym = tenis nuevos.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                    )
                }
            }
        }
        fun seccion(titulo: String, lista: List<AvancePremio>) {
            if (lista.isEmpty()) return
            item {
                Text(
                    "$titulo (${lista.size})",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // teléfono: una tarjeta por fila · tablet: dos columnas
            lista.chunked(columnas).forEach { fila ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        fila.forEach { av ->
                            Box(Modifier.weight(1f)) {
                                TarjetaPremio(
                                    av = av,
                                    actividad = actividades.firstOrNull { it.id == av.premio.actividadId },
                                    hoy = hoy,
                                    onClick = { editar = av.premio },
                                    onSumar = { vm.sumarAvance(av.premio, it) }
                                )
                            }
                        }
                        if (fila.size < columnas) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        seccion("🏆 Premios activos", activos)
        seccion("✅ Historial de premios desbloqueados", ganados)
        seccion("Inactivos y vencidos", otros)
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (nuevo) {
        EditorPremio(null, actividades, hoy, onGuardar = { vm.guardarPremio(it) }, onBorrar = {}, onCerrar = { nuevo = false })
    }
    editar?.let { p ->
        EditorPremio(p, actividades, hoy, onGuardar = { vm.guardarPremio(it) }, onBorrar = { vm.borrarPremio(it) }, onCerrar = { editar = null })
    }
}

@Composable
private fun TarjetaPremio(av: AvancePremio, actividad: Actividad?, hoy: LocalDate, onClick: () -> Unit, onSumar: (Int) -> Unit) {
    val p = av.premio
    val ganado = av.estado == EstadoPremio.DESBLOQUEADO
    val color = if (ganado) DORADO else actividad?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (ganado) DORADO.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface),
        border = if (ganado) BorderStroke(1.5.dp, DORADO) else null,
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (p.imagenPath != null) {
                    AsyncImage(
                        model = File(p.imagenPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Text("🏆", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        actividad?.let { "${it.emoji} ${it.nombre}" } ?: "✋ Avance a mano",
                        style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold
                    )
                }
                if (av.estado == EstadoPremio.INACTIVO || av.estado == EstadoPremio.VENCIDO) {
                    Surface(shape = RoundedCornerShape(8.dp), color = if (av.estado == EstadoPremio.VENCIDO) EstadoRojo else MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            av.estado.etiqueta,
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (av.estado == EstadoPremio.VENCIDO) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            p.descripcion?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "Progreso: ${av.progreso} / ${av.meta} ${p.textoUnidad(av.meta)}",
                fontWeight = FontWeight.SemiBold
            )
            BarraProgreso(av.fraccion, if (ganado) DORADO else color)
            if (ganado) {
                Text("✅ DESBLOQUEADO", fontWeight = FontWeight.Black, color = EstadoVerde)
                p.desbloqueadoDia?.let {
                    Text("📅 Conseguido el ${fechaLarga(LocalDate.ofEpochDay(it), hoy)}.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(
                    "${av.porcentaje} % completado · " +
                        if (av.restante == 1) "Falta 1 ${p.textoUnidad(1)}" else "Faltan ${av.restante} ${p.textoUnidad(av.restante)}",
                    style = MaterialTheme.typography.bodySmall
                )
                p.fechaLimite?.let {
                    Text(
                        "⏰ Fecha límite: ${fechaLarga(LocalDate.ofEpochDay(it), hoy)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (av.estado == EstadoPremio.VENCIDO) EstadoRojo else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Premios sin actividad (o extras): se suma a mano
                if (av.estado == EstadoPremio.ACTIVO && p.actividadId == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onSumar(-1) }, enabled = p.progresoManual > 0) { Icon(Icons.Default.Remove, "Restar 1") }
                        FilledTonalButton(onClick = { onSumar(1) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Sumar 1")
                        }
                    }
                }
            }
        }
    }
}

/** Celebración al desbloquear un premio. */
@Composable
fun DialogoCelebracion(av: AvancePremio, actividad: Actividad?, onCerrar: () -> Unit) {
    val escala = remember { Animatable(0.2f) }
    LaunchedEffect(av.premio.id) { escala.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 180f)) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "🎉🏆🎉",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.graphicsLayer { scaleX = escala.value; scaleY = escala.value }
                )
                Spacer(Modifier.height(8.dp))
                Text("¡Premio desbloqueado!", fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(av.premio.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DORADO, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(textoDesbloqueo(av, actividad), textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(onClick = onCerrar, colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White)) { Text("¡Genial!") }
        }
    )
}

private fun elegirDia(context: Context, inicial: Long?, onElegido: (Long) -> Unit) {
    val base = inicial?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now().plusDays(30)
    DatePickerDialog(
        context,
        { _, anio, mes, dia -> onElegido(LocalDate.of(anio, mes + 1, dia).toEpochDay()) },
        base.year, base.monthValue - 1, base.dayOfMonth
    ).apply { datePicker.minDate = System.currentTimeMillis() - 1000 }.show()
}

@Composable
private fun EditorPremio(
    existente: Premio?,
    actividades: List<Actividad>,
    hoy: LocalDate,
    onGuardar: (Premio) -> Unit,
    onBorrar: (Premio) -> Unit,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ganado = existente?.desbloqueadoDia != null

    var nombre by remember { mutableStateOf(existente?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(existente?.descripcion ?: "") }
    var actividadId by remember { mutableStateOf(if (existente != null) existente.actividadId else actividades.firstOrNull { it.activa }?.id) }
    var unidad by remember { mutableStateOf(existente?.unidad ?: UnidadMeta.DIAS) }
    var unidadTexto by remember { mutableStateOf(existente?.unidadTexto ?: "") }
    var meta by remember { mutableStateOf(existente?.meta?.toString() ?: "20") }
    var fechaLimite by remember { mutableStateOf(existente?.fechaLimite) }
    var imagen by remember { mutableStateOf(existente?.imagenPath) }
    var activo by remember { mutableStateOf(existente?.activo ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmarBorrar by remember { mutableStateOf(false) }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val ruta = withContext(Dispatchers.IO) { Fotos.copiarDesde(context, uri) }
                if (ruta != null) imagen = ruta
                else Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = hoja) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (existente == null) "🏆 Nuevo premio" else "🏆 Editar premio",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
            )
            if (ganado) {
                Text("Este premio ya está desbloqueado y queda guardado en tu historial.", style = MaterialTheme.typography.bodySmall, color = EstadoVerde)
            }
            OutlinedTextField(
                value = nombre, onValueChange = { nombre = it; error = null },
                label = { Text("Nombre del premio (ej: Tenis nuevos)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = descripcion, onValueChange = { descripcion = it },
                label = { Text("Descripción (opcional)") }, maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            if (!ganado) {
                Text("Actividad relacionada", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    actividades.filter { it.activa || it.id == actividadId }.forEach { a ->
                        ChipFiltro("${a.emoji} ${a.nombre}", actividadId == a.id, Color(a.color)) { actividadId = a.id }
                    }
                    ChipFiltro("✋ Ninguna (sumo a mano)", actividadId == null, MaterialTheme.colorScheme.secondary) { actividadId = null }
                }
                Text(
                    if (actividadId != null) "Cada vez que marques esa actividad como completada en Mi Día, el premio avanza solo."
                    else "Tú sumas el avance con el botón \"Sumar 1\" del premio.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Meta para desbloquearlo", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = meta, onValueChange = { meta = it.filter { c -> c.isDigit() }.take(5); error = null },
                        label = { Text("Cantidad") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    if (unidad == UnidadMeta.PERSONALIZADA) {
                        OutlinedTextField(
                            value = unidadTexto, onValueChange = { unidadTexto = it; error = null },
                            label = { Text("Unidad (ej: km)") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChipFiltro("Días", unidad == UnidadMeta.DIAS, MaterialTheme.colorScheme.primary) { unidad = UnidadMeta.DIAS }
                    ChipFiltro("Veces", unidad == UnidadMeta.VECES, MaterialTheme.colorScheme.primary) { unidad = UnidadMeta.VECES }
                    ChipFiltro("Sesiones", unidad == UnidadMeta.SESIONES, MaterialTheme.colorScheme.primary) { unidad = UnidadMeta.SESIONES }
                    ChipFiltro("Personalizada", unidad == UnidadMeta.PERSONALIZADA, MaterialTheme.colorScheme.primary) { unidad = UnidadMeta.PERSONALIZADA }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { elegirDia(context, fechaLimite) { fechaLimite = it } }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Event, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                        Text(fechaLimite?.let { "Fecha límite: ${fechaLarga(LocalDate.ofEpochDay(it), hoy)}" } ?: "Fecha límite (opcional)")
                    }
                    if (fechaLimite != null) IconButton(onClick = { fechaLimite = null }) { Icon(Icons.Default.Close, "Quitar fecha límite") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val img = imagen
                if (img != null) {
                    AsyncImage(model = File(img), contentDescription = "Imagen del premio", contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
                    Spacer(Modifier.width(10.dp))
                }
                OutlinedButton(
                    onClick = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text(if (img == null) "Imagen del premio (opcional)" else "Cambiar imagen")
                }
                if (img != null) IconButton(onClick = { imagen = null }) { Icon(Icons.Default.Close, "Quitar imagen") }
            }
            if (!ganado) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Activo", fontWeight = FontWeight.SemiBold)
                        Text("Si lo apagas, deja de contar hasta que lo actives otra vez.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = activo, onCheckedChange = { activo = it })
                }
            }
            error?.let { Text(it, color = EstadoRojo, fontWeight = FontWeight.SemiBold) }
            Button(
                onClick = {
                    val m = meta.toIntOrNull()
                    when {
                        nombre.isBlank() -> error = "Escribe el nombre del premio"
                        m == null || m < 1 -> error = "La cantidad debe ser 1 o más"
                        unidad == UnidadMeta.PERSONALIZADA && unidadTexto.isBlank() -> error = "Escribe la unidad personalizada"
                        else -> {
                            val base = existente ?: Premio(nombre = nombre, meta = m, inicioDia = hoy.toEpochDay())
                            onGuardar(
                                base.copy(
                                    nombre = nombre.trim(),
                                    descripcion = descripcion.trim().ifEmpty { null },
                                    actividadId = actividadId,
                                    unidad = unidad,
                                    unidadTexto = if (unidad == UnidadMeta.PERSONALIZADA) unidadTexto.trim() else null,
                                    meta = m,
                                    fechaLimite = fechaLimite,
                                    imagenPath = imagen,
                                    activo = activo
                                )
                            )
                            onCerrar()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp))
                Text(if (existente == null) "GUARDAR PREMIO" else "ACTUALIZAR", fontWeight = FontWeight.Bold)
            }
            if (existente != null && ganado) {
                // Mismo premio otra vez = objetivo nuevo desde cero; el anterior se queda en el historial
                OutlinedButton(
                    onClick = {
                        onGuardar(
                            existente.copy(
                                id = 0, inicioDia = hoy.toEpochDay(), progresoManual = 0, desbloqueadoDia = null,
                                progresoFinal = null, fechaLimite = null, activo = true, creado = System.currentTimeMillis()
                            )
                        )
                        onCerrar()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Replay, null); Spacer(Modifier.width(8.dp)); Text("Ponerme este premio otra vez")
                }
            }
            if (existente != null) {
                OutlinedButton(
                    onClick = { confirmarBorrar = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EstadoRojo)
                ) {
                    Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("ELIMINAR")
                }
            }
        }
    }
    if (confirmarBorrar && existente != null) {
        AlertDialog(
            onDismissRequest = { confirmarBorrar = false },
            title = { Text("¿Eliminar este premio?") },
            text = { Text(if (ganado) "Se borra de tu historial. No se puede deshacer." else "Se pierde su avance. No se puede deshacer.") },
            confirmButton = { TextButton(onClick = { confirmarBorrar = false; onBorrar(existente); onCerrar() }) { Text("Eliminar", color = EstadoRojo) } },
            dismissButton = { TextButton(onClick = { confirmarBorrar = false }) { Text("Cancelar") } }
        )
    }
}
