import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "id.rhiquest.mozzic"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.rhiquest.mozzic"
        minSdk = 24
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

    viewBinding {
        enable = true
    }

    buildFeatures {
        viewBinding = true
    }

    kotlin {
        jvmToolchain(17)
    }

    applicationVariants.all {
        val variantName = name
        val appVersionName = versionName ?: "1.0"
        val appVersionCode = versionCode

        val dateString = SimpleDateFormat("ddMMyyyy").format(Date())
        val finalApkName = "Mozzic_${appVersionName}_${dateString}_${appVersionCode}_${variantName}.apk"
        outputs.all {
            val defaultOutput = this as? com.android.build.gradle.internal.api.ApkVariantOutputImpl
            defaultOutput?.outputFileName = finalApkName
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.glide)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    implementation(libs.lifecycle.service)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.viewmodel)
    implementation(libs.fragment.ktx)

    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    implementation(libs.youtube.player)

    implementation(libs.room.db)
    implementation(libs.room.ktx)
    kapt(libs.androidx.room.compiler)

    implementation(libs.lottie.animation)

    implementation(libs.androidx.media)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}