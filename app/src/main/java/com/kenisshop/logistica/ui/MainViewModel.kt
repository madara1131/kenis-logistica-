package com.kenisshop.logistica.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenisshop.logistica.data.AppDatabase
import com.kenisshop.logistica.data.AuthRepository
import com.kenisshop.logistica.data.Pedido
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

    fun registrarIngreso(id: Long, fecha: Long) {
        viewModelScope.launch { dao.actualizarIngreso(id, fecha) }
    }
}
