# Keep Retrofit/Gson models
-keep class com.clipboardmemory.network.** { *; }
-keep class com.clipboardmemory.data.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }