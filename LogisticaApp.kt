package com.kenisshop.logistica

import android.app.Application
import com.kenisshop.logistica.notif.Notificaciones
import com.kenisshop.logistica.notif.Programador

class LogisticaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notificaciones.crearCanales(this)
        Programador.programarTodo(this)
    }
}
