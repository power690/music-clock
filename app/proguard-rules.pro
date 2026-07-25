-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*,Exceptions,Deprecated

-keep public class * extends java.lang.Exception
-keep public class * extends java.lang.Error

-keep class com.xiaowei.music.** { *; }
-keepclassmembers class com.xiaowei.music.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Application
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment

-keepclassmembers class * {
    public void on*Click(android.view.View);
}
-keepclassmembers class * extends android.app.Service {
    public void onStartCommand(android.content.Intent, int, int);
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class android.support.** { *; }
-dontwarn android.support.**

-keep class androidx.** { *; }
-dontwarn androidx.**

-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

-keep class com.mpatric.** { *; }
-dontwarn com.mpatric.**

-keep class org.jetbrains.** { *; }
-dontwarn org.jetbrains.**

-keep class kotlin.** { *; }
-dontwarn kotlin.**

-keep class com.github.hehang0.** { *; }
-dontwarn com.github.hehang0.**

-dontwarn org.jetbrains.annotations.**
-dontwarn javax.lang.model.element.**
-dontwarn java.lang.invoke.StringConcatFactory
