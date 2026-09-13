package thunder.hack.utility.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class ShaderProgramKeys {
    public static final RenderPipeline POSITION = RenderPipelines.DEBUG_QUADS;
    public static final RenderPipeline POSITION_COLOR = RenderPipelines.DEBUG_QUADS;
    public static final RenderPipeline POSITION_TEX = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("thunderhack", "pipeline/position_tex"))
            .withVertexShader(Identifier.fromNamespaceAndPath("minecraft", "core/position_tex"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("minecraft", "core/position_tex"))
            .withSampler("Sampler0")
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .build();
    public static final RenderPipeline POSITION_TEX_ADDITIVE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("thunderhack", "pipeline/position_tex_additive"))
            .withVertexShader(Identifier.fromNamespaceAndPath("minecraft", "core/position_tex"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("minecraft", "core/position_tex"))
            .withSampler("Sampler0")
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .build();
    public static final RenderPipeline POSITION_TEX_COLOR = RenderPipelines.GUI_TEXTURED;
    public static final RenderPipeline POSITION_TEX_COLOR_ADDITIVE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("thunderhack", "pipeline/position_tex_color_additive"))
            .withVertexShader(Identifier.fromNamespaceAndPath("minecraft", "core/position_tex_color"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("minecraft", "core/position_tex_color"))
            .withSampler("Sampler0")
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build();
    public static final RenderPipeline POSITION_TEX_COLOR_DST_ALPHA = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("thunderhack", "pipeline/position_tex_color_dst_alpha"))
            .withVertexShader(Identifier.fromNamespaceAndPath("minecraft", "core/position_tex_color"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("minecraft", "core/position_tex_color"))
            .withSampler("Sampler0")
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withColorTargetState(new ColorTargetState(new BlendFunction(
                    com.mojang.blaze3d.platform.SourceFactor.DST_ALPHA,
                    com.mojang.blaze3d.platform.DestFactor.ONE_MINUS_DST_ALPHA,
                    com.mojang.blaze3d.platform.SourceFactor.ONE,
                    com.mojang.blaze3d.platform.DestFactor.ZERO)))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build();
    public static final RenderPipeline RENDERTYPE_LINES = RenderPipelines.LINES;

    private ShaderProgramKeys() {
    }
}
