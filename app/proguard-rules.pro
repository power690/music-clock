-keepattributes Signature,InnerClasses,EnclosingMethod

-keep class com.xiaowei.music.NativeBlurUtils { *; }
-keep class com.xiaowei.music.KaraokeLyricView { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-dontwarn org.jetbrains.annotations.**
-dontwarn javax.lang.model.element.**
-dontwarn java.lang.invoke.StringConcatFactory
