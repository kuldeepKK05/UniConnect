# ── UniConnect ProGuard Rules ──────────────────────────────────

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep our model classes (used by Gson serialization)
-keep class com.uniconnect.models.** { *; }
-keep class com.uniconnect.network.ApiService$** { *; }
-keep class com.uniconnect.network.ApiResponseWrapper { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# JWT Decode
-keep class com.auth0.android.** { *; }

# Google Play Services
-keep class com.google.android.gms.** { *; }

# Material Design
-keep class com.google.android.material.** { *; }

# ViewBinding – keep generated binding classes
-keep class com.uniconnect.databinding.** { *; }

# Shimmer
-keep class com.facebook.shimmer.** { *; }

# General AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Prevent stripping of BuildConfig
-keep class com.uniconnect.BuildConfig { *; }

# Suppress warnings for missing classes in dependencies
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
