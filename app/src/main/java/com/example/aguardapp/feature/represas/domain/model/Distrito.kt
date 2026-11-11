package com.example.aguardapp.feature.represas.domain.model

import java.text.Normalizer

/** Distritos de la ciudad de Tacna que atiende EPS Tacna. */
enum class Distrito(val nombre: String, private val clave: String) {
    TACNA("Tacna (Cercado)", "tacna"),
    ALTO_DE_LA_ALIANZA("Alto de la Alianza", "alto de la alianza"),
    CIUDAD_NUEVA("Ciudad Nueva", "ciudad nueva"),
    POCOLLAY("Pocollay", "pocollay"),
    GREGORIO_ALBARRACIN("Gregorio Albarracín", "gregorio albarracin"),
    CALANA("Calana", "calana"),
    PACHIA("Pachía", "pachia"),
    LA_YARADA_LOS_PALOS("La Yarada-Los Palos", "yarada");

    companion object {
        /**
         * El distrito que nombra [texto], sin importar tildes ni mayúsculas:
         * "Coronel Gregorio Albarracín Lanchipa" es Gregorio Albarracín.
         * "Tacna" debe coincidir completo, porque también es el nombre de la región.
         */
        fun desdeTexto(texto: String?): Distrito? {
            val normalizado = texto?.sinTildes()?.lowercase()?.trim() ?: return null
            return entries.firstOrNull { distrito ->
                if (distrito == TACNA) normalizado == distrito.clave else distrito.clave in normalizado
            }
        }
    }
}

/** El distrito donde está el hogar y cómo se supo. */
data class DistritoDelHogar(val distrito: Distrito, val detectadoPorGps: Boolean)

internal fun String.sinTildes(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
