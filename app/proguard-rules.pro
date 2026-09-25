# kotlinx.serialization keeps generated serializers via its own consumer rules.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.sipwell.app.**$$serializer { *; }
-keepclassmembers class com.sipwell.app.** {
    *** Companion;
}
