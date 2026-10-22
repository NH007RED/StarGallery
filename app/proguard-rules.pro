# Gson models used by Retrofit (reflection-based)
-keep class com.stargallery.app.data.model.** { *; }
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Retrofit
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-keep interface com.stargallery.app.data.remote.TmdbApi { *; }
-keep class com.stargallery.app.domain.** { *; }
