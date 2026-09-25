@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.kenisshop.logistica.ui.traker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kenisshop.logistica.data.traker.DatosTraker
import com.kenisshop.logistica.data.traker.EstadoGasto
import com.kenisshop.logistica.data.traker.GastoCategoria
import com.kenisshop.logistica.data.traker.ItemLista
import com.kenisshop.logistica.data.traker.SeccionTraker
import com.kenisshop.logistica.data.traker.TipoLista
import com.kenisshop.logistica.data.traker.estado
import com.kenisshop.logistica.data.traker.plan
import com.kenisshop.logistica.data.traker.real
import com.kenisshop.logistica.ui.ChipFiltro
import com.kenisshop.logistica.ui.theme.AzulKenis
import com.kenisshop.logistica.ui.theme.EstadoRojo
import com.kenisshop.logistica.ui.theme.EstadoVerde
import com.kenisshop.logistica.ui.theme.RosaKenis
import com.kenisshop.logistica.ui.theme.VerdeGuardar
import com.kenisshop.logistica.util.Dinero
import com.kenisshop.logistica.util.Meses
import kotlinx.coroutines.launch

private data class EdicionCategoria(val existente: GastoCategoria?, val mes: String, val seccion: SeccionTraker)
private data class EdicionItem(val existente: ItemLista?, val tipo: TipoLista)
private data class EdicionCapital(val mes: String, val seccion: SeccionTraker)

private val COLOR_SECCION = mapOf(
    SeccionTraker.PERSONAL to AzulKenis,
    SeccionTraker.KENISSHOP to RosaKenis,
    SeccionTraker.NECESARIOS to Color(0xFFF2A12E)
)

private val MIMES_EXCEL = arrayOf(
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-excel",
    "application/octet-stream"
)

@Composable
fun TrakerScreen(vm: TrakerViewModel = viewModel()) {
    val datos by vm.datos.collectAsStateWithLifecycle()
    var vista by rememberSaveable { mutableStateOf(0) }
    var editCat by remember { mutableStateOf<EdicionCategoria?>(null) }
    var editItem by remember { mutableStateOf<EdicionItem?>(null) }
    var editCapital by remember { mutableStateOf<EdicionCapital?>(null) }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.leerArchivo(uri)
    }
    val importar = { selector.launch(MIMES_EXCEL) }

    val d = datos
    Box(Modifier.fillMaxSize()) {
        when {
            d == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            d.vacio -> TrakerVacio(onImportar = importar, onDesdeCero = { vm.empezarDesdeCero() })
            else -> Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                        val opciones = listOf("Mes", "Registros", "Gráficos", "Listas")
                        opciones.forEachIndexed { i, texto ->
                            SegmentedButton(
                                selected = vista == i,
                                onClick = { vista = i },
                                shape = SegmentedButtonDefaults.itemShape(index = i, count = opciones.size),
                                icon = {}
                            ) { Text(texto, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                        }
                    }
                    IconButton(onClick = importar) { Icon(Icons.Default.UploadFile, "Importar Excel") }
                }
                when (vista) {
                    0 -> VistaMes(
                        d, vm,
                        onEditar = { editCat = EdicionCategoria(it, it.mes, it.seccion) },
                        onNueva = { mes, sec -> editCat = EdicionCategoria(null, mes, sec) },
                        onCapital = { mes, sec -> editCapital = EdicionCapital(mes, sec) }
                    )
                    1 -> VistaRegistros(
                        d,
                        onEditar = { editCat = EdicionCategoria(it, it.mes, it.seccion) },
                        onBorrar = { vm.borrarCategoria(it) },
                        onNueva = { editCat = EdicionCategoria(null, d.meses.lastOrNull() ?: Meses.actual(), SeccionTraker.PERSONAL) }
                    )
                    2 -> VistaGraficos(d)
                    else -> VistaListas(
                        d,
                        onEditar = { editItem = EdicionItem(it, it.lista) },
                        onNuevo = { editItem = EdicionItem(null, it) },
                        onBorrar = { vm.borrarItem(it) }
                    )
                }
            }
        }
        if (vm.trabajando) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Color.White) }
        }
    }

    // ---- Hojas y diálogos
    editCat?.let { e ->
        EditorCategoria(
            existente = e.existente,
            mesInicial = e.mes,
            seccionInicial = e.seccion,
            meses = d?.meses ?: emptyList(),
            onGuardar = { vm.guardarCategoria(it) },
            onBorrar = { vm.borrarCategoria(it) },
            onCerrar = { editCat = null }
        )
    }
    editItem?.let { e ->
        EditorItem(
            existente = e.existente,
            tipo = e.tipo,
            onGuardar = { vm.guardarItem(it) },
            onBorrar = { vm.borrarItem(it) },
            onCerrar = { editItem = null }
        )
    }
    editCapital?.let { e ->
        DialogoCapital(
            mes = e.mes,
            seccion = e.seccion,
            actual = d?.capital(e.mes, e.seccion),
            onGuardar = { vm.guardarCapital(e.mes, e.seccion, it) },
            onCerrar = { editCapital = null }
        )
    }
    vm.vistaPrevia?.let { v -> DialogoImportacion(v, datosActuales = d, vm = vm) }
}

// ====================================================================== Estado vacío

@Composable
private fun TrakerVacio(onImportar: () -> Unit, onDesdeCero: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Insights, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(12.dp))
        Text("Traker de gastos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Importa tu Excel TRAKER y la app lo convierte en tarjetas, gráficos y alertas. " +
                "Después todo se controla desde aquí, sin fórmulas.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onImportar,
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White)
        ) {
            Icon(Icons.Default.UploadFile, null)
            Spacer(Modifier.width(8.dp))
            Text("IMPORTAR EXCEL", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onDesdeCero,
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Empezar desde cero (mes actual)") }
    }
}

// ====================================================================== Vista MES (deslizable)

@Composable
private fun VistaMes(
    d: DatosTraker,
    vm: TrakerViewModel,
    onEditar: (GastoCategoria) -> Unit,
    onNueva: (String, SeccionTraker) -> Unit,
    onCapital: (String, SeccionTraker) -> Unit
) {
    val meses = d.meses
    if (meses.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No hay meses todavía")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.nuevoMes() }) { Text("Crear mes") }
        }
        return
    }
    val inicial = meses.indexOf(Meses.actual()).takeIf { it >= 0 } ?: (meses.size - 1)
    val pager = rememberPagerState(initialPage = inicial, pageCount = { meses.size })
    val scope = rememberCoroutineScope()
    var borrarMes by remember { mutableStateOf<String?>(null) }

    // Saltar al mes indicado (nuevo mes o importación)
    LaunchedEffect(vm.mesEnfocado, meses) {
        val objetivo = vm.mesEnfocado ?: return@LaunchedEffect
        val i = meses.indexOf(objetivo)
        if (i >= 0) {
            pager.animateScrollToPage(i)
            vm.mesEnfocado = null
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(meses) { i, m ->
                ChipFiltro(Meses.cortoConAnio(m), pager.currentPage == i, MaterialTheme.colorScheme.primary) {
                    scope.launch { pager.animateScrollToPage(i) }
                }
            }
            item {
                AssistChip(
                    onClick = { vm.nuevoMes() },
                    label = { Text("Nuevo mes") },
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) }
                )
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { meses[it] }) { pagina ->
            PaginaMes(
                mes = meses[pagina],
                d = d,
                onEditar = onEditar,
                onBorrar = { vm.borrarCategoria(it) },
                onNueva = onNueva,
                onCapital = onCapital,
                onBorrarMes = { borrarMes = meses[pagina] }
            )
        }
    }

    borrarMes?.let { m ->
        AlertDialog(
            onDismissRequest = { borrarMes = null },
            title = { Text("¿Eliminar ${Meses.etiqueta(m)}?") },
            text = { Text("Se borrarán todas las categorías y el capital de ese mes. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { vm.borrarMes(m); borrarMes = null }) { Text("Eliminar", color = EstadoRojo) }
            },
            dismissButton = { TextButton(onClick = { borrarMes = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun PaginaMes(
    mes: String,
    d: DatosTraker,
    onEditar: (GastoCategoria) -> Unit,
    onBorrar: (GastoCategoria) -> Unit,
    onNueva: (String, SeccionTraker) -> Unit,
    onCapital: (String, SeccionTraker) -> Unit,
    onBorrarMes: () -> Unit
) {
    val esTablet = LocalConfiguration.current.screenWidthDp >= 600
    val cats = d.delMes(mes)
    val secciones = d.secciones(mes).ifEmpty { listOf(SeccionTraker.PERSONAL) }
    val plan = cats.sumOf { it.plan }
    val real = cats.sumOf { it.real }
    val capital = secciones.mapNotNull { d.capital(mes, it) }.let { if (it.isEmpty()) null else it.sum() }
    val excedidos = cats.count { it.estado() == EstadoGasto.EXCEDIDO }
    val faltantes = SeccionTraker.entries.filter { it !in secciones }

    val resumen: @Composable () -> Unit = {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Meses.etiqueta(mes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onBorrarMes) {
                        Icon(Icons.Default.DeleteSweep, "Eliminar mes", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwipeLeft, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Desliza para cambiar de mes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    Totales("Presupuesto", Dinero.fmt(plan), MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                    Totales("Gasto real", Dinero.fmt(real), MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                    val disponible = (capital ?: plan) - real
                    Totales(
                        if (capital != null) "Saldo" else "Disponible",
                        Dinero.fmt(disponible),
                        if (disponible >= 0) EstadoVerde else EstadoRojo,
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                val frac = if (plan > 0) (real / plan).toFloat() else 0f
                BarraProgreso(frac, if (frac > 1f) EstadoRojo else if (frac >= 0.9f) Color(0xFFF2C12E) else EstadoVerde)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (plan > 0) "Usado ${(real / plan * 100).toInt()}% del presupuesto" else "Sin presupuesto definido",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (excedidos > 0) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(EstadoRojo).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("$excedidos categoría(s) pasaron su presupuesto", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    val graficos: @Composable () -> Unit = {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("¿En qué se fue el dinero?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                GraficoDona(cats.map { (it.categoria + (if (secciones.size > 1) " · ${it.seccion.etiqueta.take(8)}" else "")) to it.real })
                Spacer(Modifier.height(16.dp))
                Text("Presupuesto vs gasto por sección", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                GraficoBarras(
                    etiquetas = secciones.map { it.etiqueta.replace("Gastos ", "").replaceFirstChar { c -> c.uppercase() } },
                    series = listOf(
                        Serie("Presupuesto", AzulKenis.copy(alpha = 0.45f), secciones.map { s -> cats.filter { it.seccion == s }.sumOf { it.plan } }),
                        Serie("Gasto real", RosaKenis, secciones.map { s -> cats.filter { it.seccion == s }.sumOf { it.real } })
                    ),
                    alto = 170.dp
                )
            }
        }
    }

    val tarjetas: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            secciones.forEach { s ->
                key(s) {
                    TarjetaSeccion(
                        seccion = s,
                        categorias = cats.filter { it.seccion == s },
                        capital = d.capital(mes, s),
                        onEditarCategoria = onEditar,
                        onBorrarCategoria = onBorrar,
                        onAgregar = { onNueva(mes, s) },
                        onEditarCapital = { onCapital(mes, s) }
                    )
                }
            }
            if (faltantes.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    faltantes.forEach { s ->
                        AssistChip(
                            onClick = { onNueva(mes, s) },
                            label = { Text("Agregar ${s.etiqueta}") },
                            leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }
    }

    if (esTablet) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { resumen(); tarjetas(); Spacer(Modifier.height(24.dp)) }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)
            ) { graficos(); Spacer(Modifier.height(24.dp)) }
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { resumen() }
            item { tarjetas() }
            item { graficos() }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ====================================================================== Vista REGISTROS (búsqueda y filtros)

@Composable
private fun VistaRegistros(
    d: DatosTraker,
    onEditar: (GastoCategoria) -> Unit,
    onBorrar: (GastoCategoria) -> Unit,
    onNueva: () -> Unit
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var fMes by rememberSaveable { mutableStateOf<String?>(null) }
    var fSeccion by rememberSaveable { mutableStateOf<String?>(null) }
    var fEstado by rememberSaveable { mutableStateOf<String?>(null) }
    var fCategoria by rememberSaveable { mutableStateOf<String?>(null) }
    var verFiltros by rememberSaveable { mutableStateOf(false) }

    val nombresCategorias = remember(d.categorias) { d.categorias.map { it.categoria }.distinct().sorted() }
    val filtrados = remember(d.categorias, busqueda, fMes, fSeccion, fEstado, fCategoria) {
        val q = busqueda.trim()
        d.categorias.filter { c ->
            (fMes == null || c.mes == fMes) &&
                (fSeccion == null || c.seccion.name == fSeccion) &&
                (fEstado == null || c.estado().name == fEstado) &&
                (fCategoria == null || c.categoria == fCategoria) &&
                (q.isEmpty() || c.categoria.contains(q, true) || Meses.etiqueta(c.mes).contains(q, true) || c.seccion.etiqueta.contains(q, true))
        }.sortedWith(compareByDescending<GastoCategoria> { it.mes }.thenBy { it.seccion }.thenBy { it.id })
    }
    val filtrosActivos = listOfNotNull(fMes, fSeccion, fEstado, fCategoria).size

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar categoría, mes…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onNueva,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White),
                    modifier = Modifier.height(54.dp)
                ) { Icon(Icons.Default.Add, "Nuevo registro") }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${filtrados.size} registro(s) · gastado ${Dinero.fmt(filtrados.sumOf { it.real })}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { verFiltros = !verFiltros }) {
                        Text(if (verFiltros) "Ocultar filtros" else "Filtros" + if (filtrosActivos > 0) " ($filtrosActivos)" else "")
                    }
                }
                // Estado siempre visible (lo más usado)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChipFiltro("Todos", fEstado == null, MaterialTheme.colorScheme.primary) { fEstado = null }
                    EstadoGasto.entries.forEach { e ->
                        ChipFiltro(e.etiqueta, fEstado == e.name, e.color(), e.colorTexto()) {
                            fEstado = if (fEstado == e.name) null else e.name
                        }
                    }
                }
                if (verFiltros) {
                    Text("Mes", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChipFiltro("Todos", fMes == null, MaterialTheme.colorScheme.primary) { fMes = null }
                        d.meses.reversed().forEach { m ->
                            ChipFiltro(Meses.cortoConAnio(m), fMes == m, MaterialTheme.colorScheme.primary) {
                                fMes = if (fMes == m) null else m
                            }
                        }
                    }
                    Text("Sección", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChipFiltro("Todas", fSeccion == null, MaterialTheme.colorScheme.secondary) { fSeccion = null }
                        SeccionTraker.entries.forEach { s ->
                            ChipFiltro(s.etiqueta, fSeccion == s.name, MaterialTheme.colorScheme.secondary) {
                                fSeccion = if (fSeccion == s.name) null else s.name
                            }
                        }
                    }
                    Text("Categoría", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChipFiltro("Todas", fCategoria == null, MaterialTheme.colorScheme.tertiary) { fCategoria = null }
                        nombresCategorias.forEach { n ->
                            ChipFiltro(n, fCategoria == n, MaterialTheme.colorScheme.tertiary) {
                                fCategoria = if (fCategoria == n) null else n
                            }
                        }
                    }
                    if (filtrosActivos > 0) {
                        TextButton(onClick = { fMes = null; fSeccion = null; fEstado = null; fCategoria = null }) {
                            Text("Limpiar filtros")
                        }
                    }
                }
            }
        }
        if (filtrados.isEmpty()) {
            item {
                Text(
                    "Ningún registro coincide",
                    Modifier.fillMaxWidth().padding(32.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(filtrados, key = { it.id }) { c ->
            FilaCategoria(c, onClick = { onEditar(c) }, onBorrar = { onBorrar(c) }, mostrarMes = true)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ====================================================================== Vista GRÁFICOS

@Composable
private fun VistaGraficos(d: DatosTraker) {
    val meses = d.meses
    val etiquetas = meses.map { Meses.corto(it) }
    var seccionDona by rememberSaveable { mutableStateOf<String?>(null) }
    val esTablet = LocalConfiguration.current.screenWidthDp >= 600

    val porSeccion = SeccionTraker.entries.filter { s -> d.categorias.any { it.seccion == s } }
    val seriesLineas = porSeccion.map { s ->
        Serie(s.etiqueta.replace("Gastos ", "").replaceFirstChar { it.uppercase() }, COLOR_SECCION.getValue(s),
            meses.map { m -> d.categorias.filter { it.mes == m && it.seccion == s }.sumOf { it.real } })
    }
    val totalPorMes = meses.map { m -> d.categorias.filter { it.mes == m }.sumOf { it.real } }
    val planPorMes = meses.map { m -> d.categorias.filter { it.mes == m }.sumOf { it.plan } }
    val ahorros = d.listas.filter { it.lista == TipoLista.AHORRO && it.monto != null }

    val mesMayor = meses.indices.maxByOrNull { totalPorMes[it] }
    val conGasto = totalPorMes.filter { it > 0 }
    val promedio = if (conGasto.isEmpty()) 0.0 else conGasto.average()
    val topCategoria = d.categorias.groupBy { it.categoria }.mapValues { e -> e.value.sumOf { it.real } }.maxByOrNull { it.value }

    val tarjeta: @Composable (String, @Composable () -> Unit) -> Unit = { titulo, contenido ->
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(titulo, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(10.dp))
                contenido()
            }
        }
    }

    val bloques: List<@Composable () -> Unit> = listOf<@Composable () -> Unit>(
        {
            // Datos clave
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatoClave("Promedio mensual", Dinero.fmt(promedio), Modifier.weight(1f))
                DatoClave(
                    "Mes con más gasto",
                    mesMayor?.takeIf { totalPorMes[it] > 0 }?.let { "${Meses.etiqueta(meses[it])}\n${Dinero.fmt(totalPorMes[it])}" } ?: "—",
                    Modifier.weight(1f)
                )
                DatoClave(
                    "Categoría top",
                    topCategoria?.takeIf { it.value > 0 }?.let { "${it.key}\n${Dinero.fmt(it.value)}" } ?: "—",
                    Modifier.weight(1f)
                )
            }
        },
        { tarjeta("Gasto real por mes") { GraficoLineas(etiquetas, seriesLineas) } },
        {
            tarjeta("Presupuesto vs gasto real (total)") {
                GraficoBarras(
                    etiquetas,
                    listOf(
                        Serie("Presupuesto", AzulKenis.copy(alpha = 0.45f), planPorMes),
                        Serie("Gasto real", RosaKenis, totalPorMes)
                    )
                )
            }
        },
        {
            tarjeta("Distribución por categoría (todos los meses)") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChipFiltro("Todas", seccionDona == null, MaterialTheme.colorScheme.primary) { seccionDona = null }
                    porSeccion.forEach { s ->
                        ChipFiltro(s.etiqueta, seccionDona == s.name, MaterialTheme.colorScheme.primary) {
                            seccionDona = if (seccionDona == s.name) null else s.name
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                val datosDona = d.categorias
                    .filter { seccionDona == null || it.seccion.name == seccionDona }
                    .groupBy { it.categoria }
                    .map { (k, v) -> k to v.sumOf { it.real } }
                GraficoDona(datosDona)
            }
        },
        {
            if (ahorros.isNotEmpty()) {
                tarjeta("Ahorros") {
                    GraficoBarras(
                        ahorros.map { it.nombre.take(3) },
                        listOf(Serie("Ahorro", EstadoVerde, ahorros.map { it.monto ?: 0.0 }))
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Total ahorrado: ${Dinero.fmt(ahorros.sumOf { it.monto ?: 0.0 })}", fontWeight = FontWeight.Bold)
                }
            }
        }
    )

    if (esTablet) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                bloques[0](); bloques[1](); bloques[2]()
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                bloques[3](); bloques[4]()
            }
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(bloques.size) { i -> bloques[i]() }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun DatoClave(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 3)
        }
    }
}

// ====================================================================== Vista LISTAS

@Composable
private fun VistaListas(
    d: DatosTraker,
    onEditar: (ItemLista) -> Unit,
    onNuevo: (TipoLista) -> Unit,
    onBorrar: (ItemLista) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TipoLista.entries.forEach { tipo ->
            val registros = d.listas.filter { it.lista == tipo }
            item(key = tipo.name) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${tipo.emoji} ${tipo.etiqueta}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            if (tipo.conMonto) {
                                Text(Dinero.fmt(registros.sumOf { it.monto ?: 0.0 }), fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { onNuevo(tipo) }) { Icon(Icons.Default.Add, "Agregar") }
                        }
                        if (registros.isEmpty()) {
                            Text("Sin registros", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        registros.forEach { it2 ->
                            key(it2.id) {
                                DeslizarParaBorrar("Se eliminará \"${it2.nombre}\".", { onBorrar(it2) }) {
                                    Surface(
                                        onClick = { onEditar(it2) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(it2.nombre, Modifier.weight(1f))
                                            if (tipo.conMonto) Text(Dinero.fmt(it2.monto), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ====================================================================== Diálogo de importación

@Composable
private fun DialogoImportacion(v: TrakerViewModel.VistaPrevia, datosActuales: DatosTraker?, vm: TrakerViewModel) {
    val r = v.resultado
    AlertDialog(
        onDismissRequest = { vm.cancelarImportacion() },
        icon = { Icon(Icons.Default.UploadFile, null) },
        title = { Text("Importar Excel") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(v.archivo, fontWeight = FontWeight.SemiBold)
                Text("Se encontró:")
                Text("• ${r.meses.size} mes(es): ${r.meses.joinToString { Meses.corto(it) }}")
                SeccionTraker.entries.forEach { s ->
                    val n = r.categorias.count { it.seccion == s }
                    if (n > 0) Text("• ${s.etiqueta}: $n categorías")
                }
                TipoLista.entries.forEach { t ->
                    val n = r.items.count { it.lista == t }
                    if (n > 0) Text("• ${t.etiqueta}: $n")
                }
                Spacer(Modifier.height(4.dp))
                Text("¿De qué año son los meses?", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.cambiarAnio(v.anio - 1) }) { Icon(Icons.Default.Remove, "Año anterior") }
                    Text("${v.anio}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { vm.cambiarAnio(v.anio + 1) }) { Icon(Icons.Default.Add, "Año siguiente") }
                }
                if (datosActuales != null && !datosActuales.vacio) {
                    Surface(color = EstadoRojo.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp)) {
                        Text(
                            "⚠️ Esto reemplaza los datos actuales del Traker. Te avisaremos por notificación qué cambió.",
                            color = EstadoRojo,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.confirmarImportacion() }) { Text("Importar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = { vm.cancelarImportacion() }) { Text("Cancelar") } }
    )
}
