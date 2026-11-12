package com.example.aguardapp.feature.represas.data

import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException

/**
 * Offline first: la pantalla siempre lee de Room. [actualizar] trae los datos de la ANA y los guarda;
 * si no hay internet, se siguen viendo los últimos guardados.
 */
class RepresasRepository(private val api: AnaApi, private val dao: PresaDao) {

    val presas: Flow<List<Presa>> = dao.todas()

    /** Devuelve true si se pudo hablar con la ANA. */
    suspend fun actualizar(): Boolean = try {
        val presas = api.presasDeTacna().features.orEmpty().mapNotNull { it.aPresa() }
        if (presas.isNotEmpty()) dao.guardar(presas)
        true
    } catch (e: IOException) {
        false // sin internet o la ANA no respondió a tiempo
    } catch (e: HttpException) {
        false // la ANA respondió con un error
    }
}
