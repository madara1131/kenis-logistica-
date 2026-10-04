package com.kenisshop.logistica.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {
    @Query("SELECT * FROM pedidos ORDER BY fechaPedido DESC, id DESC")
    fun observarTodos(): Flow<List<Pedido>>

    @Query("SELECT * FROM pedidos WHERE id = :id")
    fun observarPorId(id: Long): Flow<Pedido?>

    @Query("SELECT * FROM pedidos WHERE fechaIngreso IS NULL ORDER BY fechaPedido ASC")
    suspend fun sinIngreso(): List<Pedido>

    @Query("SELECT COUNT(*) FROM pedidos WHERE codigo = :codigo")
    suspend fun contarCodigo(codigo: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(pedido: Pedido): Long

    @Query("UPDATE pedidos SET fechaMiami = :fecha WHERE id = :id")
    suspend fun marcarMiami(id: Long, fecha: Long)

    @Query("UPDATE pedidos SET pesoLibras = :peso, tarifaLibra = :tarifa WHERE id = :id")
    suspend fun actualizarPeso(id: Long, peso: Double?, tarifa: Double?)

    @Query("UPDATE pedidos SET cliente = :cliente WHERE id = :id")
    suspend fun actualizarCliente(id: Long, cliente: String?)

    @Delete
    suspend fun borrar(pedido: Pedido)

    @Query("UPDATE pedidos SET fechaIngreso = :fecha WHERE id = :id")
    suspend fun actualizarIngreso(id: Long, fecha: Long)
}
