# Desplegar la API de Aguarda en un VPS

Los pasos usan Ubuntu 24.04 o Debian 12 en adelante, con `systemd`, Python 3.11 o posterior y una cuenta SSH con `sudo`. La API de FastAPI escucha dentro del VPS en `127.0.0.1:61872`. Caddy recibe las conexiones de Android por **HTTPS en el puerto `61873`**. SQLite guarda los avisos en `backend/data/cortes.sqlite3`.

Reemplaza `IP_PUBLICA_VPS` por la IP pública de tu servidor en los ejemplos.

## 1. Copiar los archivos desde Windows

Desde la raíz del proyecto en PowerShell, cambia `USUARIO_VPS` por tu usuario SSH. También se copia el catálogo que comparten la API y Android.

```powershell
$vps = 'USUARIO_VPS@IP_PUBLICA_VPS'
ssh $vps 'mkdir -p ~/aguarda-api/backend ~/aguarda-api/app/src/main/assets'
$archivos = (Get-ChildItem .\backend\*.py -File).FullName
scp @archivos .\backend\requirements.txt .\backend\ejemplos_eps.json "${vps}:aguarda-api/backend/"
scp .\backend\activar-sincronizacion.sh "${vps}:aguarda-api/backend/"
scp .\app\src\main\assets\distritos_cortes_tacna.txt "${vps}:aguarda-api/app/src/main/assets/"
scp .\app\src\main\assets\sectores_eps_tacna.json "${vps}:aguarda-api/app/src/main/assets/"
ssh $vps
```

Si tu SSH usa un puerto personalizado, agrega `-p PUERTO_SSH` a `ssh` y `-P PUERTO_SSH` a `scp`.

## 2. Iniciar la API como servicio

En el VPS, comprueba qué procesos escuchan en los puertos elegidos. Si `61872` o `61873` están ocupados, elige otros puertos libres y sustituye los valores correspondientes en los pasos siguientes. Si `80` ya está gestionado por Caddy, usa esa misma instancia. Si lo ocupa otro servidor web, usa ese proxy con un sitio HTTPS existente o integra la validación del certificado con ese servidor. [Registro de puertos de IANA](https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml)

```bash
sudo ss -ltnp '( sport = :61872 or sport = :61873 or sport = :80 )'
sudo apt update
sudo apt install -y python3 python3-venv curl
python3 --version
cd ~/aguarda-api
python3 -m venv .venv
.venv/bin/python -m pip install -r backend/requirements.txt
.venv/bin/python -m backend.importar ejemplos
```

Desde esa misma carpeta, crea el servicio con el usuario y la ruta de la sesión SSH. Arrancará con el VPS y se reiniciará si falla.

```bash
sudo tee /etc/systemd/system/aguarda-cortes.service > /dev/null <<EOF
[Unit]
Description=API de cortes de agua de Aguarda
After=network.target

[Service]
User=$(id -un)
WorkingDirectory=$(pwd)
ExecStart=$(pwd)/.venv/bin/python -m uvicorn backend.api:app --host 127.0.0.1 --port 61872
Restart=on-failure
RestartSec=3

[Install]
WantedBy=multi-user.target
EOF
sudo systemctl daemon-reload
sudo systemctl enable --now aguarda-cortes
curl -fsS --retry 5 --retry-connrefused 'http://127.0.0.1:61872/distritos'
```

La respuesta debe listar los 28 distritos de Tacna. Para ver errores del servicio, usa `sudo journalctl -u aguarda-cortes -n 50 --no-pager`.

Si el VPS ya sirve la API, repite la copia de archivos del paso 1 y ejecuta `sudo systemctl restart aguarda-cortes`. Conserva `backend/data/cortes.sqlite3`. La API amplía el esquema SQLite al recibir la primera consulta. Comprueba `curl -fsS 'http://127.0.0.1:61872/avisos?periodo=todos&ambito=provincia-tacna'` antes de actualizar Android.

## 3. Publicar HTTPS en el puerto 61873

### 3.1. Instalar Caddy en el VPS

Para usar la IP directamente se requiere **Caddy 2.11 o posterior**. Ejecuta `caddy version` en la sesión SSH. Si el comando no existe o devuelve una versión anterior, instala el paquete estable desde el repositorio oficial. La instalación crea la configuración y el servicio de Caddy. [Instalación oficial para Ubuntu y Debian](https://caddyserver.com/docs/install#debian-ubuntu-raspbian)

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg debian-keyring debian-archive-keyring
curl -fsSL https://dl.cloudsmith.io/public/caddy/stable/gpg.key \
  | sudo gpg --batch --yes --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -fsSL https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt \
  | sudo tee /etc/apt/sources.list.d/caddy-stable.list > /dev/null
sudo chmod 644 /usr/share/keyrings/caddy-stable-archive-keyring.gpg /etc/apt/sources.list.d/caddy-stable.list
sudo apt update
sudo apt install -y caddy
caddy version
```

Si al guardar aparece `Error writing /etc/caddy/Caddyfile: No such file or directory`, sal del editor y revisa `ls -ld /etc/caddy`. Si Caddy ya está instalado como servicio y falta esa carpeta, créala con `sudo mkdir -p /etc/caddy`. Si instalaste Caddy manualmente, revisa la ruta que usa el servicio con `systemctl cat caddy`.

Si aparece `unrecognized ACME issuer property: profile`, comprueba `apt-cache policy caddy` y `/usr/bin/caddy version`. Actualiza Caddy desde el repositorio estable indicado arriba y conserva el Caddyfile actual si el instalador pregunta. Si `caddy version` y `/usr/bin/caddy version` difieren, usa `type -a caddy` y `systemctl show caddy -p ExecStart` para comprobar qué binario usa cada comando. El certificado público para la IP requiere `profile shortlived`. [Certificados para IP de Let's Encrypt](https://letsencrypt.org/2026/01/15/6day-and-ip-general-availability)

### 3.2. Configurar HTTPS

Permite **TCP 80 y 61873** en el firewall del VPS y en el panel del proveedor. Let's Encrypt valida la IP por el puerto 80 y Caddy renueva el certificado. El puerto 61872 queda accesible solo desde el VPS. [Certificados para IP](https://letsencrypt.org/2026/01/15/6day-and-ip-general-availability) · [Validación HTTP-01](https://letsencrypt.org/docs/challenge-types/#http-01-challenge)

Abre la configuración con `sudo nano /etc/caddy/Caddyfile`. Si ya tiene un bloque global al inicio, coloca `default_sni` dentro de ese bloque. Agrega el sitio de la API y conserva los sitios que ya estén configurados. En Nano, pulsa **Ctrl+O**, **Enter** y **Ctrl+X** para guardar y salir.

```caddyfile
{
    default_sni IP_PUBLICA_VPS
}

https://IP_PUBLICA_VPS:61873 {
    tls {
        issuer acme https://acme-v02.api.letsencrypt.org/directory {
            profile shortlived
            disable_tlsalpn_challenge
        }
    }
    reverse_proxy 127.0.0.1:61872
}
```

```bash
sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile \
  && sudo systemctl enable --now caddy \
  && sudo systemctl reload caddy
```

Desde tu PC, abre `https://IP_PUBLICA_VPS:61873/docs`. Si aún falla HTTPS, revisa `sudo journalctl -u caddy -n 50 --no-pager` para comprobar si el certificado se emitió y si el puerto 80 es accesible para la validación.

### Si Nginx ya escucha en el puerto 80

Conserva Nginx en el puerto 80 y reserva un puerto local libre para el desafío ACME. El ejemplo usa `61874`. En el `Caddyfile`, agrega `auto_https disable_redirects` y `http_port 61874` al bloque global. Agrega `alt_http_port 61874` dentro de `issuer acme`. Al final del Caddyfile, añade este sitio. [Puerto alternativo de ACME](https://caddyserver.com/docs/caddyfile/directives/tls) · [Validación HTTP-01](https://letsencrypt.org/docs/challenge-types/#http-01-challenge)

```caddyfile
http://:61874 {
    bind 127.0.0.1
    respond 404
}
```

Con `sudo nginx -T`, ubica el `server` de puerto 80 que recibe la IP `IP_PUBLICA_VPS`. Si la IP carece de un bloque propio, usa el servidor predeterminado de ese puerto. Agrega esta ruta dentro del bloque. Si el bloque tiene una redirección `return` que se ejecuta para todas las rutas, conserva esa redirección en `location /` para que el desafío llegue al proxy. Si el bloque ya tiene una ruta para ACME, revisa qué certificados atiende antes de editarla. [Selección de servidores en Nginx](https://nginx.org/en/docs/http/request_processing.html) · [Proxy de Nginx](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_pass)

```nginx
location ^~ /.well-known/acme-challenge/ {
    proxy_set_header Host $host;
    proxy_pass http://127.0.0.1:61874;
}
```

```bash
sudo nginx -t && sudo systemctl reload nginx
sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile \
  && sudo systemctl enable caddy \
  && sudo systemctl restart caddy
```

Nginx recibe el desafío por `80/TCP`. Android entra por `61873/TCP`. Caddy y FastAPI se comunican por los puertos locales `61874` y `61872` para la validación y el proxy, respectivamente.

## 4. Conectar Android y cargar nuevos avisos

Compila y ejecuta la app. La URL HTTPS del VPS está configurada en `app/build.gradle.kts`. Si despliegas la API en otro servidor, puedes seleccionar su dirección al compilar con la propiedad de Gradle `-PCORTES_API_URL=https://IP_PUBLICA_VPS:61873/`.

En la campanita, selecciona **Todos los sectores → Todo el historial** para comprobar los dos comunicados de ejemplo. Para incorporar avisos nuevos, ejecuta el sincronizador en el VPS desde `~/aguarda-api` y verifica su salida. Puedes habilitar Facebook con las credenciales autorizadas de Meta en el proceso que ejecuta el importador. [Importación y sincronización](README.md#importar-avisos)

```bash
.venv/bin/python -m backend.importar sincronizar
```

### Sincronizar automáticamente cada 30 minutos

El instalador activa un servicio que comienza a consultar las fuentes públicas al arrancar y repite la consulta cada 30 minutos, también después de reiniciar el VPS. Usa la cuenta y el directorio del servicio de la API. Si la API tiene un `CORTES_DB` personalizado, configura el mismo valor en `/etc/default/aguarda-fuentes` antes de ejecutar el instalador. Ese archivo también admite las credenciales de Meta cuando obtengas autorización. [Configuración del importador](README.md#facebook-de-eps-tacna)

```bash
cd ~/aguarda-api
sudo bash backend/activar-sincronizacion.sh
sudo journalctl -u aguarda-fuentes -n 30 --no-pager
```

Si la API y los archivos Python ya están en el VPS, solo hace falta copiar `backend/activar-sincronizacion.sh` antes de ejecutar estos dos comandos. `active` indica que el proceso está funcionando. El registro muestra cuántos avisos importó cada fuente y los fallos de consulta.

### Activar las publicaciones y fotos de Facebook con RSS

Pega `https://www.facebook.com/EPSTacna` en el generador de FetchRSS o RSS.app. En FetchRSS, deja desmarcada la opción **Link shared posts to the original URL** para conservar el enlace al aviso en Facebook. Si ya habías generado el feed, abre su enlace guardado directamente. Comprueba que tenga publicaciones recientes de EPS Tacna y que sus imágenes se puedan leer. Copia la URL RSS que entrega el proveedor y usa `https://` si aparece como `http://`. [FetchRSS](https://fetchrss.com/source/facebook) · [RSS.app](https://rss.app/rss-feed/create-facebook-rss-feed)

Copia el código actualizado desde Windows (paso 1). En el VPS, ejecuta como root.

```bash
cd /root/aguarda-api
apt install -y tesseract-ocr tesseract-ocr-spa
touch /etc/default/aguarda-fuentes
chmod 600 /etc/default/aguarda-fuentes
nano /etc/default/aguarda-fuentes
```

Agrega o actualiza esta variable con el enlace XML real. En Nano, guarda con **Ctrl+O → Enter → Ctrl+X**.

```bash
EPS_FACEBOOK_RSS_URL="PEGA_AQUI_LA_URL_XML"
```

Prueba que pueda leer el feed y comprueba cuántos avisos guardó. Activa el servicio para repetir la consulta cada 30 minutos.

```bash
cd /root/aguarda-api
(
  set -a
  . /etc/default/aguarda-fuentes
  set +a
  .venv/bin/python -m backend.importar sincronizar --solo-facebook
) && bash backend/activar-sincronizacion.sh && systemctl restart aguarda-fuentes
```

La prueba imprime `EPS Tacna · Facebook (RSS)` y el número de registros que guardó. Abre la campanita en **Todos los sectores → Todo el historial** y comprueba que el aviso muestre la fuente y abra la publicación original. El servicio revisa el feed cada 30 minutos y el proveedor determina cuándo lo actualiza.

### Opción con Meta Graph

Con credenciales de Meta, puedes cargar una vez el historial disponible ejecutando esto como root en la carpeta del VPS. La siguiente ronda del servicio continúa con los avisos más recientes.

```bash
(
  set -a
  . /etc/default/aguarda-fuentes
  set +a
  .venv/bin/python -m backend.importar sincronizar --solo-facebook --facebook-historial
)
```
