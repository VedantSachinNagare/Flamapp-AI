"use strict";
const SAMPLE_FRAME_BASE64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAQAAAAAYLlVAAAAVklEQVR4Ae3SQQ0AIAwAsXjc/7WBFFWci04MxJ6/rzkAIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIj4BiQAB2snccAAAAAElFTkSuQmCC";
const state = {
    fps: 12.4,
    width: 1280,
    height: 720
};
function updateViewer(meta) {
    const metadataEl = document.getElementById("metadata");
    const imageEl = document.getElementById("processed-image");
    if (!metadataEl || !imageEl) {
        throw new Error("Viewer DOM is not ready.");
    }
    metadataEl.textContent = `FPS: ${meta.fps.toFixed(1)} | Resolution: ${meta.width} × ${meta.height}`;
    imageEl.src = SAMPLE_FRAME_BASE64;
    imageEl.title = "Static sample processed frame rendered by OpenCV Canny edge detection.";
}
function simulateFps(meta) {
    let tick = 0;
    return window.setInterval(() => {
        tick = (tick + 1) % 5;
        const dynamicFps = meta.fps + Math.sin(tick) * 2;
        const metadataEl = document.getElementById("metadata");
        if (metadataEl) {
            metadataEl.textContent = `FPS: ${dynamicFps.toFixed(1)} | Resolution: ${meta.width} × ${meta.height}`;
        }
    }, 1000);
}
document.addEventListener("DOMContentLoaded", () => {
    updateViewer(state);
    simulateFps(state);
});
