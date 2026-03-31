package balbucio.browser4j.streaming;

public interface FrameEncoder {
    void encode(Frame frame);
    void close();
}
