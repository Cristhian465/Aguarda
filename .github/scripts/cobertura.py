"""Resume la cobertura de pruebas unitarias por módulo, a partir del reporte XML de JaCoCo.

Imprime una tabla en Markdown para el resumen del workflow de GitHub Actions.
"""
import collections
import pathlib
import sys
import xml.etree.ElementTree as ET

reportes = sorted(pathlib.Path("app/build/reports/coverage").rglob("report.xml"))
if not reportes:
    print("## Cobertura de pruebas\nNo se encontró el reporte de cobertura.")
    sys.exit(0)

# Agrupa los paquetes por módulo: feature/eps, feature/liga, core/di, ...
lineas = collections.defaultdict(lambda: [0, 0])
for paquete in ET.parse(reportes[0]).getroot().findall("package"):
    partes = paquete.get("name", "").split("aguardapp/")[-1].split("/")
    modulo = "/".join(partes[:2]) or "(raíz)"
    for contador in paquete.findall("counter"):
        if contador.get("type") == "LINE":
            lineas[modulo][0] += int(contador.get("covered"))
            lineas[modulo][1] += int(contador.get("missed"))


def porcentaje(cubiertas, sin_cubrir):
    total = cubiertas + sin_cubrir
    return "n/d" if total == 0 else f"{100 * cubiertas / total:.0f} %"


cubiertas_total = sum(valor[0] for valor in lineas.values())
sin_cubrir_total = sum(valor[1] for valor in lineas.values())

print("## Cobertura de pruebas unitarias")
print()
print(f"Total: **{porcentaje(cubiertas_total, sin_cubrir_total)}** de las líneas ({cubiertas_total} de {cubiertas_total + sin_cubrir_total}).")
print()
print("| Módulo | Líneas cubiertas | Líneas totales | Cobertura |")
print("|---|---:|---:|---:|")
for modulo, (cubiertas, sin_cubrir) in sorted(lineas.items()):
    print(f"| {modulo} | {cubiertas} | {cubiertas + sin_cubrir} | {porcentaje(cubiertas, sin_cubrir)} |")
