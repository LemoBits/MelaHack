package net.minecraft.client.gl;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.UniformType;

public final class ShaderProgramKeys {
    public static final RenderPipeline POSITION = RenderPipelines.DEBUG_QUADS;
    public static final RenderPipeline POSITION_COLOR = RenderPipelines.DEBUG_QUADS;
    public static final RenderPipeline POSITION_TEX = RenderPipeline.builder()
            .withLocation(Identifier.of("thunderhack", "pipeline/position_tex"))
            .withVertexShader(Identifier.of("minecraft", "core/position_tex"))
            .withFragmentShader(Identifier.of("minecraft", "core/position_tex"))
            .withSampler("Sampler0")
            .withUniform("ModelViewMat", UniformType.MATRIX4X4)
            .withUniform("ProjMat", UniformType.MATRIX4X4)
            .withUniform("ColorModulator", UniformType.VEC4)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS)
            .build();
    public static final RenderPipeline POSITION_TEX_ADDITIVE = RenderPipeline.builder()
            .withLocation(Identifier.of("thunderhack", "pipeline/position_tex_additive"))
            .withVertexShader(Identifier.of("minecraft", "core/position_tex"))
            .withFragmentShader(Identifier.of("minecraft", "core/position_tex"))
            .withSampler("Sampler0")
            .withUniform("ModelViewMat", UniformType.MATRIX4X4)
            .withUniform("ProjMat", UniformType.MATRIX4X4)
            .withUniform("ColorModulator", UniformType.VEC4)
            .withBlend(BlendFunction.ADDITIVE)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS)
            .build();
    public static final RenderPipeline POSITION_TEX_COLOR = RenderPipelines.GUI_TEXTURED;
    public static final RenderPipeline POSITION_TEX_COLOR_ADDITIVE = RenderPipeline.builder()
            .withLocation(Identifier.of("thunderhack", "pipeline/position_tex_color_additive"))
            .withVertexShader(Identifier.of("minecraft", "core/position_tex_color"))
            .withFragmentShader(Identifier.of("minecraft", "core/position_tex_color"))
            .withSampler("Sampler0")
            .withUniform("ModelViewMat", UniformType.MATRIX4X4)
            .withUniform("ProjMat", UniformType.MATRIX4X4)
            .withUniform("ColorModulator", UniformType.VEC4)
            .withBlend(BlendFunction.ADDITIVE)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .build();
    public static final RenderPipeline POSITION_TEX_COLOR_DST_ALPHA = RenderPipeline.builder()
            .withLocation(Identifier.of("thunderhack", "pipeline/position_tex_color_dst_alpha"))
            .withVertexShader(Identifier.of("minecraft", "core/position_tex_color"))
            .withFragmentShader(Identifier.of("minecraft", "core/position_tex_color"))
            .withSampler("Sampler0")
            .withUniform("ModelViewMat", UniformType.MATRIX4X4)
            .withUniform("ProjMat", UniformType.MATRIX4X4)
            .withUniform("ColorModulator", UniformType.VEC4)
            .withBlend(new BlendFunction(
                    com.mojang.blaze3d.platform.SourceFactor.DST_ALPHA,
                    com.mojang.blaze3d.platform.DestFactor.ONE_MINUS_DST_ALPHA,
                    com.mojang.blaze3d.platform.SourceFactor.ONE,
                    com.mojang.blaze3d.platform.DestFactor.ZERO))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .build();
    public static final RenderPipeline RENDERTYPE_LINES = RenderPipelines.LINES;

    private ShaderProgramKeys() {
    }
}
