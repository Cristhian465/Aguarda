package com.example.aguardapp.feature.represas.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import com.example.aguardapp.feature.represas.data.local.PresaDao
import com.example.aguardapp.feature.represas.data.local.PresaEntity
import com.example.aguardapp.feature.represas.data.remote.AnaApi
import com.example.aguardapp.feature.represas.data.remote.PresaAna
import com.example.aguardapp.feature.represas.domain.model.Presa
import com.example.aguardapp.feature.represas.domain.repository.RepresasRepository

/**
 * Offline first: la pantalla siempre lee de Room. [actualizar] trae los datos de la ANA y los guarda;
 * si no hay internet, se siguen viendo los últimos guardados.
 */
class RepresasRepositoryImpl(private val api: AnaApi, private val dao: PresaDao) : RepresasRepository {

    override val presas: Flow<List<Presa>> = dao.todas().map { lista -> lista.map { it.aDominio() } }

    override suspend fun actualizar(): Boolean = try {
        val presas = api.presasDeTacna().features.orEmpty().mapNotNull { it.aEntidad() }
        if (presas.isNotEmpty()) dao.guardar(presas)
        true
    } catch (e: IOException) {
        false // sin internet o la ANA no respondió a tiempo
    } catch (e: HttpException) {
        false // la ANA respondió con un error
    }
}

/** Convierte lo que manda la ANA en una fila de Room. Si falta el nombre o la ubicación, se descarta. */
private fun PresaAna.aEntidad(): PresaEntity? {
    val nombre = attributes.nombre?.trim() ?: return null
    val punto = geometry ?: return null
    val uso = attributes.uso.orEmpty().lowercase()
    return PresaEntity(
        nombre = nombre.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) },
        distrito = attributes.distrito?.trim().orEmpty(),
        uso = when {
            "sumin" in uso -> "Agua potable y riego"
            "miner" in uso -> "Minería"
            else -> "Riego"
        },
        volumenHm3 = attributes.volumen?.toDoubleOrNull(),
        latitud = punto.y,
        longitud = punto.x
    )
}

private fun PresaEntity.aDominio() = Presa(nombre, distrito, uso, volumenHm3, latitud, longitud)
