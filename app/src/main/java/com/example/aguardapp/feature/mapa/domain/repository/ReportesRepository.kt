package com.example.aguardapp.feature.mapa.domain.repository

import com.example.aguardapp.feature.mapa.domain.model.ReporteVecinos
import com.example.aguardapp.feature.mapa.domain.model.TipoReporte

interface ReportesRepository {
    suspend fun recientes(): Result<List<ReporteVecinos>>

    suspend fun enviar(tipo: TipoReporte, ubigeo: String, puntoId: String?): Result<Unit>
}
