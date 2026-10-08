package com.kenisshop.logistica.data.midia

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MiDiaDao {
    // ---- Temporadas
    @Query("SELECT * FROM temporadas ORDER BY orden ASC, id ASC")
    fun observarTemporadas(): Flow<List<Temporada>>

    @Query("SELECT * FROM temporadas ORDER BY orden ASC, id ASC")
    suspend fun temporadas(): List<Temporada>

    @Insert
    suspend fun insertarTemporada(t: Temporada): Long

    @Update
    suspend fun actualizarTemporada(t: Temporada)

    @Delete
    suspend fun borrarTemporada(t: Temporada)

    // ---- Actividades
    @Query("SELECT * FROM actividades ORDER BY orden ASC, id ASC")
    fun observarActividades(): Flow<List<Actividad>>

    @Query("SELECT * FROM actividades ORDER BY orden ASC, id ASC")
    suspend fun actividades(): List<Actividad>

    @Insert
    suspend fun insertarActividad(a: Actividad): Long

    @Update
    suspend fun actualizarActividad(a: Actividad)

    // ---- Cumplimientos
    @Query("SELECT * FROM cumplimientos ORDER BY dia DESC")
    fun observarCumplimientos(): Flow<List<Cumplimiento>>

    @Query("SELECT * FROM cumplimientos")
    suspend fun cumplimientos(): List<Cumplimiento>

    /** Reemplaza lo que hubiera para esa actividad y ese día (índice único). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarCumplimiento(c: Cumplimiento)

    @Query("DELETE FROM cumplimientos WHERE actividadId = :actividadId AND dia = :dia")
    suspend fun borrarCumplimiento(actividadId: Long, dia: Long)

    // ---- Premios
    @Query("SELECT * FROM premios ORDER BY creado DESC, id DESC")
    fun observarPremios(): Flow<List<Premio>>

    @Query("SELECT * FROM premios")
    suspend fun premios(): List<Premio>

    @Insert
    suspend fun insertarPremio(p: Premio): Long

    @Update
    suspend fun actualizarPremio(p: Premio)

    @Delete
    suspend fun borrarPremio(p: Premio)
}
