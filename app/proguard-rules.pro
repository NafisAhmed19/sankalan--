# Add project specific ProGuard / R8 rules here.

# Preserve stack-trace line numbers while hiding original source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Kotlin metadata and runtime annotations required by Moshi KotlinJsonAdapterFactory.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,Signature,InnerClasses,EnclosingMethod
-keep class kotlin.Metadata { *; }

# Keep @Keep annotated Firestore models and cloud DTOs under R8 minification.
-keep @androidx.annotation.Keep class * { *; }
-keep class com.example.core.cloud.** { *; }

# Keep Moshi reflection adapters and serialized domain/archive/update payload models intact.
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.example.core.model.** { *; }
-keep class com.example.features.update.UpdateReleaseManifest { *; }
-keep class com.example.features.export.SankalanBackupManifest { *; }
-keep class com.example.features.editor.StrokePoint { *; }
-keep class com.example.features.editor.StrokePayload { *; }
-keep class com.example.features.editor.ImageFramePayload { *; }
-keep class com.example.features.studio.UserUploadedAsset { *; }
-dontwarn com.squareup.moshi.**
-dontwarn javax.annotation.**

# Keep Room database, entities, and DAOs.
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.core.data.local.** { *; }

# Keep manifest-declared Android entry points.
-keep class com.example.MainActivity { *; }
-keep class com.example.features.update.AppUpdateJobService { *; }

# Strip verbose/debug/info logs from release builds.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
