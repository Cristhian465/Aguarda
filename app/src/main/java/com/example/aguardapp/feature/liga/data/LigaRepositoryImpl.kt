package com.example.aguardapp.feature.liga.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import com.example.aguardapp.feature.liga.data.local.LigaDao
import com.example.aguardapp.feature.liga.data.local.MarcaNivelEntity
import com.example.aguardapp.feature.liga.domain.LigaRepository
import com.example.aguardapp.feature.liga.domain.MarcaNivel

class LigaRepositoryImpl(private val dao: LigaDao) : LigaRepository {

    override fun observarMarcas(): Flow<List<MarcaNivel>> =
        dao.observarMarcas().map { marcas -> marcas.map { MarcaNivel(LocalDateTime.parse(it.momento), it.litros) } }

    override suspend fun guardarMarca(marca: MarcaNivel) {
        dao.guardarMarca(MarcaNivelEntity(momento = marca.momento.toString(), litros = marca.litros))
    }
}
