#!/bin/bash
set -eu

if [ "$(id -u)" -ne 0 ]; then
    echo 'Ejecuta este instalador como root en el VPS.' >&2
    exit 1
fi

cd -- "$(dirname -- "$0")/.."
proyecto=$(pwd -P)
api=aguarda-cortes.service
unidad=aguarda-fuentes.service
servicio=/etc/systemd/system/$unidad

if ! systemctl is-active --quiet "$api"; then
    echo 'Primero inicia el servicio aguarda-cortes.' >&2
    exit 1
fi

ruta_api=$(systemctl show --property=WorkingDirectory --value "$api")
usuario=$(systemctl show --property=User --value "$api")
usuario=${usuario:-root}
if [ "$ruta_api" != "$proyecto" ] || [ ! -x "$proyecto/.venv/bin/python" ]; then
    echo 'Coloca el instalador en backend/ dentro del proyecto que usa aguarda-cortes.' >&2
    exit 1
fi

if [ ! -e "$servicio" ]; then
    cat > "$servicio" <<EOF
[Unit]
Description=Sincronizador de avisos de Aguarda
Wants=network-online.target
After=network-online.target

[Service]
User=$usuario
WorkingDirectory=$proyecto
EnvironmentFile=-/etc/default/aguarda-fuentes
ExecStart="$proyecto/.venv/bin/python" -u -m backend.importar sincronizar --cada-minutos 30
Restart=on-failure
RestartSec=15

[Install]
WantedBy=multi-user.target
EOF
fi

systemctl daemon-reload
systemctl enable --now "$unidad"
systemctl is-active "$unidad"
echo 'Para ver el resultado de cada fuente: journalctl -u aguarda-fuentes -n 30 --no-pager'
