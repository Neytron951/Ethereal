plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.neytron.ethereal"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.neytron.ethereal"
        minSdk = 27
        targetSdk = 37
        versionCode = 14
        versionName = "1.9.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    signingConfigs {
        getByName("debug") {
            // Отладочный ключ лежит в проекте, чтобы сборка не зависела от
            // C:\Users\<user>\.android (внешняя папка может быть недоступна на запись)
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")

    // Firebase / AdMob / Crashlytics удалены полностью

    // === WorkManager (фоновые проверки обновлений) ===
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    // === Markdown Renderer (changelog) ===
    implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.30.0")
    implementation("com.mikepenz:multiplatform-markdown-renderer-code:0.30.0")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}