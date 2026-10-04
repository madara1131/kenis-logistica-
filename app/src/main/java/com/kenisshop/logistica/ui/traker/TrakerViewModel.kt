package com.kenisshop.logistica.ui.traker

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenisshop.logistica.data.traker.AjustesTraker
import com.kenisshop.logistica.data.traker.CapitalMes
import com.kenisshop.logistica.data.traker.TipoLista
import com.kenisshop.logistica.data.traker.TrakerPrefs
import com.kenisshop.logistica.data.traker.DatosTraker
import com.kenisshop.logistica.data.traker.GastoCategoria
import com.kenisshop.logistica.data.traker.ItemLista
import com.kenisshop.logistica.data.traker.SeccionTraker
import com.kenisshop.logistica.data.traker.TrakerDatabase
import com.kenisshop.logistica.notif.Notificaciones
import com.kenisshop.logistica.notif.RecordatoriosNotas
import com.kenisshop.logistica.util.AnalizadorTraker
import com.kenisshop.logistica.util.ImportadorTraker
import com.kenisshop.logistica.util.LectorXlsx
import com.kenisshop.logistica.util.Meses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class TrakerViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = TrakerDatabase.get(app).dao()

    val datos: StateFlow<DatosTraker?> = combine(
        dao.observarCategorias(), dao.observarCapital(), dao.observarListas()
    ) { a, b, c -> DatosTraker(a, b, c) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    data class VistaPrevia(
        val archivo: String,
        val hoja: LectorXlsx.Hoja,
        val anio: Int,
        val resultado: ImportadorTraker.Resultado
    )

    var vistaPrevia by mutableStateOf<VistaPrevia?>(null)
        private set

    var trabajando by mutableStateOf(false)
        private set

    /** Mes al que la pantalla debe saltar (por ejemplo, después de crear un mes nuevo). */
    var mesEnfocado by mutableStateOf<String?>(null)

    private fun aviso(texto: String) {
        Toast.makeText(getApplication<Application>(), texto, Toast.LENGTH_SHORT).show()
    }

    private suspend fun foto(): DatosTraker = DatosTraker(dao.categorias(), dao.capitales(), dao.listas())

    /** Ejecuta un cambio y avisa por notificación si aparecieron situaciones importantes nuevas. */
    private suspend fun conAlertas(accion: suspend () -> Unit) {
        val antes = foto()
        accion()
        val despues = foto()
        val nuevas = AnalizadorTraker.nuevas(antes, despues)
        if (nuevas.isNotEmpty()) {
            Notificaciones.traker(
                getApplication<Application>(), 3002,
                if (nuevas.any { it.grave }) "Traker: ¡presupuesto en riesgo!" else "Traker: atención",
                nuevas.map { it.texto },
                urgente = nuevas.any { it.grave }
            )
        }
    }

    // ------------------------------------------------------------ Importar Excel

    private fun nombreArchivo(uri: Uri): String = try {
        getApplication<Application>().contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "Excel"
    } catch (e: Exception) {
        "Excel"
    }

    fun leerArchivo(uri: Uri) {
        viewModelScope.launch {
            trabajando = true
            try {
                val ctx = getApplication<Application>()
                val hoja = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { LectorXlsx.leer(it) }
                        ?: throw IllegalArgumentException("No se pudo abrir el archivo")
                }
                val anio = LocalDate.now().year
                val resultado = withContext(Dispatchers.Default) { ImportadorTraker.interpretar(hoja, anio) }
                if (resultado.vacio) {
                    aviso("No se encontraron datos del Traker en ese Excel")
                } else {
                    vistaPrevia = VistaPrevia(nombreArchivo(uri), hoja, anio, resultado)
                }
            } catch (e: Exception) {
                aviso(e.message ?: "No se pudo leer el Excel")
            } finally {
                trabajando = false
            }
        }
    }

    fun cambiarAnio(anio: Int) {
        val v = vistaPrevia ?: return
        vistaPrevia = v.copy(anio = anio, resultado = ImportadorTraker.interpretar(v.hoja, anio))
    }

    fun cancelarImportacion() {
        vistaPrevia = null
    }

    fun confirmarImportacion() {
        val v = vistaPrevia ?: return
        vistaPrevia = null
        viewModelScope.launch {
            trabajando = true
            try {
                val antes = foto()
                dao.reemplazarTodo(v.resultado.categorias, v.resultado.capitales, v.resultado.items)
                val despues = foto()
                notificarImportacion(antes, despues)
                mesEnfocado = despues.meses.lastOrNull()
                aviso("Excel importado ✔")
            } catch (e: Exception) {
                aviso("Error al importar: ${e.message ?: ""}")
            } finally {
                trabajando = false
            }
        }
    }

    private fun notificarImportacion(antes: DatosTraker, despues: DatosTraker) {
        val lineas = ArrayList<String>()
        if (antes.vacio) {
            lineas.add("Datos iniciales cargados: ${despues.meses.size} meses, ${despues.categorias.size} categorías")
        } else {
            val cambios = AnalizadorTraker.cambios(antes.categorias, despues.categorias)
            lineas.add(if (cambios.isEmpty()) "Sin cambios en los presupuestos" else "${cambios.size} cambio(s) detectado(s)")
            lineas.addAll(cambios)
        }
        val nuevas = AnalizadorTraker.nuevas(antes, despues)
        val graves = nuevas.count { it.grave }
        if (graves > 0) lineas.add(1.coerceAtMost(lineas.size), "⚠️ $graves alerta(s): presupuestos excedidos o saldo negativo")
        lineas.addAll(nuevas.filter { it.grave }.map { it.texto })
        Notificaciones.traker(getApplication<Application>(), 3001, "Traker: Excel importado", lineas, urgente = graves > 0)
    }

    // ------------------------------------------------------------ Categorías

    fun guardarCategoria(c: GastoCategoria) {
        viewModelScope.launch {
            try {
                val limpia = c.copy(categoria = c.categoria.trim(), actualizado = System.currentTimeMillis())
                conAlertas {
                    if (limpia.id == 0L) dao.insertarCategoria(limpia) else dao.actualizarCategoria(limpia)
                }
                aviso("Guardado ✔")
            } catch (e: SQLiteConstraintException) {
                aviso("Ya existe \"${c.categoria}\" en ese mes y sección")
            } catch (e: Exception) {
                aviso("No se pudo guardar")
            }
        }
    }

    fun borrarCategoria(c: GastoCategoria) {
        viewModelScope.launch {
            dao.borrarCategoria(c)
            aviso("Registro eliminado")
        }
    }

    fun guardarCapital(mes: String, seccion: SeccionTraker, valor: Double?) {
        viewModelScope.launch {
            conAlertas { dao.guardarCapital(CapitalMes(mes, seccion, valor)) }
            aviso("Capital actualizado ✔")
        }
    }

    // ------------------------------------------------------------ Meses

    fun nuevoMes() {
        viewModelScope.launch {
            val d = foto()
            val ultimo = d.meses.lastOrNull()
            var nuevo = if (ultimo == null) Meses.actual() else Meses.siguiente(ultimo)
            while (nuevo in d.meses) nuevo = Meses.siguiente(nuevo)
            val base = if (ultimo != null && d.delMes(ultimo).isNotEmpty()) {
                d.delMes(ultimo).map { it.copy(id = 0, mes = nuevo, gastoReal = null, actualizado = System.currentTimeMillis()) }
            } else {
                categoriasPorDefecto(nuevo)
            }
            try {
                dao.insertarCategorias(base)
                mesEnfocado = nuevo
                aviso("${Meses.etiqueta(nuevo)} creado con los mismos presupuestos")
            } catch (e: Exception) {
                aviso("No se pudo crear el mes")
            }
        }
    }

    fun empezarDesdeCero() {
        viewModelScope.launch {
            val mes = Meses.actual()
            dao.insertarCategorias(categoriasPorDefecto(mes))
            mesEnfocado = mes
        }
    }

    fun borrarMes(mes: String) {
        viewModelScope.launch {
            dao.borrarMes(mes)
            aviso("${Meses.etiqueta(mes)} eliminado")
        }
    }

    private fun categoriasPorDefecto(mes: String): List<GastoCategoria> {
        fun c(s: SeccionTraker, n: String) = GastoCategoria(mes = mes, seccion = s, categoria = n, presupuesto = null, gastoReal = null)
        return listOf(
            c(SeccionTraker.PERSONAL, "🏠 Casa"), c(SeccionTraker.PERSONAL, "🚗 Vehículo"),
            c(SeccionTraker.PERSONAL, "🎉 Diversión"), c(SeccionTraker.PERSONAL, "🔧 Extras"),
            c(SeccionTraker.KENISSHOP, "🛍️ Negocio"), c(SeccionTraker.KENISSHOP, "📱 Facebook Ads"),
            c(SeccionTraker.KENISSHOP, "📢 Publicidad"), c(SeccionTraker.KENISSHOP, "🔧 Extras"),
            c(SeccionTraker.KENISSHOP, "📲 Recargas"),
            c(SeccionTraker.NECESARIOS, "🛒 Supermercado"), c(SeccionTraker.NECESARIOS, "📺 Suscripciones"),
            c(SeccionTraker.NECESARIOS, "💳 Pagos Tarjetas"), c(SeccionTraker.NECESARIOS, "🔧 Gym")
        )
    }

    // ------------------------------------------------------------ Listas, finanzas y libreta

    private val prefs = TrakerPrefs(app)

    var ajustes by mutableStateOf(prefs.leer())
        private set

    /** Ejecuta un cambio en las listas y avisa si se pasó un límite o se cumplió una meta. */
    private suspend fun conAlertasListas(nuevosAjustes: AjustesTraker? = null, accion: suspend () -> Unit) {
        val antes = dao.listas()
        val ajAntes = ajustes
        accion()
        if (nuevosAjustes != null) {
            prefs.guardar(nuevosAjustes)
            ajustes = nuevosAjustes
        }
        val nuevas = AnalizadorTraker.nuevasListas(antes, ajAntes, dao.listas(), ajustes)
        if (nuevas.isNotEmpty()) {
            Notificaciones.traker(
                getApplication<Application>(), 3003,
                if (nuevas.any { it.grave }) "Traker: ¡límite superado!" else "Traker: buenas noticias",
                nuevas.map { it.texto },
                urgente = nuevas.any { it.grave }
            )
        }
    }

    fun guardarItem(i: ItemLista) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val limpio = i.copy(nombre = i.nombre.trim(), actualizado = System.currentTimeMillis())
            conAlertasListas {
                val id = if (limpio.id == 0L) dao.insertarItem(limpio) else {
                    dao.actualizarItem(limpio); limpio.id
                }
                if (limpio.lista == TipoLista.NOTA) {
                    val r = limpio.recordatorio
                    if (r != null && r > System.currentTimeMillis()) RecordatoriosNotas.programar(ctx, id, r)
                    else RecordatoriosNotas.cancelar(ctx, id)
                }
            }
            val r = limpio.recordatorio
            aviso(
                if (limpio.lista == TipoLista.NOTA && r != null && r > System.currentTimeMillis())
                    "Nota guardada ✔ · te avisaremos el ${java.time.Instant.ofEpochMilli(r).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM 'a las' HH:mm"))}"
                else "Guardado ✔"
            )
        }
    }

    fun borrarItem(i: ItemLista) {
        viewModelScope.launch {
            if (i.lista == TipoLista.NOTA) RecordatoriosNotas.cancelar(getApplication<Application>(), i.id)
            dao.borrarItem(i)
            aviso("Registro eliminado")
        }
    }

    /** Guarda el nuevo orden después de arrastrar y soltar. */
    fun reordenar(lista: List<ItemLista>) {
        viewModelScope.launch {
            dao.actualizarItems(lista.mapIndexed { idx, it -> it.copy(orden = idx) })
        }
    }

    fun abonar(deuda: ItemLista, monto: Double) {
        viewModelScope.launch {
            conAlertasListas {
                val nuevo = ((deuda.pagado ?: 0.0) + monto).coerceAtMost(deuda.monto ?: Double.MAX_VALUE)
                dao.actualizarItem(deuda.copy(pagado = nuevo, actualizado = System.currentTimeMillis()))
            }
            aviso("Abono registrado ✔")
        }
    }

    fun alternarFijada(nota: ItemLista) {
        viewModelScope.launch {
            dao.actualizarItem(nota.copy(fijada = !nota.fijada))
            aviso(if (nota.fijada) "Nota desfijada" else "📌 Nota fijada arriba")
        }
    }

    fun guardarAjustes(nuevos: AjustesTraker) {
        viewModelScope.launch {
            conAlertasListas(nuevosAjustes = nuevos) { }
        }
    }

    fun agregarPagina(nombre: String) {
        val n = nombre.trim()
        if (n.isEmpty() || ajustes.paginas.any { it.equals(n, true) }) return
        guardarAjustes(ajustes.copy(paginas = ajustes.paginas + n))
    }

    /** Elimina la página; sus notas pasan a "General". */
    fun borrarPagina(nombre: String) {
        if (nombre == "General") return
        viewModelScope.launch {
            val notas = dao.listas().filter { it.lista == TipoLista.NOTA && it.pagina == nombre }
            if (notas.isNotEmpty()) dao.actualizarItems(notas.map { it.copy(pagina = null) })
            val nuevos = ajustes.copy(paginas = ajustes.paginas.filter { it != nombre })
            prefs.guardar(nuevos)
            ajustes = nuevos
            aviso("Página eliminada · sus notas pasaron a General")
        }
    }
}
