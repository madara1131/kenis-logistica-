package com.kenisshop.logistica.ui.midia

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenisshop.logistica.data.AppDatabase
import com.kenisshop.logistica.data.midia.Actividad
import com.kenisshop.logistica.data.midia.AjustesTemporadas
import com.kenisshop.logistica.data.midia.AvancePremio
import com.kenisshop.logistica.data.midia.Cumplimiento
import com.kenisshop.logistica.data.midia.EstadoActividad
import com.kenisshop.logistica.data.midia.EstadoPremio
import com.kenisshop.logistica.data.midia.MiDiaPrefs
import com.kenisshop.logistica.data.midia.Premio
import com.kenisshop.logistica.data.midia.Temporada
import com.kenisshop.logistica.data.midia.calcularAvance
import com.kenisshop.logistica.data.midia.porDesbloquear
import com.kenisshop.logistica.data.midia.textoCasi
import com.kenisshop.logistica.data.midia.textoDesbloqueo
import com.kenisshop.logistica.notif.Notificaciones
import com.kenisshop.logistica.notif.RevisorTemporadas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

class MiDiaViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).miDiaDao()
    private val prefs = MiDiaPrefs(app)

    val temporadas: StateFlow<List<Temporada>> = dao.observarTemporadas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val actividades: StateFlow<List<Actividad>> = dao.observarActividades()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val cumplimientos: StateFlow<List<Cumplimiento>> = dao.observarCumplimientos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val premios: StateFlow<List<Premio>> = dao.observarPremios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var ajustes by mutableStateOf(prefs.ajustes())
        private set

    /** Premio recién desbloqueado: la pantalla muestra la celebración. */
    var celebracion by mutableStateOf<AvancePremio?>(null)
        private set

    init {
        // Al abrir la app también se revisan las temporadas (además de la revisión diaria)
        viewModelScope.launch { runCatching { RevisorTemporadas.revisar(getApplication<Application>()) } }
    }

    private fun aviso(texto: String) {
        Toast.makeText(getApplication<Application>(), texto, Toast.LENGTH_SHORT).show()
    }

    fun cerrarCelebracion() {
        celebracion = null
    }

    // ------------------------------------------------------------ Temporadas

    fun guardarTemporada(t: Temporada) {
        viewModelScope.launch {
            if (t.id == 0L) dao.insertarTemporada(t.copy(orden = (temporadas.value.maxOfOrNull { it.orden } ?: 0) + 1))
            else dao.actualizarTemporada(t)
            aviso("Temporada guardada ✔")
        }
    }

    fun borrarTemporada(t: Temporada) {
        viewModelScope.launch {
            dao.borrarTemporada(t)
            aviso("Temporada eliminada")
        }
    }

    fun guardarAjustes(a: AjustesTemporadas) {
        prefs.guardarAjustes(a)
        ajustes = prefs.ajustes()
        aviso("Tiempos actualizados ✔")
    }

    // ------------------------------------------------------------ Actividades

    fun guardarActividad(a: Actividad) {
        viewModelScope.launch {
            if (a.id == 0L) dao.insertarActividad(a.copy(orden = (actividades.value.maxOfOrNull { it.orden } ?: 0) + 1))
            else dao.actualizarActividad(a)
            aviso("Actividad guardada ✔")
        }
    }

    /**
     * Marca la actividad en un día. PENDIENTE borra la marca.
     * Al completar, el avance pasa solo a los premios de esa actividad.
     */
    fun marcar(actividad: Actividad, dia: Long, estado: EstadoActividad) {
        viewModelScope.launch {
            if (estado == EstadoActividad.PENDIENTE) dao.borrarCumplimiento(actividad.id, dia)
            else dao.guardarCumplimiento(Cumplimiento(actividadId = actividad.id, dia = dia, estado = estado))
            revisarPremios(avisarProgresoDe = if (estado == EstadoActividad.COMPLETADA) actividad.id else null)
        }
    }

    // ------------------------------------------------------------ Premios

    private suspend fun revisarPremios(avisarProgresoDe: Long? = null, premioManual: Long? = null) {
        val ctx = getApplication<Application>()
        val hoy = LocalDate.now().toEpochDay()
        val lista = dao.premios()
        val cumplidos = dao.cumplimientos()
        val acts = dao.actividades().associateBy { it.id }

        val ganados = porDesbloquear(lista, cumplidos, hoy)
        ganados.forEach { a ->
            val p = a.premio.copy(desbloqueadoDia = hoy, progresoFinal = a.progreso)
            dao.actualizarPremio(p)
            val listo = AvancePremio(p, a.progreso, EstadoPremio.DESBLOQUEADO)
            Notificaciones.miDia(ctx, 60_000 + (p.id % 10_000).toInt(), "🏆 ¡Premio desbloqueado!", textoDesbloqueo(listo, p.actividadId?.let { acts[it] }), urgente = false)
            celebracion = listo
        }
        // "¡Vas muy bien!" solo para los premios que acaban de avanzar
        val idsGanados = ganados.map { it.premio.id }.toSet()
        lista.filter { it.id !in idsGanados }
            .filter { (avisarProgresoDe != null && it.actividadId == avisarProgresoDe) || it.id == premioManual }
            .forEach { p ->
                textoCasi(calcularAvance(p, cumplidos, hoy), p.actividadId?.let { acts[it] })?.let { texto ->
                    Notificaciones.miDia(ctx, 70_000 + (p.id % 10_000).toInt(), "🎯 ¡Vas muy bien!", texto)
                }
            }
    }

    fun guardarPremio(p: Premio) {
        viewModelScope.launch {
            if (p.id == 0L) dao.insertarPremio(p) else dao.actualizarPremio(p)
            aviso("Premio guardado ✔")
            revisarPremios()
        }
    }

    /** Suma (o resta) avance a mano. */
    fun sumarAvance(p: Premio, cantidad: Int) {
        viewModelScope.launch {
            val nuevo = (p.progresoManual + cantidad).coerceAtLeast(0)
            dao.actualizarPremio(p.copy(progresoManual = nuevo))
            revisarPremios(premioManual = if (cantidad > 0) p.id else null)
        }
    }

    fun borrarPremio(p: Premio) {
        viewModelScope.launch {
            dao.borrarPremio(p)
            p.imagenPath?.let { runCatching { File(it).delete() } }
            aviso("Premio eliminado")
        }
    }
}
