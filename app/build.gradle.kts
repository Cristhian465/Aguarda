plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

val urlApiCortes = providers.gradleProperty("CORTES_API_URL")
    .orElse("https://169.58.234.123:61873/")
    .get()

fun literalUrlApi(valor: String): String = "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

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
        buildConfigField("String", "CORTES_API_URL", literalUrlApi(urlApiCortes))
    }

    buildTypes {
        debug {
            // Reporte de cobertura de las pruebas unitarias: ./gradlew createDebugUnitTestCoverageReport
            enableUnitTestCoverage = true
        }
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.glance.appwidget)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.datetime)

    implementation(libs.maps.compose)

    implementation(libs.mlkit.text.recognition)
    implementation(libs.jsoup)
    implementation(libs.pdfbox.android)

    // Modo familia: QR de invitación
    implementation(libs.zxing.core)
    implementation(libs.play.services.code.scanner)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    implementation(libs.koin.androidx.compose)

    testImplementation(libs.junit)
}
