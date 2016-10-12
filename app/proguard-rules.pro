# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\admin\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific toKeep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontwarn retrofit2.**
-dontwarn rx.**
-dontwarn butterknife.internal.**
-dontwarn com.squareup.okhttp.**
-dontwarn com.squareup.okhttp.internal.huc.**
-dontwarn retrofit.appengine.UrlFetchClient
-dontwarn okio.**
-dontwarn com.viewpagerindicator.**
-toKeep class com.squareup.okhttp.** { *; }
-toKeep interface com.squareup.okhttp.** { *; }
-toKeep class retrofit2.** { *; }

-toKeep public class android.support.v7.widget.** { *; }
-toKeep public class android.support.v7.internal.widget.** { *; }

-keepnames class !android.support.v7.internal.view.menu.**, ** { *; }

-toKeep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}
-keepattributes *Annotation*

-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}

-toKeep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keepattributes innerClasses
-keepattributes EnclosingMethod

-toKeep class butterknife.** { *; }
-toKeep class **$$ViewInjector { *; }

-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}
-keepclasseswithmembers class * {
    @retrofit.http.* <methods>;
}
-keepattributes Signature
-toKeep class com.google.gson.** { *;}

-dontwarn sun.misc.**
-toKeep public class com.google.android.gms.* { public *; }
-dontwarn com.google.android.gms.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}





