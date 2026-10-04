plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace  = "com.echo.discord"
    compileSdk = 37
    ndkVersion = "27.0.12077973"

    defaultConfig {
        minSdk = 29
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        externalNativeBuild {
            cmake { cppFlags += "-std=c++17" }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures { prefab = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // The Discord Social SDK is not committed. tools/fetch-discord-sdk.sh puts it in libs/.
    implementation(":discord_partner_sdk@aar")
    implementation(libs.androidx.browser)
    implementation(libs.timber)

    implementation(project(":core:core-domain"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
