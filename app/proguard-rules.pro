# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\tools\adt-bundle-windows-x86_64-20131030\sdk/tools/proguard/proguard-android.txt
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


-optimizationpasses 5
-allowaccessmodification

-repackageclasses

-ignorewarnings
-dontwarn
-dontnote


#-keepattributes SourceFile,LineNumberTable
-keep class net.openvpn.openvpn.ClientAPI** { *; }
-keep class net.openvpn.openvpn.SWIG** { *; }
-keep class net.openvpn.openvpn.ovpn** { *; }
-keep class org.** { *; }
-keep class app.ovpnudp.developer.service.vpn.** { *; }