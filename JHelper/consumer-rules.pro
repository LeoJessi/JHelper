# ==============================================
# JHelper Library Consumer ProGuard Rules
# ==============================================
# 这些规则会自动传递给使用此库的 App

# ==================== 通用属性 ====================

# 保留注解
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# 保留泛型签名（Gson、Retrofit 等需要）
-keepattributes Signature

# 保留内部类
-keepattributes InnerClasses

# 保留异常信息
-keepattributes Exceptions

# 保留源文件名和行号（崩溃日志定位）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==================== JHelper 库公共 API ====================

# 保留所有公共类和方法
-keep public class top.jessi.jhelper.** {
    public protected *;
}

# 保留 IGlide 工具类
-keep class top.jessi.jhelper.image.IGlide { *; }
-keep class top.jessi.jhelper.image.IGlide$Companion { *; }
-keep interface top.jessi.jhelper.image.IGlide$GlideCallback { *; }

# 保留枚举
-keepclassmembers enum top.jessi.jhelper.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==================== Glide 4.x ====================

# Glide 核心
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}

# Glide Generated API
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }

# Glide OkHttp3 集成
-keep class com.bumptech.glide.integration.okhttp3.OkHttpGlideModule

# Glide 注解
-keep class com.bumptech.glide.annotation.** { *; }
-keep @com.bumptech.glide.annotation.GlideModule class * { *; }

# ==================== Gson 2.x ====================

# Gson 类
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# 保留使用 @SerializedName 注解的字段
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保留 TypeAdapter 和 TypeAdapterFactory
-keep class * extends com.google.gson.TypeAdapter { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

# 保留 Gson 内部类
-keep class com.google.gson.internal.** { *; }
-keep class com.google.gson.internal.bind.** { *; }

# ==================== Kotlin ====================

# Kotlin 标准库
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Kotlin 协程
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlin 反射
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# ==================== OkHttp3 ====================

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# OkHttp Platform
-keep class okhttp3.internal.platform.** { *; }
-keep class okhttp3.internal.tls.** { *; }

# ==================== AndroidX ====================

# AppCompat
-keep class androidx.appcompat.** { *; }
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends androidx.appcompat.app.Fragment

# Fragment
-keep class androidx.fragment.** { *; }
-keep public class * extends androidx.fragment.app.Fragment

# RecyclerView
-keep class androidx.recyclerview.** { *; }

# ConstraintLayout
-keep class androidx.constraintlayout.** { *; }

# ==================== AndroidSVG ====================

# AndroidSVG
-keep class com.caverock.androidsvg.** { *; }
-dontwarn com.caverock.androidsvg.**

# ==================== Android 通用 ====================

# Activity
-keep public class * extends android.app.Activity {
    public void *(android.view.View);
}

# Application
-keep public class * extends android.app.Application

# Service
-keep public class * extends android.app.Service

# BroadcastReceiver
-keep public class * extends android.content.BroadcastReceiver

# ContentProvider
-keep public class * extends android.content.ContentProvider

# View
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# 枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# R 文件
-keepclassmembers class **.R$* {
    public static <fields>;
}

# JavaScript 接口
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
