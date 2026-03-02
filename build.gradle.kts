buildscript {
    repositories {
        mavenCentral()
        google()
        // AppLovin Quality Service 插件需要从 AppLovin Maven 仓库获取
        maven { url = uri("https://artifacts.applovin.com/android") }
    }
    dependencies {
        //noinspection UseTomlInstead
        classpath("com.github.megatronking.stringfog:gradle-plugin:5.2.0")
        classpath("com.github.megatronking.stringfog:xor:5.0.0")
        // AppLovin Quality Service 插件 - 使用动态版本（文档中只显示 + 号）
        // 如果构建失败，可以尝试注释掉这行和 app/build.gradle.kts 中的插件引用
        classpath ("com.applovin.quality:AppLovinQualityServiceGradlePlugin:+")
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}