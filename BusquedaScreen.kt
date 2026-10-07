@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kenisshop.logistica.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kenisshop.logistica.data.EMPRESAS_ENVIO
import com.kenisshop.logistica.data.EstadoPedido
import com.kenisshop.logistica.data.FiltroBusqueda
import com.kenisshop.logistica.data.OrdenBusqueda
import com.kenisshop.logistica.data.Pedido
import com.kenisshop.logistica.data.TipoMercaderia
import com.kenisshop.logistica.data.buscarPedidos
import com.kenisshop.logistica.data.resumenPorOrigen
import com.kenisshop.logistica.ui.theme.EstadoAzul
import com.kenisshop.logistica.ui.theme.EstadoRojo
import com.kenisshop.logistica.ui.theme.VerdeGuardar
import com.kenisshop.logistica.ui.theme.color
import com.kenisshop.logistica.ui.theme.colorCabecera
import com.kenisshop.logistica.ui.theme.colorTexto
import com.kenisshop.logistica.ui.traker.DeslizarParaBorrar
import com.kenisshop.logistica.util.Dinero
import com.kenisshop.logistica.util.Fechas
import com.kenisshop.logistica.util.Dolares
import com.kenisshop.logistica.util.numeroDe

/**
 * Pestaña independiente de búsqueda: muestra SOLO los pedidos que cumplen los criterios.
 * La búsqueda queda guardada para la próxima vez.
 */
@Composable
fun BusquedaScreen(vm: MainViewModel, todos: List<Pedido>, onVolver: () -> Unit) {
    val f = vm.filtro
    val resultados = remember(todos, f) { buscarPedidos(todos, f) }
    var detalleId by rememberSaveable { mutableStateOf<Long?>(null) }
    val esTablet = LocalConfiguration.current.screenWidthDp >= 600

    BackHandler {
        if (!esTablet && detalleId != null) detalleId = null else onVolver()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Buscar pedidos", fontWeight = FontWeight.Bold)
                        Text("Aérea y Marítima", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (!esTablet && detalleId != null) detalleId = null else onVolver() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (f.activo) {
                        TextButton(onClick = { vm.cambiarFiltro(FiltroBusqueda(orden = f.orden, descendente = f.descendente)) }) {
                            Icon(Icons.Default.FilterAltOff, null, tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Limpiar", color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorCabecera(),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (esTablet) {
                Row(Modifier.fillMaxSize()) {
                    PanelResultados(vm, f, resultados, todos.size, detalleId, { detalleId = it.id }, Modifier.weight(0.45f).fillMaxHeight())
                    VerticalDivider()
                    Box(Modifier.weight(0.55f).fillMaxHeight()) {
                        val id = detalleId
                        if (id != null && resultados.any { it.id == id }) {
                            DetallePedido(id = id, vm = vm, mostrarVolver = false, onCerrar = { detalleId = null })
                        } else {
                            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                                Text("Toca un pedido para ver el detalle", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                val id = detalleId
                if (id != null) {
                    DetallePedido(id = id, vm = vm, mostrarVolver = true, onCerrar = { detalleId = null })
                } else {
                    PanelResultados(vm, f, resultados, todos.size, null, { detalleId = it.id }, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun PanelResultados(
    vm: MainViewModel,
    f: FiltroBusqueda,
    resultados: List<Pedido>,
    totalPedidos: Int,
    seleccionado: Long?,
    onAbrir: (Pedido) -> Unit,
    modifier: Modifier
) {
    var verFiltros by rememberSaveable { mutableStateOf(true) }
    var pesoMin by rememberSaveable { mutableStateOf(Dinero.editable(f.pesoMin)) }
    var pesoMax by rememberSaveable { mutableStateOf(Dinero.editable(f.pesoMax)) }
    val cambiar: (FiltroBusqueda) -> Unit = { vm.cambiarFiltro(it) }
    // Si se limpian los filtros desde arriba, también se vacían las casillas de peso
    LaunchedEffect(f.pesoMin, f.pesoMax) {
        if (f.pesoMin == null && numeroDe(pesoMin) != null) pesoMin = ""
        if (f.pesoMax == null && numeroDe(pesoMax) != null) pesoMax = ""
    }
    val (totales, _) = remember(resultados) { resumenPorOrigen(resultados) }

    LazyColumn(modifier, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(
                value = f.texto,
                onValueChange = { cambiar(f.copy(texto = it)) },
                placeholder = { Text("Cliente, código, marca o empresa") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (f.texto.isNotEmpty()) IconButton(onClick = { cambiar(f.copy(texto = "")) }) { Icon(Icons.Default.Clear, "Borrar") }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Filtros" + if (f.criterios > 0) " (${f.criterios})" else "",
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { verFiltros = !verFiltros }) { Text(if (verFiltros) "Ocultar" else "Mostrar") }
                    }
                    if (verFiltros) {
                        Text("Tipo", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChipFiltro("Ambos", f.tipo == null, MaterialTheme.colorScheme.primary) { cambiar(f.copy(tipo = null)) }
                            TipoMercaderia.entries.forEach { t ->
                                ChipFiltro(if (t == TipoMercaderia.AEREA) "✈️ Aérea" else "🚢 Marítima", f.tipo == t, MaterialTheme.colorScheme.primary) {
                                    cambiar(f.copy(tipo = if (f.tipo == t) null else t))
                                }
                            }
                        }
                        Text("Fecha del pedido", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CampoFecha("Desde", f.desde, { cambiar(f.copy(desde = it)) }, Modifier.weight(1f), permitirQuitar = true)
                            CampoFecha("Hasta", f.hasta, { cambiar(f.copy(hasta = it)) }, Modifier.weight(1f), minimo = f.desde, permitirQuitar = true)
                        }
                        Text("Empresa de envío", style = MaterialTheme.typography.labelLarge)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChipFiltro("Todas", f.empresa == null, MaterialTheme.colorScheme.secondary) { cambiar(f.copy(empresa = null)) }
                            EMPRESAS_ENVIO.forEach { e ->
                                ChipFiltro(e, f.empresa == e, MaterialTheme.colorScheme.secondary) { cambiar(f.copy(empresa = if (f.empresa == e) null else e)) }
                            }
                        }
                        Text("Estado", style = MaterialTheme.typography.labelLarge)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChipFiltro("Todos", f.estado == null, MaterialTheme.colorScheme.primary) { cambiar(f.copy(estado = null)) }
                            EstadoPedido.entries.forEach { e ->
                                ChipFiltro(e.etiqueta, f.estado == e, e.color(), e.colorTexto()) { cambiar(f.copy(estado = if (f.estado == e) null else e)) }
                            }
                        }
                        Text("Peso (libras)", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = pesoMin,
                                onValueChange = { pesoMin = it; cambiar(f.copy(pesoMin = numeroDe(it), soloSinPeso = false)) },
                                label = { Text("Mínimo") }, suffix = { Text("lb") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pesoMax,
                                onValueChange = { pesoMax = it; cambiar(f.copy(pesoMax = numeroDe(it), soloSinPeso = false)) },
                                label = { Text("Máximo") }, suffix = { Text("lb") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        ChipFiltro("Sin peso registrado", f.soloSinPeso, MaterialTheme.colorScheme.tertiary) {
                            pesoMin = ""; pesoMax = ""
                            cambiar(f.copy(soloSinPeso = !f.soloSinPeso, pesoMin = null, pesoMax = null))
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${resultados.size} de $totalPedidos pedido(s) · ${Dolares.libras(totales.libras)} · ${Dolares.fmt(totales.dolares)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ordenar:", style = MaterialTheme.typography.labelLarge)
                    OrdenBusqueda.entries.forEach { o ->
                        val sel = f.orden == o
                        AssistChip(
                            onClick = { cambiar(if (sel) f.copy(descendente = !f.descendente) else f.copy(orden = o, descendente = true)) },
                            label = { Text(o.etiqueta, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                            trailingIcon = if (sel) {
                                { Icon(if (f.descendente) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = if (sel) AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                labelColor = Color.White,
                                trailingIconContentColor = Color.White
                            ) else AssistChipDefaults.assistChipColors()
                        )
                    }
                }
                Text(
                    when (f.orden) {
                        OrdenBusqueda.FECHA -> if (f.descendente) "Más recientes primero" else "Más antiguos primero"
                        OrdenBusqueda.PESO -> if (f.descendente) "Más pesados primero" else "Más livianos primero"
                        OrdenBusqueda.COSTO -> if (f.descendente) "Más caros primero" else "Más baratos primero"
                        OrdenBusqueda.ESTADO -> if (f.descendente) "Urgentes primero (vencidos)" else "Ingresados primero"
                    } + " · toca de nuevo para invertir",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (resultados.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("Ningún pedido cumple la búsqueda", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(resultados, key = { it.id }) { p ->
            ResultadoPedido(p, vm, seleccionado = p.id == seleccionado, onAbrir = { onAbrir(p) })
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** Tarjeta del resultado con acciones rápidas; desliza ← para eliminar. */
@Composable
private fun ResultadoPedido(p: Pedido, vm: MainViewModel, seleccionado: Boolean, onAbrir: () -> Unit) {
    val context = LocalContext.current
    var peso by remember { mutableStateOf(false) }
    var cliente by remember { mutableStateOf(false) }
    var eliminar by remember { mutableStateOf(false) }

    DeslizarParaBorrar("Se eliminará el pedido #${p.codigo} y su foto.", {
        vm.eliminarPedido(p)
        Toast.makeText(context, "Pedido eliminado", Toast.LENGTH_SHORT).show()
    }) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
            TarjetaPedido(p, seleccionado = seleccionado, onClick = onAbrir)
            FlowRow(
                Modifier.padding(start = 6.dp, top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (p.fechaMiami == null && p.fechaIngreso == null) {
                    AccionRapida("Llegó a Miami", Icons.Default.Warehouse, EstadoAzul) {
                        abrirSelectorFecha(context, Fechas.hoy(), minimo = p.fechaPedido) { f ->
                            if (f != null) { vm.marcarLlegadaMiami(p.id, f); Toast.makeText(context, "En tránsito ✔", Toast.LENGTH_SHORT).show() }
                        }
                    }
                }
                AccionRapida(if (p.fechaIngreso == null) "Ingreso" else "Editar ingreso", Icons.Default.CheckCircle, VerdeGuardar) {
                    abrirSelectorFecha(context, p.fechaIngreso ?: Fechas.hoy(), minimo = p.fechaPedido) { f ->
                        if (f != null) { vm.registrarIngreso(p.id, f); Toast.makeText(context, "Ingreso guardado ✔", Toast.LENGTH_SHORT).show() }
                    }
                }
                AccionRapida(if (p.pesoLibras == null) "Peso" else "Editar peso", Icons.Default.Scale, MaterialTheme.colorScheme.primary) { peso = true }
                AccionRapida("Cliente", Icons.Default.Person, MaterialTheme.colorScheme.secondary) { cliente = true }
                AccionRapida("Eliminar", Icons.Default.Delete, EstadoRojo) { eliminar = true }
            }
        }
    }
    if (peso) DialogoPeso(p, onGuardar = { pv, tv -> vm.actualizarPeso(p.id, pv, tv) }, onCerrar = { peso = false })
    if (cliente) DialogoCliente(p, onGuardar = { vm.actualizarCliente(p.id, it) }, onCerrar = { cliente = false })
    if (eliminar) DialogoEliminarPedido(p, onEliminar = {
        vm.eliminarPedido(p)
        Toast.makeText(context, "Pedido eliminado", Toast.LENGTH_SHORT).show()
    }, onCerrar = { eliminar = false })
}

@Composable
private fun AccionRapida(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(texto, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = { Icon(icono, null, Modifier.size(16.dp), tint = color) }
    )
}
