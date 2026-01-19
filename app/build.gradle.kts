import com.android.build.api.dsl.Packaging
import com.github.megatronking.stringfog.plugin.StringFogExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("stringfog")
    //id("com.google.gms.google-services")
    //id("com.google.firebase.crashlytics")
}

apply(plugin = "stringfog")

configure<StringFogExtension> {
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    enable = true
    fogPackages = arrayOf("com.sphere.shortvideos")
    kg = com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator()
    mode = com.github.megatronking.stringfog.plugin.StringFogMode.bytes
}

android {
    namespace = "com.sphere.shortvideos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rapid.short.tv"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += setOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    fun Packaging.() {
        jniLibs.useLegacyPackaging = true
    }
    bundle.language.enableSplit = false
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    configurations.all {
        exclude(group = "com.google.android.gms", module = "play-services-ads")
        exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
    }

}

dependencies {
//    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.browser)
    // Room https://developer.android.com/training/data-storage/room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.mmkv)
    implementation(libs.xor)
    implementation(libs.glide)
    implementation(libs.okhttp)
    implementation(libs.ttsdk.player.standard)

    // https://github.com/scwang90/SmartRefreshLayout?tab=readme-ov-file
    implementation(libs.refresh.layout.kernel)
    implementation(libs.refresh.header.material)
    implementation(libs.refresh.footer.ball)
    // Gson
    implementation(libs.gson)
    implementation(libs.facebook.android.sdk)

    // installReferrer
    implementation(libs.installreferrer)

    // admob
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:0.20.0-beta01")
    implementation("com.google.ads.mediation:pangle:7.6.0.2.0")
    implementation("com.google.ads.mediation:facebook:6.20.0.1")
    implementation("com.google.ads.mediation:applovin:13.4.0.0")
    implementation("com.google.ads.mediation:mintegral:16.9.91.1")
    implementation("com.google.ads.mediation:vungle:7.5.1.0")
    implementation("com.unity3d.ads:unity-ads:4.16.1")
    implementation("com.google.ads.mediation:unity:4.16.1.0")
    // firebase
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-crashlytics-ndk")

    implementation("com.bytedance.dramaverse:pssdk:1.8.0.1")
}