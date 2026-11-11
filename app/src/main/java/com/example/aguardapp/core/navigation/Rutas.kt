package com.example.aguardapp.core.navigation

import android.net.Uri

/** Todas las rutas de la app. Las que llevan datos tienen una función para armar la ruta completa. */
object Rutas {
    const val BIENVENIDA = "bienvenida"
    const val CONFIGURAR_HOGAR = "configurar_hogar"
    const val MI_DEPOSITO = "mi_deposito"
    const val ME_QUEDE_SIN_AGUA = "me_quede_sin_agua"
    const val AVISOS = "avisos"
    const val QUE_RECORTAR = "que_recortar"
    const val MAPA = "mapa"
    const val REPRESAS = "represas"

    // Rutas con argumentos.
    const val ARG_TIPO = "tipo"
    const val REGISTRAR_LLENADO = "registrar_llenado/{$ARG_TIPO}"
    const val ARG_PRESA = "presa"
    const val DETALLE_PRESA = "represa/{$ARG_PRESA}"

    /** "registrar_llenado/completo" o "registrar_llenado/parcial". */
    fun registrarLlenado(tipo: String): String = "registrar_llenado/$tipo"

    /** "represa/Pampa%20de%20Vaca": el nombre va codificado porque puede tener espacios y eñes. */
    fun detallePresa(nombre: String): String = "represa/${Uri.encode(nombre)}"
}
