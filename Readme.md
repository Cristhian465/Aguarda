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
| 🏅 **Ahorro** | Marca los retos de hoy y consulta tu racha, calendario e insignias. El progreso se guarda en el teléfono. |
| 🗺️ **Mapa de cisternas** | Puntos de reparto de agua en Tacna sobre Google Maps, con su horario y estado, la lista de los más cercanos a tu ubicación el aviso "Antes de que se acabe", que te dice a qué cisterna ir antes de quedarte sin agua, y los límites reales de los distritos |
| 📶 **Sensor IoT del tanque** | Lee el nivel real del tanque (ESP32 + sensor ultrasónico, simulado en Wokwi) desde ThingSpeak y lo aplica como nivel actual |
| 🔔 **Avisos** | Alerta dentro de la app si el agua no alcanza, si queda menos del 20 % o si falta registrar un llenado |
| 🧾 **Consulta integrada EPS (en validación)** | Vincula la cuenta, conserva la sesión cifrada y consulta el PDF del recibo mediante HTTP en la oficina virtual; extrae consumo, periodo y total con ML Kit dentro del teléfono. Validado con cuenta y recibo reales en el Samsung conectado. |
| 📡 **Datos del hogar locales** | El depósito se guarda en el teléfono; mapa, sensor, ranking opcional y consulta EPS requieren internet. La clave EPS se envía únicamente a su portal HTTP (sin cifrado en tránsito) y no se guarda |

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
| **UI** | ![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white) ![Material 3](https://img.shields.io/badge/Material_3-757575?style=flat-square&logo=materialdesign&logoColor=white) · Jetpack Glance 1.1.1 (widget) |
| **Base de datos** | ![Room](https://img.shields.io/badge/Room_(SQLite)-003B57?style=flat-square&logo=sqlite&logoColor=white) |
| **Mapas** | ![Google Maps](https://img.shields.io/badge/Google_Maps_Compose-4285F4?style=flat-square&logo=googlemaps&logoColor=white) |
| **Red / HTML** | jsoup 1.24.1 · ![Retrofit](https://img.shields.io/badge/Retrofit-48B983?style=flat-square&logo=square&logoColor=white) ![ThingSpeak](https://img.shields.io/badge/ThingSpeak-1D6FA5?style=flat-square) |
| **DI** | ![Koin](https://img.shields.io/badge/Koin-F5A623?style=flat-square) |
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
│   ├── 📁 schemas/                        Esquemas exportados de Room
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
│           │   ├── 📁 di/                 Koin: base de datos, repositorios y ViewModels
│           │   ├── 📁 navigation/         Rutas, NavHost y cuadrícula de Inicio
│           │   ├── 📁 ui/theme/           Colores, tipografías y tema
│           │   └── 📁 util/               Reloj y UUID
│           │
│           └── 📁 feature/
│               ├── 📁 bienvenida/         Pantalla de entrada (solo el primer uso)
│               ├── 📁 deposito/           Depósito de agua del hogar (MVVM + DDD)
│               ├── 📁 liga/               Liga del Agua: medición del consumo y puntaje del hogar
│               ├── 📁 mapa/               Mapa de puntos de cisterna (Google Maps)
│               ├── 📁 sensor/             Nivel del tanque y detector de fugas desde el sensor IoT (HTTP directo a ThingSpeak)
│               ├── 📁 ahorro/             Retos, racha, calendario e insignias (SharedPreferences)
│               ├── 📁 eps/                Consulta del portal, sesión cifrada y OCR del PDF
│               └── 📁 widget/             Widget 2x1 de pantalla de inicio con el nivel del tanque (Glance)
│
├── 📁 iot/wokwi/                          Simulación del sensor: ESP32 + HC-SR04 (diagram.json, sketch.ino)
│
└── 📁 informes/                           📋 Documentación académica
    ├── 📄 FD01-Factibilidad.md
    ├── 📄 FD02-Vision.md
    ├── 📄 FD03-SRS.md
    ├── 📄 FD04-SAD.md
    └── 📄 FD05-Informe-Final.md
```

### Capas internas de cada Feature

Las features `deposito` y `mapa` siguen el patrón **MVVM + DDD** con estas capas (`bienvenida` solo tiene `presentation/`; `mapa` no tiene Room y su único caso de uso es `BuscarCisternasCercanas`):

| Capa | Contenido | Depende de |
|---|---|---|
| 📐 `domain/` | `model/`, `repository/` (interfaces), `usecase/` — Kotlin puro | Nada |
| 💾 `data/` | Implementaciones de repositorios, `local/` (Room), `mapper/` | `domain/` |
| 🖼️ `presentation/` | Por pantalla: `*Screen.kt` y `*ViewModel.kt` (con su `UiState`); `componentes/` con las piezas comunes | `domain/` |

---

## 🏗️ Diagrama C4 — Nivel de Contexto

```mermaid
C4Context
    title Sistema AguardApp — Diagrama de Contexto

    Person(user, "Vecino de Tacna", "Hogar que gestiona su reserva de agua durante el racionamiento")

    System(app, "AguardApp", "App Android que gestiona el depósito de agua del hogar")


    Rel(user, app, "Usa", "Android")

```

## 🏗️ Diagrama C4 — Nivel de Contenedores

```mermaid
C4Container
    title Sistema AguardApp — Diagrama de Contenedores

    Person(user, "Vecino de Tacna")

    Container_Boundary(mobile, "App Android") {
        Container(ui, "Presentation Layer", "Jetpack Compose", "Screens, ViewModels y UiStates con MVVM")
        Container(domain, "Domain Layer", "Kotlin puro", "Models, UseCases y Repositories (interfaces)")
        Container(data, "Data Layer", "Room", "Implementación de repositorios, DAOs y Entities")
        ContainerDb(roomdb, "Room Database", "SQLite", "Fuente de verdad local: perfil del hogar, llenados, novedades y plan de recortes (versión 3)")
        ContainerDb(ahorro, "Progreso de ahorro", "SharedPreferences", "Retos cumplidos por fecha")
    }


    Rel(user, ui, "Interactúa")
    Rel(ui, domain, "Usa UseCases")
    Rel(data, domain, "Implementa interfaces")
    Rel(data, roomdb, "Lee/Escribe")
    Rel(ui, ahorro, "Consulta y guarda a través de AhorroLocal")
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

# 3. Claves: ya están en el código, no hay que configurar nada.
#    ThingSpeak (canal y clave de lectura) -> feature/sensor/data/remote/ThingSpeakApi.kt
#    Supabase (URL y clave anon)           -> feature/liga/data/remote/SupabaseApi.kt
#    Google Maps                           -> AndroidManifest.xml

# 4. Sincronizar Gradle y ejecutar
./gradlew :app:assembleDebug

# O simplemente presionar ▶️ Run en Android Studio
```

---

## 📋 Requerimientos Funcionales Principales

| ID | Requerimiento | Prioridad |
|---|---|:-:|
| RF-02 | Configurar el hogar | 🔴 Alta |
| RF-04 | Consultar el depósito (nivel y hasta cuándo alcanza) | 🔴 Alta |
| RF-05 | Calcular el déficit hasta el próximo llenado | 🔴 Alta |
| RF-08 | Operar sin conexión | 🔴 Alta |
| RF-12 | Registrar y consultar el progreso de ahorro | Media |

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

## 🧾 Consulta integrada de EPS Tacna — en validación

En **Inicio → Recibos EPS**, la tarjeta **Tu recibo desde EPS Tacna** ofrece:

1. **Vincular cuenta EPS**: ingresar el usuario y la clave impresos en el recibo. El conector lee el formulario y sus campos ocultos; envía las credenciales mediante HTTP únicamente al host exacto de la oficina virtual. La cuenta queda vinculada únicamente si la respuesta contiene evidencia de acceso y una cookie de sesión.
2. **Actualizar recibo**: reutiliza la sesión, sigue enlaces directos del portal al PDF y extrae primero el texto digital con PDFBox Android y, si no basta, procesa hasta cuatro páginas con `PdfRenderer` y ML Kit, asociando etiquetas y cifras por posición. Guarda automáticamente mes facturado, consumo explícito en m³ y **total a pagar** (puede incluir deuda, no equivale al cargo del mes). No estima consumo a partir del importe o de las lecturas.
3. **Desvincular cuenta**: elimina la sesión de este teléfono y conserva el último recibo. No revoca la sesión en el servidor de EPS.

La contraseña no se persiste ni se añade a logs. Las cookies y el usuario se cifran con AES/GCM y una clave de Android Keystore, en `noBackupFilesDir/eps-sesion.enc`. El PDF se procesa en caché privada y se elimina al terminar o fallar. No se envía el documento a una IA externa. Un error conserva el recibo guardado; una sesión vencida obliga a vincular de nuevo. Los datos EPS se reflejan en la tarjeta de recibo existente y en la referencia de consumo de la Liga. El origen EPS cuenta como verificado si el recibo sigue vigente, sin afirmar que exista una firma digital validada.

**Estado real de la integración:** se inspeccionó el HTML público del acceso oficial, que utiliza `usu` y `pas` con POST a `../controlador/login.php`. Se verificaron el acceso autenticado y la descarga de un PDF real desde el Samsung conectado; la validación de campos se describe al final de esta sección. No hay una API oficial documentada identificada. El enlace de Gob.pe usa HTTP y la prueba HTTPS falló al validar el certificado. Por solicitud del usuario se permite HTTP solo para `oficinavirtual.epstacna.com.pe`, sin subdominios y en el puerto estándar. Usuario, clave, cookies y documento viajan sin cifrado y podrían ser interceptados; la pantalla lo informa antes de vincular. HTTPS sigue validando certificados si el portal redirige allí.

El descubrimiento usa enlaces HTML y el flujo AJAX observado del botón «Recibo virtual PDF»: interpreta las rutas literales y métodos de `CargarRecibo(...)` y `MostrarRecibo()`, solicita el visor y sigue su iframe, siempre en el mismo host. Envía `Referer` y `X-Requested-With` en las solicitudes AJAX. No ejecuta JavaScript arbitrario, no resuelve CAPTCHA y no inventa rutas de descarga. Si hay varios documentos, exige una etiqueta explícita de último recibo o periodos identificables para elegir el más reciente. Formularios adicionales, variantes de JavaScript distintas del flujo observado o una disposición de PDF no reconocible requieren adaptar el conector. La extracción rechaza campos ausentes o ambiguos, periodos futuros y recibos anteriores al guardado.

**Dependencias nuevas:** `org.jsoup:jsoup:1.24.1` para formularios, enlaces y transporte HTTP/HTTPS, y `com.tom-roush:pdfbox-android:2.0.27.0` para texto digital del PDF. PDFBox extrae el texto digital; `PdfRenderer`, Android Keystore y ML Kit reutilizan la plataforma y dependencias existentes. No requiere claves API nuevas, servidor propio ni cambios de Supabase. El permiso `INTERNET` ya existe.

**Paquete:** `feature/eps/{domain,data,presentation}`, registrado en `ModuloRecibos.kt`. No cambia el esquema de Room. Los campos opcionales de origen EPS, total y fecha de actualización se guardan junto al último recibo en las preferencias existentes, con valores por defecto compatibles con instalaciones previas.

```mermaid
flowchart LR
    UI[TarjetaEps] --> VM[EpsViewModel]
    VM --> CP[ConsultaEpsPortal]
    CP --> CL[ClientePortalEps + jsoup]
    CL -->|HTTP y cookies de sesión| EPS[Oficina virtual EPS Tacna]
    CP --> AS[AlmacenSesionEps + Android Keystore]
    CP --> PDF[LectorPdfEps: PDFBox + PdfRenderer + ML Kit]
    PDF --> EX[extraerReciboEps]
    VM --> PREF[RecibosLocales: último recibo]
    PREF --> REC[TarjetaRecibo y resumen de Liga]
```

**Validación ejecutada antes del ajuste HTTP (18/11/2026):** `:app:testDebugUnitTest` y `:app:assembleDebug` completaron correctamente: 77 pruebas, incluidas 18 de EPS, sin fallos. APK generado en `app/build/outputs/apk/debug/app-debug.apk`. No se realizó prueba en dispositivo ni consulta con cuenta real.

**Validación reproducible:** ejecutar `sh ./gradlew :app:testDebugUnitTest :app:assembleDebug` con JDK 17 o superior compatible con Gradle y el SDK Android configurado. Las pruebas de `feature/eps` usan respuestas HTML/PDF sintéticas; no consultan datos de usuarios. Se completó la prueba con una cuenta y un recibo reales en el Samsung conectado: acceso HTTP, cookies, menú AJAX, descarga, extracción y guardado. Otros diseños de recibo o cambios del portal requieren nueva validación.

---

## 📝 Changelog

### 2026-11-20
- [Cambiado] La tarjeta "Mi depósito" de Inicio usa las mismas dos olas del fondo del widget (`AguaOla` `#80AAD3` con transparencia) en lugar de los círculos decorativos.
- [Cambiado] Los fondos que usaban `#000F22` (tarjeta de Mi depósito en Inicio y widget) pasan a `#1B3554`; el texto principal (`Tinta`) se mantiene en `#000F22`. Los elementos que iban sobre fondos `#1B3554` (botón de ajustes de Mi depósito, logo, tarjeta y casilla de la bienvenida, círculo de Inicio, pastilla y círculo del widget) usan `AguaSuave` `#3F6593` para seguir viéndose.
- [Cambiado] Las barras superiores (`BarraSuperior`: Mapa, Represas, Liga del Agua, Recibos EPS, Avisos, Configurar, Registrar llenado, Sin agua y Qué recortar) y el encabezado "Aguarda" de Inicio son `#1B3554` hasta la barra de estado, con título blanco, subtítulo `#C0E6FD` e íconos de estado claros.
- [Cambiado] El agua de los tres tanques (widget, Mi depósito y Registrar llenado completo/parcial) pasa a un cian apagado (`#51AFBC`) para que no resalte tanto sobre la paleta azul.
- [Cambiado] Paleta azul del tema (`Color.kt`): `AguaProfunda`/`AguaMedia` `#1B3554`, `Tinta` `#000F22`, `AguaSuave` `#3F6593`, `TintaSuave` `#3F6593`, `Agua`/`TintaTenue` `#5B86B6` y `AguaClara` `#C0E6FD`; el widget usa fondo `#1B3554` con ondas `#80AAD3`.
- [Cambiado] La aplicación se llama "Aguarda" (nombre en el teléfono y en la bienvenida).
- [Cambiado] Ícono de la aplicación en azul marino (`#1B3554`, el mismo del logo de Inicio).
- [Cambiado] Widget del tanque con fondo azul marino de la paleta actual; el interior de los tanques mantiene el fondo turquesa oscuro original (`#06323A`).
- [Corregido] Widget: el texto ya no se corta en teléfonos con la letra del sistema agrandada (los tamaños compensan `fontScale`).
- [Cambiado] Inicio: las 5 tarjetas de "Explorar Aguarda" son cuadradas y del mismo tamaño (`aspectRatio(1f)`), incluida la que queda sola en la última fila.
- [Agregado] Widget 2x1 "Nivel del tanque" (`feature/widget`, Jetpack Glance): muestra el porcentaje, los litros sobre la capacidad y un ícono de wifi que indica si el tanque envió una lectura en los últimos 10 minutos. Consulta ThingSpeak una vez por hora (`updatePeriodMillis`) para ahorrar batería y datos; al tocarlo abre la app. Usa colores turquesa propios. Recursos: `widget_nivel_tanque_info.xml`, `widget_fondo`, `widget_circulo`, `widget_pastilla`, `ic_wifi` e `ic_wifi_off`.
- [Cambiado] El dibujo del tanque de Mi depósito se separó en `dibujarIndicadorNivel()` (con ola y colores opcionales) para que el widget lo reutilice sin animación.
- [Cambiado] La conexión con ThingSpeak es directa, como la de Maps: `ThingSpeakApi` hace un GET a la URL del canal (con la clave de lectura incluida) tanto para la última lectura como para el historial del detector de fugas, y arma las `LecturaSensor` con `org.json`. Ya no usa Retrofit ni DTOs; Retrofit sigue para la ANA y Supabase.
- [Cambiado] La URL y la clave de Supabase están en `SupabaseApi.kt`. La clave de Google Maps sigue en el `AndroidManifest.xml`.
- [Eliminado] `claves.properties`, la lectura de claves en `app/build.gradle.kts` (`Properties`, `manifestPlaceholders`, `buildConfigField`) y `buildConfig = true`.
- [Eliminado] Las comprobaciones "sin configurar" que ya no pueden ocurrir: `SensorRepository.estaConfigurado`, `RankingRepository.conectado` y la tarjeta "El ranking todavía no está conectado".
- [Eliminado] Código sin uso: íconos `ic_info`, `ic_reloj` e `ic_flecha_abajo`, `ExampleUnitTest.kt`, las dependencias `ui-tooling` / `ui-tooling-preview` (no hay `@Preview`) y un import sin uso.
- [Eliminado] Los comentarios `//` y `/* */` (incluidos los KDoc `/** */`) de todo el código propio: Kotlin de `app/src`, `app/build.gradle.kts` y `iot/wokwi/sketch.ino`. Los archivos de la plantilla del proyecto quedan como estaban.
- [Corregido] `SeccionRanking` usa `LocalLifecycleOwner` de `androidx.lifecycle.compose` (el de `compose.ui.platform` está obsoleto).
- Inicio reemplaza la barra inferior y muestra el nivel del depósito, un acceso a llenado y tarjetas independientes para Mapa, Represas, Ahorro, Liga del Agua y Recibos EPS.
- Se aplica la paleta azul al tema. Recibos EPS agrupa la consulta en línea y la lectura por foto. Liga usa el recibo guardado como referencia de consumo sin gestionar la cuenta EPS. Se reutilizan las claves locales para conservar los recibos anteriores.

### 2026-11-18
- [Validado] Flujo EPS completo en el Samsung conectado con cuenta y recibo reales: sesión persistida, descarga del PDF, extracción de periodo/consumo/total y guardado. Se retiró el diagnóstico temporal. La lectura de casillas pequeñas usa un recorte ampliado; las cifras se asocian por posición, sin inferir consumo a partir de lecturas o importes.
- [Corregido] Consulta EPS: navegación del menú dinámico CargarRecibo/MostrarRecibo, cabeceras AJAX/Referer y visor con iframe. Lectura del texto digital con PDFBox Android y respaldo OCR de mayor resolución con alineación de etiquetas y cifras por posición; soporte de «Volumen Fac m³», valores separados y asteriscos del importe.
- [Cambiado] Por solicitud del usuario, acceso EPS mediante HTTP limitado a `oficinavirtual.epstacna.com.pe` (sin subdominios). Advertencia visible de credenciales y sesión sin cifrado en tránsito. Se mantiene el cifrado local, el bloqueo de destinos externos y la validación TLS de HTTPS.
- [Agregado] Conector EPS en validación: vinculación mediante el formulario del portal, sesión AES/GCM en Android Keystore, consulta del PDF por HTTPS y extracción local de consumo, periodo y total a pagar. Incluye desvinculación, errores de sesión/certificado/formato y pruebas con respuestas sintéticas. La primera implementación requería validación real; se completó para un recibo tras adaptar el menú AJAX y el OCR. El acceso HTTP al portal fue solicitado por el usuario.
- [Agregado] Liga del Agua, recibo verificado y duelos: la app lee los m³ de la foto del recibo dentro del teléfono con ML Kit (`com.google.mlkit:text-recognition`); la foto no se guarda ni se envía. El hogar queda "Verificado" solo si el número salió de la foto, el usuario no lo cambió y el recibo tiene hasta 2 meses; su dato pesa el doble en la mediana. Duelos semanales entre distritos que ya compiten, con rivales que rotan cada semana.
- [Agregado] Liga del Agua, ranking real: consentimiento explícito, envío semanal anónimo (código anónimo, distrito, litros por persona y puntaje) y ranking de distritos con la mediana de sus hogares; un distrito necesita al menos 5 hogares para competir. El mapa pinta cada distrito según su puntaje real. Salir de la Liga borra los datos enviados. El servidor es Supabase: ejecutar `supabase/liga_del_agua.sql` y poner `SUPABASE_URL` y `SUPABASE_ANON_KEY` en `claves.properties`; sin esas claves el ranking queda desconectado y el resto de la app funciona igual.
- [Agregado] Liga del Agua, medición real (`feature/liga`): pantalla abierta desde el mapa donde el usuario marca cuánta agua hay en su tanque. El consumo sale de lo que bajó el nivel entre dos marcas, descartando los tramos con llenados; hacen falta al menos 24 horas medidas. Puntaje semanal del hogar: mejora frente a su propio consumo habitual (50), retos de Ahorro (30) y constancia (20). Las marcas se guardan solo en el teléfono (`liga.db`).
- [Agregado] Mapa por distritos: límites reales de Tacna, Alto de la Alianza, Ciudad Nueva, Pocollay y Coronel Gregorio Albarracín Lanchipa en `app/src/main/assets/distritos_tacna.geojson` (OpenStreetMap, licencia ODbL, con el ubigeo del INEI). Con el mapa en pantalla completa, al tocar un distrito se muestra su tarjeta. Los distritos salen en gris porque la Liga del Agua aún no tiene datos de hogares: la app no muestra puntajes inventados. El distrito del usuario se calcula en el teléfono y la ubicación no se envía a ningún servidor.

### 2026-11-12
- [Agregado] Pestaña Ahorro con retos de la semana, racha, calendario e insignias. Los retos se registran por día y funcionan sin internet.
- [Agregado] Mapa en pantalla completa: al tocar el mapa se ocultan el título y la lista; "Volver a la lista" o el botón atrás lo devuelven a su tamaño. El permiso de ubicación y el botón pasan a `ControlesMapa.kt`.
- [Agregado] Mapa, aviso "Antes de que se acabe": cruza la hora en que se acaba el agua del tanque (`Deposito.agotamientoProyectado()`) con el próximo llenado y con las cisternas. Si el agua no alcanza, una tarjeta coral dice cuándo se acaba y a qué cisterna ir ("Ve a Viñani (a 3.8 km): reparte hasta la 1:00 p.m."); si alcanza, una tarjeta celeste sugiere la más conveniente por si se necesita más. La cisterna sugerida se marca "Te conviene" en la lista. Nuevo caso de uso `ElegirCisternaConveniente` (la más cercana que no terminó y no pasó su hora de fin) y 13 pruebas unitarias nuevas (27 en total en `feature/mapa`). `MapaViewModel` ahora recibe `DepositoRepository` y el reloj (Koin).
- [Cambiado] Mapa: el aviso queda fijo sobre la lista y la lista se desplaza en un panel más bajo, para que el mapa conserve su espacio.

### 2026-11-11
- [Agregado] Feature `mapa`, cisternas cercanas en pantalla: al abrir el mapa con permiso de ubicación, se lee la ubicación una sola vez (`UbicacionRepository`, con `LocationManagerCompat`, sin guardar la coordenada) y bajo el mapa aparece la lista "Cisternas cercanas" con la distancia ("a 1.2 km"), el horario y el estado. Sin permiso, sin ubicación o sin puntos a menos de 10 km, se listan todos los puntos con un aviso. Al tocar una fila, el mapa se centra en ese punto y abre su ficha. Leyenda de colores (Activo, En ruta, Terminado) sobre el mapa. `UbicacionRepository` se registra en `moduloApp` (Koin).
- [Agregado] Feature `mapa`, dominio de cisternas cercanas: `Coordenada.distanciaKmHasta` (distancia real con la fórmula de Haversine), el modelo `CisternaCercana` y el caso de uso `BuscarCisternasCercanas` (descarta los puntos a más de 10 km y ordena del más cercano al más lejano, incluidos los que ya terminaron). Con 14 pruebas unitarias en `app/src/test/.../feature/mapa/` (distancia, búsqueda y formato de la distancia).
- [Agregado] `iot/wokwi/`: código del ESP32 con sensor ultrasónico que publica el nivel del tanque en ThingSpeak.
- [Agregado] `claves.properties`: claves compartidas del equipo; `local.properties` tiene prioridad si define una clave.
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

**Validación del ajuste HTTP (18/11/2026):** `:app:testDebugUnitTest` y `:app:assembleDebug` completaron correctamente: 78 pruebas, incluidas 19 de EPS, sin fallos. Se comprobó que la política Android bloquea HTTP por defecto y lo permite solo para el host exacto de la oficina virtual. La consulta autenticada del recibo real continúa pendiente.

**Validación final del conector EPS (18/11/2026):** 89 pruebas unitarias aprobadas (30 de EPS), APK Debug compilado y actualización completa verificada en el Samsung con una cuenta y un recibo reales, incluyendo el guardado desde EpsViewModel. La verificación cubre ese formato de recibo; no garantiza disponibilidad del portal ni otros diseños. Se retiraron la actividad temporal y los puntos de diagnóstico. No se incorporaron datos personales, cookies, contraseñas ni documentos reales al repositorio.
