-keepattributes Signature,InnerClasses,EnclosingMethod

-keep class com.xiaowei.music.NativeBlurUtils { *; }
-keep class com.xiaowei.music.KaraokeLyricView { *; }

-keep class androidx.media3.decoder.flac.** { *; }
-keep class androidx.media3.extractor.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-dontwarn org.jetbrains.annotations.**
-dontwarn javax.lang.model.element.**
-dontwarn java.lang.invoke.StringConcatFactory
