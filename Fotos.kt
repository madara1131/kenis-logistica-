package com.kenisshop.logistica.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object Fotos {
    fun nuevoArchivo(context: Context): File {
        val dir = File(context.filesDir, "fotos").apply { mkdirs() }
        return File(dir, "pedido_${System.currentTimeMillis()}.jpg")
    }

    fun uriPara(context: Context, archivo: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)

    /** Copia la imagen elegida de la galería dentro de la app, para que no se pierda si la borran de la galería. */
    fun copiarDesde(context: Context, origen: Uri): String? = try {
        val destino = nuevoArchivo(context)
        context.contentResolver.openInputStream(origen)?.use { entrada ->
            destino.outputStream().use { salida -> entrada.copyTo(salida) }
        }
        if (destino.exists() && destino.length() > 0) destino.absolutePath else null
    } catch (e: Exception) {
        null
    }
}
