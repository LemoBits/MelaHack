package net.minecraft.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.ShaderProgramKeys;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class BufferRenderer {
    private BufferRenderer() {
    }

    public static void drawWithGlobalProgram(BuiltBuffer buffer) {
        BuiltBuffer.DrawParameters parameters = buffer.getDrawParameters();
        RenderPipeline pipeline = thunder.hack.utility.render.compat.RenderSystem.getCurrentPipeline();
        if (pipeline == null) {
            pipeline = parameters.mode().name().contains("LINE")
                    ? RenderPipelines.LINES
                    : RenderPipelines.DEBUG_QUADS;
        } else if (pipeline == ShaderProgramKeys.POSITION_COLOR) {
            pipeline = switch (parameters.mode()) {
                case TRIANGLE_FAN -> RenderPipelines.DEBUG_TRIANGLE_FAN;
                case TRIANGLE_STRIP -> RenderPipelines.DEBUG_FILLED_BOX;
                case DEBUG_LINE_STRIP -> RenderPipelines.DEBUG_LINE_STRIP;
                case LINES, DEBUG_LINES -> RenderPipelines.LINES;
                default -> pipeline;
            };
        }
        thunder.hack.utility.render.compat.RenderSystem.BlendMode blendMode =
                thunder.hack.utility.render.compat.RenderSystem.getBlendMode();
        if (blendMode == thunder.hack.utility.render.compat.RenderSystem.BlendMode.ADDITIVE) {
            if (pipeline == ShaderProgramKeys.POSITION_TEX) {
                pipeline = ShaderProgramKeys.POSITION_TEX_ADDITIVE;
            } else if (pipeline == ShaderProgramKeys.POSITION_TEX_COLOR) {
                pipeline = ShaderProgramKeys.POSITION_TEX_COLOR_ADDITIVE;
            }
        } else if (blendMode == thunder.hack.utility.render.compat.RenderSystem.BlendMode.DST_ALPHA
                && pipeline == ShaderProgramKeys.POSITION_TEX_COLOR) {
            pipeline = ShaderProgramKeys.POSITION_TEX_COLOR_DST_ALPHA;
        }

        GpuBuffer vertices = parameters.format().uploadImmediateVertexBuffer(buffer.getBuffer());
        com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer shapeIndexBuffer = com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(parameters.mode());
        GpuBuffer indices = shapeIndexBuffer.getIndexBuffer(parameters.indexCount());
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();

        try (RenderPass pass = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                framebuffer.getColorAttachment(), OptionalInt.empty(),
                framebuffer.getDepthAttachment(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            thunder.hack.utility.render.compat.RenderSystem.applyScissor(pass);
            setCommonUniforms(pass, pipeline);
            for (Map.Entry<String, Object> uniform : thunder.hack.utility.render.compat.RenderSystem.getCurrentUniforms().entrySet()) {
                Object value = uniform.getValue();
                if (value instanceof float[] floats) {
                    pass.setUniform(uniform.getKey(), floats);
                } else if (value instanceof int[] ints) {
                    pass.setUniform(uniform.getKey(), ints);
                } else if (value instanceof Matrix4f matrix) {
                    pass.setUniform(uniform.getKey(), matrix);
                }
            }
            for (int i = 0; i < pipeline.getSamplers().size(); i++) {
                GpuTexture texture = com.mojang.blaze3d.systems.RenderSystem.getShaderTexture(i);
                if (texture != null) {
                    pass.bindSampler(pipeline.getSamplers().get(i), texture);
                }
            }
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indices, shapeIndexBuffer.getIndexType());
            pass.drawIndexed(0, parameters.indexCount());
        } finally {
            buffer.close();
        }
    }

    private static void setCommonUniforms(RenderPass pass, RenderPipeline pipeline) {
        boolean needsColor = false;
        for (RenderPipeline.UniformDescription uniform : pipeline.getUniforms()) {
            if ("ColorModulator".equals(uniform.name())) {
                needsColor = true;
                break;
            }
        }

        if (needsColor) {
            pass.setUniform("ColorModulator", com.mojang.blaze3d.systems.RenderSystem.getShaderColor());
        }
    }
}
