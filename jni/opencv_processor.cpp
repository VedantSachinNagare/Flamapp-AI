#include "opencv_processor.hpp"

#include <opencv2/imgproc.hpp>
#include <opencv2/core.hpp>
#include <chrono>
#include <cstring>

namespace opencv {

struct Processor::Impl {
    int width = 0;
    int height = 0;
    float fps = 0.f;

    std::chrono::steady_clock::time_point lastTick =
            std::chrono::steady_clock::now();
};

Processor::Processor() : impl_(std::make_unique<Impl>()) {}

Processor::~Processor() = default;

void Processor::initialize(int width, int height) {
    impl_->width = width;
    impl_->height = height;
}

void Processor::processFrame(const std::vector<uint8_t>& nv21,
                             int width,
                             int height,
                             std::vector<uint8_t>& rgbaOut) {
    if (nv21.empty() || width <= 0 || height <= 0) {
        return;
    }

    const cv::Mat yuv(height + height / 2, width, CV_8UC1,
                      const_cast<uint8_t*>(nv21.data()));

    cv::Mat bgr;
    cv::cvtColor(yuv, bgr, cv::COLOR_YUV2BGR_NV21);

    cv::Mat gray;
    cv::cvtColor(bgr, gray, cv::COLOR_BGR2GRAY);

    cv::Mat edges;
    cv::Canny(gray, edges, 80.0, 160.0);

    cv::Mat edgesColor;
    cv::cvtColor(edges, edgesColor, cv::COLOR_GRAY2RGBA);

    rgbaOut.resize(static_cast<size_t>(width * height * 4));
    std::memcpy(rgbaOut.data(), edgesColor.data, rgbaOut.size());

    const auto now = std::chrono::steady_clock::now();
    const auto delta =
            std::chrono::duration_cast<std::chrono::milliseconds>(now - impl_->lastTick)
                    .count();
    if (delta > 0) {
        impl_->fps = 1000.f / static_cast<float>(delta);
    }
    impl_->lastTick = now;
}

float Processor::lastFps() const {
    return impl_->fps;
}

}  // namespace opencv

