package thunder.hack.utility.render.compat;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Fog;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RenderSystem {
    public enum BlendMode {
        DEFAULT,
        ADDITIVE,
        DST_ALPHA,
        CUSTOM
    }

    private static RenderPipeline currentPipeline;
    private static final Map<String, Object> currentUniforms = new LinkedHashMap<>();
    private static BlendMode blendMode = BlendMode.DEFAULT;
    private static boolean scissorEnabled;
    private static int scissorX;
    private static int scissorY;
    private static int scissorWidth;
    private static int scissorHeight;

    private RenderSystem() {
    }

    public static RenderPipeline getCurrentPipeline() {
        return currentPipeline;
    }

    public static Map<String, Object> getCurrentUniforms() {
        return currentUniforms;
    }

    public static BlendMode getBlendMode() {
        return blendMode;
    }

    public static void setShader(RenderPipeline pipeline) {
        if (currentPipeline != pipeline) {
            currentUniforms.clear();
        }
        currentPipeline = pipeline;
    }

    public static void setShader(Object ignored) {
        if (ignored instanceof RenderPipeline pipeline) {
            setShader(pipeline);
        } else {
            currentPipeline = null;
            currentUniforms.clear();
        }
    }

    public static void setShaderUniform(String name, float... values) {
        currentUniforms.put(name, values);
    }

    public static void setShaderUniform(String name, int... values) {
        currentUniforms.put(name, values);
    }

    public static void setShaderUniform(String name, Matrix4f value) {
        currentUniforms.put(name, value);
    }

    public static void enableBlend() {
        com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
    }

    public static void disableBlend() {
        com.mojang.blaze3d.opengl.GlStateManager._disableBlend();
    }

    public static void defaultBlendFunc() {
        com.mojang.blaze3d.opengl.GlStateManager._blendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        blendMode = BlendMode.DEFAULT;
    }

    public static void blendFunc(GlStateManager.SrcFactor src, GlStateManager.DstFactor dst) {
        blendFunc(src.value, dst.value);
    }

    public static void blendFunc(int src, int dst) {
        com.mojang.blaze3d.opengl.GlStateManager._blendFuncSeparate(src, dst, GL11.GL_ONE, GL11.GL_ZERO);
        blendMode = classifyBlend(src, dst, GL11.GL_ONE, GL11.GL_ZERO);
    }

    public static void blendFuncSeparate(
            GlStateManager.SrcFactor srcRgb,
            GlStateManager.DstFactor dstRgb,
            GlStateManager.SrcFactor srcAlpha,
            GlStateManager.DstFactor dstAlpha
    ) {
        com.mojang.blaze3d.opengl.GlStateManager._blendFuncSeparate(
                srcRgb.value, dstRgb.value, srcAlpha.value, dstAlpha.value);
        blendMode = classifyBlend(srcRgb.value, dstRgb.value, srcAlpha.value, dstAlpha.value);
    }

    private static BlendMode classifyBlend(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        if (srcRgb == GL11.GL_SRC_ALPHA
                && dstRgb == GL11.GL_ONE
                && srcAlpha == GL11.GL_ONE
                && dstAlpha == GL11.GL_ZERO) {
            return BlendMode.ADDITIVE;
        }

        if (srcRgb == GL11.GL_SRC_ALPHA
                && dstRgb == GL11.GL_ONE_MINUS_SRC_ALPHA
                && srcAlpha == GL11.GL_ONE
                && dstAlpha == GL11.GL_ZERO) {
            return BlendMode.DEFAULT;
        }

        if (srcRgb == GL11.GL_DST_ALPHA
                && dstRgb == GL11.GL_ONE_MINUS_DST_ALPHA
                && srcAlpha == GL11.GL_ONE
                && dstAlpha == GL11.GL_ZERO) {
            return BlendMode.DST_ALPHA;
        }

        return BlendMode.CUSTOM;
    }

    public static void enableDepthTest() {
        com.mojang.blaze3d.opengl.GlStateManager._enableDepthTest();
    }

    public static void disableDepthTest() {
        com.mojang.blaze3d.opengl.GlStateManager._disableDepthTest();
    }

    public static void depthMask(boolean mask) {
        com.mojang.blaze3d.opengl.GlStateManager._depthMask(mask);
    }

    public static void depthFunc(int func) {
        com.mojang.blaze3d.opengl.GlStateManager._depthFunc(func);
    }

    public static void enableCull() {
        com.mojang.blaze3d.opengl.GlStateManager._enableCull();
    }

    public static void disableCull() {
        com.mojang.blaze3d.opengl.GlStateManager._disableCull();
    }

    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        com.mojang.blaze3d.opengl.GlStateManager._colorMask(red, green, blue, alpha);
    }

    public static void lineWidth(float width) {
        com.mojang.blaze3d.systems.RenderSystem.lineWidth(width);
    }

    public static void setShaderColor(float red, float green, float blue, float alpha) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(red, green, blue, alpha);
    }

    public static void setShaderTexture(int slot, Identifier id) {
        GpuTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(id).getGlTexture();
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(slot, texture);
    }

    public static void setShaderTexture(int slot, GpuTexture texture) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(slot, texture);
    }

    public static void clearColor(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
    }

    public static void clear(int mask) {
        com.mojang.blaze3d.opengl.GlStateManager._clear(mask);
    }

    public static void texParameter(int target, int pname, int param) {
        com.mojang.blaze3d.opengl.GlStateManager._texParameter(target, pname, param);
    }

    public static void enableScissor(int x, int y, int width, int height) {
        scissorEnabled = true;
        scissorX = x;
        scissorY = y;
        scissorWidth = width;
        scissorHeight = height;
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(x, y, width, height);
    }

    public static void disableScissor() {
        scissorEnabled = false;
        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }

    public static void applyScissor(com.mojang.blaze3d.systems.RenderPass pass) {
        if (scissorEnabled) {
            pass.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);
        } else {
            pass.disableScissor();
        }
    }

    public static void backupProjectionMatrix() {
        com.mojang.blaze3d.systems.RenderSystem.backupProjectionMatrix();
    }

    public static void restoreProjectionMatrix() {
        com.mojang.blaze3d.systems.RenderSystem.restoreProjectionMatrix();
    }

    public static Matrix4f getProjectionMatrix() {
        return com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix();
    }

    public static Matrix4f getModelViewMatrix() {
        return com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix();
    }

    public static Matrix4fStack getModelViewStack() {
        return com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
    }

    public static void resetTextureMatrix() {
        com.mojang.blaze3d.systems.RenderSystem.resetTextureMatrix();
    }

    public static Fog getShaderFog() {
        return com.mojang.blaze3d.systems.RenderSystem.getShaderFog();
    }

    public static void setShaderFog(Fog fog) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderFog(fog);
    }

    public static boolean isOnRenderThread() {
        return com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread();
    }

    public static void assertOnRenderThreadOrInit() {
        if (!isOnRenderThread()) {
            com.mojang.blaze3d.systems.RenderSystem.assertOnRenderThread();
        }
    }

    public static void assertInInitPhase() {
    }

    public static void recordRenderCall(Runnable runnable) {
        runnable.run();
    }
}
