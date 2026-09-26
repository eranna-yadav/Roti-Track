# kotlinx.serialization keeps generated serializers via its own consumer rules.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.rotitrack.app.**$$serializer { *; }
-keepclassmembers class com.rotitrack.app.** {
    *** Companion;
}

# Razorpay Checkout (from Razorpay's integration guide).
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-dontwarn com.razorpay.**
-keep class com.razorpay.** { *; }
-optimizations !method/inlining/*
-keepclasseswithmembers class * {
    public void onPayment*(...);
}
