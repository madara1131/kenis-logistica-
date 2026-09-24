@file:OptIn(ExperimentalMaterial3Api::class)

package com.kenisshop.logistica.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kenisshop.logistica.data.EstadoPedido
import com.kenisshop.logistica.data.Pedido
import com.kenisshop.logistica.data.TipoMercaderia
import com.kenisshop.logistica.ui.theme.EstadoAzul
import com.kenisshop.logistica.ui.theme.EstadoRojo
import com.kenisshop.logistica.ui.theme.EstadoVerde
import com.kenisshop.logistica.ui.theme.VerdeGuardar
import com.kenisshop.logistica.ui.theme.colorCabecera
import com.kenisshop.logistica.util.Fechas
import java.io.File

@Composable
fun DetallePedido(id: Long, vm: MainViewModel, mostrarVolver: Boolean, onCerrar: () -> Unit) {
    val flujo = remember(id) { vm.observarPedido(id) }
    val pedido by flujo.collectAsStateWithLifecycle(initialValue = null)
    val p = pedido
    if (p == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Cargando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val context = LocalContext.current
    val estado = p.estado()
    var verFoto by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(3.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(colorCabecera())
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mostrarVolver) {
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    if (p.tipo == TipoMercaderia.AEREA) Icons.Default.Flight else Icons.Default.DirectionsBoat,
                    null, tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("#${p.codigo}", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Mercadería ${p.tipo.etiqueta}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
                EstadoBadge(estado, Modifier.padding(end = 8.dp))
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (estado == EstadoPedido.VENCIDO) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(EstadoRojo)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "¡Pedido ${p.tipo.etiqueta.lowercase()} retrasado! Lleva ${p.diasTranscurridos()} días sin ingreso (límite ${p.tipo.diasLimite}).",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                LineaTiempo(p)

                p.fotoPath?.let { ruta ->
                    AsyncImage(
                        model = File(ruta),
                        contentDescription = "Foto del pedido",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { verFoto = true }
                    )
                }

                Dato("Marca de ingreso", p.marcaIngreso)
                Dato("Origen de la mercadería", p.origen)
                Dato("Empresa de envío a casillero Miami", p.empresaEnvio)
                Dato("Fecha del pedido", Fechas.formatear(p.fechaPedido))
                Dato("Llegada a Miami", if (p.fechaMiami == null) "Pendiente" else Fechas.formatear(p.fechaMiami))
                Dato("Fecha de ingreso", if (p.fechaIngreso == null) "Pendiente" else Fechas.formatear(p.fechaIngreso))
                Dato(
                    if (p.fechaIngreso == null) "Días transcurridos" else "Días que tardó",
                    "${p.diasTranscurridos()} de ${p.tipo.diasLimite} permitidos"
                )

                HorizontalDivider()

                // Etapa 2: llegada a Miami (solo una vez)
                if (p.fechaMiami == null && p.fechaIngreso == null) {
                    Button(
                        onClick = {
                            abrirSelectorFecha(context, Fechas.hoy(), minimo = p.fechaPedido) { f ->
                                if (f != null) {
                                    vm.marcarLlegadaMiami(p.id, f)
                                    Toast.makeText(context, "Marcado en tránsito (Miami)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EstadoAzul, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Warehouse, null)
                        Spacer(Modifier.width(8.dp))
                        Text("MARCAR LLEGADA A MIAMI", fontWeight = FontWeight.Bold)
                    }
                }

                // Etapa 3: ingreso final (se puede corregir solo la fecha)
                if (p.fechaIngreso == null) {
                    Button(
                        onClick = {
                            abrirSelectorFecha(context, Fechas.hoy(), minimo = p.fechaPedido) { f ->
                                if (f != null) {
                                    vm.registrarIngreso(p.id, f)
                                    Toast.makeText(context, "Ingreso registrado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("REGISTRAR INGRESO", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            abrirSelectorFecha(context, p.fechaIngreso, minimo = p.fechaPedido) { f ->
                                if (f != null) {
                                    vm.registrarIngreso(p.id, f)
                                    Toast.makeText(context, "Fecha de ingreso actualizada", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.EditCalendar, null)
                        Spacer(Modifier.width(8.dp))
                        Text("EDITAR FECHA DE INGRESO")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "El código y los datos originales no se pueden modificar. Solo se edita la fecha de ingreso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (verFoto && p.fotoPath != null) {
        Dialog(onDismissRequest = { verFoto = false }) {
            AsyncImage(
                model = File(p.fotoPath),
                contentDescription = "Foto del pedido",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { verFoto = false }
            )
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

/** Línea de tiempo de las 3 etapas: Pedido → Casillero Miami → Ingreso final. */
@Composable
private fun LineaTiempo(p: Pedido) {
    data class Etapa(val titulo: String, val fecha: Long?, val hecha: Boolean, val icono: ImageVector)

    val etapas = listOf(
        Etapa("Pedido", p.fechaPedido, true, Icons.Default.ShoppingCart),
        Etapa("Casillero Miami", p.fechaMiami, p.fechaMiami != null || p.fechaIngreso != null, Icons.Default.Warehouse),
        Etapa("Ingreso final", p.fechaIngreso, p.fechaIngreso != null, Icons.Default.Inventory)
    )
    val vencido = p.estado() == EstadoPedido.VENCIDO

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        etapas.forEachIndexed { i, etapa ->
            val hecha = etapa.hecha
            val colorEtapa = when {
                hecha -> EstadoVerde
                vencido -> EstadoRojo
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Column(Modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (hecha) colorEtapa else Color.Transparent)
                        .border(2.dp, colorEtapa, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(etapa.icono, null, tint = if (hecha) Color.White else colorEtapa)
                }
                Spacer(Modifier.height(6.dp))
                Text(etapa.titulo, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(
                    when {
                        etapa.fecha != null -> Fechas.formatear(etapa.fecha)
                        hecha -> "Sin fecha"
                        else -> "Pendiente"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hecha) EstadoVerde else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (i < etapas.lastIndex) {
                val siguienteHecha = etapas[i + 1].hecha
                Box(
                    Modifier
                        .weight(1f)
                        .padding(top = 22.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (siguienteHecha) EstadoVerde else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}
