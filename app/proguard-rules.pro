# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/hefuyi/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# mvp models (incl. ArtistArt parsed by Gson, MusicPlaybackTrack parcelable)
-keep class io.hefuyi.listener.mvp.model.** { *; }

-keep class androidx.fragment.app.** { *; }
-keep interface androidx.fragment.app.** { *; }
-keep class androidx.appcompat.app.** { *; }
-keep interface androidx.appcompat.app.** { *; }

-dontwarn io.reactivex.**

-keepattributes Exceptions
-keepattributes Signature
-keepattributes *Annotation*

#searchview
-keep class androidx.appcompat.widget.SearchView { *; }

# Dagger2 — generated factories must survive R8 full mode
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class io.hefuyi.listener.injector.** { *; }
-keep class **_MembersInjector { *; }
-keep class **_Factory { *; }
-keep class **_Provide*Factory { *; }
-keepattributes *Annotation*, Signature

# Glide 5.x — reflection-based API (ships its own consumer rules;
# explicit keeps below survive R8 full mode)
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**

# AIDL service interface — called across processes via reflection/binder
-keep class io.hefuyi.listener.IListenerService { *; }
-keep class io.hefuyi.listener.IListenerService$Stub { *; }

# Parcelable model used in queue/history
-keep class io.hefuyi.listener.mvp.model.MusicPlaybackTrack { *; }

# App widgets inflated from layout XML (TimelyView, FastScrollRecyclerView,
# SquareImageView, ForegroundImageView, LyricView, PlayPauseView) and the
# CoordinatorLayout behavior referenced by string (RotationFabBehavior).
# AGP auto-keeps most of these; explicit keep survives R8 full-mode upgrades.
-keep class io.hefuyi.listener.widget.** { *; }

# SlidingUpPanel / ATE / MaterialDialogs — reflection and custom views
-keep class com.sothree.slidinguppanel.** { *; }
-keep class com.afollestad.appthemeengine.** { *; }
-keep class com.afollestad.materialdialogs.** { *; }
-keep class androidx.media.** { *; }
-keep class android.support.v4.media.** { *; }
-keep class androidx.lifecycle.** { *; }

# RxJava3 / RxAndroid3 (ships its own consumer rules; only silence Unsafe warnings)
-dontwarn sun.misc.**
