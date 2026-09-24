@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.kenisshop.logistica.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kenisshop.logistica.data.EMPRESAS_ENVIO
import com.kenisshop.logistica.data.Pedido
import com.kenisshop.logistica.data.TipoMercaderia
import com.kenisshop.logistica.ui.theme.EstadoRojo
import com.kenisshop.logistica.ui.theme.VerdeGuardar
import com.kenisshop.logistica.ui.theme.colorCabecera
import com.kenisshop.logistica.util.Fechas
import com.kenisshop.logistica.util.Fotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val ORIGENES_RAPIDOS = listOf("China", "EE.UU.", "Shein", "Amazon", "Temu", "AliExpress")

@Composable
fun FormularioPedido(
    tipo: TipoMercaderia,
    vm: MainViewModel,
    mostrarVolver: Boolean,
    onCerrar: () -> Unit,
    onGuardado: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var codigo by rememberSaveable { mutableStateOf("") }
    var empresa by rememberSaveable { mutableStateOf<String?>(null) }
    var origen by rememberSaveable { mutableStateOf("") }
    var marca by rememberSaveable { mutableStateOf("") }
    var fechaPedido by rememberSaveable { mutableStateOf<Long?>(Fechas.hoy()) }
    var fechaMiami by rememberSaveable { mutableStateOf<Long?>(null) }
    var fechaIngreso by rememberSaveable { mutableStateOf<Long?>(null) }
    var fotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var archivoCamara by rememberSaveable { mutableStateOf<String?>(null) }
    var mostrarErrores by rememberSaveable { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) fotoPath = archivoCamara
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val ruta = withContext(Dispatchers.IO) { Fotos.copiarDesde(context, uri) }
                if (ruta != null) fotoPath = ruta
                else Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun limpiar() {
        codigo = ""; empresa = null; origen = ""; marca = ""
        fechaPedido = Fechas.hoy(); fechaMiami = null; fechaIngreso = null
        fotoPath = null; archivoCamara = null; mostrarErrores = false; mensajeError = null
    }

    fun validar(): String? {
        val fp = fechaPedido
        val fm = fechaMiami
        val fi = fechaIngreso
        return when {
            codigo.isBlank() -> "Ingrese el código de pedido"
            empresa == null -> "Seleccione la empresa de envío al casillero en Miami"
            origen.isBlank() -> "Ingrese el origen de la mercadería"
            marca.isBlank() -> "Ingrese la marca de ingreso"
            fp == null -> "Seleccione la fecha del pedido"
            fotoPath == null -> "Agregue la foto del pedido (Tomar foto o Subir imagen)"
            fm != null && fm < fp -> "La llegada a Miami no puede ser antes del pedido"
            fi != null && fi < fp -> "La fecha de ingreso no puede ser antes del pedido"
            else -> null
        }
    }

    fun guardar() {
        mostrarErrores = true
        val error = validar()
        if (error != null) {
            mensajeError = error
            return
        }
        guardando = true
        val pedido = Pedido(
            codigo = codigo.trim().uppercase(),
            tipo = tipo,
            fotoPath = fotoPath,
            marcaIngreso = marca.trim(),
            origen = origen.trim(),
            empresaEnvio = empresa!!,
            fechaPedido = fechaPedido!!,
            fechaMiami = fechaMiami,
            fechaIngreso = fechaIngreso
        )
        scope.launch {
            val resultado = vm.guardar(pedido)
            guardando = false
            resultado.onSuccess { id ->
                Toast.makeText(context, "Pedido #${pedido.codigo} guardado", Toast.LENGTH_SHORT).show()
                limpiar()
                onGuardado(id)
            }.onFailure {
                mensajeError = it.message ?: "No se pudo guardar"
            }
        }
    }

    val estadoPrevio = Pedido(
        codigo = "", tipo = tipo, fotoPath = null, marcaIngreso = "", origen = "", empresaEnvio = "",
        fechaPedido = fechaPedido ?: Fechas.hoy(), fechaMiami = fechaMiami, fechaIngreso = fechaIngreso
    ).estado()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(3.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Encabezado azul
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
                Column(Modifier.weight(1f)) {
                    Text("Registrar Pedido", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Mercadería ${tipo.etiqueta}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
                EstadoBadge(estadoPrevio, Modifier.padding(end = 8.dp))
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Etapa 1 · Realización del pedido",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it.uppercase(); mensajeError = null },
                    label = { Text("Código de pedido *") },
                    placeholder = { Text("Ej: A12345") },
                    singleLine = true,
                    isError = mostrarErrores && codigo.isBlank(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        "Empresa de envío al casillero en Miami *",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mostrarErrores && empresa == null) EstadoRojo else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        EMPRESAS_ENVIO.forEach { emp ->
                            ChipFiltro(emp, empresa == emp, MaterialTheme.colorScheme.primary) {
                                empresa = emp; mensajeError = null
                            }
                        }
                    }
                }

                Column {
                    OutlinedTextField(
                        value = origen,
                        onValueChange = { origen = it; mensajeError = null },
                        label = { Text("Origen de la mercadería (país / empresa) *") },
                        singleLine = true,
                        isError = mostrarErrores && origen.isBlank(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ORIGENES_RAPIDOS.forEach { o ->
                            SuggestionChip(onClick = { origen = o; mensajeError = null }, label = { Text(o) })
                        }
                    }
                }

                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it; mensajeError = null },
                    label = { Text("Marca de ingreso *") },
                    singleLine = true,
                    isError = mostrarErrores && marca.isBlank(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )

                CampoFecha(
                    etiqueta = "Fecha de realización del pedido *",
                    valor = fechaPedido,
                    onCambio = { if (it != null) fechaPedido = it; mensajeError = null },
                    error = mostrarErrores && fechaPedido == null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Foto
                Text(
                    "Foto del pedido *",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (mostrarErrores && fotoPath == null) EstadoRojo else MaterialTheme.colorScheme.onSurfaceVariant
                )
                val foto = fotoPath
                if (foto != null) {
                    Box {
                        AsyncImage(
                            model = File(foto),
                            contentDescription = "Foto del pedido",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        IconButton(
                            onClick = { fotoPath = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Close, "Quitar foto", tint = Color.White)
                        }
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val archivo = Fotos.nuevoArchivo(context)
                                archivoCamara = archivo.absolutePath
                                camara.launch(Fotos.uriPara(context, archivo))
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se encontró app de cámara", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Tomar foto")
                    }
                    OutlinedButton(
                        onClick = {
                            galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Subir imagen")
                    }
                }

                HorizontalDivider()
                Text(
                    "Etapas 2 y 3 (opcional, si ya ocurrieron)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                CampoFecha(
                    etiqueta = "Llegada al casillero en Miami",
                    valor = fechaMiami,
                    onCambio = { fechaMiami = it; mensajeError = null },
                    minimo = fechaPedido,
                    permitirQuitar = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CampoFecha(
                    etiqueta = "Fecha de ingreso (en nuestras manos)",
                    valor = fechaIngreso,
                    onCambio = { fechaIngreso = it; mensajeError = null },
                    minimo = fechaPedido,
                    permitirQuitar = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "El estado se calcula solo: Pendiente → En tránsito (llegó a Miami) → Ingresado. " +
                        "Si pasan ${tipo.diasLimite} días sin ingreso queda Vencido.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                mensajeError?.let {
                    Text(
                        it,
                        color = EstadoRojo,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(EstadoRojo.copy(alpha = 0.1f))
                            .padding(12.dp)
                    )
                }

                Button(
                    onClick = { guardar() },
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeGuardar, contentColor = Color.White)
                ) {
                    if (guardando) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("GUARDAR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
