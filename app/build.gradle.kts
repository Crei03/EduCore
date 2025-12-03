import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Carga de variables desde .env para configurar URLs base de la API.
val envProps: Properties = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.inputStream().use { fis ->
            this.load(fis)
        }
    }
}

fun envOrDefault(key: String, default: String): String {
    val fromEnv = System.getenv(key)
    val fromFile = envProps.getProperty(key)
    return when {
        !fromEnv.isNullOrBlank() -> fromEnv
        !fromFile.isNullOrBlank() ->  fromFile
        else -> default
    }
}

val urlBase: String = envProps.getProperty("URL_BASE", "").ifBlank {
    System.getenv("URL_BASE") ?: throw GradleException(
        "Falta IP en la configuración."
    )
}

android {
    namespace = "com.proyect.educore"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.proyect.educore"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "LOGIN_URL",
            "\"$urlBase/Auth.php?action=login\""
        )
        buildConfigField(
            "String",
            "REGISTER_URL",
            "\"$urlBase/Auth.php?action=register\""
        )
        buildConfigField(
            "String",
            "TIPOS_TRAMITE_URL",
            "\"$urlBase/TiposTramite.php\""
        )
        buildConfigField(
            "String",
            "TURNOS_URL",
            "\"$urlBase/Turnos.php\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
