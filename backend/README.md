# API de avisos de agua y saneamiento de Aguarda

Ejecuta estos comandos en PowerShell desde la raíz de Aguarda. Se requiere Python 3.11 o posterior.

```powershell
python -m pip install -r backend/requirements.txt
python -m backend.importar ejemplos
.\backend\iniciar.ps1
```

En el navegador abre [la documentación de la API](http://127.0.0.1:8000/docs). La ruta [avisos con historial](http://127.0.0.1:8000/avisos?periodo=todos) muestra dos comunicados históricos de EPS Tacna adaptados desde sus textos publicados.

## Ver los avisos en el celular

1. Si usas un teléfono, conéctalo por USB, habilita la depuración USB y acepta la autorización en el teléfono. Luego ejecuta `.\backend\iniciar.ps1`. El script conecta el puerto con `adb reverse` e inicia la API. Déjalo abierto mientras usas la app.
2. Ejecuta la variante **debug** de Aguarda desde Android Studio. Si usas el emulador oficial de Android Studio, la app se conecta a la PC por `10.0.2.2`.
3. Pulsa la campana de Inicio. Para ver los ejemplos elige **Todos los sectores** y **Todo el historial**. Busca `Las Américas` o `Los Cedros` para comprobar el filtro por zona.

Si PowerShell no permite ejecutar el script, puedes iniciar la API con `python -m uvicorn backend.api:app --port 8000`. Para el teléfono, en otra terminal ejecuta `adb reverse tcp:8000 tcp:8000` o `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000` si `adb` no está en el PATH. Si sale un error de conexión, abre [Swagger en la PC](http://127.0.0.1:8000/docs) para comprobar que la API está iniciada.

Para usar otra dirección, define `CORTES_API_URL=https://tu-servidor/` en `local.properties` y vuelve a compilar. Una compilación de distribución requiere una URL HTTPS.

Para ejecutar la API en Linux con HTTPS y un puerto dedicado, sigue la [guía de despliegue en un VPS](DESPLIEGUE.md).

## Importar avisos

**SUNASS.** Descarga el CSV de interrupciones de CAMI YAKU y pásalo al importador. Filtra agua potable en Tacna, conserva las subzonas y actualiza los avisos con el mismo identificador de incidente y zona. Admite CSV con coma, punto y coma o tabulador. [Catálogo de datos abiertos](https://servicio.sunass.gob.pe/camiyaku/pages/datos_abiertos/catalogo.html)

```powershell
python -m backend.importar sunass "C:\ruta\interrupciones.csv"
```

**EPS Tacna.** Reemplaza la URL de ejemplo por la página oficial del comunicado. El lector intenta identificar el tipo de corte, las zonas y el horario. Cuando falta un dato, conserva el campo vacío para que la app lo indique. Si la fuente limita el acceso directo, puedes importar una copia HTML con `--html` o usar un texto revisado.

```powershell
python -m backend.importar eps "https://www.gob.pe/institucion/epstacna/noticias/1198584-eps-tacna-restablece-servicio-tras-rotura-de-tuberia-en-la-asociacion-las-americas"
```

**Imágenes.** Con Tesseract y el idioma español (`spa`) instalados, genera un borrador. Revisa las fechas, horarios, distritos y sectores en el comunicado original antes de importar el texto.

```powershell
$aviso = 'https://www.gob.pe/institucion/epstacna/noticias/1198584-eps-tacna-restablece-servicio-tras-rotura-de-tuberia-en-la-asociacion-las-americas'
python -m backend.importar ocr aviso.jpg aviso.txt
python -m backend.importar eps $aviso --texto aviso.txt --titulo "Corte de agua" --publicado 2025-06-05
```

Puedes indicar `--distrito` y `--sector` para corregir la zona cuando la hayas confirmado en la fuente.

**Actualización automática.** La sincronización consulta las noticias de EPS Tacna y SUNASS en Gob.pe y el RSS de Radio Uno. Radio Uno se identifica como medio y se incluyen extractos de avisos de servicio que mencionan a EPS Tacna. Con `--sunass-url` se puede añadir un CSV público de SUNASS. Si una fuente falla, se continúa con las demás y se conserva lo importado. [EPS Tacna](https://www.gob.pe/institucion/epstacna/noticias) · [Noticias de SUNASS](https://www.gob.pe/institucion/sunass/noticias) · [RSS de Radio Uno](https://radiouno.pe/feed/)

En el VPS, `bash backend/activar-sincronizacion.sh`, ejecutado como root, activa el importador cada 30 minutos y lo inicia de nuevo tras cada reinicio. [Activar el servicio](DESPLIEGUE.md#sincronizar-automáticamente-cada-30-minutos)

```powershell
python -m backend.importar sincronizar --cada-minutos 30
python -m backend.importar sincronizar --eps --sunass-url "URL_HTTPS_DEL_CSV"
```

### Facebook de EPS Tacna

Para consultar las publicaciones recientes sin un token de Meta, genera un RSS con la URL directa `https://www.facebook.com/EPSTacna` en FetchRSS o RSS.app. Confirma que el feed muestra las publicaciones de EPS Tacna y sus fotos. Copia el enlace HTTPS generado como `EPS_FACEBOOK_RSS_URL` en `/etc/default/aguarda-fuentes` y reinicia el sincronizador. Se admiten las rutas `/rss/ID.xml` y `/feed/ID.rss` de FetchRSS. El importador lee el texto y procesa con OCR las imágenes disponibles. La cobertura y la frecuencia dependen del proveedor. [FetchRSS para Facebook](https://fetchrss.com/source/facebook) · [RSS.app para Facebook](https://rss.app/rss-feed/create-facebook-rss-feed) · [Imágenes de RSS.app](https://help.rss.app/en/articles/10769849-guide-to-feed-output)

El importador también admite Meta Graph. El administrador de EPS puede habilitar la integración para su propia página. Si el acceso se solicita desde una app que carece de permisos sobre la página, Meta pide **Page Public Content Access**. Guarda `EPS_FACEBOOK_PAGE_ID` y `EPS_FACEBOOK_TOKEN` en el entorno del importador en el VPS. `EPS_FACEBOOK_GRAPH_VERSION` permite ajustar la versión de Graph. [Página de EPS Tacna](https://www.facebook.com/EPSTacna) · [Permiso para páginas públicas](https://developers.facebook.com/documentation/development/features-reference/page-public-content-access)

En Ubuntu, instala Tesseract con español mediante `sudo apt install tesseract-ocr tesseract-ocr-spa`. El importador reconoce el texto de las imágenes, reutiliza el resultado en las siguientes consultas y conserva el enlace al aviso original. Si se configura el RSS, el importador lo usa para la consulta habitual. Con Graph, la consulta habitual cubre 14 días y `--facebook-historial` solicita todas las páginas de resultados disponibles para el token. La cobertura histórica depende de lo que entregue Meta. [Límites de publicaciones](https://developers.facebook.com/docs/graph-api/reference/page/published_posts/) · [Adjuntos de publicaciones](https://developers.facebook.com/docs/graph-api/reference/story-attachment/)

```bash
python -m backend.importar sincronizar --solo-facebook
python -m backend.importar sincronizar --solo-facebook --facebook-historial
```

Mientras se habilita el acceso, puedes importar el texto revisado de una publicación oficial o un archivo JSON de la página, con los campos `message`, `created_time` y `permalink_url`. Para avisos que vienen como imagen, extrae el texto con el comando `ocr`, confirma las fechas y las zonas en el original y guárdalo como texto antes de importarlo.

```powershell
python -m backend.importar facebook --texto aviso.txt --url "ENLACE_DE_LA_PUBLICACION" --publicado "AAAA-MM-DD"
python -m backend.importar facebook --archivo publicaciones.json
```

Para consultar las zonas y contrastar un aviso, el [Semáforo de SUNASS](https://aplicaciones.sunass.gob.pe/Interrupciones/consultaInterrupcion.html) sigue accesible en el navegador. La disponibilidad de nuevos avisos depende de la frecuencia de publicación y del acceso a cada fuente.

## Datos y pruebas

| Ruta | Respuesta |
|---|---|
| `GET /avisos?ambito=provincia-tacna&categoria=MANTENIMIENTO&periodo=7d&sector=V` | Avisos por tipo, sector y periodo. Agrega `buscar` para calles, asociaciones, distritos o palabras clave. `limite` y `desde` permiten paginar. |
| `GET /sectores` | Siete sectores EPS con subsectores, distritos y zonas de referencia, además de los 11 distritos de la provincia. |
| `GET /cortes?distrito=Ciudad%20Nueva` | Avisos próximos o recientes del distrito confirmado. Sin distrito incluye todos los avisos de Tacna. Los distritos fuera del catálogo devuelven HTTP 422. |
| `GET /cortes?historial=true&limite=100` | Incluye el historial. El límite predeterminado es 50 y el máximo es 100. |
| `GET /distritos` | Los 28 distritos del departamento de Tacna, incluso con la base de avisos vacía. |

En `/avisos`, `periodo` admite `24h`, `7d`, `30d`, `365d` y `todos`. Se toma la fecha de publicación y, si falta, se usa la fecha del evento disponible. Los registros sin fecha aparecen en el historial completo. `/cortes` conserva su ventana histórica para versiones anteriores de Android.

El selector comparte el catálogo de siete sectores con la API. Si un aviso menciona una asociación o calle reconocible de forma inequívoca, se etiqueta como coincidencia por referencia. Una localidad ambigua sigue disponible por búsqueda. La app consulta los 11 distritos de la provincia de Tacna y conserva comunicados generales de EPS que no indican un distrito. El ámbito predeterminado de la API abarca el departamento y `/distritos` conserva sus 28 distritos. [División político-administrativa de Tacna](https://geotakana.regiontacna.gob.pe/pagefiles/pdf/a_ee_se21.pdf)

SQLite se guarda por defecto en `backend/data/cortes.sqlite3`. Si defines `CORTES_DB`, usa el mismo valor al importar y al iniciar la API. En un servidor, pon ese archivo en un volumen persistente.

```powershell
python -m unittest backend.test_cortes -v
```

Fuentes de los ejemplos históricos. [Avenida Manuel A. Odría y Los Cedros](https://www.gob.pe/institucion/epstacna/noticias/1182986-eps-tacna-garantiza-abastecimiento-de-agua-potable-durante-trabajos-de-reparacion) · [Asociación Las Américas](https://www.gob.pe/institucion/epstacna/noticias/1198584-eps-tacna-restablece-servicio-tras-rotura-de-tuberia-en-la-asociacion-las-americas) · [Datos abiertos de SUNASS](https://servicio.sunass.gob.pe/camiyaku/)
