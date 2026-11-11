package com.example.aguardapp.feature.mapa.data

import kotlinx.datetime.LocalTime
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.EstadoCisterna
import com.example.aguardapp.feature.mapa.domain.model.PuntoCisterna
import com.example.aguardapp.feature.mapa.domain.repository.MapaRepository

class MapaRepositoryImpl : MapaRepository {
    override fun centroDelMapa(): Coordenada = CENTRO_TACNA

    override suspend fun obtenerPuntosCisterna(): List<PuntoCisterna> = PUNTOS

    private companion object {
        val CENTRO_TACNA = Coordenada(-18.0130, -70.2500)

        val PUNTOS = listOf(
            PuntoCisterna(
                id = "PC-01", nombre = "Viñani · Asoc. La Esperanza",
                ubicacion = Coordenada(-18.0495, -70.2548),
                horarioInicio = LocalTime(7, 0), horarioFin = LocalTime(13, 0),
                estado = EstadoCisterna.ACTIVO
            ),
            PuntoCisterna(
                id = "PC-02", nombre = "Alto de la Alianza · Plaza",
                ubicacion = Coordenada(-17.9915, -70.2535),
                horarioInicio = LocalTime(8, 0), horarioFin = LocalTime(12, 0),
                estado = EstadoCisterna.EN_RUTA
            ),
            PuntoCisterna(
                id = "PC-03", nombre = "Ciudad Nueva · Mercado",
                ubicacion = Coordenada(-17.9845, -70.2365),
                horarioInicio = LocalTime(6, 0), horarioFin = LocalTime(10, 0),
                estado = EstadoCisterna.TERMINADO
            )
        )
    }
}
