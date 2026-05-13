package thunder.hack.utility.render;

import thunder.hack.utility.render.compat.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;

import java.util.HashMap;
import java.util.Map;

public class MSAAFramebuffer extends Framebuffer {
    public static final int MAX_SAMPLES = GL30.glGetInteger(GL30C.GL_MAX_SAMPLES);
    private static final Map<Integer, MSAAFramebuffer> INSTANCES = new HashMap<>();

    private final int samples;
    private int rboColor;
    private int rboDepth;

    public MSAAFramebuffer(int samples) {
        super("melahack_msaa", true);
        this.samples = samples;
    }

    public static MSAAFramebuffer getInstance(int samples) {
        return INSTANCES.computeIfAbsent(samples, x -> new MSAAFramebuffer(samples));
    }

    public static void use(boolean fancy, Runnable drawAction) {
        use(Math.min(fancy ? 16 : 4, MAX_SAMPLES), MinecraftClient.getInstance().getFramebuffer(), drawAction);
    }

    public static void use(int samples, @NotNull Framebuffer mainBuffer, @NotNull Runnable drawAction) {
        drawAction.run();
    }

    @Override
    public void resize(int width, int height) {
        if (textureWidth != width || textureHeight != height) {
            super.resize(width, height);
        }
    }

    @Override
    public void initFbo(int width, int height) {
        RenderSystem.assertOnRenderThreadOrInit();
        viewportWidth = width;
        viewportHeight = height;
        textureWidth = width;
        textureHeight = height;

        colorAttachment = MinecraftClient.getInstance().getFramebuffer().getColorAttachment();
        depthAttachment = MinecraftClient.getInstance().getFramebuffer().getDepthAttachment();
    }

    @Override
    public void delete() {
        RenderSystem.assertOnRenderThreadOrInit();
        colorAttachment = null;
        depthAttachment = null;
        textureWidth = -1;
        textureHeight = -1;
    }
}
