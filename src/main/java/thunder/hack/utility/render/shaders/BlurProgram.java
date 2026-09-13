package thunder.hack.utility.render.shaders;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import thunder.hack.utility.render.compat.RenderSystem;

import java.awt.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import static thunder.hack.features.modules.Module.mc;

public class BlurProgram {
    private float uSizeX;
    private float uSizeY;
    private float uLocationX;
    private float uLocationY;
    private float radius;
    private float brightness;
    private float quality;
    private Color color1 = Color.WHITE;
    private RenderTarget input;
    private boolean captureValid = false;

    public static final RenderPipeline BLUR_SHADER = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("thunderhack", "pipeline/blur"))
            .withVertexShader(Identifier.fromNamespaceAndPath("minecraft", "core/position_only"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("minecraft", "core/blur"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withSampler("InputSampler")
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("ThunderHackCustom", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
            .build();

    public BlurProgram() {
    }

    public void setParameters(float x, float y, float width, float height, float r, Color c1, float blurStrenth, float blurOpacity) {
        if (input == null) {
            input = new TextureTarget("thunderhack_blur", mc.getWindow().getWidth(), mc.getWindow().getHeight(), false);
        }

        float i = (float) mc.getWindow().getGuiScale();
        radius = r * i;
        uLocationX = x * i;
        uLocationY = -y * i + mc.getWindow().getGuiScaledHeight() * i - height * i;
        uSizeX = width * i;
        uSizeY = height * i;
        brightness = blurOpacity;
        quality = blurStrenth;
        color1 = c1;
    }

    public void use() {
        RenderTarget framebuffer = Minecraft.getInstance().getMainRenderTarget();
        if (input == null) {
            input = new TextureTarget("thunderhack_blur", framebuffer.width, framebuffer.height, false);
        }
        if (input.width != framebuffer.width || input.height != framebuffer.height) {
            input.resize(framebuffer.width, framebuffer.height);
            captureValid = false;
        }

        if (!captureValid) {
            input.blitAndBlendToTexture(framebuffer.getColorTextureView());
            captureValid = true;
        }

        RenderSystem.setShader(BLUR_SHADER);
        RenderSystem.setShaderTexture(0, input.getColorTextureView());
        RenderSystem.setShaderUniform("InputResolution", (float) framebuffer.width, (float) framebuffer.height);
        RenderSystem.setShaderUniform("Quality", quality);
        RenderSystem.setShaderUniform("Brightness", brightness);
        RenderSystem.setShaderUniform("color1",
                color1.getRed() / 255f,
                color1.getGreen() / 255f,
                color1.getBlue() / 255f,
                color1.getAlpha() / 255f);
        RenderSystem.setShaderUniform("uSize", uSizeX, uSizeY);
        RenderSystem.setShaderUniform("uLocation", uLocationX, uLocationY);
        RenderSystem.setShaderUniform("radius", radius);
    }

    public void invalidateCapture() {
        captureValid = false;
    }
}
