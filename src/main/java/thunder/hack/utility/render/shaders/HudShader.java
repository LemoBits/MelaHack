package thunder.hack.utility.render.shaders;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import thunder.hack.utility.render.compat.RenderSystem;

import java.awt.*;

import static thunder.hack.features.modules.Module.mc;

public class HudShader {
    private float uSizeX;
    private float uSizeY;
    private float uLocationX;
    private float uLocationY;
    private float radius;
    private float blend = 1f;
    private float alpha = 1f;
    private float outline;
    private float glow;
    private Color color1 = Color.WHITE;
    private Color color2 = Color.WHITE;
    private Color color3 = Color.WHITE;
    private Color color4 = Color.WHITE;

    public static final RenderPipeline HUD_SHADER = RenderPipeline.builder()
            .withLocation(Identifier.of("thunderhack", "pipeline/hudshader"))
            .withVertexShader(Identifier.of("minecraft", "core/position_only"))
            .withFragmentShader(Identifier.of("minecraft", "core/hudshader"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withUniform("ModelViewMat", UniformType.MATRIX4X4)
            .withUniform("ProjMat", UniformType.MATRIX4X4)
            .withUniform("color1", UniformType.VEC4)
            .withUniform("color2", UniformType.VEC4)
            .withUniform("color3", UniformType.VEC4)
            .withUniform("color4", UniformType.VEC4)
            .withUniform("uSize", UniformType.VEC2)
            .withUniform("uLocation", UniformType.VEC2)
            .withUniform("radius", UniformType.FLOAT)
            .withUniform("blend", UniformType.FLOAT)
            .withUniform("alpha", UniformType.FLOAT)
            .withUniform("glow", UniformType.FLOAT)
            .withUniform("outline", UniformType.FLOAT)
            .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
            .build();

    public HudShader() {
    }

    public void setParameters(float x, float y, float width, float height, float r, float externalAlpha, float internalAlpha) {
        float i = (float) mc.getWindow().getScaleFactor();
        radius = r * i;
        uLocationX = x * i;
        uLocationY = -y * i + mc.getWindow().getScaledHeight() * i - height * i;
        uSizeX = width * i;
        uSizeY = height * i;

        Color c1 = thunder.hack.features.modules.client.HudEditor.getColor(270);
        Color c2 = thunder.hack.features.modules.client.HudEditor.getColor(0);
        Color c3 = thunder.hack.features.modules.client.HudEditor.getColor(180);
        Color c4 = thunder.hack.features.modules.client.HudEditor.getColor(90);
        int alphaByte = MathHelper.clamp(Math.round(externalAlpha * 255f), 0, 255);

        color1 = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), alphaByte);
        color2 = new Color(c2.getRed(), c2.getGreen(), c2.getBlue(), alphaByte);
        color3 = new Color(c3.getRed(), c3.getGreen(), c3.getBlue(), alphaByte);
        color4 = new Color(c4.getRed(), c4.getGreen(), c4.getBlue(), alphaByte);
        blend = thunder.hack.features.modules.client.HudEditor.blend.getValue();
        outline = thunder.hack.features.modules.client.HudEditor.outline.getValue();
        glow = thunder.hack.features.modules.client.HudEditor.glow1.getValue();
        alpha = MathHelper.clamp(internalAlpha, 0f, 1f);
    }

    public void use() {
        RenderSystem.setShader(HUD_SHADER);
        RenderSystem.setShaderUniform("uSize", uSizeX, uSizeY);
        RenderSystem.setShaderUniform("uLocation", uLocationX, uLocationY);
        RenderSystem.setShaderUniform("radius", radius);
        RenderSystem.setShaderUniform("blend", blend);
        RenderSystem.setShaderUniform("alpha", alpha);
        RenderSystem.setShaderUniform("outline", outline);
        RenderSystem.setShaderUniform("glow", glow);
        RenderSystem.setShaderUniform("color1", color1.getRed() / 255f, color1.getGreen() / 255f, color1.getBlue() / 255f, color1.getAlpha() / 255f);
        RenderSystem.setShaderUniform("color2", color2.getRed() / 255f, color2.getGreen() / 255f, color2.getBlue() / 255f, color2.getAlpha() / 255f);
        RenderSystem.setShaderUniform("color3", color3.getRed() / 255f, color3.getGreen() / 255f, color3.getBlue() / 255f, color3.getAlpha() / 255f);
        RenderSystem.setShaderUniform("color4", color4.getRed() / 255f, color4.getGreen() / 255f, color4.getBlue() / 255f, color4.getAlpha() / 255f);
    }
}
