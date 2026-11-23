package com.example.aguardapp.core.navigation

object Rutas {
    const val BIENVENIDA = "bienvenida"
    const val CONFIGURAR_HOGAR = "configurar_hogar"
    const val INICIO = "inicio"
    const val RECIBOS_EPS = "recibos_eps"
    const val MI_DEPOSITO = "mi_deposito"
    const val ME_QUEDE_SIN_AGUA = "me_quede_sin_agua"
    const val AVISOS = "avisos"
    const val CORTES = "cortes"
    const val QUE_RECORTAR = "que_recortar"
    const val MAPA = "mapa"
    const val REPRESAS = "represas"
    const val AHORRO = "ahorro"
    const val LIGA = "liga"
    const val FAMILIA = "familia"

    const val ARG_TIPO = "tipo"
    const val REGISTRAR_LLENADO = "registrar_llenado/{$ARG_TIPO}"

    fun registrarLlenado(tipo: String): String = "registrar_llenado/$tipo"
}
