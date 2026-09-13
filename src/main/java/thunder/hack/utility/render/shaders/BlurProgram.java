package thunder.hack.utility.render.shaders;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import thunder.hack.utility.render.ShaderProgramKeys;
import thunder.hack.utility.render.compat.RenderSystem;

import java.awt.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
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
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(ShaderProgramKeys.customUniformLayout())
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("InputSampler").build())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.POSITION)
            .withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.QUADS)
            .build();

    public BlurProgram() {
    }

    public void setParameters(float x, float y, float width, float height, float r, Color c1, float blurStrenth, float blurOpacity) {
        if (input == null) {
            input = new TextureTarget("thunderhack_blur", mc.getWindow().getWidth(), mc.getWindow().getHeight(), false, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM);
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
        RenderTarget framebuffer = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if (input == null) {
            input = new TextureTarget("thunderhack_blur", framebuffer.width, framebuffer.height, false, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM);
        }
        if (input.width != framebuffer.width || input.height != framebuffer.height) {
            input.resize(framebuffer.width, framebuffer.height);
            captureValid = false;
        }

        if (!captureValid) {
            // The capture is replayed together with the draw so that it reads the frame the blur
            // belongs to instead of whatever the main render target happened to hold at record time.
            RenderSystem.scheduleCapture(() -> input.blitAndBlendToTexture(
                    framebuffer.getColorTextureView(), framebuffer.getDepthTextureView()));
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
