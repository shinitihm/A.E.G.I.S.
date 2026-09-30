import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Endereço do backend FastAPI. Configure em local.properties:
//   aegis.api.url=http://10.0.2.2:8000/        (emulador -> seu PC)
//   aegis.api.url=http://192.168.0.10:8000/    (celular na mesma rede Wi-Fi)
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
// O Retrofit derruba o app se a URL não tiver http:// ou a "/" final, então corrigimos aqui.
val apiUrl: String = localProps.getProperty("aegis.api.url", "http://10.0.2.2:8000/")
    .trim().trim('"')
    .let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
    .let { if (it.endsWith("/")) it else "$it/" }

android {
    namespace = "com.example.aegis"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.aegis"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
    }

    buildFeatures {
        buildConfig = true
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.biometric)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.glide)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}