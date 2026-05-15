package thunder.hack.utility.render.shaders;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import thunder.hack.utility.render.compat.RenderSystem;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import thunder.hack.utility.render.animation.AnimationUtility;

import static thunder.hack.features.modules.Module.mc;

public class MainMenuProgram {
    private float width;
    private float height;
    private float time;
    public static float time_ = 10000f;

    public static final RenderPipeline MAIN_MENU = RenderPipeline.builder()
            .withLocation(Identifier.of("thunderhack", "pipeline/main_menu"))
            .withVertexShader(Identifier.of("minecraft", "core/position_only"))
            .withFragmentShader(Identifier.of("minecraft", "core/mainmenu"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withUniform("ModelViewMat", UniformType.MATRIX4X4)
            .withUniform("ProjMat", UniformType.MATRIX4X4)
            .withUniform("uSize", UniformType.VEC2)
            .withUniform("Time", UniformType.FLOAT)
            .withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS)
            .build();

    public MainMenuProgram() {
    }

    public void setParameters(float x, float y, float width, float height) {
        float i = (float) mc.getWindow().getScaleFactor();
        this.width = width * i;
        this.height = height * i;
        time_ += (float) (0.55 * AnimationUtility.deltaTime());
        this.time = time_;
    }

    public void use() {
        RenderSystem.setShader(MAIN_MENU);
        RenderSystem.setShaderUniform("uSize", width, height);
        RenderSystem.setShaderUniform("Time", time);
    }
}
