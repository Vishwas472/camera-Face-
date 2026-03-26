# Add project specific ProGuard rules here.

# Keep MLKit Face Detection classes
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep CameraView classes
-keep class com.otaliastudios.cameraview.** { *; }

# Keep our app classes
-keep class com.ibrahimcanerdogan.facedetectionapp.** { *; }
