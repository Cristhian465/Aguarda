package com.example.aguardapp.feature.mapa.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.aguardapp.feature.mapa.domain.model.Coordenada
import com.example.aguardapp.feature.mapa.domain.model.Distrito
import com.example.aguardapp.feature.mapa.domain.repository.DistritosRepository

private const val ARCHIVO_DISTRITOS = "distritos_tacna.geojson"

/** Lee los límites guardados en la app (OpenStreetMap, con el ubigeo del INEI). No usa internet. */
class DistritosRepositoryImpl(context: Context) : DistritosRepository {
    private val contexto = context.applicationContext

    override suspend fun obtenerDistritos(): List<Distrito> = withContext(Dispatchers.IO) {
        leerDistritos(contexto.assets.open(ARCHIVO_DISTRITOS).bufferedReader().use { it.readText() })
    }
}

private data class ColeccionGeoJson(val features: List<FeatureGeoJson>)
private data class FeatureGeoJson(val properties: PropiedadesGeoJson, val geometry: GeometriaGeoJson)
private data class PropiedadesGeoJson(val ubigeo: String, val nombre: String)
private data class GeometriaGeoJson(val coordinates: List<List<List<List<Double>>>>)

/** GeoJSON MultiPolygon: cada punto viene como [longitud, latitud]; solo se usa el borde exterior. */
fun leerDistritos(geoJson: String): List<Distrito> =
    Gson().fromJson(geoJson, ColeccionGeoJson::class.java).features.map { feature ->
        Distrito(
            ubigeo = feature.properties.ubigeo,
            nombre = feature.properties.nombre,
            poligonos = feature.geometry.coordinates.map { poligono ->
                poligono.first().map { (longitud, latitud) -> Coordenada(latitud, longitud) }
            }
        )
    }
