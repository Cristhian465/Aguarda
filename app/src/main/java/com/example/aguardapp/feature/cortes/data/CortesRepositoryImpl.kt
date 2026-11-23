package com.example.aguardapp.feature.cortes.data

import android.content.SharedPreferences
import com.example.aguardapp.feature.cortes.data.remote.CortesApi
import com.example.aguardapp.feature.cortes.domain.AVISOS_POR_PAGINA
import com.example.aguardapp.feature.cortes.domain.CortesRepository
import com.example.aguardapp.feature.cortes.domain.FiltrosAvisos

class CortesRepositoryImpl(private val api: CortesApi?) : CortesRepository {
    override val configurado: Boolean get() = api != null

    override suspend fun obtenerAvisos(filtros: FiltrosAvisos, desde: Int) =
        requireNotNull(api).obtenerAvisos(
            filtros.sector, filtros.categoria?.name, filtros.periodo.codigo,
            filtros.buscar.trim().takeIf { it.isNotEmpty() }, AVISOS_POR_PAGINA, desde
        ).map { it.aDominio() }
}

class PreferenciasCortes(private val preferencias: SharedPreferences) {
    var sectorEps: String?
        get() = preferencias.getString("sector_eps", null)
        set(valor) { preferencias.edit().putString("sector_eps", valor).apply() }
}
