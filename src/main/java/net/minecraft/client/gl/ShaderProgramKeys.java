package net.minecraft.client.gl;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

public final class ShaderProgramKeys {
    public static final RenderPipeline POSITION = RenderPipelines.DEBUG_QUADS;
    public static final RenderPipeline POSITION_COLOR = RenderPipelines.DEBUG_QUADS;
    public static final RenderPipeline POSITION_TEX = RenderPipeline.builder()
            .withLocation(ResourceLocation.fromNamespaceAndPath("thunderhack", "pipeline/position_tex"))
            .withVertexShader(ResourceLocation.fromNamespaceAndPath("minecraft", "core/position_tex"))
            .withFragmentShader(ResourceLocation.fromNamespaceAndPath("minecraft", "core/position_tex"))
            .withSampler("Sampler0")
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .build();
    public static final RenderPipeline POSITION_TEX_ADDITIVE = RenderPipeline.builder()
            .withLocation(ResourceLocation.fromNamespaceAndPath("thunderhack", "pipeline/position_tex_additive"))
            .withVertexShader(ResourceLocation.fromNamespaceAndPath("minecraft", "core/position_tex"))
            .withFragmentShader(ResourceLocation.fromNamespaceAndPath("minecraft", "core/position_tex"))
            .withSampler("Sampler0")
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.ADDITIVE)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .build();
    public static final RenderPipeline POSITION_TEX_COLOR = RenderPipelines.GUI_TEXTURED;
    public static final RenderPipeline POSITION_TEX_COLOR_ADDITIVE = RenderPipeline.builder()
            .withLocation(ResourceLocation.fromNamespaceAndPath("thunderhack", "pipeline/position_tex_color_additive"))
            .withVertexShader(ResourceLocation.fromNamespaceAndPath("minecraft", "core/position_tex_color"))
            .withFragmentShader(ResourceLocation.fromNamespaceAndPath("minecraft", "core/position_tex_color"))
            .withSampler("Sampler0")
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.ADDITIVE)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build();
    public static final RenderPipeline POSITION_TEX_COLOR_DST_ALPHA = RenderPipeline.builder()
            .withLocation(ResourceLocation.fromNamespaceAndPath("thunderhack", "pipeline/position_tex_color_dst_alpha"))
            .withVertexShader(ResourceLocation.fromNamespaceAndPath("minecraft", "core/position_tex_color"))
            .withFragmentShader(ResourceLocation.fromNamespaceAndPath("minecraft", "core/position_tex_color"))
            .withSampler("Sampler0")
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withBlend(new BlendFunction(
                    com.mojang.blaze3d.platform.SourceFactor.DST_ALPHA,
                    com.mojang.blaze3d.platform.DestFactor.ONE_MINUS_DST_ALPHA,
                    com.mojang.blaze3d.platform.SourceFactor.ONE,
                    com.mojang.blaze3d.platform.DestFactor.ZERO))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build();
    public static final RenderPipeline RENDERTYPE_LINES = RenderPipelines.LINES;

    private ShaderProgramKeys() {
    }
}
