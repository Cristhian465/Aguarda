<p align="center">
  <img src="https://img.shields.io/badge/💧_AguardApp-Gestión_Hídrica-0ea5e9?style=for-the-badge&labelColor=0c4a6e" alt="AguardApp"/>
</p>

<h1 align="center">💧 AguardApp</h1>

<p align="center">
  <em>Sistema móvil para la gestión de la reserva domiciliaria de agua y la anticipación de cortes durante el racionamiento hídrico en Tacna</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Android-34A853?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Room-003B57?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room"/>
  <img src="https://img.shields.io/badge/Koin-F5A623?style=for-the-badge&logo=koin&logoColor=white" alt="Koin"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Plataforma-Android_7.0+-34A853?style=flat-square&logo=android&logoColor=white" alt="Android 7.0+"/>
  <img src="https://img.shields.io/badge/Min_SDK-24-blue?style=flat-square" alt="Min SDK 24"/>
  <img src="https://img.shields.io/badge/Target_SDK-36-blue?style=flat-square" alt="Target SDK 36"/>
  <img src="https://img.shields.io/badge/Arquitectura-MVVM_+_DDD-purple?style=flat-square" alt="MVVM + DDD"/>
  <img src="https://img.shields.io/badge/Versión-1.0-orange?style=flat-square" alt="Versión 1.0"/>
</p>

---

## 👥 Integrantes del Proyecto

| # | Integrante | Código | Rol |
|:-:|---|---|---|
| 🔵 | **Mamani Cori, Cristhian Carlos** | `2023077282` | Jefe de Proyecto · Core y Reserva |
| 🟢 | **Jahuira Pilco, Dayan Elvis** | `2022075749` | Sector y Mapas |
| 🟠 | **Sierra Ruiz, Iker Alberto** | `2023077090` | Recibo e IA · Líder de Pruebas |
| 🟣 | **Llica Mamani, Jimmy Mijair** | `2023076789` | Retos, Reportes y Despliegue |

> **Universidad Privada de Tacna** · Facultad de Ingeniería · Escuela Profesional de Ingeniería de Sistemas  
> **Curso:** Soluciones Móviles I · **Docente:** Mag. Alberto Johnatan Flor Rodríguez

---

## ❗ Problemática

La ciudad de **Tacna** se abastece de agua potable bajo un esquema de **racionamiento por sectores**, con servicio solo durante algunas horas al día. Los hogares enfrentan:

| Problema | Impacto |
|---|---|
| 🔴 **Sin información clara** de cuándo llega el agua a su sector | Desabastecimiento imprevisto |
| 🔴 **Sin control** sobre cuánta reserva queda en el hogar | Desperdicio del recurso |
| 🔴 **Información oficial dispersa** y poco confiable | Mala planificación del consumo |
| 🔴 **Compra de agua de emergencia** a sobreprecio | Gasto innecesario |

```mermaid
flowchart TD
    A([Inicio]) --> B["El vecino escucha rumores<br/>del horario del agua"]
    B --> C{"¿Llegó el agua?"}
    C -- No --> D["Espera sin saber<br/>cuándo llegará"]
    D --> C
    C -- Sí --> E["Llena tanques y<br/>recipientes de golpe"]
    E --> F["Usa el agua sin<br/>controlar la reserva"]
    F --> G{"¿Se quedó sin agua?"}
    G -- Sí --> H["💸 Compra agua de<br/>emergencia a sobreprecio"]
    G -- No --> Z([Fin])
    H --> Z
```

---

## ✅ Solución

**AguardApp** es una aplicación Android que permite a los hogares de Tacna:

| Funcionalidad | Descripción |
|---|---|
| 📊 **Gestionar el depósito** | Saber cuántos litros quedan y hasta cuándo alcanza |
| 💧 **Registrar llenados** | Un toque cuando llega el agua (completo o a la mitad) |
| 📉 **Aprender el consumo** | Ajusta la proyección cuando el agua se acaba antes de lo previsto |
| ✂️ **Qué recortar** | Sugerencias de ahorro cuando el agua no alcanza hasta el próximo llenado |
| 🗺️ **Mapa de cisternas** | Puntos de reparto de agua en Tacna sobre Google Maps, con su horario y estado |
| 📶 **Sensor IoT del tanque** | Lee el nivel real del tanque (ESP32 + sensor ultrasónico, simulado en Wokwi) desde ThingSpeak y lo aplica como nivel actual |
| 🔔 **Avisos** | Alerta dentro de la app si el agua no alcanza, si queda menos del 20 % o si falta registrar un llenado |
| 🏞️ **Represas: de dónde viene tu agua** | El recorrido del agua de tu distrito (detectado por GPS o elegido de una lista) y las represas de Tacna con datos oficiales de la ANA, en un mapa y una lista; se ve sin conexión una vez descargado |
| 📡 **Datos del hogar locales** | Los datos del hogar se guardan solo en el teléfono (SQLite) y no se copian a la nube; internet se usa para el mapa, el sensor y descargar las represas, que quedan guardadas |

---

## 📐 Factibilidad

Según el Informe de Factibilidad **(FD01, Versión 1.0)**, el proyecto es viable en todas las dimensiones:

| Dimensión | Estado | Detalle |
|---|:-:|---|
| 🔧 **Técnica** | ✅ | Equipo competente, tecnologías gratuitas, MVP operativo |
| 💰 **Económica** | ✅ | Inversión: S/ 17,720.00 · VAN positivo · TIR: 22.2% |
| ⚙️ **Operativa** | ✅ | Flujos simples, interfaz accesible |
| ⚖️ **Legal** | ✅ | Cumple Ley N.° 29733 de Protección de Datos Personales |
| 🌱 **Social y Ambiental** | ✅ | Fomenta el ahorro y consumo responsable del agua |

---

## 🎯 Visión y Misión

> ### 🔭 Visión
> Ser un referente en el desarrollo de soluciones móviles con **impacto social** en la región de Tacna, aplicando buenas prácticas de ingeniería de software para resolver problemas reales de la comunidad.

> ### 🚀 Misión
> Desarrollar aplicaciones móviles **accesibles y de calidad** que mejoren la vida de los ciudadanos; para este proyecto, ayudando a los hogares de Tacna a gestionar su reserva de agua durante el racionamiento.

---

## 🛠️ Stack Tecnológico

| Categoría | Tecnologías |
|---|---|
| **Lenguaje** | ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) |
| **UI** | ![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white) ![Material 3](https://img.shields.io/badge/Material_3-757575?style=flat-square&logo=materialdesign&logoColor=white) |
| **Base de datos** | ![Room](https://img.shields.io/badge/Room_(SQLite)-003B57?style=flat-square&logo=sqlite&logoColor=white) |
| **Mapas** | ![Google Maps](https://img.shields.io/badge/Google_Maps_Compose-4285F4?style=flat-square&logo=googlemaps&logoColor=white) |
| **Red** | ![Retrofit](https://img.shields.io/badge/Retrofit-48B983?style=flat-square&logo=square&logoColor=white) ![ThingSpeak](https://img.shields.io/badge/ThingSpeak-1D6FA5?style=flat-square) |
| **Datos públicos** | ![ANA](https://img.shields.io/badge/ANA-ArcGIS_REST-0B6C7A?style=flat-square) ![Nominatim](https://img.shields.io/badge/Nominatim-OpenStreetMap-7EBC6F?style=flat-square&logo=openstreetmap&logoColor=white) |
| **DI** | ![Koin](https://img.shields.io/badge/Koin-F5A623?style=flat-square) |
| **Pruebas** | ![JUnit 4](https://img.shields.io/badge/JUnit_4-25A162?style=flat-square&logo=junit5&logoColor=white) |
| **Fechas** | ![kotlinx.datetime](https://img.shields.io/badge/kotlinx.datetime-7F52FF?style=flat-square) |
| **Arquitectura** | ![MVVM](https://img.shields.io/badge/MVVM-purple?style=flat-square) ![DDD](https://img.shields.io/badge/DDD-darkblue?style=flat-square) ![Clean Architecture](https://img.shields.io/badge/Clean_Architecture-teal?style=flat-square) |

---

## 📂 Estructura del Proyecto

```
AguardApp/
├── 📄 build.gradle.kts                    Configuración raíz de Gradle
├── 📄 settings.gradle.kts                 Módulos y repositorios
├── 📄 Readme.md                           ← Estás aquí
│
├── 📁 app/                                Módulo principal Android
│   ├── 📄 build.gradle.kts                Dependencias y plugins
│   ├── 📁 schemas/                        Esquemas exportados de Room (versiones 1 a 4)
│   ├── 📁 src/test/                       Pruebas unitarias de Represas y respuestas reales de la ANA y Nominatim
│   └── 📁 src/main/
│       ├── 📄 AndroidManifest.xml
│       ├── 📁 res/                        Recursos (iconos, strings, etc.)
│       └── 📁 java/com/example/aguardapp/
│           ├── 📄 AguardApplication.kt    Inicia la base de datos y Koin
│           ├── 📄 MainActivity.kt         Muestra la app
│           ├── 📄 App.kt                  Tema + navegación
│           │
│           ├── 📁 core/                   🔧 Infraestructura transversal
│           │   ├── 📁 data/               Usuario local (UUID) y DAO
│           │   ├── 📁 db/                 AguardAppDatabase (Room)
│           │   ├── 📁 di/                 Koin: base de datos, red, repositorios y ViewModels
│           │   ├── 📁 navigation/         Rutas, NavHost, barra inferior y pantalla inicial
│           │   ├── 📁 ui/theme/           Colores, tipografías y tema
│           │   └── 📁 util/               Reloj y UUID
│           │
│           └── 📁 feature/
│               ├── 📁 bienvenida/         Pantalla de entrada (solo el primer uso)
│               ├── 📁 deposito/           Depósito de agua del hogar (MVVM + DDD)
│               ├── 📁 mapa/               Mapa de puntos de cisterna (Google Maps)
│               ├── 📁 sensor/             Nivel del tanque desde el sensor IoT (Retrofit + ThingSpeak)
│               └── 📁 represas/           De dónde viene tu agua y represas de Tacna (Retrofit + ANA + Nominatim + Room)
│
└── 📁 informes/                           📋 Documentación académica
    ├── 📄 FD01-Factibilidad.md
    ├── 📄 FD02-Vision.md
    ├── 📄 FD03-SRS.md
    ├── 📄 FD04-SAD.md
    └── 📄 FD05-Informe-Final.md
```

### Capas internas de cada Feature

Las features `deposito`, `mapa`, `sensor` y `represas` siguen el patrón **MVVM + DDD** con estas capas (`bienvenida` solo tiene `presentation/`; solo `deposito` tiene `usecase/`; `sensor` y `represas` tienen `remote/` con sus APIs de Retrofit; `mapa` no usa Room):

| Capa | Contenido | Depende de |
|---|---|---|
| 📐 `domain/` | `model/`, `repository/` (interfaces), `usecase/` — Kotlin puro | Nada |
| 💾 `data/` | Implementaciones de repositorios, `local/` (Room), `remote/` (Retrofit y DTO), `mapper/` | `domain/` |
| 🖼️ `presentation/` | Por pantalla: `*Screen.kt` y `*ViewModel.kt` (con su `UiState`); `componentes/` con las piezas comunes | `domain/` |

---

## 🏗️ Diagrama C4 — Nivel de Contexto

```mermaid
C4Context
    title Sistema AguardApp — Diagrama de Contexto

    Person(user, "Vecino de Tacna", "Hogar que gestiona su reserva de agua durante el racionamiento")

    System(app, "AguardApp", "App Android que gestiona el depósito de agua del hogar")

    System_Ext(ana, "ANA", "Inventario oficial de presas (ArcGIS REST)")
    System_Ext(osm, "OpenStreetMap - Nominatim", "Distrito que corresponde a una ubicación")
    System_Ext(gmaps, "Google Maps", "Mapas base")
    System_Ext(ts, "ThingSpeak", "Lecturas del sensor del tanque")

    Rel(user, app, "Usa", "Android")
    Rel(app, ana, "Descarga las presas de Tacna", "HTTPS/JSON")
    Rel(app, osm, "Consulta el distrito", "HTTPS/JSON")
    Rel(app, gmaps, "Carga mapas", "HTTPS")
    Rel(app, ts, "Lee el nivel del tanque", "HTTPS/JSON")
```

## 🏗️ Diagrama C4 — Nivel de Contenedores

```mermaid
C4Container
    title Sistema AguardApp — Diagrama de Contenedores

    Person(user, "Vecino de Tacna")

    Container_Boundary(mobile, "App Android") {
        Container(ui, "Presentation Layer", "Jetpack Compose", "Screens, ViewModels y UiStates con MVVM")
        Container(domain, "Domain Layer", "Kotlin puro", "Models, UseCases y Repositories (interfaces)")
        Container(data, "Data Layer", "Room + Retrofit", "Implementación de repositorios, DAOs, Entities, APIs y DTO")
        ContainerDb(roomdb, "Room Database", "SQLite", "Fuente de verdad local: perfil del hogar, llenados, novedades, plan de recortes, represas y distrito del hogar (versión 4)")
    }

    System_Ext(ana, "ANA", "Inventario de presas")
    System_Ext(osm, "Nominatim", "OpenStreetMap")

    Rel(user, ui, "Interactúa")
    Rel(ui, domain, "Usa UseCases")
    Rel(data, domain, "Implementa interfaces")
    Rel(data, roomdb, "Lee/Escribe")
    Rel(data, ana, "GET presas de Tacna", "Retrofit")
    Rel(data, osm, "GET reverse", "Retrofit")
```

## 🏗️ Diagrama C4 — Nivel de Componentes (Feature Depósito)

```mermaid
C4Component
    title Feature Depósito — Nivel de Componentes

    Container_Boundary(deposito, "feature/deposito") {
        Component(screen, "DepositoScreen", "Compose", "Pantalla principal del depósito")
        Component(vm, "DepositoViewModel", "MVVM", "Gestiona el estado de la UI del depósito")
        Component(uc_armar, "ArmarDeposito", "Domain", "Calcula nivel, consumo y agotamiento")
        Component(uc_deficit, "CalcularDeficit", "Domain", "Litros que faltarán antes del próximo llenado")
        Component(repo_if, "DepositoRepository", "Interface", "Contrato de acceso a datos del depósito")
        Component(repo_impl, "DepositoRepositoryImpl", "Data", "Implementación con Room")
        Component(dao, "DepositoDao", "Room", "Queries SQLite para perfil, llenados y novedades")
    }

    Rel(screen, vm, "Observa UiState")
    Rel(vm, repo_if, "Usa")
    Rel(repo_impl, uc_armar, "Invoca")
    Rel(vm, uc_deficit, "Invoca")
    Rel(repo_impl, repo_if, "Implementa")
    Rel(repo_impl, dao, "Lee/Escribe")
```

## 🏗️ Diagrama C4 — Nivel de Componentes (Feature Represas)

```mermaid
C4Component
    title Feature Represas — Nivel de Componentes

    Container_Boundary(represas, "feature/represas") {
        Component(screen, "RepresasScreen", "Compose", "Tu recorrido, mapa, filtros y lista de represas")
        Component(vm, "RepresasViewModel", "MVVM", "Une represas, distrito, recorrido y tanque en el UiState")
        Component(recorridos, "RecorridosDeTacna", "Domain", "Recorrido del agua de cada distrito")
        Component(repo_impl, "RepresasRepositoryImpl", "Data", "Caché en Room y renovación semanal")
        Component(ubicacion, "UbicacionRepositoryImpl", "Data", "Ubicación por red o GPS (LocationManager)")
        Component(ana_api, "AnaApi", "Retrofit", "Inventario de presas de Tacna")
        Component(nom_api, "NominatimApi", "Retrofit", "Dirección de unas coordenadas")
        Component(dao, "RepresasDao", "Room", "Tablas presa y distrito_hogar")
    }

    System_Ext(ana, "ANA", "ArcGIS REST")
    System_Ext(osm, "Nominatim", "OpenStreetMap")

    Rel(screen, vm, "Observa UiState")
    Rel(vm, recorridos, "Pide el recorrido del distrito")
    Rel(vm, repo_impl, "Usa")
    Rel(vm, ubicacion, "Pide la ubicación")
    Rel(repo_impl, ana_api, "Descarga")
    Rel(repo_impl, nom_api, "Consulta el distrito")
    Rel(repo_impl, dao, "Lee/Escribe")
    Rel(ana_api, ana, "HTTPS/JSON")
    Rel(nom_api, osm, "HTTPS/JSON")
```

---

## 📊 Diagrama de Clases Principal

```mermaid
classDiagram
    class Usuario {
        id
        bienvenidaCompletada
    }
    class PerfilHogar {
        tipoReservorio
        capacidadLitros
        habitantes
    }
    class Deposito {
        nivelLitros
        consumoEstimado
        agotamientoProyectado
    }
    class EventoLlenado {
        fechaHora
        tipo
    }

    Usuario "1" -- "1" PerfilHogar
    PerfilHogar "1" -- "0..1" Deposito
    Deposito "1" -- "0..*" EventoLlenado
```

---

## 🔧 Requisitos Previos

| Requisito | Versión |
|---|---|
| Android Studio | Ladybug o superior |
| JDK | 17+ |
| Android SDK | API 24 (mínimo) — API 36 (target) |
| Dispositivo/Emulador | Android 7.0+ (API 24) |

## 🚀 Cómo Ejecutar

```bash
# 1. Clonar el repositorio
git clone https://github.com/tu-usuario/AguardApp.git

# 2. Abrir en Android Studio

# 3. Poner la API key de Google Maps en local.properties
#    MAPS_API_KEY=tu_api_key
#    THINGSPEAK_CHANNEL_ID=id_del_canal
#    THINGSPEAK_READ_KEY=read_api_key_del_canal
#    (Represas no necesita claves: la API de la ANA y Nominatim son públicas)

# 4. Sincronizar Gradle y ejecutar
./gradlew :app:assembleDebug

# O simplemente presionar ▶️ Run en Android Studio

# 5. Pruebas unitarias (no necesitan internet)
./gradlew :app:testDebugUnitTest
```

---

## 📋 Requerimientos Funcionales Principales

| ID | Requerimiento | Prioridad |
|---|---|:-:|
| RF-02 | Configurar el hogar | 🔴 Alta |
| RF-04 | Consultar el depósito (nivel y hasta cuándo alcanza) | 🔴 Alta |
| RF-05 | Calcular el déficit hasta el próximo llenado | 🔴 Alta |
| RF-08 | Operar sin conexión | 🔴 Alta |
| RF-10 | Consultar de dónde viene el agua de mi distrito | 🟡 Media |
| RF-12 | Consultar las represas de Tacna (datos de la ANA) | 🟡 Media |

> 📄 Ver documentación completa en [`informes/FD03-SRS.md`](./informes/FD03-SRS.md)

---

## 📚 Documentación

| Documento | Descripción |
|---|---|
| [`FD01-Factibilidad.md`](./informes/FD01-Factibilidad.md) | Estudio de factibilidad técnica, económica y operativa |
| [`FD02-Vision.md`](./informes/FD02-Vision.md) | Documento de visión del proyecto |
| [`FD03-SRS.md`](./informes/FD03-SRS.md) | Especificación de Requerimientos de Software |
| [`FD04-SAD.md`](./informes/FD04-SAD.md) | Documento de Arquitectura de Software |
| [`FD05-Informe-Final.md`](./informes/FD05-Informe-Final.md) | Informe final del proyecto |

---

## 📝 Changelog

### 2026-11-11
- [Agregado] Feature `represas` (Unidad II): pestaña "Represas" con "Tu recorrido" (de qué represa o pozo viene el agua del distrito del hogar), las represas de Tacna en un mapa con marcadores propios y en una lista con filtros por uso, y el detalle de cada represa (`represa/{presa}`) con la comparación con el tanque.
- [Agregado] Consumo de la API REST de la ANA (inventario de presas, ArcGIS) y de Nominatim (OpenStreetMap) con Retrofit, inyectados con Koin en `moduloRepresas` con un `OkHttpClient` propio (tiempos de espera de 20 s y cabecera User-Agent). El inventario se renueva como máximo una vez por semana.
- [Agregado] Ubicación del teléfono con `LocationManager` (sin Play Services) para detectar el distrito, solo cuando el usuario toca "Usar mi ubicación" (aunque ya haya dado el permiso en el Mapa); también se puede elegir de una lista. Solo se guarda el distrito, no las coordenadas.
- [Agregado] Base de datos versión 4 con `AutoMigration` de la 3 a la 4: tablas `presa` y `distrito_hogar`.
- [Cambiado] Barra inferior con tres pestañas: Mi depósito, Mapa y Represas. Al cambiar entre Mapa y Represas, "atrás" vuelve siempre a Mi depósito.
- [Agregado] 21 pruebas unitarias de Represas, con respuestas reales de la ANA y de Nominatim en `src/test/resources`.
- [Cambiado] Informes FD03, FD04 y FD05 actualizados a la versión 2.3 con Represas, la inyección completa con Koin y la privacidad con internet y ubicación.
- [Agregado] Feature `sensor`: tarjeta "Sensor del tanque" en Mi depósito que consulta cada 20 s el canal de ThingSpeak (Retrofit, inyectado con Koin en `moduloRed`). Una lectura más reciente que el último nivel registrado se aplica sola; una edición manual posterior se respeta hasta la siguiente lectura. El botón "Usar esta medición como nivel actual" la vuelve a aplicar.
- [Corregido] Consumo absurdo (p. ej. 144000 L/h): un "me quedé sin agua" registrado menos de 1 h después del llenado ya no se usa para calcular el consumo.
- [Cambiado] Inyección de dependencias completa: los ViewModels se registran en Koin (`moduloViewModels` en `AppModule.kt`) y cada pantalla los obtiene con `koinViewModel()`. Se eliminaron los `desdeInyeccion()` y se cambió `koin-core` por `koin-androidx-compose`.
- [Agregado] Feature `mapa`: pantalla "Puntos de cisterna" con Google Maps (`maps-compose`), marcadores por estado y "mi ubicación" si se concede el permiso.
- [Agregado] Barra inferior con dos pestañas: Mi depósito y Mapa (`core/navigation/BarraInferior.kt`).
- [Agregado] Permisos `INTERNET` y de ubicación; API key leída de `local.properties` (`MAPS_API_KEY`).
- [Cambiado] El proyecto compila con Java 17 (lo exige `maps-compose`).

### 2026-10-14
- [Cambiado] El consentimiento de la bienvenida empieza sin marcar: "Empezar" se habilita solo al aceptarlo, y el texto ahora explica que los datos se borran desinstalando la app.
- [Cambiado] Respaldo automático de Android desactivado (`allowBackup="false"` y reglas que excluyen todos los datos): la base local no se copia a la nube ni a otro teléfono.
- [Eliminado] Constante sin uso `AGUA_WATER` de `ParametrosConsumo` y comentario huérfano de la ruta `que_recortar/{deficit}` en `Rutas.kt`.
- [Corregido] Comentario de `AguardAppDatabase` sobre migraciones: la versión 3 usa `AutoMigration`.
- [Corregido] Informes FD03, FD04 y FD05 alineados con el código (versión 2.2): conflictos de merge resueltos, base de datos versión 3 y plan de recortes en los diagramas.

---

## 📜 Licencia

Proyecto académico desarrollado en la **Universidad Privada de Tacna** — 2026.

---

<p align="center">
  <sub>Hecho con 💧 para los hogares de Tacna</sub>
</p>
