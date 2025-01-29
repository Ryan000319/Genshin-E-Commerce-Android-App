plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    kotlin("kapt")
}

android {
    namespace = "com.example.genshin_shop"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.genshin_shop"
        minSdk = 28
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX libraries
    implementation(libs.androidx.core.ktx.v1150)
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout.v220)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.activity.ktx)

    // Firebase BOM and libraries
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.google.firebase.auth.ktx)
    implementation(libs.google.firebase.database.ktx)
    implementation(libs.google.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.google.firebase.messaging.ktx)

    // Third-party libraries
    implementation(libs.glide)
    implementation(libs.volley)
    implementation(libs.google.exoplayer)
    implementation(libs.androidx.media3.ui)
    kapt(libs.compiler)
    implementation(libs.dotsindicator)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Stripe
    implementation(libs.stripe.android)

    implementation(libs.androidx.fragment.ktx)

    implementation ("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation ("androidx.media3:media3-exoplayer:1.5.0")
    implementation ("androidx.media3:media3-ui:1.5.0")

    // For offline download functionality
    implementation ("androidx.media3:media3-datasource-okhttp:1.0.2")


}



