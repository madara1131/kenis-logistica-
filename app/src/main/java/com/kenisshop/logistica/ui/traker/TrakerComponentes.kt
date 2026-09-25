@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kenisshop.logistica.ui.traker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kenisshop.logistica.data.traker.EstadoGasto
import com.kenisshop.logistica.data.traker.GastoCategoria
import com.kenisshop.logistica.data.traker.ItemLista
import com.kenisshop.logistica.data.traker.SeccionTraker
import com.kenisshop.logistica.data.traker.TipoLista
import com.kenisshop.logistica.data.traker.diferencia
import com.kenisshop.logistica.data.traker.estado
import com.kenisshop.logistica.data.traker.plan
import com.kenisshop.logistica.data.traker.real
import com.kenisshop.logistica.ui.ChipFiltro
import com.kenisshop.logistica.ui.theme.EstadoAmarillo
import com.kenisshop.logistica.ui.theme.EstadoRojo
import com.kenisshop.logistica.ui.theme.EstadoVerde
import com.kenisshop.logistica.ui.theme.VerdeGuardar
import com.kenisshop.logistica.ui.theme.colorCabecera
import com.kenisshop.logistica.util.Dinero
import com.kenisshop.logistica.util.Meses

// ------------------------------------------------------------------ Estados

val ColorSinGasto = Color(0xFF8A94A6)

fun EstadoGasto.color(): Color = when (this) {
    EstadoGasto.SIN_GASTO -> ColorSinGasto
    EstadoGasto.DENTRO -> EstadoVerde
    EstadoGasto.CERCA -> EstadoAmarillo
    EstadoGasto.EXCEDIDO -> EstadoRojo
}

fun EstadoGasto.colorTexto(): Color = if (this == EstadoGasto.CERCA) Color(0xFF3A2E00) else Color.White

@Composable
fun ChipEstadoGasto(e: EstadoGasto) {
    Surface(color = e.color(), shape = RoundedCornerShape(6.dp)) {
        Text(
            when (e) {
                EstadoGasto.SIN_GASTO -> "Sin gasto"
                EstadoGasto.DENTRO -> "OK"
                EstadoGasto.CERCA -> "Cerca"
                EstadoGasto.EXCEDIDO -> "Excedido"
            },
            color = e.colorTexto(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

// ------------------------------------------------------------------ Deslizar para borrar

/** Desliza a la izquierda para borrar (pide confirmación). */
@Composable
fun DeslizarParaBorrar(mensaje: String, onBorrar: () -> Unit, contenido: @Composable () -> Unit) {
    var confirmar by remember { mutableStateOf(false) }
    val estado = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart) {
                confirmar = true
                false
            } else true
        }
    )
    SwipeToDismissBox(
        state = estado,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(EstadoRojo)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Delete, null, tint = Color.White)
                }
            }
        }
    ) {
        contenido()
    }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("¿Eliminar?") },
            text = { Text(mensaje) },
            confirmButton = {
                TextButton(onClick = { confirmar = false; onBorrar() }) { Text("Eliminar", color = EstadoRojo) }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
        )
    }
}

// ------------------------------------------------------------------ Fila de categoría

@Composable
fun FilaCategoria(
    c: GastoCategoria,
    onClick: () -> Unit,
    onBorrar: () -> Unit,
    mostrarMes: Boolean = false
) {
    val estado = c.estado()
    DeslizarParaBorrar("Se eliminará \"${c.categoria}\" de ${Meses.etiqueta(c.mes)}.", onBorrar) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = if (estado == EstadoGasto.EXCEDIDO) EstadoRojo.copy(alpha = 0.07f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            c.categoria,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (mostrarMes) {
                            Text(
                                "${Meses.etiqueta(c.mes)} · ${c.seccion.etiqueta}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ChipEstadoGasto(estado)
                }
                Spacer(Modifier.height(6.dp))
                BarraProgreso(
                    fraccion = if (c.plan > 0) (c.real / c.plan).toFloat() else if (c.real > 0) 1f else 0f,
                    color = estado.color()
                )
                Spacer(Modifier.height(6.dp))
                Row {
                    Text(
                        "Gastado ${Dinero.fmt(c.real)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "de ${Dinero.fmt(c.presupuesto)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (c.diferencia >= 0) "Disponible ${Dinero.fmt(c.diferencia)}" else "Pasado por ${Dinero.fmt(-c.diferencia)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (c.diferencia >= 0) EstadoVerde else EstadoRojo
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Tarjeta de sección (un bloque del Excel)

@Composable
fun TarjetaSeccion(
    seccion: SeccionTraker,
    categorias: List<GastoCategoria>,
    capital: Double?,
    onEditarCategoria: (GastoCategoria) -> Unit,
    onBorrarCategoria: (GastoCategoria) -> Unit,
    onAgregar: () -> Unit,
    onEditarCapital: () -> Unit
) {
    val plan = categorias.sumOf { it.plan }
    val real = categorias.sumOf { it.real }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(colorCabecera())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${seccion.emoji} ${seccion.etiqueta}", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(Dinero.fmt(real), color = Color.White, fontWeight = FontWeight.Bold)
            }
            // capital y saldo (tocar para editar)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditarCapital)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Capital", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (capital == null) "Tocar para agregar" else Dinero.fmt(capital), fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f)) {
                    Text("Saldo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val saldo = capital?.let { it - real }
                    Text(
                        Dinero.fmt(saldo),
                        fontWeight = FontWeight.Bold,
                        color = when {
                            saldo == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            saldo < 0 -> EstadoRojo
                            else -> EstadoVerde
                        }
                    )
                }
                Icon(Icons.Default.Edit, "Editar capital", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            HorizontalDivider()
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                categorias.forEach { c ->
                    key(c.id) {
                        FilaCategoria(c, onClick = { onEditarCategoria(c) }, onBorrar = { onBorrarCategoria(c) })
                    }
                }
                TextButton(onClick = onAgregar) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Agregar categoría")
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Totales("Presupuesto", Dinero.fmt(plan), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                Totales("Gasto real", Dinero.fmt(real), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                Totales("Diferencia", Dinero.fmt(plan - real), if (plan - real >= 0) EstadoVerde else EstadoRojo, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun Totales(etiqueta: String, valor: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}

// ------------------------------------------------------------------ Editor de categoría (hoja inferior)

@Composable
fun EditorCategoria(
    existente: GastoCategoria?,
    mesInicial: String,
    seccionInicial: SeccionTraker,
    meses: List<String>,
    onGuardar: (GastoCategoria) -> Unit,
    onBorrar: (GastoCategoria) -> Unit,
    onCerrar: () -> Unit
) {
    val hoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mes by remember { mutableStateOf(existente?.mes ?: mesInicial) }
    var seccion by remember { mutableStateOf(existente?.seccion ?: seccionInicial) }
    var nombre by remember { mutableStateOf(existente?.categoria ?: "") }
    var presupuesto by remember { mutableStateOf(Dinero.editable(existente?.presupuesto)) }
    var real by remember { mutableStateOf(Dinero.editable(existente?.gastoReal)) }
    var sumar by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmarBorrar by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = hoja) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (existente == null) "Nuevo registro" else "Editar registro",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Mes", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (meses + listOf(mes)).distinct().sorted().forEach { m ->
                    ChipFiltro(Meses.cortoConAnio(m), mes == m, MaterialTheme.colorScheme.primary) { mes = m }
                }
            }
            Text("Sección", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SeccionTraker.entries.forEach { s ->
                    ChipFiltro("${s.emoji} ${s.etiqueta}", seccion == s, MaterialTheme.colorScheme.secondary) { seccion = s }
                }
            }
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; error = null },
                label = { Text("Categoría (ej: 🚗 Vehículo)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = presupuesto,
                    onValueChange = { presupuesto = it; error = null },
                    label = { Text("Presupuesto") },
                    prefix = { Text("C$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = real,
                    onValueChange = { real = it; error = null },
                    label = { Text("Gasto real") },
                    prefix = { Text("C$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            // Sumar o restar un gasto rápido (lo más común en el teléfono)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sumar,
                    onValueChange = { sumar = it },
                    label = { Text("Gasto de hoy") },
                    prefix = { Text("C$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(onClick = {
                    val v = Dinero.parsear(sumar)
                    if (v != null) {
                        real = Dinero.editable((Dinero.parsear(real) ?: 0.0) + v); sumar = ""
                    }
                }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Text("Sumar")
                }
                IconButton(onClick = {
                    val v = Dinero.parsear(sumar)
                    if (v != null) {
                        real = Dinero.editable(((Dinero.parsear(real) ?: 0.0) - v).coerceAtLeast(0.0)); sumar = ""
                    }
                }) {
                    Icon(Icons.Default.Remove, "Restar")
                }
            }
            error?.let { Text(it, color = EstadoRojo, fontWeight = FontWeight.SemiBold) }
            Button(
                onClick = {
                    val p = if (presupuesto.isBlank()) null else Dinero.parsear(presupuesto)
                    val r = if (real.isBlank()) null else Dinero.parsear(real)
                    when {
                        nombre.isBlank() -> error = "Escribe el nombre de la categoría"
                        presupuesto.isNotBlank() && p == null -> error = "El presupuesto no es un número válido"
                        real.isNotBlank() && r == null -> error = "El gasto real no es un número válido"
                        else -> {
                            val base = existente ?: GastoCategoria(mes = mes, seccion = seccion, categoria = nombre, presupuesto = null, gastoReal = null)
                            onGuardar(base.copy(mes = mes, seccion = seccion, categoria = nombre.trim(), presupuesto = p, gastoReal = r))
                            onCerrar()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(if (existente == null) "GUARDAR" else "ACTUALIZAR", fontWeight = FontWeight.Bold)
            }
            if (existente != null) {
                OutlinedButton(
                    onClick = { confirmarBorrar = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EstadoRojo)
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("ELIMINAR")
                }
            }
        }
    }
    if (confirmarBorrar && existente != null) {
        AlertDialog(
            onDismissRequest = { confirmarBorrar = false },
            title = { Text("¿Eliminar registro?") },
            text = { Text("Se eliminará \"${existente.categoria}\" de ${Meses.etiqueta(existente.mes)}.") },
            confirmButton = {
                TextButton(onClick = { confirmarBorrar = false; onBorrar(existente); onCerrar() }) {
                    Text("Eliminar", color = EstadoRojo)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrar = false }) { Text("Cancelar") } }
        )
    }
}

// ------------------------------------------------------------------ Editor de listas

@Composable
fun EditorItem(
    existente: ItemLista?,
    tipo: TipoLista,
    onGuardar: (ItemLista) -> Unit,
    onBorrar: (ItemLista) -> Unit,
    onCerrar: () -> Unit
) {
    val hoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nombre by remember { mutableStateOf(existente?.nombre ?: "") }
    var monto by remember { mutableStateOf(Dinero.editable(existente?.monto)) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = hoja) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "${tipo.emoji} ${if (existente == null) "Agregar a" else "Editar en"} ${tipo.etiqueta}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; error = null },
                label = { Text(tipo.etiquetaNombre) },
                singleLine = tipo != TipoLista.NOTA,
                minLines = if (tipo == TipoLista.NOTA) 3 else 1,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            if (tipo == TipoLista.AHORRO || tipo == TipoLista.DIEZMO) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..12).forEach { m ->
                        ChipFiltro(Meses.nombre(m).take(3), nombre == Meses.nombre(m), MaterialTheme.colorScheme.primary) {
                            nombre = Meses.nombre(m)
                        }
                    }
                }
            }
            if (tipo.conMonto) {
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it; error = null },
                    label = { Text("Monto") },
                    prefix = { Text("C$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            error?.let { Text(it, color = EstadoRojo, fontWeight = FontWeight.SemiBold) }
            Button(
                onClick = {
                    val m = if (monto.isBlank()) null else Dinero.parsear(monto)
                    when {
                        nombre.isBlank() -> error = "Escribe ${tipo.etiquetaNombre.lowercase()}"
                        tipo.conMonto && monto.isNotBlank() && m == null -> error = "El monto no es un número válido"
                        else -> {
                            val base = existente ?: ItemLista(lista = tipo, nombre = nombre, monto = null, orden = Int.MAX_VALUE / 2)
                            onGuardar(base.copy(nombre = nombre.trim(), monto = if (tipo.conMonto) m else null))
                            onCerrar()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(if (existente == null) "GUARDAR" else "ACTUALIZAR", fontWeight = FontWeight.Bold)
            }
            if (existente != null) {
                OutlinedButton(
                    onClick = { onBorrar(existente); onCerrar() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EstadoRojo)
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("ELIMINAR")
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Capital

@Composable
fun DialogoCapital(mes: String, seccion: SeccionTraker, actual: Double?, onGuardar: (Double?) -> Unit, onCerrar: () -> Unit) {
    var texto by remember { mutableStateOf(Dinero.editable(actual)) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Capital · ${seccion.etiqueta}") },
        text = {
            Column {
                Text(Meses.etiqueta(mes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it; error = false },
                    label = { Text("Capital del mes") },
                    prefix = { Text("C$ ") },
                    isError = error,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = if (texto.isBlank()) null else Dinero.parsear(texto)
                if (texto.isNotBlank() && v == null) error = true else { onGuardar(v); onCerrar() }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
