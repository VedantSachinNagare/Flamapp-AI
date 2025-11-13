#pragma once

#include <cstddef>
#include <cstdint>
#include <memory>
#include <vector>

namespace opencv {

class Processor {
public:
    Processor();
    ~Processor();

    void initialize(int width, int height);
    void processFrame(const std::vector<uint8_t>& nv21,
                      int width,
                      int height,
                      std::vector<uint8_t>& rgbaOut);
    float lastFps() const;

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

}  // namespace opencv

