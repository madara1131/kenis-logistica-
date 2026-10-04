@file:OptIn(ExperimentalMaterial3Api::class)

package com.kenisshop.logistica.ui

import com.kenisshop.logistica.data.resumenPorOrigen
import com.kenisshop.logistica.data.totalPagar
import com.kenisshop.logistica.util.Dolares
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import com.kenisshop.logistica.data.FiltroBusqueda
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kenisshop.logistica.data.EstadoPedido
import com.kenisshop.logistica.data.Pedido
import com.kenisshop.logistica.ui.theme.EstadoRojo
import com.kenisshop.logistica.ui.theme.VerdeGuardar
import com.kenisshop.logistica.ui.theme.color
import com.kenisshop.logistica.ui.theme.colorTexto
import com.kenisshop.logistica.util.Fechas
import java.io.File
import java.time.LocalDate

// ------------------------------------------------------------------ Estado

@Composable
fun EstadoBadge(estado: EstadoPedido, modifier: Modifier = Modifier) {
    Surface(color = estado.color(), shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Text(
            estado.etiqueta,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = estado.colorTexto(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

// ------------------------------------------------------------------ Fechas

fun abrirSelectorFecha(
    context: Context,
    inicial: Long?,
    minimo: Long? = null,
    permitirQuitar: Boolean = false,
    onCambio: (Long?) -> Unit
) {
    val d = inicial?.let { Fechas.aLocalDate(it) } ?: LocalDate.now()
    val dialogo = DatePickerDialog(
        context,
        { _, anio, mes, dia -> onCambio(Fechas.desdeLocalDate(LocalDate.of(anio, mes + 1, dia))) },
        d.year, d.monthValue - 1, d.dayOfMonth
    )
    dialogo.datePicker.maxDate = System.currentTimeMillis()
    minimo?.let { dialogo.datePicker.minDate = it }
    if (permitirQuitar) {
        dialogo.setButton(DialogInterface.BUTTON_NEUTRAL, "Quitar fecha") { _, _ -> onCambio(null) }
    }
    dialogo.show()
}

@Composable
fun CampoFecha(
    etiqueta: String,
    valor: Long?,
    onCambio: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    minimo: Long? = null,
    permitirQuitar: Boolean = false,
    error: Boolean = false
) {
    val context = LocalContext.current
    Box(modifier) {
        OutlinedTextField(
            value = if (valor == null) "" else Fechas.formatear(valor),
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            placeholder = { Text("Seleccionar fecha") },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, null) },
            isError = error,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable { abrirSelectorFecha(context, valor, minimo, permitirQuitar, onCambio) }
        )
    }
}

// ------------------------------------------------------------------ Chips

@Composable
fun ChipFiltro(texto: String, seleccionado: Boolean, colorSel: Color, colorTextoSel: Color = Color.White, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal) },
        leadingIcon = if (seleccionado) {
            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colorSel,
            selectedLabelColor = colorTextoSel,
            selectedLeadingIconColor = colorTextoSel
        )
    )
}

// ------------------------------------------------------------------ Lista de pedidos

fun lineaEstado(p: Pedido, estado: EstadoPedido): String {
    val dias = p.diasTranscurridos()
    return when (estado) {
        EstadoPedido.INGRESADO -> "Ingreso: ${Fechas.formatear(p.fechaIngreso)} · tardó $dias días"
        EstadoPedido.EN_TRANSITO -> "En tránsito en Miami desde ${Fechas.formatear(p.fechaMiami)} · $dias días"
        EstadoPedido.PENDIENTE -> "Pedido: ${Fechas.formatear(p.fechaPedido)} · $dias días"
        EstadoPedido.VENCIDO -> "¡$dias días sin ingreso! (límite ${p.tipo.diasLimite})"
    }
}

@Composable
fun TarjetaPedido(p: Pedido, seleccionado: Boolean, onClick: () -> Unit) {
    val estado = p.estado()
    val vencido = estado == EstadoPedido.VENCIDO
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                seleccionado -> MaterialTheme.colorScheme.primaryContainer
                vencido -> EstadoRojo.copy(alpha = 0.08f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (vencido) BorderStroke(1.5.dp, EstadoRojo) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(5.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(estado.color())
            )
            Spacer(Modifier.width(10.dp))
            if (p.fotoPath != null) {
                AsyncImage(
                    model = File(p.fotoPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (vencido) {
                        Icon(Icons.Default.Warning, null, tint = EstadoRojo, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        "#${p.codigo}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                p.cliente?.let {
                    Text("👤 $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    "${p.empresaEnvio} · Origen: ${p.origen}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    lineaEstado(p, estado),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (vencido) EstadoRojo else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (vencido) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                textoPesoCobro(p.pesoLibras, p.totalPagar)?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = VerdeGuardar)
                }
            }
            Spacer(Modifier.width(8.dp))
            EstadoBadge(estado)
        }
    }
}

@Composable
fun ListaPedidos(
    pedidos: List<Pedido>,
    seleccionado: Long?,
    onSeleccionar: (Pedido) -> Unit,
    onNuevo: () -> Unit,
    onBuscar: (EstadoPedido?) -> Unit,
    filtroGuardado: FiltroBusqueda,
    modifier: Modifier = Modifier
) {
    // La página principal muestra TODOS los pedidos; las búsquedas van en su propia pestaña.
    val vencidos = remember(pedidos) { pedidos.count { it.estado() == EstadoPedido.VENCIDO } }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = onNuevo,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("REGISTRAR PEDIDO", fontWeight = FontWeight.Bold)
            }
        }
        item {
            // Barra que abre la pestaña de búsqueda
            Surface(
                onClick = { onBuscar(null) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Buscar pedidos", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (filtroGuardado.activo) "Búsqueda guardada · ${filtroGuardado.criterios} filtro(s) · toca para ver"
                            else "Por cliente, fecha, empresa, peso o estado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (filtroGuardado.activo) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Text(
                                "${filtroGuardado.criterios}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
        item {
            ResumenLibras(pedidos)
        }
        if (vencidos > 0) {
            item {
                Surface(
                    color = EstadoRojo,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onBuscar(EstadoPedido.VENCIDO) }
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("¡ALERTA DE ATRASO!", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                "$vencidos pedido(s) vencido(s) sin ingreso. Toca para verlos.",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                "Lista de Pedidos (${pedidos.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (pedidos.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Inventory2, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Todavía no hay pedidos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(pedidos, key = { it.id }) { p ->
            TarjetaPedido(p, seleccionado = p.id == seleccionado, onClick = { onSeleccionar(p) })
        }
    }
}

/** Libras y dólares a pagar del listado visible, y el detalle por cada marca/origen. */
@Composable
fun ResumenLibras(pedidos: List<Pedido>) {
    if (pedidos.isEmpty()) return
    val (total, porOrigen) = remember(pedidos) { resumenPorOrigen(pedidos) }
    var abierto by rememberSaveable { mutableStateOf(false) }
    Card(
        onClick = { abierto = !abierto },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("⚖️ Peso total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(Dolares.libras(total.libras), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("💵 Dinero a pagar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(Dolares.fmt(total.dolares), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = VerdeGuardar)
                }
            }
            if (total.sinPeso > 0) {
                Text("${total.sinPeso} pedido(s) sin peso todavía", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
            }
            Text(
                if (abierto) "▲ Ocultar detalle por marca" else "▼ Ver libras por marca / origen (${porOrigen.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (abierto) {
                Spacer(Modifier.height(6.dp))
                porOrigen.forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.origen, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${r.pedidos} pedido(s)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                        Text(Dolares.libras(r.libras), modifier = Modifier.padding(end = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(Dolares.fmt(r.dolares), fontWeight = FontWeight.Bold, color = VerdeGuardar)
                    }
                }
            }
        }
    }
}
