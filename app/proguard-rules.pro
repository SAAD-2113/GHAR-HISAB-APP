# GharHisab ProGuard Rules
# Add project specific ProGuard rules here.

# Keep DataStore Preferences
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep Compose
-dontwarn androidx.compose.**
