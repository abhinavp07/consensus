plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.abhinavpinisetti.consensus"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.abhinavpinisetti.consensus"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

secrets {
    // Real key goes in local.properties (git-ignored); this file only holds a placeholder.
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // AndroidX + Material
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.fragment:fragment:1.8.5")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-ai")

    // Google Sign-In via Credential Manager
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Required by Firebase AI Logic when calling it from Java
    implementation("com.google.guava:guava:33.3.1-android")
    implementation("org.reactivestreams:reactive-streams:1.0.4")

    // Maps + Places
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.android.libraries.places:places:4.4.1")

    // Utilities
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    testImplementation("junit:junit:4.13.2")
}
