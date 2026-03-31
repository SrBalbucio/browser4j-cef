package balbucio.browser4j.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SoftwareFrameEncoder implements FrameEncoder {
    private static final Logger logger = LoggerFactory.getLogger(SoftwareFrameEncoder.class);
    private final ConcurrentLinkedQueue<byte[]> frameBacklog = new ConcurrentLinkedQueue<>();
    private final int maxBacklogSize;

    public SoftwareFrameEncoder(int maxBacklogSize) {
        this.maxBacklogSize = maxBacklogSize;
    }

    @Override
    public void encode(Frame frame) {
        if (frameBacklog.size() >= maxBacklogSize) {
            frameBacklog.poll(); // Omit the oldest to avoid memory leak if unconsumed
        }

        ByteBuffer buffer = frame.getBuffer();
        if (buffer != null) {
            buffer.rewind();
            byte[] rawBytes = new byte[buffer.remaining()];
            buffer.get(rawBytes);
            frameBacklog.add(rawBytes);
        }
    }

    public byte[] pollNextFrameBytes() {
        return frameBacklog.poll();
    }

    @Override
    public void close() {
        frameBacklog.clear();
        logger.info("SoftwareFrameEncoder was closed and cleared.");
    }
}
