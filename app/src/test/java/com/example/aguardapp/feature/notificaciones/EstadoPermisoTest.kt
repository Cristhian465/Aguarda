package com.example.aguardapp.feature.notificaciones

import org.junit.Assert.assertEquals
import org.junit.Test
import com.example.aguardapp.feature.notificaciones.presentation.EstadoPermiso
import com.example.aguardapp.feature.notificaciones.presentation.estadoPermiso

class EstadoPermisoTest {
    @Test
    fun concedidoGanaSobreTodo() {
        assertEquals(EstadoPermiso.CONCEDIDO, estadoPermiso(concedido = true, debeExplicar = true, solicitadoAntes = true, puedeSolicitar = false))
    }

    @Test
    fun sinPedirTodaviaSePuedeSolicitar() {
        assertEquals(EstadoPermiso.SIN_SOLICITAR, estadoPermiso(false, debeExplicar = false, solicitadoAntes = false, puedeSolicitar = true))
    }

    @Test
    fun trasUnaNegativaHayQueExplicar() {
        assertEquals(EstadoPermiso.EXPLICACION, estadoPermiso(false, debeExplicar = true, solicitadoAntes = true, puedeSolicitar = true))
    }

    @Test
    fun trasDosNegativasQuedaBloqueado() {
        assertEquals(EstadoPermiso.BLOQUEADO, estadoPermiso(false, debeExplicar = false, solicitadoAntes = true, puedeSolicitar = true))
    }

    @Test
    fun apagadasEnAjustesSoloSeArreglanAhi() {
        assertEquals(EstadoPermiso.BLOQUEADO, estadoPermiso(false, debeExplicar = false, solicitadoAntes = false, puedeSolicitar = false))
    }
}
