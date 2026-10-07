@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kenisshop.logistica.ui.midia

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kenisshop.logistica.data.midia.AjustesTemporadas
import com.kenisshop.logistica.data.midia.EstadoTemporada
import com.kenisshop.logistica.data.midia.InfoTemporada
import com.kenisshop.logistica.data.midia.Temporada
import com.kenisshop.logistica.data.midia.fechaLarga
import com.kenisshop.logistica.data.midia.textoCuentaRegresiva
import com.kenisshop.logistica.ui.ChipFiltro
import com.kenisshop.logistica.ui.theme.EstadoAmarillo
import com.kenisshop.logistica.ui.theme.EstadoAzul
import com.kenisshop.logistica.ui.theme.EstadoRojo
import com.kenisshop.logistica.ui.theme.EstadoVerde
import com.kenisshop.logistica.ui.theme.VerdeGuardar
import com.kenisshop.logistica.util.Meses
import java.time.LocalDate
import java.time.YearMonth

/** Mismos colores de estado que el resto de la app. */
fun colorDe(e: EstadoTemporada): Color = when (e) {
    EstadoTemporada.A_TIEMPO -> EstadoVerde
    EstadoTemporada.SE_ACERCA -> EstadoAmarillo
    EstadoTemporada.LIMITE_PASADO -> EstadoRojo
    EstadoTemporada.ACTUAL -> EstadoAzul
}

@Composable
fun ChipEstadoTemporada(e: EstadoTemporada) {
    Surface(color = colorDe(e), shape = RoundedCornerShape(8.dp)) {
        Text(
            e.etiqueta,
            color = if (e == EstadoTemporada.SE_ACERCA) Color(0xFF3A2E00) else Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun VistaTemporadas(infos: List<InfoTemporada>, hoy: LocalDate, vm: MiDiaViewModel) {
    val esTablet = LocalConfiguration.current.screenWidthDp >= 600
    var seleccion by rememberSaveable { mutableStateOf<Long?>(null) }
    var editar by remember { mutableStateOf<Temporada?>(null) }
    var nueva by remember { mutableStateOf(false) }
    var tiempos by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf<Temporada?>(null) }

    val elegida = infos.firstOrNull { it.temporada.id == seleccion }

    val lista: @Composable (Modifier) -> Unit = { modifier ->
        LazyColumn(modifier, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Calendario de temporadas",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { tiempos = true }) { Icon(Icons.Default.Tune, "Tiempos de tránsito") }
                    IconButton(onClick = { nueva = true }) { Icon(Icons.Default.Add, "Nueva temporada") }
                }
            }
            item {
                Text(
                    "✈️ Aérea: ${vm.ajustes.diasAerea} días antes · 🚢 Marítima: ${vm.ajustes.diasMaritima} días antes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (infos.isEmpty()) {
                item { Text("No hay temporadas. Toca + para agregar una.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(infos, key = { it.temporada.id }) { info ->
                TarjetaTemporada(info, hoy, seleccionada = esTablet && info.temporada.id == (elegida ?: infos.first()).temporada.id) {
                    seleccion = info.temporada.id
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (esTablet) {
        // Tablet: lista a la izquierda y detalle a la derecha
        Row(Modifier.fillMaxSize()) {
            lista(Modifier.weight(0.45f).fillMaxHeight())
            VerticalDivider()
            Box(Modifier.weight(0.55f).fillMaxHeight()) {
                val mostrar = elegida ?: infos.firstOrNull()
                if (mostrar != null) {
                    DetalleTemporada(mostrar, hoy, vm.ajustes, mostrarVolver = false, onVolver = {}, onEditar = { editar = mostrar.temporada }, onBorrar = { borrar = mostrar.temporada })
                }
            }
        }
    } else if (elegida != null) {
        BackHandler { seleccion = null }
        DetalleTemporada(elegida, hoy, vm.ajustes, mostrarVolver = true, onVolver = { seleccion = null }, onEditar = { editar = elegida.temporada }, onBorrar = { borrar = elegida.temporada })
    } else {
        lista(Modifier.fillMaxSize())
    }

    if (nueva) DialogoTemporada(null, onGuardar = { vm.guardarTemporada(it) }, onCerrar = { nueva = false })
    editar?.let { t -> DialogoTemporada(t, onGuardar = { vm.guardarTemporada(it) }, onCerrar = { editar = null }) }
    if (tiempos) DialogoTiempos(vm.ajustes, onGuardar = { vm.guardarAjustes(it) }, onCerrar = { tiempos = false })
    borrar?.let { t ->
        AlertDialog(
            onDismissRequest = { borrar = null },
            title = { Text("¿Eliminar ${t.nombre}?") },
            text = { Text("Se quita del calendario. Tus pedidos no cambian.") },
            confirmButton = {
                TextButton(onClick = { vm.borrarTemporada(t); if (seleccion == t.id) seleccion = null; borrar = null }) { Text("Eliminar", color = EstadoRojo) }
            },
            dismissButton = { TextButton(onClick = { borrar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaTemporada(info: InfoTemporada, hoy: LocalDate, seleccionada: Boolean, onClick: () -> Unit) {
    val color = colorDe(info.estado)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(64.dp).clip(RoundedCornerShape(3.dp)).background(color))
            Spacer(Modifier.width(10.dp))
            Text(info.temporada.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(info.temporada.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("🗓️ ${fechaLarga(info.fecha, hoy)}", style = MaterialTheme.typography.bodySmall)
                Text(
                    if (info.esHoy) "🎉 ¡Es hoy!" else "⏳ ${textoCuentaRegresiva(info.diasRestantes)}",
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium
                )
            }
            ChipEstadoTemporada(info.estado)
        }
    }
}

@Composable
private fun DetalleTemporada(
    info: InfoTemporada,
    hoy: LocalDate,
    ajustes: AjustesTemporadas,
    mostrarVolver: Boolean,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onBorrar: () -> Unit
) {
    val t = info.temporada
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (mostrarVolver) {
                IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
            }
            Text("${t.emoji} ${t.nombre}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            ChipEstadoTemporada(info.estado)
        }
        // Cuenta regresiva
        Surface(shape = RoundedCornerShape(18.dp), color = colorDe(info.estado).copy(alpha = 0.14f), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📅 Fecha: ${fechaLarga(info.fecha, hoy)}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                if (info.esHoy) {
                    Text("🎉 ¡Hoy es ${t.nombre}!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                } else {
                    Text("${info.diasRestantes}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text(if (info.diasRestantes == 1L) "⏳ día restante" else "⏳ días restantes", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (!info.esHoy) {
            TarjetaLimite("Aérea", Icons.Default.Flight, info.limiteAerea, info.diasParaAerea, ajustes.diasAerea, hoy)
            TarjetaLimite("Marítima", Icons.Default.DirectionsBoat, info.limiteMaritima, info.diasParaMaritima, ajustes.diasMaritima, hoy)
        }
        Text(
            "Las fechas se calculan solas: fecha de la temporada − ${ajustes.diasAerea} días (aérea) y − ${ajustes.diasMaritima} días (marítima). " +
                "Cuando pase la fecha, se prepara sola la del próximo año.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onEditar, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Editar")
            }
            OutlinedButton(
                onClick = onBorrar, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EstadoRojo)
            ) {
                Icon(Icons.Default.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Eliminar")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TarjetaLimite(via: String, icono: ImageVector, limite: LocalDate, dias: Long, transito: Int, hoy: LocalDate) {
    val color = when {
        dias < 0 -> EstadoRojo
        dias <= 7 -> EstadoAmarillo
        else -> EstadoVerde
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("$via · $transito días de tránsito", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Pide antes del: ${fechaLarga(limite, hoy)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        dias < 0 -> "🔴 La fecha límite pasó hace ${-dias} día${if (dias == -1L) "" else "s"}"
                        dias == 0L -> "🟡 Hoy es el último día recomendado"
                        dias == 1L -> "🟡 Queda 1 día para pedir"
                        dias <= 7 -> "🟡 Quedan $dias días para pedir"
                        else -> "🟢 Quedan $dias días para pedir"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DialogoTemporada(existente: Temporada?, onGuardar: (Temporada) -> Unit, onCerrar: () -> Unit) {
    var nombre by remember { mutableStateOf(existente?.nombre ?: "") }
    var emoji by remember { mutableStateOf(existente?.emoji ?: "🛍️") }
    var mes by remember { mutableStateOf(existente?.mes ?: LocalDate.now().monthValue) }
    var dia by remember { mutableStateOf((existente?.dia ?: 1).toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (existente == null) "Nueva temporada" else "Editar temporada") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it; error = null },
                    label = { Text("Nombre") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = emoji, onValueChange = { emoji = it.take(4) }, label = { Text("Emoji") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = dia, onValueChange = { dia = it.filter { c -> c.isDigit() }.take(2); error = null },
                        label = { Text("Día") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                    )
                }
                Text("Mes", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..12).forEach { m ->
                        ChipFiltro(Meses.nombre(m).take(3), mes == m, MaterialTheme.colorScheme.primary) { mes = m; error = null }
                    }
                }
                error?.let { Text(it, color = EstadoRojo, fontWeight = FontWeight.SemiBold) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val d = dia.toIntOrNull()
                val maximo = YearMonth.of(2024, mes).lengthOfMonth()
                when {
                    nombre.isBlank() -> error = "Escribe el nombre"
                    d == null || d < 1 || d > maximo -> error = "El día debe estar entre 1 y $maximo"
                    else -> {
                        val base = existente ?: Temporada(nombre = nombre, emoji = emoji, mes = mes, dia = d)
                        onGuardar(base.copy(nombre = nombre.trim(), emoji = emoji.trim().ifEmpty { "🛍️" }, mes = mes, dia = d))
                        onCerrar()
                    }
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoTiempos(a: AjustesTemporadas, onGuardar: (AjustesTemporadas) -> Unit, onCerrar: () -> Unit) {
    var aerea by remember { mutableStateOf(a.diasAerea.toString()) }
    var maritima by remember { mutableStateOf(a.diasMaritima.toString()) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { Icon(Icons.Default.Tune, null) },
        title = { Text("Tiempos de tránsito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Días que tarda en llegar la mercadería. Solo cambia el calendario de temporadas: tus pedidos y sus alertas no se tocan.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = aerea, onValueChange = { aerea = it.filter { c -> c.isDigit() }.take(3); error = false },
                    label = { Text("Aérea (días)") }, isError = error, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = maritima, onValueChange = { maritima = it.filter { c -> c.isDigit() }.take(3); error = false },
                    label = { Text("Marítima (días)") }, isError = error, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextButton(onClick = { aerea = "15"; maritima = "25" }) { Text("Volver a 15 y 25 días") }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val x = aerea.toIntOrNull(); val y = maritima.toIntOrNull()
                    if (x == null || y == null || x < 1 || y < 1) error = true else { onGuardar(a.copy(diasAerea = x, diasMaritima = y)); onCerrar() }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White)
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
