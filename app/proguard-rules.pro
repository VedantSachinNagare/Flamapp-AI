# Keep JNI bridge classes to avoid stripping native methods
-keep class com.example.flappai.nativebridge.** { *; }

# Keep GLSurfaceView renderer utilities
-keep class com.example.flappai.ui.** { *; }

