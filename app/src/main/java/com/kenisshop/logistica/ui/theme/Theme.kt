package com.kenisshop.logistica.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kenisshop.logistica.data.EstadoPedido

val AzulKenis = Color(0xFF1E4E9A)
val AzulOscuro = Color(0xFF173E7C)
val VerdeGuardar = Color(0xFF2E9E44)
val RosaKenis = Color(0xFFD9468F)

val EstadoVerde = Color(0xFF2E9E44)
val EstadoAzul = Color(0xFF1E6FD9)
val EstadoAmarillo = Color(0xFFF2C12E)
val EstadoRojo = Color(0xFFD93B30)

fun EstadoPedido.color(): Color = when (this) {
    EstadoPedido.INGRESADO -> EstadoVerde
    EstadoPedido.EN_TRANSITO -> EstadoAzul
    EstadoPedido.PENDIENTE -> EstadoAmarillo
    EstadoPedido.VENCIDO -> EstadoRojo
}

fun EstadoPedido.colorTexto(): Color =
    if (this == EstadoPedido.PENDIENTE) Color(0xFF3A2E00) else Color.White

private val Claro = lightColorScheme(
    primary = AzulKenis,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F7),
    onPrimaryContainer = Color(0xFF0D2A5C),
    secondary = RosaKenis,
    onSecondary = Color.White,
    tertiary = VerdeGuardar,
    background = Color(0xFFF3F6FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EDF5),
    onSurfaceVariant = Color(0xFF4A5568),
    error = EstadoRojo
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF8FB4F5),
    onPrimary = Color(0xFF0B2552),
    primaryContainer = Color(0xFF1E3A6B),
    onPrimaryContainer = Color(0xFFDCE6F7),
    secondary = Color(0xFFF08BBE),
    tertiary = Color(0xFF6FD48A),
    background = Color(0xFF10141B),
    surface = Color(0xFF1A2029),
    surfaceVariant = Color(0xFF252D39),
    onSurfaceVariant = Color(0xFFB8C2D1),
    error = Color(0xFFFF8A80)
)

@Composable
fun colorCabecera(): Color = if (isSystemInDarkTheme()) Color(0xFF15305E) else AzulKenis

@Composable
fun LogisticaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro,
        content = content
    )
}
