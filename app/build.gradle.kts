plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // REMOVIDO: id("org.jetbrains.kotlin.compose") - Não usaremos Compose
}

android {
    namespace = "com.example.neverdice2"
    compileSdk = 34  // CORRIGIDO: sintaxe correta

    defaultConfig {
        applicationId = "com.example.neverdice2"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
        }
    }

    // REMOVIDO: buildFeatures { compose = true } - Não usamos Compose
}

dependencies {
    // ===== DEPENDÊNCIAS BÁSICAS =====
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // ===== MQTT - HIVEMQ (MODERNO E COMPATÍVEL) =====
    implementation("com.hivemq:hivemq-mqtt-client:1.3.3")

    // ===== RENDERIZAÇÃO 3D =====
    implementation("io.github.sceneview:sceneview:2.2.1")
    implementation("com.google.android.filament:filament-android:1.53.4")
    implementation("com.google.android.filament:filament-utils-android:1.53.4")
    implementation("com.google.android.filament:gltfio-android:1.53.4")

    // ===== CORROTINAS =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ===== TESTES =====
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}