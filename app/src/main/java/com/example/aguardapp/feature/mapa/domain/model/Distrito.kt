package com.example.aguardapp.feature.mapa.domain.model

/** Distrito de Tacna con sus límites. Un distrito puede tener más de un polígono. */
data class Distrito(
    val ubigeo: String,
    val nombre: String,
    val poligonos: List<List<Coordenada>>
) {
    fun contiene(punto: Coordenada): Boolean = poligonos.any { dentroDe(it, punto) }

    // Lanza una línea desde el punto: si cruza el borde un número impar de veces, está dentro.
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

/** Distrito donde cae el punto, o `null` si está fuera de todos. */
fun List<Distrito>.distritoDe(punto: Coordenada): Distrito? = firstOrNull { it.contiene(punto) }
