package thunder.hack.utility.render.shaders;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import thunder.hack.utility.render.compat.RenderSystem;

import java.awt.*;

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
    private Framebuffer input;
    private boolean captureValid = false;

    public static final RenderPipeline BLUR_SHADER = RenderPipeline.builder()
            .withLocation(Identifier.of("thunderhack", "pipeline/blur"))
            .withVertexShader(Identifier.of("minecraft", "core/position_only"))
            .withFragmentShader(Identifier.of("minecraft", "core/blur"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withSampler("InputSampler")
            .withUniform("ModelViewMat", UniformType.MATRIX4X4)
            .withUniform("ProjMat", UniformType.MATRIX4X4)
            .withUniform("InputResolution", UniformType.VEC2)
            .withUniform("Quality", UniformType.FLOAT)
            .withUniform("Brightness", UniformType.FLOAT)
            .withUniform("color1", UniformType.VEC4)
            .withUniform("uSize", UniformType.VEC2)
            .withUniform("uLocation", UniformType.VEC2)
            .withUniform("radius", UniformType.FLOAT)
            .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
            .build();

    public BlurProgram() {
    }

    public void setParameters(float x, float y, float width, float height, float r, Color c1, float blurStrenth, float blurOpacity) {
        if (input == null) {
            input = new SimpleFramebuffer("thunderhack_blur", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false);
        }

        float i = (float) mc.getWindow().getScaleFactor();
        radius = r * i;
        uLocationX = x * i;
        uLocationY = -y * i + mc.getWindow().getScaledHeight() * i - height * i;
        uSizeX = width * i;
        uSizeY = height * i;
        brightness = blurOpacity;
        quality = blurStrenth;
        color1 = c1;
    }

    public void use() {
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (input == null) {
            input = new SimpleFramebuffer("thunderhack_blur", framebuffer.textureWidth, framebuffer.textureHeight, false);
        }
        if (input.textureWidth != framebuffer.textureWidth || input.textureHeight != framebuffer.textureHeight) {
            input.resize(framebuffer.textureWidth, framebuffer.textureHeight);
            captureValid = false;
        }

        if (!captureValid) {
            input.drawBlit(framebuffer.getColorAttachment());
            captureValid = true;
        }

        RenderSystem.setShader(BLUR_SHADER);
        RenderSystem.setShaderTexture(0, input.getColorAttachment());
        RenderSystem.setShaderUniform("InputResolution", (float) framebuffer.textureWidth, (float) framebuffer.textureHeight);
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
