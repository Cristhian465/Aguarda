package com.example.aguardapp.feature.mapa.domain.model

data class Distrito(
    val ubigeo: String,
    val nombre: String,
    val poligonos: List<List<Coordenada>>
) {
    fun contiene(punto: Coordenada): Boolean = poligonos.any { dentroDe(it, punto) }

    private fun dentroDe(poligono: List<Coordenada>, punto: Coordenada): Boolean {
        var dentro = false
        poligono.zipWithNext { a, b ->
            val cruza = (a.latitud > punto.latitud) != (b.latitud > punto.latitud)
            if (cruza) {
                val longitudDelCruce = a.longitud +
                    (punto.latitud - a.latitud) / (b.latitud - a.latitud) * (b.longitud - a.longitud)
                if (punto.longitud < longitudDelCruce) dentro = !dentro
            }
        }
        return dentro
    }
}

fun List<Distrito>.distritoDe(punto: Coordenada): Distrito? = firstOrNull { it.contiene(punto) }
