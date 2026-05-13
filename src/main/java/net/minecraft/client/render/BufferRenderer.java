package net.minecraft.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import thunder.hack.utility.render.compat.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;

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
        }

        try (buffer; GpuBuffer vertices = parameters.format().uploadImmediateVertexBuffer(buffer.getBuffer())) {
            RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(parameters.mode());
            GpuBuffer indices = shapeIndexBuffer.getIndexBuffer(parameters.indexCount());
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    framebuffer.getColorAttachment(), OptionalInt.empty(),
                    framebuffer.getDepthAttachment(), OptionalDouble.empty())) {
                pass.setPipeline(pipeline);
                GpuTexture texture = RenderSystem.getShaderTexture(0);
                if (texture != null) {
                    pass.bindSampler("Sampler0", texture);
                }
                pass.setVertexBuffer(0, vertices);
                pass.setIndexBuffer(indices, shapeIndexBuffer.getIndexType());
                pass.drawIndexed(0, parameters.indexCount());
            }
        }
    }
}
