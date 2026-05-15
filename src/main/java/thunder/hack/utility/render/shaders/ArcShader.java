package thunder.hack.utility.render.shaders;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import thunder.hack.utility.render.compat.RenderSystem;

import java.awt.*;

import static thunder.hack.features.modules.Module.mc;

public class ArcShader {
    private float uLocationX;
    private float uLocationY;
    private float uSizeX;
    private float uSizeY;
    private float radius;
    private float thickness;
    private float time;
    private float start;
    private float end;
    private Color color1 = Color.WHITE;
    private Color color2 = Color.WHITE;

    public static final RenderPipeline ARC_SHADER = RenderPipeline.builder()
            .withLocation(Identifier.of("thunderhack", "pipeline/arc"))
            .withVertexShader(Identifier.of("minecraft", "core/position_only"))
            .withFragmentShader(Identifier.of("minecraft", "core/arc"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withUniform("ModelViewMat", UniformType.MATRIX4X4)
            .withUniform("ProjMat", UniformType.MATRIX4X4)
            .withUniform("color1", UniformType.VEC4)
            .withUniform("color2", UniformType.VEC4)
            .withUniform("uSize", UniformType.VEC2)
            .withUniform("uLocation", UniformType.VEC2)
            .withUniform("radius", UniformType.FLOAT)
            .withUniform("thickness", UniformType.FLOAT)
            .withUniform("start", UniformType.FLOAT)
            .withUniform("end", UniformType.FLOAT)
            .withUniform("time", UniformType.FLOAT)
            .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
            .build();

    public ArcShader() {
    }

    public void setParameters(float x, float y, float width, float height, float r, float thickness, float start, float end, Color c1, Color c2) {
        if (mc.player == null) {
            return;
        }

        float i = (float) mc.getWindow().getScaleFactor();
        radius = r * i;
        uLocationX = x * i;
        uLocationY = -y * i + mc.getWindow().getScaledHeight() * i - height * i;
        uSizeX = width * i;
        uSizeY = height * i;
        color1 = c1;
        color2 = c2;
        time = mc.player.age * 4f;
        this.thickness = thickness;
        this.start = start;
        this.end = end;
    }

    public void use() {
        if (mc.player == null) {
            return;
        }

        RenderSystem.setShader(ARC_SHADER);
        RenderSystem.setShaderUniform("uSize", uSizeX, uSizeY);
        RenderSystem.setShaderUniform("uLocation", uLocationX, uLocationY);
        RenderSystem.setShaderUniform("radius", radius);
        RenderSystem.setShaderUniform("thickness", thickness);
        RenderSystem.setShaderUniform("start", start);
        RenderSystem.setShaderUniform("end", end);
        RenderSystem.setShaderUniform("time", time);
        RenderSystem.setShaderUniform("color1", color1.getRed() / 255f, color1.getGreen() / 255f, color1.getBlue() / 255f, color1.getAlpha() / 255f);
        RenderSystem.setShaderUniform("color2", color2.getRed() / 255f, color2.getGreen() / 255f, color2.getBlue() / 255f, color2.getAlpha() / 255f);
    }
}
