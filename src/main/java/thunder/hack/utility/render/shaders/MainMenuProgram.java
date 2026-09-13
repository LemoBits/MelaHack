package thunder.hack.utility.render.shaders;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.BindGroupLayouts;
import thunder.hack.utility.render.ShaderProgramKeys;
import thunder.hack.utility.render.compat.RenderSystem;
import thunder.hack.utility.render.animation.AnimationUtility;

import static thunder.hack.features.modules.Module.mc;

public class MainMenuProgram {
    private float width;
    private float height;
    private float time;
    public static float time_ = 10000f;

    public static final RenderPipeline MAIN_MENU = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("thunderhack", "pipeline/main_menu"))
            .withVertexShader(Identifier.fromNamespaceAndPath("minecraft", "core/position_only"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("minecraft", "core/mainmenu"))
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(ShaderProgramKeys.customUniformLayout())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.POSITION)
            .withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.QUADS)
            .build();

    public MainMenuProgram() {
    }

    public void setParameters(float x, float y, float width, float height) {
        float i = (float) mc.getWindow().getGuiScale();
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
