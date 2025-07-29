# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================= Gson 관련 규칙 =============================
# Gson 라이브러리 자체 보호
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses

# Gson 내부 클래스 보호
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Gson의 TypeToken 관련 클래스 보호
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Gson이 사용하는 리플렉션 관련 클래스 보호
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ============================= 추상 클래스 관련 Gson 에러 해결 =============================
# 추상 클래스나 인터페이스가 Gson에 의해 인스턴스화되는 것을 방지
-keep class * {
    @com.google.gson.annotations.SerializedName *;
}

# 모든 데이터 클래스의 기본 생성자 보호 (추상 클래스 문제 해결)
-keepclassmembers class * {
    public <init>();
}

# Gson이 사용하는 모든 클래스의 필드 보호
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 특정 패키지의 모든 클래스 보호 (v1.a 같은 축소된 클래스명 방지)
-keep class com.stip.** { *; }
-keep class com.stip.**.model.** { *; }
-keep class com.stip.**.data.** { *; }
-keep class com.stip.api.** { *; }

# ============================= Retrofit 관련 규칙 =============================
# Retrofit 인터페이스와 관련 클래스 보호
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Retrofit 서비스 인터페이스 보호
-keep interface com.stip.**.api.** { *; }
-keep interface com.stip.**.service.** { *; }

# ============================= 데이터 모델 클래스 보호 =============================
# API 응답 모델 클래스들 보호
-keep class com.stip.**.model.** { *; }
-keep class com.stip.**.data.** { *; }
-keep class com.stip.api.model.** { *; }

# Response 클래스들 보호
-keep class **.*Response { *; }
-keep class **.*Request { *; }
-keep class **.*Dto { *; }

# Gson이 사용하는 데이터 클래스의 필드와 생성자 보호
-keepclassmembers class com.stip.**.model.** {
    <fields>;
    <init>(...);
}

-keepclassmembers class com.stip.**.data.** {
    <fields>;
    <init>(...);
}

-keepclassmembers class com.stip.api.model.** {
    <fields>;
    <init>(...);
}

# ============================= Kotlin 관련 규칙 =============================
# Kotlin data class 보호
-keep @kotlin.Metadata class com.stip.**.model.**
-keep @kotlin.Metadata class com.stip.**.data.**
-keep @kotlin.Metadata class com.stip.api.model.**

# ============================= Hilt 관련 규칙 =============================
# Hilt DI 관련 클래스 보호
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ============================= OkHttp 관련 규칙 =============================
# OkHttp 관련 클래스 보호
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ============================= 기타 라이브러리 규칙 =============================
# Room 데이터베이스 관련 보호
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Navigation 관련 보호
-keep class androidx.navigation.fragment.NavHostFragment
-keepnames class * extends android.os.Parcelable

# ============================= 제네릭 타입 보호 =============================
# 제네릭 타입 정보를 유지하여 Gson의 TypeToken 문제 해결

# List, ArrayList 등 컬렉션 타입 보호
-keep class java.util.** { *; }
-keep class * implements java.util.List { *; }
-keep class * implements java.util.Map { *; }

# ApiResponse 같은 제네릭 래퍼 클래스 특별 보호
-keep class com.stip.**.api.model.ApiResponse { *; }
-keepclassmembers class com.stip.**.api.model.ApiResponse {
    <fields>;
    <methods>;
}

# 익명 클래스와 람다 표현식에서 사용되는 타입 보호
-keepclassmembers class * {
    synthetic <fields>;
}

# Reflection으로 접근하는 클래스의 기본 생성자 보호
-keepclassmembers class * {
    public <init>();
}