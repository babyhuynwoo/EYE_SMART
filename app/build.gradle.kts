import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.eye_smart"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.eye_smart"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }

        val eyedIdApiKey = localProperties.getProperty("EYEDID_API_KEY") ?: ""
        val serverIpAddress = localProperties.getProperty("SERVER_IP_ADDRESS") ?: ""

        // buildConfigField 설정
        buildConfigField("String", "EYEDID_API_KEY", "\"${eyedIdApiKey}\"")
        buildConfigField("String", "SERVER_IP_ADDRESS", "\"${serverIpAddress}\"")

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

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.eyedid.gazetracker)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
