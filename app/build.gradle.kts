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

        // La API key de Google Maps se lee de local.properties (no se sube al repositorio).
        val propiedadesLocales = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
        }
        manifestPlaceholders["MAPS_API_KEY"] = propiedadesLocales.getProperty("MAPS_API_KEY", "")

        // Canal de ThingSpeak donde el sensor del tanque (ESP32) publica el nivel.
        buildConfigField("String", "THINGSPEAK_CHANNEL_ID", "\"${propiedadesLocales.getProperty("THINGSPEAK_CHANNEL_ID", "")}\"")
        buildConfigField("String", "THINGSPEAK_READ_KEY", "\"${propiedadesLocales.getProperty("THINGSPEAK_READ_KEY", "")}\"")
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
