plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "io.github.draumaz.nickel"
    compileSdk = 37

    defaultConfig {
        applicationId = "xyz.draumaz.nickel"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // No release keystore yet: sign with the local debug key so the release variant
            // installs straight from Android Studio.
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            // Installs alongside release as its own app, with the wrench icon from src/debug.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)
    // Installs the bundled baseline profile on sideloaded installs too, where the Play Store
    // isn't there to do it, so release starts precompiled instead of running interpreted.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    testImplementation(libs.junit)
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}

// The baseline profile plugin builds and installs release-like variants to profile, and
// Gradle uninstalls them afterwards. Under the release id that would wipe the real ledger,
// so they install as their own app. Profile rules match classes, not the package.
androidComponents {
    onVariants(selector().all()) { variant ->
        val type = variant.buildType ?: return@onVariants
        if (type == "nonMinifiedRelease" || type.startsWith("benchmark")) {
            variant.applicationId.set("xyz.draumaz.nickel.profiling")
        }
    }
}
