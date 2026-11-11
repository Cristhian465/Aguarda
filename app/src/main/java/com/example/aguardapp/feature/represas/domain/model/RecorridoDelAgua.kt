package com.example.aguardapp.feature.represas.domain.model

/** Un paso del camino del agua, desde la fuente hasta el distrito. [presas] son nombres del inventario de la ANA. */
data class TramoDelRecorrido(
    val titulo: String,
    val detalle: String? = null,
    val porConfirmar: Boolean = false,
    val presas: List<String> = emptyList()
)

data class RecorridoDelAgua(val tramos: List<TramoDelRecorrido>, val aviso: String) {
    val presas: List<String> get() = tramos.flatMap { it.presas }
    val confirmado: Boolean get() = tramos.none { it.porConfirmar }

    fun abasteceDesde(nombrePresa: String): Boolean = presas.any { it.equals(nombrePresa, ignoreCase = true) }
}

/**
 * De dónde viene el agua de cada distrito. La ANA no publica esta relación: el equipo la armó con
 * noticias y estudios (Andina, enero de 2026; exposiciones de EPS Tacna en el Congreso) y debe
 * confirmarla con EPS Tacna. Lo que aún no se sabe queda marcado "por confirmar".
 */
object RecorridosDeTacna {

    private val REPRESAS_DEL_UCHUSUMA = TramoDelRecorrido(
        "Represas del Uchusuma",
        "En la sierra de Tacna, a más de 4 500 m",
        presas = listOf("Paucarani", "Casiri", "Condorpico")
    )

    private val POR_PLANTA_CALANA = RecorridoDelAgua(
        tramos = listOf(
            REPRESAS_DEL_UCHUSUMA,
            TramoDelRecorrido("Captación Cerro Blanco", "Allí se junta el agua de las represas"),
            TramoDelRecorrido("Planta de tratamiento Calana", "Allí se potabiliza"),
            TramoDelRecorrido("Reservorio R1", "Reparte a Ciudad Nueva y Alto de la Alianza")
        ),
        aviso = "Cuando bajan las represas, la planta Calana produce menos y tu distrito pasa a horarios restringidos."
    )

    private val POR_CONFIRMAR_CON_RIO = RecorridoDelAgua(
        tramos = listOf(
            REPRESAS_DEL_UCHUSUMA,
            TramoDelRecorrido("Planta Calana o Alto Lima", "Por confirmar con EPS Tacna", porConfirmar = true)
        ),
        aviso = "Este recorrido aún no está confirmado. Lo validaremos con EPS Tacna."
    )

    private val SIN_DATOS = RecorridoDelAgua(
        tramos = listOf(TramoDelRecorrido("Fuente por confirmar", "Aún no tenemos datos de tu distrito", porConfirmar = true)),
        aviso = "Todavía no sabemos de dónde viene el agua de tu distrito. Lo consultaremos con EPS Tacna."
    )

    fun de(distrito: Distrito): RecorridoDelAgua = when (distrito) {
        Distrito.CIUDAD_NUEVA, Distrito.ALTO_DE_LA_ALIANZA -> POR_PLANTA_CALANA
        Distrito.GREGORIO_ALBARRACIN -> RecorridoDelAgua(
            tramos = listOf(TramoDelRecorrido("Pozos de Viñani", "Agua subterránea: no viene de las represas")),
            aviso = "Tu agua depende menos de las lluvias y del nivel de las represas."
        )
        Distrito.TACNA, Distrito.POCOLLAY -> POR_CONFIRMAR_CON_RIO
        Distrito.CALANA, Distrito.PACHIA, Distrito.LA_YARADA_LOS_PALOS -> SIN_DATOS
    }
}
