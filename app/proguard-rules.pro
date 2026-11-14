# Google Play Release ProGuard / R8 Keep Rules for BlockMine

# Preserve WebView JavascriptInterface bridge methods
-keepattributes JavascriptInterface
-keepclassmembers class com.example.ui.BlockMineWebBridge {
    @android.webkit.JavascriptInterface <methods>;
    public *;
}

# Preserve Firestore @IgnoreExtraProperties data models
-keepclassmembers class com.example.data.** {
    *;
}
-keepattributes Signature,InnerClasses,EnclosingMethod

# Preserve line numbers for Google Play Console crash symbolication
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
