package com.kenisshop.logistica.ui

import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.kenisshop.logistica.data.Pedido
import com.kenisshop.logistica.data.tarifaAplicada
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kenisshop.logistica.data.TipoMercaderia
import com.kenisshop.logistica.data.calcularCobro
import com.kenisshop.logistica.ui.theme.EstadoRojo
import com.kenisshop.logistica.ui.theme.VerdeGuardar
import com.kenisshop.logistica.util.Dinero
import com.kenisshop.logistica.util.Dolares
import com.kenisshop.logistica.util.numeroDe

/**
 * Peso total + dinero a pagar por libra, con el cálculo en vivo:
 * Aérea US$ 5.50/lb · Marítima US$ 2.00/lb (se puede cambiar si la tarifa cambia).
 */
@Composable
fun CalculadoraPeso(
    tipo: TipoMercaderia,
    peso: String,
    onPeso: (String) -> Unit,
    tarifa: String,
    onTarifa: (String) -> Unit
) {
    val p = numeroDe(peso)
    val t = numeroDe(tarifa)
    val total = calcularCobro(p, t)
    val pesoInvalido = peso.isNotBlank() && (p == null || p < 0)
    val tarifaInvalida = tarifa.isNotBlank() && (t == null || t < 0)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Scale, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Peso y cobro por libra", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = peso,
                onValueChange = onPeso,
                label = { Text("Peso total") },
                suffix = { Text("lb") },
                isError = pesoInvalido,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = tarifa,
                onValueChange = onTarifa,
                label = { Text("Pago por libra") },
                prefix = { Text("US$ ") },
                isError = tarifaInvalida,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Tarifa ${tipo.etiqueta.lowercase()}: ${Dolares.fmt(tipo.tarifaLibra)} por libra",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (t != tipo.tarifaLibra) {
                TextButton(onClick = { onTarifa(Dinero.editable(tipo.tarifaLibra)) }) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Usar tarifa")
                }
            }
        }
        // Resultado en tiempo real
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (total != null) VerdeGuardar.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
                .padding(14.dp)
        ) {
            Text("Dinero a pagar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                Dolares.fmt(total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = if (total != null) VerdeGuardar else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                when {
                    pesoInvalido -> "El peso no es un número válido"
                    tarifaInvalida -> "La tarifa no es un número válida"
                    total != null -> "${Dolares.libras(p)} × ${Dolares.fmt(t)} = ${Dolares.fmt(total)}"
                    else -> "Escribe el peso y se calcula solo. Si aún no lo sabes, lo agregas cuando llegue a Miami."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (pesoInvalido || tarifaInvalida) EstadoRojo else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Fila compacta para las tarjetas de la lista: "12.5 lb · US$ 68.75". */
fun textoPesoCobro(peso: Double?, total: Double?): String? =
    if (peso == null) null else "⚖️ ${Dolares.libras(peso)} · ${Dolares.fmt(total)}"

/** Diálogo para registrar o corregir el peso y la tarifa de un pedido. */
@Composable
fun DialogoPeso(p: Pedido, onGuardar: (Double?, Double) -> Unit, onCerrar: () -> Unit) {
    val context = LocalContext.current
    var peso by remember { mutableStateOf(Dinero.editable(p.pesoLibras)) }
    var tarifa by remember { mutableStateOf(Dinero.editable(p.tarifaAplicada)) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Peso de #${p.codigo}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                CalculadoraPeso(p.tipo, peso, { peso = it }, tarifa, { tarifa = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val pv = if (peso.isBlank()) null else numeroDe(peso)
                val tv = numeroDe(tarifa)
                if ((peso.isBlank() || (pv != null && pv >= 0)) && tv != null && tv >= 0) {
                    onGuardar(pv, tv)
                    Toast.makeText(context, "Peso guardado ✔", Toast.LENGTH_SHORT).show()
                    onCerrar()
                } else {
                    Toast.makeText(context, "Revisa el peso y la tarifa", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

/** Diálogo para poner o cambiar el cliente de un pedido. */
@Composable
fun DialogoCliente(p: Pedido, onGuardar: (String?) -> Unit, onCerrar: () -> Unit) {
    var texto by remember { mutableStateOf(p.cliente ?: "") }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Cliente de #${p.codigo}") },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Nombre del cliente") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
        },
        confirmButton = { TextButton(onClick = { onGuardar(texto.trim().ifEmpty { null }); onCerrar() }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

/** Confirmación antes de eliminar un pedido. */
@Composable
fun DialogoEliminarPedido(p: Pedido, onEliminar: () -> Unit, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("¿Eliminar pedido #${p.codigo}?") },
        text = { Text("Se borra el pedido y su foto. Esto no se puede deshacer.") },
        confirmButton = { TextButton(onClick = { onEliminar(); onCerrar() }) { Text("Eliminar", color = EstadoRojo) } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
