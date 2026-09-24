@file:OptIn(ExperimentalMaterial3Api::class)

package com.kenisshop.logistica.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kenisshop.logistica.R
import com.kenisshop.logistica.data.TipoMercaderia
import com.kenisshop.logistica.ui.theme.colorCabecera

private const val PANEL_NINGUNO = -1L
private const val PANEL_NUEVO = 0L

@Composable
fun MainScreen(vm: MainViewModel) {
    val todos by vm.pedidos.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    var panel by rememberSaveable { mutableStateOf(PANEL_NINGUNO) }   // -1 nada, 0 formulario, >0 detalle (id)
    var verReportes by rememberSaveable { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }

    val esTablet = LocalConfiguration.current.screenWidthDp >= 600
    val tipo = if (tab == 0) TipoMercaderia.AEREA else TipoMercaderia.MARITIMA
    val delTipo = remember(todos, tipo) { todos.filter { it.tipo == tipo } }
    val cabecera = colorCabecera()

    if (verReportes) {
        BackHandler { verReportes = false }
        ReportesScreen(todos, onVolver = { verReportes = false })
        return
    }

    BackHandler(enabled = !esTablet && panel != PANEL_NINGUNO) { panel = PANEL_NINGUNO }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painterResource(R.drawable.logo_kenis), null,
                                modifier = Modifier.size(38.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Keni's Shop", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Logística y control de mercadería",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { verReportes = true }) {
                            Icon(Icons.Default.Assessment, "Reportes")
                        }
                        IconButton(onClick = { confirmarSalida = true }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = cabecera,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = cabecera,
                    contentColor = Color.White,
                    indicator = { posiciones ->
                        if (tab < posiciones.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(posiciones[tab]),
                                height = 4.dp,
                                color = Color.White
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = tab == 0,
                        onClick = { if (tab != 0) { tab = 0; panel = PANEL_NINGUNO } },
                        text = { Text("MERCADERÍA AÉREA", fontWeight = FontWeight.Bold, maxLines = 1) },
                        icon = { Icon(Icons.Default.Flight, null) },
                        unselectedContentColor = Color.White.copy(alpha = 0.65f)
                    )
                    Tab(
                        selected = tab == 1,
                        onClick = { if (tab != 1) { tab = 1; panel = PANEL_NINGUNO } },
                        text = { Text("MERCADERÍA MARÍTIMA", fontWeight = FontWeight.Bold, maxLines = 1) },
                        icon = { Icon(Icons.Default.DirectionsBoat, null) },
                        unselectedContentColor = Color.White.copy(alpha = 0.65f)
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (esTablet) {
                // Tablet: lista a la izquierda, detalle o formulario a la derecha
                Row(Modifier.fillMaxSize()) {
                    ListaPedidos(
                        pedidos = delTipo,
                        seleccionado = panel.takeIf { it > 0 },
                        onSeleccionar = { panel = it.id },
                        onNuevo = { panel = PANEL_NUEVO },
                        modifier = Modifier.weight(0.42f).fillMaxHeight()
                    )
                    VerticalDivider()
                    Box(Modifier.weight(0.58f).fillMaxHeight()) {
                        PanelDerecho(
                            panel = panel,
                            tipo = tipo,
                            vm = vm,
                            mostrarVolver = false,
                            onCerrar = { panel = PANEL_NINGUNO },
                            onGuardado = { id -> panel = id }
                        )
                    }
                }
            } else if (panel == PANEL_NINGUNO) {
                ListaPedidos(
                    pedidos = delTipo,
                    seleccionado = null,
                    onSeleccionar = { panel = it.id },
                    onNuevo = { panel = PANEL_NUEVO },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                PanelDerecho(
                    panel = panel,
                    tipo = tipo,
                    vm = vm,
                    mostrarVolver = true,
                    onCerrar = { panel = PANEL_NINGUNO },
                    onGuardado = { panel = PANEL_NINGUNO }
                )
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Desea salir del panel de administrador?") },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; vm.cerrarSesion() }) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun PanelDerecho(
    panel: Long,
    tipo: TipoMercaderia,
    vm: MainViewModel,
    mostrarVolver: Boolean,
    onCerrar: () -> Unit,
    onGuardado: (Long) -> Unit
) {
    if (panel > 0) {
        DetallePedido(id = panel, vm = vm, mostrarVolver = mostrarVolver, onCerrar = onCerrar)
    } else {
        // En tablet, si no hay nada seleccionado se muestra el formulario de registro.
        androidx.compose.runtime.key(tipo) {
            FormularioPedido(
                tipo = tipo,
                vm = vm,
                mostrarVolver = mostrarVolver,
                onCerrar = onCerrar,
                onGuardado = onGuardado
            )
        }
    }
}
