package com.kenisshop.logistica.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenisshop.logistica.data.AppDatabase
import com.kenisshop.logistica.data.AuthRepository
import com.kenisshop.logistica.data.EstadoPedido
import com.kenisshop.logistica.data.FiltroBusqueda
import com.kenisshop.logistica.data.OrdenBusqueda
import com.kenisshop.logistica.data.Pedido
import com.kenisshop.logistica.data.TipoMercaderia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).pedidoDao()
    private val auth = AuthRepository(app)

    var sesionIniciada by mutableStateOf(false)
        private set

    val pedidos: StateFlow<List<Pedido>> = dao.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun iniciarSesion(usuario: String, clave: String): Boolean {
        val ok = auth.validar(usuario, clave)
        sesionIniciada = ok
        return ok
    }

    fun cerrarSesion() {
        sesionIniciada = false
    }

    fun observarPedido(id: Long): Flow<Pedido?> = dao.observarPorId(id)

    /** Devuelve el id del pedido guardado, o un error con mensaje claro. */
    suspend fun guardar(pedido: Pedido): Result<Long> {
        if (dao.contarCodigo(pedido.codigo) > 0) {
            return Result.failure(IllegalStateException("Ya existe un pedido con el código ${pedido.codigo}"))
        }
        return try {
            Result.success(dao.insertar(pedido))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("No se pudo guardar el pedido"))
        }
    }

    fun marcarLlegadaMiami(id: Long, fecha: Long) {
        viewModelScope.launch { dao.marcarMiami(id, fecha) }
    }

    fun actualizarPeso(id: Long, peso: Double?, tarifa: Double?) {
        viewModelScope.launch { dao.actualizarPeso(id, peso, tarifa) }
    }

    fun registrarIngreso(id: Long, fecha: Long) {
        viewModelScope.launch { dao.actualizarIngreso(id, fecha) }
    }

    // ------------------------------------------------ Búsqueda (pestaña independiente)

    private val prefsBusqueda = app.getSharedPreferences("busqueda_pedidos", android.content.Context.MODE_PRIVATE)

    /** La búsqueda se guarda: al volver a la pestaña se ven los mismos resultados. */
    var filtro by mutableStateOf(leerFiltro())
        private set

    fun cambiarFiltro(nuevo: FiltroBusqueda) {
        filtro = nuevo
        prefsBusqueda.edit().apply {
            putString("texto", nuevo.texto)
            putString("tipo", nuevo.tipo?.name)
            if (nuevo.desde != null) putLong("desde", nuevo.desde) else remove("desde")
            if (nuevo.hasta != null) putLong("hasta", nuevo.hasta) else remove("hasta")
            putString("empresa", nuevo.empresa)
            putString("estado", nuevo.estado?.name)
            putString("pesoMin", nuevo.pesoMin?.toString())
            putString("pesoMax", nuevo.pesoMax?.toString())
            putBoolean("sinPeso", nuevo.soloSinPeso)
            putString("orden", nuevo.orden.name)
            putBoolean("desc", nuevo.descendente)
        }.apply()
    }

    private fun leerFiltro(): FiltroBusqueda = try {
        val p = prefsBusqueda
        FiltroBusqueda(
            texto = p.getString("texto", "") ?: "",
            tipo = p.getString("tipo", null)?.let { TipoMercaderia.valueOf(it) },
            desde = if (p.contains("desde")) p.getLong("desde", 0) else null,
            hasta = if (p.contains("hasta")) p.getLong("hasta", 0) else null,
            empresa = p.getString("empresa", null),
            estado = p.getString("estado", null)?.let { EstadoPedido.valueOf(it) },
            pesoMin = p.getString("pesoMin", null)?.toDoubleOrNull(),
            pesoMax = p.getString("pesoMax", null)?.toDoubleOrNull(),
            soloSinPeso = p.getBoolean("sinPeso", false),
            orden = p.getString("orden", null)?.let { OrdenBusqueda.valueOf(it) } ?: OrdenBusqueda.FECHA,
            descendente = p.getBoolean("desc", true)
        )
    } catch (e: Exception) {
        FiltroBusqueda()
    }

    // ------------------------------------------------ Acciones rápidas

    fun actualizarCliente(id: Long, cliente: String?) {
        viewModelScope.launch { dao.actualizarCliente(id, cliente?.trim()?.ifEmpty { null }) }
    }

    /** Elimina el pedido y su foto. */
    fun eliminarPedido(p: Pedido) {
        viewModelScope.launch {
            dao.borrar(p)
            p.fotoPath?.let { runCatching { java.io.File(it).delete() } }
            androidx.core.app.NotificationManagerCompat.from(getApplication<Application>()).cancel(1000 + p.id.toInt())
        }
    }
}
