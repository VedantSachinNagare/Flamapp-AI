# FlappAI Assignment Skeleton

Minimal multi-platform project showcasing Android camera streaming, native OpenCV processing, OpenGL ES rendering, and a TypeScript web viewer.

---

## Features Implemented
- **Android Camera Preview** – `TextureView` + Camera2 pipeline streaming NV21 frames.
- **JNI Bridge** – Kotlin `NativeBridge` marshals frames to C++ for processing.
- **OpenCV Processing (C++)** – Grayscale + Canny edge detection with FPS reporting.
- **OpenGL ES 2.0 Renderer** – `GLSurfaceView` texture renderer module (shared `gl` library).
- **Web Viewer (TypeScript)** – Static processed frame preview, FPS/resolution overlay, DOM updates via TypeScript build.

---

## Project Structure

```
app/        # Android application (Kotlin + Camera + JNI bindings)
gl/         # Android library module housing OpenGL ES renderer + shaders
jni/        # Native C++ sources & CMake with OpenCV integration
web/        # TypeScript web viewer (tsconfig + static assets)
README.md   # This document
settings.gradle / build.gradle / gradle.properties
```

---

## Architecture Overview

```mermaid
flowchart LR
    Camera(Camera2 TextureView) -->|NV21 frame| Kotlin[JNI Bridge (Kotlin)]
    Kotlin -->|ByteArray| Cpp[OpenCV Processor (C++)]
    Cpp -->|RGBA buffer| Renderer[OpenGL ES Renderer]
    Renderer -->|Texture| Screen[GLSurfaceView Overlay]
```

**Frame Flow:**  
`Camera (NV21) → Kotlin (CameraController) → JNI → C++ (OpenCV grayscale + Canny) → RGBA buffer → OpenGL texture → GLSurfaceView`

---

## Android Setup

### Prerequisites
- Android Studio Giraffe (AGP 8.2+) with SDK 34.
- **NDK r26** (configurable via Android Studio > SDK Manager > SDK Tools).
- CMake 3.22+ (bundled with Android Studio or standalone).
- OpenCV Android SDK extracted locally.

### OpenCV Integration
1. Download OpenCV for Android from [opencv.org](https://opencv.org/releases/).
2. Extract and note the absolute path to `OpenCV-android-sdk/sdk/native`.
3. Set `OpenCV_DIR` environment variable before syncing/building:
   - macOS/Linux:
     ```bash
     export OpenCV_DIR=/path/to/OpenCV-android-sdk/sdk/native/jni
     ```
   - Windows PowerShell:
     ```powershell
     $env:OpenCV_DIR="C:\path\to\OpenCV-android-sdk\sdk\native\jni"
     ```
4. Sync Gradle. CMake `find_package(OpenCV REQUIRED ...)` will resolve includes/libs.

### Build & Run
1. Open Android Studio and import the project root (`flappai/`).
2. Allow Gradle sync; ensure `:gl` module is included.
3. Connect an ARM64 device (min SDK 24) and run the default `app` configuration.
4. Grant the camera permission when prompted.

---

## Native Processing Notes
- `jni/CMakeLists.txt` builds `flappnative` shared library targeting C++17.
- `opencv_processor.cpp` converts NV21 → BGR → grayscale → Canny edges → RGBA output.
- FPS is calculated per processed frame and exposed through `NativeBridge.getLastProcessingFps()`.
- Logging uses Android `__android_log_print` for quick debugging.

---

## OpenGL ES Renderer
- Located under `gl/src/main/java/com/example/flappai/gl`.
- `ProcessedFrameRenderer` uploads RGBA frames into a GL texture each draw pass.
- Vertex/fragment shaders are stored in `gl/src/main/res/raw`.
- Renderer publishes instantaneous FPS back to `ProcessedFrameSurfaceView` via callback.

---

## Web Viewer (`/web`)

### Install & Build
```bash
cd web
npm install
npm run build
```

`tsc` outputs to `web/dist/main.js`. Open `web/index.html` in a browser (or run `npm run start` to serve locally) to see the sample processed frame and dynamic FPS ticker.

### Features
- Loads a bundled base64 PNG representing a processed Canny frame.
- Updates FPS text every second to mimic live stats.
- Responsive layout with simple styling in `web/styles.css`.

---

## Screenshots & Media
- `docs/images/android-preview.png` *(placeholder – add real screenshot)*
- `docs/images/web-viewer.png` *(placeholder – add real screenshot)*
- Animated GIF recommended for OpenGL overlay demonstration.

---

## Testing Checklist
- [ ] Android build succeeds (Debug + Release).
- [ ] C++ shared library loads successfully (verify logcat `Processor initialized`).
- [ ] Processed frame visible with edges overlaid; FPS text updates.
- [ ] Web `npm run build` compiles TypeScript without errors.
- [ ] Web viewer displays static frame + dynamic FPS ticker.

---

## Suggested Commit Log
- `init: base project structure`
- `feat(android): add camera preview via TextureView`
- `feat(jni): add native bridge and grayscale filter`
- `feat(opencv): implement canny edge detection`
- `feat(opengl): add texture shader renderer`
- `feat(web): add TypeScript viewer`
- `docs: add README with setup instructions`

---

## Next Steps
- Replace placeholder screenshot paths with real captures/GIFs.
- Add unit/instrumented tests for camera permission handling.
- Integrate a streaming transport to push frames to the web viewer if desired.

