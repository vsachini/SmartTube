package com.liskovsoft.smartyoutubetv2.tv.ui.common;

import android.graphics.Bitmap;

/**
 * Cheap blur for small bitmaps (ambient backdrop). Three box blur passes approximate a gaussian.
 * Works on every API level, unlike RenderEffect (API 31+).
 */
public class AmbientBlur {
    private static final int PASSES = 3;

    public static Bitmap blur(Bitmap source, int radius) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = new int[width * height];
        int[] buffer = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < PASSES; i++) {
            boxBlur(pixels, buffer, width, height, radius, true);
            boxBlur(buffer, pixels, width, height, radius, false);
        }

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    /**
     * Running-sum box blur along rows (horizontal) or columns, clamping at the edges.
     */
    private static void boxBlur(int[] in, int[] out, int width, int height, int radius, boolean horizontal) {
        int lines = horizontal ? height : width;
        int length = horizontal ? width : height;
        int window = radius * 2 + 1;

        for (int line = 0; line < lines; line++) {
            int r = 0, g = 0, b = 0;

            for (int k = -radius; k <= radius; k++) {
                int c = in[index(line, clamp(k, length), width, horizontal)];
                r += (c >> 16) & 0xFF;
                g += (c >> 8) & 0xFF;
                b += c & 0xFF;
            }

            for (int pos = 0; pos < length; pos++) {
                out[index(line, pos, width, horizontal)] = 0xFF000000 | ((r / window) << 16) | ((g / window) << 8) | (b / window);

                int removed = in[index(line, clamp(pos - radius, length), width, horizontal)];
                int added = in[index(line, clamp(pos + radius + 1, length), width, horizontal)];
                r += ((added >> 16) & 0xFF) - ((removed >> 16) & 0xFF);
                g += ((added >> 8) & 0xFF) - ((removed >> 8) & 0xFF);
                b += (added & 0xFF) - (removed & 0xFF);
            }
        }
    }

    private static int index(int line, int pos, int width, boolean horizontal) {
        return horizontal ? line * width + pos : pos * width + line;
    }

    private static int clamp(int pos, int length) {
        return Math.max(0, Math.min(length - 1, pos));
    }
}
