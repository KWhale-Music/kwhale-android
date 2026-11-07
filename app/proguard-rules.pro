# kotlinx.serialization — keep generated serializers and companion objects
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class org.dueattendant149.kwhale.**$$serializer { *; }
-keepclassmembers class org.dueattendant149.kwhale.** { *** Companion; }
-keepclasseswithmembers class org.dueattendant149.kwhale.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Media3 / ExoPlayer (reflection + media session)
-keep class androidx.media3.** { *; }

# Glance appwidget
-keep class androidx.glance.** { *; }

# OkHttp / Coil
-dontwarn okhttp3.**
-dontwarn okio.**
