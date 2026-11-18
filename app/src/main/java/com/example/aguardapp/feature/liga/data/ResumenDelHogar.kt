package com.example.aguardapp.feature.liga.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDateTime
import kotlin.math.roundToInt
import com.example.aguardapp.feature.ahorro.data.AhorroLocal
import com.example.aguardapp.feature.deposito.domain.repository.DepositoRepository
import com.example.aguardapp.feature.liga.domain.CalcularPuntaje
import com.example.aguardapp.feature.liga.domain.LigaRepository
import com.example.aguardapp.feature.liga.domain.ResumenSemanal

data class DatosDelHogar(val capacidadLitros: Int, val resumen: ResumenSemanal)

/** Junta las marcas, los llenados y los retos en el resumen semanal del hogar. */
class ResumenDelHogar(
    private val liga: LigaRepository,
    private val deposito: DepositoRepository,
    private val ahorro: AhorroLocal,
    private val ahora: () -> LocalDateTime,
    private val calcular: CalcularPuntaje = CalcularPuntaje()
) {
    fun observar(): Flow<DatosDelHogar?> =
        combine(liga.observarMarcas(), deposito.observarMomentosDeLlenado(), deposito.observarPerfil()) { marcas, llenados, perfil ->
            perfil?.let {
                val resumen = calcular.resumen(marcas, llenados, ahorro.historial().keys, it.habitantes.cantidad, ahora().date)
                DatosDelHogar(it.capacidad.litros.valor.roundToInt(), resumen)
            }
        }
}
