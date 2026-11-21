package com.example.aguardapp.feature.eps.data

import android.content.Context
import com.example.aguardapp.feature.eps.domain.ConsultaEps
import com.example.aguardapp.feature.eps.domain.CuentaEps
import com.example.aguardapp.feature.eps.domain.ErrorEps
import com.example.aguardapp.feature.eps.domain.ReciboEps
import com.example.aguardapp.feature.eps.domain.extraerReciboEps
import java.io.IOException
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ConsultaEpsPortal(context: Context) : ConsultaEps {
    private val almacen = AlmacenSesionEps(context)
    private val cliente = ClientePortalEps(TransporteJsoupEps())
    private val lector = LectorPdfEps(context)
    private val mutex = Mutex()
    @Volatile private var sesion = almacen.leer()

    override fun cuenta(): CuentaEps? = sesion?.let { CuentaEps(it.usuario) }

    override suspend fun vincular(usuario: String, clave: String) = operar {
        val nueva = cliente.vincular(usuario, clave)
        almacen.guardar(nueva)
        sesion = nueva
    }

    override suspend fun actualizar(): ReciboEps = operar {
        val actual = sesion ?: throw ErrorEps("Primero vincula tu cuenta EPS.", true)
        val (pdf, renovada) = cliente.documento(actual)
        almacen.guardar(renovada)
        sesion = renovada
        val texto = lector.leer(pdf)
        extraerReciboEps(texto)
    }

    override suspend fun consultarHistorial(): List<ReciboEps> = operar {
        val actual = sesion ?: throw ErrorEps("Primero vincula tu cuenta EPS.", true)
        val (historial, renovada) = cliente.historial(actual)
        almacen.guardar(renovada)
        sesion = renovada
        historial
    }

    override suspend fun desvincular() = mutex.withLock {
        withContext(Dispatchers.IO) { almacen.borrar(); sesion = null }
    }

    private suspend fun <T> operar(accion: suspend () -> T): T = mutex.withLock {
        withContext(Dispatchers.IO) {
            try { accion() }
            catch (error: CancellationException) { throw error }
            catch (error: ErrorEps) {
                if (error.requiereVincular) { almacen.borrar(); sesion = null }
                throw error
            }
            catch (error: SSLException) {
                throw ErrorEps("EPS redirigió a HTTPS, pero no pudimos validar su certificado. El acceso inicial usa HTTP; no ignoramos errores de certificado.")
            }
            catch (error: IOException) { throw ErrorEps("No pudimos comunicarnos con EPS. Revisa tu conexión e intenta más tarde.") }
            catch (error: Exception) { throw ErrorEps("No pudimos procesar el recibo de EPS. Tu recibo guardado se conserva.") }
        }
    }
}
