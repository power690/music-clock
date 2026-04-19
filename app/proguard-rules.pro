-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.xiaowei.music.** { *; }
-keepclassmembers class * {
    public void on*Click(android.view.View);
}
-keepclassmembers class * extends android.app.Service {
    public void onStartCommand(android.content.Intent, int, int);
}
-keep class android.support.** { *; }
-dontwarn android.support.**
