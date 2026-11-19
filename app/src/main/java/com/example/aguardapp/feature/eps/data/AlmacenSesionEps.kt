package com.example.aguardapp.feature.eps.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.gson.Gson
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** La clave EPS nunca se persiste. Solo la sesión, cifrada y excluida de copias de seguridad. */
internal class AlmacenSesionEps(context: Context) {
    private val archivo = File(context.noBackupFilesDir, "eps-sesion.enc")
    private val gson = Gson()

    fun leer(): SesionEps? = runCatching {
        val atomico = android.util.AtomicFile(archivo)
        if (!archivo.exists() && !File(archivo.path + ".bak").exists()) return null
        val bytes = atomico.openRead().use { it.readBytes() }
        require(bytes.size in 29..65_536)
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        gson.fromJson(String(cifrador.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8), SesionEps::class.java)
            .also { require(it.usuario.isNotBlank() && it.cookies.isNotEmpty()); urlPermitidaEps(it.inicio) }
    }.getOrElse { borrar(); null }

    fun guardar(sesion: SesionEps) {
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.ENCRYPT_MODE, clave())
        val bytes = cifrador.iv + cifrador.doFinal(gson.toJson(sesion).toByteArray(Charsets.UTF_8))
        require(bytes.size <= 65_536)
        // AtomicFile evita dejar una sesión a medio escribir si se interrumpe la aplicación.
        val atomico = android.util.AtomicFile(archivo)
        val salida = atomico.startWrite()
        try {
            salida.write(bytes)
            atomico.finishWrite(salida)
        } catch (error: Exception) {
            atomico.failWrite(salida)
            throw error
        }
    }

    fun borrar() { android.util.AtomicFile(archivo).delete() }

    private fun clave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    private companion object { const val ALIAS = "aguarda.eps.sesion.v1" }
}
