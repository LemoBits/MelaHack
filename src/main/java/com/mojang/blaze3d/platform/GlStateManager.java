package com.mojang.blaze3d.platform;

public final class GlStateManager {
    private GlStateManager() {
    }

    public enum SrcFactor {
        ZERO(0),
        ONE(1),
        SRC_ALPHA(770);

        public final int value;

        SrcFactor(int value) {
            this.value = value;
        }
    }

    public enum DstFactor {
        ZERO(0),
        ONE(1),
        ONE_MINUS_SRC_ALPHA(771);

        public final int value;

        DstFactor(int value) {
            this.value = value;
        }
    }

    public static void _glBindFramebuffer(int target, int framebuffer) {
        com.mojang.blaze3d.opengl.GlStateManager._glBindFramebuffer(target, framebuffer);
    }

    public static void _glBlitFrameBuffer(
            int srcX0, int srcY0, int srcX1, int srcY1,
            int dstX0, int dstY0, int dstX1, int dstY1,
            int mask, int filter
    ) {
        com.mojang.blaze3d.opengl.GlStateManager._glBlitFrameBuffer(
                srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    public static void _viewport(int x, int y, int width, int height) {
        com.mojang.blaze3d.opengl.GlStateManager._viewport(x, y, width, height);
    }
}
