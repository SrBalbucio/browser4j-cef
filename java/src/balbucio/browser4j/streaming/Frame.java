package balbucio.browser4j.streaming;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Frame {
    private final ByteBuffer buffer;
    private final int width;
    private final int height;
    private final long captureTimestampMs;

    public Frame(ByteBuffer buffer, int width, int height, long captureTimestampMs) {
        this.buffer = buffer;
        this.width = width;
        this.height = height;
        this.captureTimestampMs = captureTimestampMs;
    }

    public ByteBuffer getBuffer() { return buffer; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getCaptureTimestampMs() { return captureTimestampMs; }

    public BufferedImage toBufferedImage() {
        int bytesPerPixel = 4; // assumir BGRA (padrão JCEF) ou ARGB
        int expected = width * height * bytesPerPixel;
        if (buffer.remaining() < expected) {
            throw new IllegalStateException("Frame buffer length " + buffer.remaining() + " menor que esperado " + expected);
        }

        ByteBuffer copy = buffer.duplicate();
        copy.order(java.nio.ByteOrder.LITTLE_ENDIAN);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width * height];

        for (int i = 0; i < width * height; i++) {
            int b = copy.get() & 0xFF;
            int g = copy.get() & 0xFF;
            int r = copy.get() & 0xFF;
            int a = copy.get() & 0xFF;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    public void saveAsPng(File file) throws IOException {
        BufferedImage image = toBufferedImage();
        javax.imageio.ImageIO.write(image, "PNG", file);
    }
}
