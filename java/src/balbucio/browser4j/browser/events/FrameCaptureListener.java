package balbucio.browser4j.browser.events;

import balbucio.browser4j.streaming.Frame;

public interface FrameCaptureListener {
    void onFrame(Frame frame);
}
