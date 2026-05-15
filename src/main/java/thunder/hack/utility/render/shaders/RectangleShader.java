package thunder.hack.utility.render.shaders;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.utility.render.compat.RenderSystem;

import java.awt.*;

import static thunder.hack.features.modules.Module.mc;

public class RectangleShader {
    private float uLocationX;
    private float uLocationY;
    private float uSizeX;
    private float uSizeY;
    private float radius;
    private Color color1 = Color.WHITE;
    private Color color2 = Color.WHITE;
    private Color color3 = Color.WHITE;
    private Color color4 = Color.WHITE;

    public static final RenderPipeline RECTANGLE_SHADER = RenderPipeline.builder()
            .withLocation(Identifier.of("thunderhack", "pipeline/rectangle"))
            .withVertexShader(Identifier.of("minecraft", "core/position_only"))
            .withFragmentShader(Identifier.of("minecraft", "core/rectangle"))
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
            .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
            .build();

    public RectangleShader() {
    }

    public void setParameters(float x, float y, float width, float height, float r, float alpha) {
        float i = (float) mc.getWindow().getScaleFactor();
        radius = r * i;
        uLocationX = x * i;
        uLocationY = -y * i + mc.getWindow().getScaledHeight() * i - height * i;
        uSizeX = width * i;
        uSizeY = height * i;

        Color c1 = HudEditor.getColor(270);
        Color c2 = HudEditor.getColor(0);
        Color c3 = HudEditor.getColor(180);
        Color c4 = HudEditor.getColor(90);

        color1 = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), Math.round(alpha * 255f));
        color2 = new Color(c2.getRed(), c2.getGreen(), c2.getBlue(), Math.round(alpha * 255f));
        color3 = new Color(c3.getRed(), c3.getGreen(), c3.getBlue(), Math.round(alpha * 255f));
        color4 = new Color(c4.getRed(), c4.getGreen(), c4.getBlue(), Math.round(alpha * 255f));
    }

    public void setParameters(float x, float y, float width, float height, float r, float alpha, Color c1, Color c2, Color c3, Color c4) {
        float i = (float) mc.getWindow().getScaleFactor();
        radius = r * i;
        uLocationX = x * i;
        uLocationY = -y * i + mc.getWindow().getScaledHeight() * i - height * i;
        uSizeX = width * i;
        uSizeY = height * i;
        color1 = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), Math.round(alpha * 255f));
        color2 = new Color(c2.getRed(), c2.getGreen(), c2.getBlue(), Math.round(alpha * 255f));
        color3 = new Color(c3.getRed(), c3.getGreen(), c3.getBlue(), Math.round(alpha * 255f));
        color4 = new Color(c4.getRed(), c4.getGreen(), c4.getBlue(), Math.round(alpha * 255f));
    }

    public void use() {
        RenderSystem.setShader(RECTANGLE_SHADER);
        RenderSystem.setShaderUniform("uSize", uSizeX, uSizeY);
        RenderSystem.setShaderUniform("uLocation", uLocationX, uLocationY);
        RenderSystem.setShaderUniform("radius", radius);
        RenderSystem.setShaderUniform("color1", color1.getRed() / 255f, color1.getGreen() / 255f, color1.getBlue() / 255f, color1.getAlpha() / 255f);
        RenderSystem.setShaderUniform("color2", color2.getRed() / 255f, color2.getGreen() / 255f, color2.getBlue() / 255f, color2.getAlpha() / 255f);
        RenderSystem.setShaderUniform("color3", color3.getRed() / 255f, color3.getGreen() / 255f, color3.getBlue() / 255f, color3.getAlpha() / 255f);
        RenderSystem.setShaderUniform("color4", color4.getRed() / 255f, color4.getGreen() / 255f, color4.getBlue() / 255f, color4.getAlpha() / 255f);
    }
}
