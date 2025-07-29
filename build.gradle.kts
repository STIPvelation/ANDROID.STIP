// Root build.gradle.kts (Project-level)
buildscript {
    dependencies {
        classpath(libs.androidx.navigation.safe.args.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
}

// ✅ 더 이상 allprojects { repositories { ... } } 블럭은 사용하지 않음
//    레포지토리 설정은 settings.gradle.kts에만 작성 (이미 되어 있음!)
