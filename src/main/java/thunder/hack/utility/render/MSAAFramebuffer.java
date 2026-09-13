package thunder.hack.utility.render;

import thunder.hack.utility.render.compat.RenderSystem;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

public class MSAAFramebuffer extends RenderTarget {
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
        use(Math.min(fancy ? 16 : 4, MAX_SAMPLES), Minecraft.getInstance().getMainRenderTarget(), drawAction);
    }

    public static void use(int samples, @NotNull RenderTarget mainBuffer, @NotNull Runnable drawAction) {
        drawAction.run();
    }

    @Override
    public void resize(int width, int height) {
        if (width != width || height != height) {
            super.resize(width, height);
        }
    }

    @Override
    public void createBuffers(int width, int height) {
        RenderSystem.assertOnRenderThreadOrInit();
        viewWidth = width;
        viewHeight = height;
        width = width;
        height = height;

        colorTexture = Minecraft.getInstance().getMainRenderTarget().getColorTexture();
        depthTexture = Minecraft.getInstance().getMainRenderTarget().getDepthTexture();
    }

    @Override
    public void destroyBuffers() {
        RenderSystem.assertOnRenderThreadOrInit();
        colorTexture = null;
        depthTexture = null;
        width = -1;
        height = -1;
    }
}
