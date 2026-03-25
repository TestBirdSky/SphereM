import com.android.build.api.dsl.Packaging
import com.github.megatronking.stringfog.plugin.StringFogExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("stringfog")
    // todo test
    id("com.google.gms.google-services")
//    id("com.google.firebase.crashlytics")

    id("applovin-quality-service")
}

applovin {
    // todo modify
    apiKey = "«ad-review-key»"
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
        versionName = "1.0.2"

        ndk { //noinspection ChromeOsAbiSupport
            abiFilters += setOf("arm64-v8a", "armeabi-v7a")
        }
        setProperty("archivesBaseName", "${rootProject.name}_v${versionName}")
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
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        jniLibs.keepDebugSymbols.add("*/arm64-v8a/libdu.so")
        jniLibs.keepDebugSymbols.add("*/armeabi-v7a/libdu.so")
        jniLibs.keepDebugSymbols.add("*/x86/libdu.so")
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

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }

}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.browser) // Room https://developer.android.com/training/data-storage/room
    implementation(libs.androidx.room.runtime)
    implementation(libs.play.services.ads.api)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.mmkv)
    implementation(libs.xor)
    implementation(libs.glide)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.okhttp)
    implementation(libs.ttsdk.player.standard)

    // https://github.com/scwang90/SmartRefreshLayout?tab=readme-ov-file
    implementation(libs.refresh.layout.kernel)
    implementation(libs.refresh.header.material)
    implementation(libs.refresh.footer.ball) // Gson
    implementation(libs.gson)
    implementation(libs.facebook.android.sdk)

    // installReferrer
    implementation(libs.installreferrer)

    // admob
//    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:0.20.0-beta01")
    implementation("com.google.android.gms:play-services-ads:24.9.0")
    implementation("com.google.ads.mediation:applovin:13.5.0.0")
    implementation("com.google.ads.mediation:chartboost:9.11.0.0")
    implementation("com.google.ads.mediation:fyber:8.4.2.0")
    implementation("com.google.ads.mediation:inmobi:11.1.0.0")
    implementation("com.google.ads.mediation:ironsource:9.2.0.0")
    implementation("com.google.ads.mediation:vungle:7.6.3.0")
    implementation("com.google.ads.mediation:facebook:6.21.0.0")
    implementation("com.google.ads.mediation:mintegral:17.0.61.0")
    implementation("com.google.ads.mediation:moloco:4.4.0.0")
    implementation("com.google.ads.mediation:pangle:7.8.5.2.0")
    implementation("com.unity3d.ads:unity-ads:4.16.5")
    implementation("com.google.ads.mediation:unity:4.16.5.0")
    // firebase
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-messaging")
//    implementation("com.google.firebase:firebase-crashlytics-ndk")

    // pangle 短剧sdk
    implementation("com.bytedance.dramaverse:pssdk:1.8.0.1")

    //adjust
    implementation("com.adjust.sdk:adjust-android:5.5.0")
    implementation("com.android.installreferrer:installreferrer:2.2")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")

    implementation("com.esotericsoftware.spine:spine-android:4.2.10")
    
    // 测试依赖
    testImplementation("junit:junit:4.13.2")

    // topon聚合Max
    api("io.github.alex-only:max_adapter_tpn:1.2.9")

    // Max 聚合 13.5.0
    implementation("com.applovin:applovin-sdk:13.5.0")
    implementation("com.applovin.mediation:bidmachine-adapter:3.5.1.0")
    implementation("com.applovin.mediation:bigoads-adapter:5.7.0.0")
    implementation("com.applovin.mediation:chartboost-adapter:9.11.0.0")
    implementation("com.google.android.gms:play-services-base:16.1.0")
    implementation("com.applovin.mediation:fyber-adapter:8.4.2.0")
    implementation("com.applovin.mediation:google-ad-manager-adapter:24.9.0.0")
    implementation("com.applovin.mediation:google-adapter:24.9.0.0")
    implementation("com.applovin.mediation:inmobi-adapter:11.1.0.0")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("com.applovin.mediation:ironsource-adapter:9.2.0.0.0")
    implementation("com.applovin.mediation:vungle-adapter:7.6.3.0")
    implementation("com.applovin.mediation:facebook-adapter:6.21.0.0")
    implementation("com.applovin.mediation:mintegral-adapter:17.0.61.0")
    implementation("com.applovin.mediation:moloco-adapter:4.4.0.0")
    implementation("com.applovin.mediation:bytedance-adapter:7.8.5.2.0")
    implementation("com.applovin.mediation:unityads-adapter:4.16.5.0")

    // topon聚合 6.5.52
    //TU (Necessary)
    api("com.thinkup.sdk:core-tpn:6.5.52")
    //Androidx (Necessary)
    api("androidx.appcompat:appcompat:1.6.1")
    api("androidx.browser:browser:1.4.0")
    //Vungle
    api("com.thinkup.sdk:adapter-tpn-vungle:7.6.1.1.0")
    api("com.vungle:vungle-ads:7.6.1")
    api("com.google.android.gms:play-services-basement:18.1.0")
    api("com.google.android.gms:play-services-ads-identifier:18.0.1")
    //Ironsource
    api("com.thinkup.sdk:adapter-tpn-ironsource:9.2.0.1.0")
    api("com.unity3d.ads-mediation:mediation-sdk:9.2.0")
    api("com.google.android.gms:play-services-appset:16.0.2")
    api("com.google.android.gms:play-services-ads-identifier:18.0.1")
    api("com.google.android.gms:play-services-basement:18.1.0")
    //Bigo
    api("com.thinkup.sdk:adapter-tpn-bigo:5.7.0.1.0")
    api("com.bigossp:bigo-ads:5.7.0")
    //Pangle
    api("com.thinkup.sdk:adapter-tpn-pangle:7.8.5.2.1.0")
    api("com.pangle.global:pag-sdk:7.8.5.2")
    api("com.google.android.gms:play-services-ads-identifier:18.2.0")
    //Kwai
    api("com.thinkup.sdk:adapter-tpn-kwai:1.2.21.1.0")
    api("io.github.kwainetwork:adApi:1.2.21")
    api("io.github.kwainetwork:adImpl:1.2.21")
    api("androidx.media3:media3-exoplayer:1.0.0-alpha01")
    api("androidx.appcompat:appcompat:1.6.1")
    api("com.google.android.material:material:1.2.1")
    api("androidx.annotation:annotation:1.2.0")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.4.10")
    api("com.google.android.gms:play-services-ads-identifier:18.0.1")
    //Facebook
    api("com.thinkup.sdk:adapter-tpn-facebook:6.21.0.1.0")
    api("com.facebook.android:audience-network-sdk:6.21.0")
    api("androidx.annotation:annotation:1.0.0")
    //Admob
    api("com.thinkup.sdk:adapter-tpn-admob:24.9.0.1.0")
    api("com.google.android.gms:play-services-ads:24.9.0")
    //Inmobi
    api("com.thinkup.sdk:adapter-tpn-inmobi:11.1.1.1.0")
    api("com.inmobi.monetization:inmobi-ads-kotlin:11.1.0")
    //TU Adx SDK(Necessary)
    api("com.thinkup.sdk:adapter-tpn-sdm:6.5.54.1.0")
    api("com.smartdigimkttech.sdk:smartdigimkttech-sdk:6.5.54")
    //AppLovin
    api("com.thinkup.sdk:adapter-tpn-applovin:13.5.0.1.0")
    api("com.applovin:applovin-sdk:13.5.0")
    //Mintegral
    api("com.thinkup.sdk:adapter-tpn-mintegral:17.0.41.1.0")
    api("com.mbridge.msdk.oversea:mbridge_android_sdk:17.0.41")
    api("androidx.recyclerview:recyclerview:1.1.0")
    //Chartboost
    api("com.thinkup.sdk:adapter-tpn-chartboost:9.11.0.1.1")
    api("com.chartboost:chartboost-sdk:9.11.0")
    api("com.chartboost:chartboost-mediation-sdk:5.3.0")
    api("com.chartboost:chartboost-core-sdk:1.1.0")
    api("com.chartboost:chartboost-mediation-adapter-chartboost:5.9.11.0.0")
    api("androidx.media3:media3-exoplayer:1.4.1")
    api("androidx.media3:media3-ui:1.4.1")
    api("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    api("com.squareup.okhttp3:logging-interceptor:4.11.0")
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("com.squareup.retrofit2:converter-scalars:2.9.0")
    api("com.squareup.retrofit2:retrofit:2.9.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    //Fyber
    api("com.thinkup.sdk:adapter-tpn-fyber:8.3.7.1.0")
    api("com.fyber:marketplace-sdk:8.3.7")
    api("com.google.android.gms:play-services-ads-identifier:18.0.1")
    //Tramini
    api("com.thinkup.sdk:tramini-plugin-tpn:6.5.52")

    //Moloco
    api("com.thinkup.sdk:adapter-tpn-moloco:4.3.1.1.0")
    api("com.moloco.sdk:moloco-sdk:4.3.1")

}