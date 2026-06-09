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
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontshrink
-keep class kotlin.jvm.internal.** { *; }
-keep public class com.android.installreferrer.** { *; }
-keep class com.bytedance.sdk.shortplay.** {*;}

-keep class cn.shuzilm.core.** {*;}

# ============ Gson 混淆规则 ============
# Gson 使用泛型反序列化对象时，会用到反射，需要保留相关类
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.google.gson.examples.android.model.** { <fields>; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 保留使用 Gson 反序列化的类（使用 @SerializedName 注解的类）
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}


# 保留 Kotlin data class 的构造函数和属性（Gson 需要）
-keepclassmembers class * {
    <init>(...);
}

# 保留使用 TypeToken 的泛型类
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken


# 保留 Parcelable 实现类（如果使用）
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keep class com.alex.** { *;}
-keepclassmembers public class com.alex.** {
   public *;
}
-keep class com.bytedance.sdk.** { *; }