plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")

    id("kotlinx-serialization")
}

android {
    namespace = "com.example.smartrecept"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.smartrecept"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DEEPSEEK_API_KEY", "\"${project.findProperty("deepseek.api.key") ?: ""}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("gemini.api.key") ?: ""}\"")

        val groqApiKey = providers.gradleProperty("groq.api.key")
            .orNull ?: ""
            .trim()  // <-- Добавляем trim() для удаления пробелов

        println("Groq key from gradle: '$groqApiKey'") // Для отладки

        buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // --- Платформа (BOM - Bill of Materials) ---
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))

    // --- Основные зависимости AndroidX ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.1") // Для совместимости тем

    // --- База данных Room ---
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-config")

    // --- Firebase AI (LIVE и обычная генерация) ---
    implementation("com.google.firebase:firebase-ai:0.3.0-beta01")

    // --- Gemini AI (через Google SDK) ---
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // --- DataStore для настроек ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // --- UI и Compose ---
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.34.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.1")
    implementation("androidx.compose.material3:material3") // Основная библиотека Material3, версия из BOM
    implementation("androidx.compose.material:material-icons-extended:1.6.0") // Иконки
    implementation("androidx.compose.material3:material3-window-size-class") // Для адаптивного дизайна
    implementation("androidx.compose.runtime:runtime-livedata") // Для связки LiveData и Compose

    // --- Навигация ---
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // --- Вспомогательные библиотеки ---
    implementation(libs.firebase.crashlytics.buildtools) // Удаленная база
    implementation("io.coil-kt:coil-compose:2.6.0") // Загрузка изображений
    implementation("com.google.code.gson:gson:2.10.1") // Для JSON
    implementation("com.google.accompanist:accompanist-permissions:0.34.0") // Для работы с разрешениями

    // --- CameraX ---
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // --- Тестирование ---
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
