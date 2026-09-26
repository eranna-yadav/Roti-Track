# kotlinx.serialization keeps generated serializers via its own consumer rules.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.rotitrack.app.**$$serializer { *; }
-keepclassmembers class com.rotitrack.app.** {
    *** Companion;
}
