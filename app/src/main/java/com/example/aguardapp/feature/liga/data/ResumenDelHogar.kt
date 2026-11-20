package com.example.aguardapp.feature.liga.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDateTime
import kotlin.math.roundToInt
import com.example.aguardapp.feature.ahorro.data.AhorroLocal
import com.example.aguardapp.feature.deposito.domain.repository.DepositoRepository
import com.example.aguardapp.feature.eps.data.RecibosLocales
import com.example.aguardapp.feature.liga.domain.CalcularPuntaje
import com.example.aguardapp.feature.liga.domain.LigaRepository
import com.example.aguardapp.feature.liga.domain.LitrosSemana
import com.example.aguardapp.feature.liga.domain.ResumenSemanal
import com.example.aguardapp.feature.liga.domain.historialSemanal

data class DatosDelHogar(
    val capacidadLitros: Int,
    val resumen: ResumenSemanal,
    val verificado: Boolean,
    val historial: List<LitrosSemana>
)

class ResumenDelHogar(
    private val liga: LigaRepository,
    private val deposito: DepositoRepository,
    private val ahorro: AhorroLocal,
    private val recibos: RecibosLocales,
    private val ahora: () -> LocalDateTime,
    private val calcular: CalcularPuntaje = CalcularPuntaje()
) {
    fun observar(): Flow<DatosDelHogar?> = combine(
        liga.observarMarcas(), deposito.observarMomentosDeLlenado(), deposito.observarPerfil(), recibos.recibo
    ) { marcas, llenados, perfil, recibo ->
        perfil?.let {
            val hoy = ahora().date
            val habitantes = it.habitantes.cantidad
            val base = recibo?.takeIf { r -> r.vigente(hoy) }?.litrosPorPersonaDia(habitantes)
            val resumen = calcular.resumen(marcas, llenados, ahorro.historial().keys, habitantes, hoy, base)
            val historial = historialSemanal(marcas, llenados, habitantes, hoy)
            DatosDelHogar(it.capacidad.litros.valor.roundToInt(), resumen, recibo?.verificado(hoy) == true, historial)
        }
    }
}
