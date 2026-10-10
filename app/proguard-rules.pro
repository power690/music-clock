-keepattributes Signature,InnerClasses,EnclosingMethod

-keep class com.xiaowei.music.NativeBlurUtils { *; }
-keep class com.xiaowei.music.KaraokeLyricView { *; }

-keep class androidx.media3.datasource.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-dontwarn org.jetbrains.annotations.**
-dontwarn javax.lang.model.element.**
-dontwarn java.lang.invoke.StringConcatFactory
