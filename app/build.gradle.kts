plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("kotlin-kapt") // Glide, Room 등 annotation processor
    id("androidx.navigation.safeargs.kotlin")
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.stip.stip"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stip.stip"
        minSdk = 24
        targetSdk = 34
        versionCode = 11
        versionName = "2.0.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            // Only configure signing if all required properties are present
            if (project.hasProperty("RELEASE_STORE_FILE") &&
                project.hasProperty("RELEASE_STORE_PASSWORD") &&
                project.hasProperty("RELEASE_KEY_ALIAS") &&
                project.hasProperty("RELEASE_KEY_PASSWORD")) {
                storeFile = file(project.property("RELEASE_STORE_FILE") as String)
                storePassword = project.property("RELEASE_STORE_PASSWORD") as String
                keyAlias = project.property("RELEASE_KEY_ALIAS") as String
                keyPassword = project.property("RELEASE_KEY_PASSWORD") as String
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
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

    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }
    
    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {

    // ───────────────────────────────── AndroidX Core ─────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.viewpager2)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // ───────────────────────────────── UI / 디자인 ─────────────────────────────────
    implementation(libs.material)
    implementation(libs.hdodenhof.circleimageview)
    implementation(libs.lottie)

    // ───────────────────────────────── Glide ─────────────────────────────────
    implementation(libs.github.glide.core)
    kapt(libs.github.glide.compiler)
    implementation(libs.glide.transformations) // blur, crop 등 확장


    // ───────────────────────────────── MPAndroidChart ─────────────────────────────────
    implementation(libs.github.mpandroidchart)

    // ───────────────────────────────── WebView for Lightweight Chart ─────────────────────────────────
    implementation("androidx.webkit:webkit:1.8.0")

    // ───────────────────────────────── Coroutine ─────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ───────────────────────────────── Gson ─────────────────────────────────
    implementation(libs.google.gson)

    // ───────────────────────────────── Retrofit & OkHttp ─────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.retrofit.adapter.rxjava)
    implementation(libs.okhttp3.interceptor)

    // ───────────────────────────────── Room DB ─────────────────────────────────
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // ───────────────────────────────── Hilt DI ─────────────────────────────────
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // ───────────────────────────────── Navigation ─────────────────────────────────
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // ───────────────────────────────── 기타 ─────────────────────────────────
    implementation(libs.prof18.rssparser)
    implementation(libs.zxing.android.embedded)
    implementation(libs.androidx.biometric)
    implementation(libs.sandwich)
    implementation(libs.rx.binding)

    // ───────────────────────────────── 테스트 ─────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
