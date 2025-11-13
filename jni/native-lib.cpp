#include <jni.h>
#include <android/log.h>
#include <mutex>
#include <vector>
#include "opencv_processor.hpp"

namespace {
constexpr const char* TAG = "FlappNative";

std::mutex gMutex;
std::unique_ptr<opencv::Processor> gProcessor;
}  // namespace

extern "C"
JNIEXPORT void JNICALL
Java_com_example_flappai_nativebridge_NativeBridge_nativeInit(
        JNIEnv* env,
        jobject /* this */,
        jint width,
        jint height) {
    std::lock_guard<std::mutex> lock(gMutex);
    if (!gProcessor) {
        gProcessor = std::make_unique<opencv::Processor>();
    }
    gProcessor->initialize(width, height);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Processor initialized %dx%d", width, height);
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_flappai_nativebridge_NativeBridge_nativeProcessFrame(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray nv21Buffer,
        jint width,
        jint height) {
    jsize length = env->GetArrayLength(nv21Buffer);
    std::vector<uint8_t> input(static_cast<size_t>(length));
    env->GetByteArrayRegion(nv21Buffer, 0, length,
                            reinterpret_cast<jbyte*>(input.data()));

    std::vector<uint8_t> output;

    {
        std::lock_guard<std::mutex> lock(gMutex);
        if (!gProcessor) {
            gProcessor = std::make_unique<opencv::Processor>();
            gProcessor->initialize(width, height);
        }
        gProcessor->processFrame(input, width, height, output);
    }

    jbyteArray result = env->NewByteArray(static_cast<jsize>(output.size()));
    env->SetByteArrayRegion(
            result,
            0,
            static_cast<jsize>(output.size()),
            reinterpret_cast<const jbyte*>(output.data()));
    return result;
}

extern "C"
JNIEXPORT jfloat JNICALL
Java_com_example_flappai_nativebridge_NativeBridge_nativeGetLastProcessingFps(
        JNIEnv* /* env */,
        jobject /* this */) {
    std::lock_guard<std::mutex> lock(gMutex);
    if (!gProcessor) {
        return 0.0f;
    }
    return gProcessor->lastFps();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_flappai_nativebridge_NativeBridge_nativeRelease(
        JNIEnv* /* env */,
        jobject /* this */) {
    std::lock_guard<std::mutex> lock(gMutex);
    gProcessor.reset();
    __android_log_print(ANDROID_LOG_INFO, TAG, "Processor released");
}

