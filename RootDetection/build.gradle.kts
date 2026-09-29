plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.netlify.dev4rju9.rootdetection"
    ndkVersion = "30.0.16248370"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 29
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // arm64-v8a + x86_64 cover every real 64-bit device/emulator;
            // armeabi-v7a kept for older 32-bit hardware still in the field.
            // 32-bit x86 (emulator-only, effectively unused on real devices)
            // is dropped to keep the APK smaller.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                // Exceptions deliberately left enabled: the JNI entry point
                // wraps its body in try/catch so an unexpected allocation
                // failure degrades to "no native signal" instead of
                // std::terminate()-ing the process.
                cppFlags += listOf("-std=c++17", "-fno-rtti")
            }
        }

    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        publishing {
            singleVariant("release") {
                withSourcesJar()
                withJavadocJar()
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.annotation)
}

version = project.property("ROOT_DETECTION_VERSION").toString()
extra["ARTIFACT_ID"] = project.property("ROOT_DETECTION").toString()
apply(from = "${rootProject.projectDir}/publish-module.gradle.kts")