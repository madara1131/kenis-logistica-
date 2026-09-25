package com.kenisshop.logistica.data.traker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TrakerDao {
    @Query("SELECT * FROM traker_categorias ORDER BY mes ASC, seccion ASC, id ASC")
    abstract fun observarCategorias(): Flow<List<GastoCategoria>>

    @Query("SELECT * FROM traker_capital ORDER BY mes ASC")
    abstract fun observarCapital(): Flow<List<CapitalMes>>

    @Query("SELECT * FROM traker_listas ORDER BY lista ASC, orden ASC, id ASC")
    abstract fun observarListas(): Flow<List<ItemLista>>

    @Query("SELECT * FROM traker_categorias ORDER BY mes ASC, seccion ASC, id ASC")
    abstract suspend fun categorias(): List<GastoCategoria>

    @Query("SELECT * FROM traker_capital ORDER BY mes ASC")
    abstract suspend fun capitales(): List<CapitalMes>

    @Query("SELECT * FROM traker_listas ORDER BY lista ASC, orden ASC, id ASC")
    abstract suspend fun listas(): List<ItemLista>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertarCategoria(c: GastoCategoria): Long

    @Update
    abstract suspend fun actualizarCategoria(c: GastoCategoria)

    @Delete
    abstract suspend fun borrarCategoria(c: GastoCategoria)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardarCapital(c: CapitalMes)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertarItem(i: ItemLista): Long

    @Update
    abstract suspend fun actualizarItem(i: ItemLista)

    @Delete
    abstract suspend fun borrarItem(i: ItemLista)

    @Query("DELETE FROM traker_categorias WHERE mes = :mes")
    abstract suspend fun borrarCategoriasMes(mes: String)

    @Query("DELETE FROM traker_capital WHERE mes = :mes")
    abstract suspend fun borrarCapitalMes(mes: String)

    @Query("DELETE FROM traker_categorias")
    abstract suspend fun borrarTodasCategorias()

    @Query("DELETE FROM traker_capital")
    abstract suspend fun borrarTodoCapital()

    @Query("DELETE FROM traker_listas")
    abstract suspend fun borrarTodasListas()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertarCategorias(lista: List<GastoCategoria>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertarCapitales(lista: List<CapitalMes>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertarItems(lista: List<ItemLista>)

    @Transaction
    open suspend fun reemplazarTodo(
        categorias: List<GastoCategoria>,
        capitales: List<CapitalMes>,
        items: List<ItemLista>
    ) {
        borrarTodasCategorias()
        borrarTodoCapital()
        borrarTodasListas()
        insertarCategorias(categorias)
        insertarCapitales(capitales)
        insertarItems(items)
    }

    @Transaction
    open suspend fun borrarMes(mes: String) {
        borrarCategoriasMes(mes)
        borrarCapitalMes(mes)
    }
}
