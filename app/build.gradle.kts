import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.example.aguardapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.aguardapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Claves: primero local.properties (propias de cada PC); si no están, claves.properties (compartido en el repo).
        fun leer(archivo: String) = Properties().apply {
            rootProject.file(archivo).takeIf { it.exists() }?.inputStream()?.use(::load)
        }
        val locales = leer("local.properties")
        val compartidas = leer("claves.properties")
        fun clave(nombre: String): String =
            locales.getProperty(nombre)?.takeIf { it.isNotBlank() } ?: compartidas.getProperty(nombre, "")

        manifestPlaceholders["MAPS_API_KEY"] = clave("MAPS_API_KEY")

        // Canal de ThingSpeak donde el sensor del tanque (ESP32) publica el nivel.
        buildConfigField("String", "THINGSPEAK_CHANNEL_ID", "\"${clave("THINGSPEAK_CHANNEL_ID")}\"")
        buildConfigField("String", "THINGSPEAK_READ_KEY", "\"${clave("THINGSPEAK_READ_KEY")}\"")

        // Proyecto de Supabase del ranking de la Liga del Agua. Vacío: el ranking queda desconectado.
        buildConfigField("String", "SUPABASE_URL", "\"${clave("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${clave("SUPABASE_ANON_KEY")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Android y Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Datos locales
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.datetime)

    // Mapas
    implementation(libs.maps.compose)

    // Red
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Inyección de dependencias
    implementation(libs.koin.androidx.compose)

    testImplementation(libs.junit)
}
