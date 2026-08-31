# Aturan ProGuard/R8 untuk Kuronoa Expense Tracker.

# Moshi (JSON) — pertahankan model data & metadata Kotlin.
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keepnames @kotlin.Metadata class com.kuronoa.expensetracker.data.remote.**
-keep class com.kuronoa.expensetracker.data.remote.** { *; }
-keep class com.kuronoa.expensetracker.core.model.** { *; }
-dontwarn kotlin.**

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Room
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
