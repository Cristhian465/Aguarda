package com.example.aguardapp.feature.eps

import com.example.aguardapp.feature.eps.domain.LineaReciboEps
import com.example.aguardapp.feature.eps.domain.alinearCamposEps
import org.junit.Assert.assertEquals
import org.junit.Test

class AlinearCamposEpsTest {
    @Test fun emparejaVolumenEImportePorFilaSinUsarElOrdenDeLosBloques() {
        val lineas = listOf(LineaReciboEps("Volumen Fac m", 100, 100, 250, 120),
            LineaReciboEps("TOTAL A PAGAR: S/", 450, 800, 700, 830),
            LineaReciboEps("*****45.20", 800, 800, 920, 830),
            LineaReciboEps("15", 330, 100, 350, 120))
        assertEquals("Consumo del mes: 15 m3\nTotal a pagar: S/ 45.20", alinearCamposEps(lineas, 1000))
    }

    @Test fun reconoceElTotalDebajoDeLaEtiquetaEnSuMismoRecuadro() {
        val lineas = listOf(LineaReciboEps("TOTAL A PAGAR:", 450, 800, 650, 820),
            LineaReciboEps("S/ ****45.20", 700, 835, 890, 865))
        assertEquals("Total a pagar: S/ 45.20", alinearCamposEps(lineas, 1000))
    }

    @Test fun noConfundeElSuperindiceLaOtraColumnaNiLaFilaAnteriorConElVolumen() {
        val lineas = listOf(LineaReciboEps("Volumen Fac m", 100, 100, 250, 120),
            LineaReciboEps("3", 251, 90, 258, 102),
            LineaReciboEps("180", 330, 60, 350, 80),
            LineaReciboEps("45.20", 800, 100, 920, 120))
        assertEquals("", alinearCamposEps(lineas, 1000))
    }

    @Test fun noEligeCuandoHayDosCifrasCasiSuperpuestas() {
        val lineas = listOf(LineaReciboEps("Volumen Fac m", 100, 100, 250, 120),
            LineaReciboEps("15", 330, 100, 350, 120), LineaReciboEps("16", 335, 100, 355, 120))
        assertEquals("", alinearCamposEps(lineas, 1000))
    }
}
