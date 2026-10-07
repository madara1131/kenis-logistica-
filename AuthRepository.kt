package com.kenisshop.logistica.data

import android.content.Context
import java.security.MessageDigest

/** Credenciales del administrador guardadas localmente (la contraseña se guarda cifrada con SHA-256). */
class AuthRepository(context: Context) {
    private val prefs = context.getSharedPreferences("auth_admin", Context.MODE_PRIVATE)

    init {
        if (!prefs.contains(CLAVE_HASH)) {
            prefs.edit()
                .putString(CLAVE_USUARIO, "admin")
                .putString(CLAVE_HASH, sha256("admin"))
                .apply()
        }
    }

    fun validar(usuario: String, clave: String): Boolean =
        usuario.trim() == prefs.getString(CLAVE_USUARIO, "admin") &&
            sha256(clave) == prefs.getString(CLAVE_HASH, null)

    private fun sha256(texto: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(texto.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val CLAVE_USUARIO = "usuario"
        const val CLAVE_HASH = "hash"
    }
}
