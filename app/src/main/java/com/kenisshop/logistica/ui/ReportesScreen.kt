@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kenisshop.logistica.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kenisshop.logistica.data.EMPRESAS_ENVIO
import com.kenisshop.logistica.data.EstadoPedido
import com.kenisshop.logistica.data.Pedido
import com.kenisshop.logistica.data.TipoMercaderia
import com.kenisshop.logistica.ui.theme.EstadoRojo
import com.kenisshop.logistica.ui.theme.VerdeGuardar
import com.kenisshop.logistica.ui.theme.color
import com.kenisshop.logistica.ui.theme.colorCabecera
import com.kenisshop.logistica.ui.theme.colorTexto
import com.kenisshop.logistica.util.Exportador
import com.kenisshop.logistica.util.Fechas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReportesScreen(todos: List<Pedido>, onVolver: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var desde by rememberSaveable { mutableStateOf<Long?>(null) }
    var hasta by rememberSaveable { mutableStateOf<Long?>(null) }
    var tipo by rememberSaveable { mutableStateOf<String?>(null) }
    var empresa by rememberSaveable { mutableStateOf<String?>(null) }
    var estado by rememberSaveable { mutableStateOf<String?>(null) }
    var exportando by remember { mutableStateOf(false) }

    val filtrados = remember(todos, desde, hasta, tipo, empresa, estado) {
        todos.filter { p ->
            (desde == null || p.fechaPedido >= desde!!) &&
                (hasta == null || p.fechaPedido <= hasta!!) &&
                (tipo == null || p.tipo.name == tipo) &&
                (empresa == null || p.empresaEnvio == empresa) &&
                (estado == null || p.estado().name == estado)
        }
    }

    val descripcionFiltros = buildString {
        append(tipo?.let { t -> "Mercadería " + TipoMercaderia.valueOf(t).etiqueta } ?: "Aérea y Marítima")
        append(" · ")
        append(empresa ?: "Todas las empresas")
        append(" · ")
        append(estado?.let { e -> EstadoPedido.valueOf(e).etiqueta } ?: "Todos los estados")
        if (desde != null || hasta != null) {
            append(" · Del ${if (desde != null) Fechas.formatear(desde) else "inicio"} al ${if (hasta != null) Fechas.formatear(hasta) else "hoy"}")
        }
    }

    fun exportar(esPdf: Boolean) {
        if (exportando) return
        exportando = true
        scope.launch {
            try {
                val archivo = withContext(Dispatchers.IO) {
                    if (esPdf) Exportador.pdf(context, filtrados, descripcionFiltros)
                    else Exportador.excel(context, filtrados, descripcionFiltros)
                }
                Exportador.compartir(context, archivo, if (esPdf) Exportador.MIME_PDF else Exportador.MIME_XLSX)
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo generar el reporte", Toast.LENGTH_LONG).show()
            } finally {
                exportando = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes de pedidos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorCabecera(),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------------- Filtros
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Filtros",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                desde = null; hasta = null; tipo = null; empresa = null; estado = null
                            }) {
                                Icon(Icons.Default.FilterAltOff, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Limpiar")
                            }
                        }
                        Text("Fecha del pedido", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CampoFecha("Desde", desde, { desde = it }, Modifier.weight(1f), permitirQuitar = true)
                            CampoFecha("Hasta", hasta, { hasta = it }, Modifier.weight(1f), minimo = desde, permitirQuitar = true)
                        }
                        Text("Tipo de mercadería", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChipFiltro("Todas", tipo == null, MaterialTheme.colorScheme.primary) { tipo = null }
                            TipoMercaderia.entries.forEach { t ->
                                ChipFiltro(t.etiqueta, tipo == t.name, MaterialTheme.colorScheme.primary) {
                                    tipo = if (tipo == t.name) null else t.name
                                }
                            }
                        }
                        Text("Empresa de envío a casillero Miami", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChipFiltro("Todas", empresa == null, MaterialTheme.colorScheme.secondary) { empresa = null }
                            EMPRESAS_ENVIO.forEach { emp ->
                                ChipFiltro(emp, empresa == emp, MaterialTheme.colorScheme.secondary) {
                                    empresa = if (empresa == emp) null else emp
                                }
                            }
                        }
                        Text("Estado", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChipFiltro("Todos", estado == null, MaterialTheme.colorScheme.primary) { estado = null }
                            EstadoPedido.entries.forEach { e ->
                                ChipFiltro(e.etiqueta, estado == e.name, e.color(), e.colorTexto()) {
                                    estado = if (estado == e.name) null else e.name
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- Resumen
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Resumen: ${filtrados.size} pedido(s)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EstadoPedido.entries.forEach { e ->
                                val n = filtrados.count { it.estado() == e }
                                Column(
                                    Modifier
                                        .widthIn(min = 76.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(e.color())
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("$n", color = e.colorTexto(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    Text(e.etiqueta, color = e.colorTexto(), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Text("Por empresa de envío", style = MaterialTheme.typography.labelLarge)
                        EMPRESAS_ENVIO.forEach { emp ->
                            val n = filtrados.count { it.empresaEnvio == emp }
                            if (n > 0) {
                                Row {
                                    Text(emp, modifier = Modifier.weight(1f))
                                    Text("$n", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text("Por tipo", style = MaterialTheme.typography.labelLarge)
                        TipoMercaderia.entries.forEach { t ->
                            Row {
                                Text("Mercadería ${t.etiqueta}", modifier = Modifier.weight(1f))
                                Text("${filtrados.count { it.tipo == t }}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ---------------- Exportar
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { exportar(esPdf = false) },
                        enabled = !exportando,
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.TableChart, null)
                        Spacer(Modifier.width(6.dp))
                        Text("EXCEL", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { exportar(esPdf = true) },
                        enabled = !exportando,
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EstadoRojo, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null)
                        Spacer(Modifier.width(6.dp))
                        Text("PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(filtrados, key = { it.id }) { p ->
                TarjetaPedido(p, seleccionado = false, onClick = {})
            }
        }
    }
}
