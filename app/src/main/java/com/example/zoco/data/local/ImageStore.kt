package com.example.zoco.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File

/**
 * Guarda copias propias de las fotos elegidas (galería o cámara) para poder subirlas al servidor
 * incluso más tarde, sin depender de permisos temporales de la galería, y las reduce de tamaño.
 */
class ImageStore(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, "images").apply { mkdirs() }

    /** Devuelve la URI de la copia reducida (file://). Las URLs web se devuelven tal cual; null si falla. */
    fun saveLocalCopy(uriString: String): String? {
        if (isRemote(uriString)) return uriString
        return try {
            val uri = Uri.parse(uriString)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE_PX) sample *= 2

            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            bitmap.recycle()
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo copiar la imagen: ${e.message}")
            null
        }
    }

    /** Lee los bytes de una imagen local; null si el archivo ya no existe. */
    fun readBytes(uriString: String): ByteArray? {
        return try {
            context.contentResolver.openInputStream(Uri.parse(uriString))?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer la imagen: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "ImageStore"
        private const val MAX_SIDE_PX = 1280
        private const val JPEG_QUALITY = 85

        fun isRemote(uri: String): Boolean = uri.startsWith("http://", true) || uri.startsWith("https://", true)
    }
}
