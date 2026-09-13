package thunder.hack.utility.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import thunder.hack.utility.render.ShaderProgramKeys;
import net.minecraft.client.renderer.RenderPipelines;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.joml.Matrix4f;

public final class BufferRenderer {
    private static final String THUNDERHACK_UNIFORMS = "ThunderHackCustom";
    private static final int THUNDERHACK_UNIFORM_SIZE = 256;
    private static final ByteBuffer THUNDERHACK_UNIFORM_DATA =
            ByteBuffer.allocateDirect(THUNDERHACK_UNIFORM_SIZE).order(ByteOrder.nativeOrder());
    private static final ByteBuffer GUI_PROJECTION_DATA =
            ByteBuffer.allocateDirect(com.mojang.blaze3d.systems.RenderSystem.PROJECTION_MATRIX_UBO_SIZE).order(ByteOrder.nativeOrder());
    private static final ByteBuffer GUI_TRANSFORMS_DATA =
            ByteBuffer.allocateDirect(256).order(ByteOrder.nativeOrder());
    private static GpuBuffer thunderHackUniformBuffer;
    private static GpuBuffer guiProjectionBuffer;
    private static GpuBuffer guiTransformsBuffer;
    private static int guiProjectionWidth;
    private static int guiProjectionHeight;

    private BufferRenderer() {
    }

    public static void drawWithGlobalProgram(MeshData buffer) {
        MeshData.DrawState parameters = buffer.drawState();
        RenderPipeline pipeline = thunder.hack.utility.render.compat.RenderSystem.getCurrentPipeline();
        if (pipeline == null) {
            pipeline = parameters.mode().name().contains("LINE")
                    ? RenderPipelines.LINES
                    : RenderPipelines.DEBUG_QUADS;
        } else if (pipeline == ShaderProgramKeys.POSITION_COLOR) {
            pipeline = switch (parameters.mode()) {
                case TRIANGLE_FAN -> RenderPipelines.DEBUG_TRIANGLE_FAN;
                case TRIANGLE_STRIP -> RenderPipelines.DEBUG_FILLED_BOX;
                case DEBUG_LINE_STRIP -> RenderPipelines.LINES;
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

        GpuBuffer vertices = parameters.format().uploadImmediateVertexBuffer(buffer.vertexBuffer());
        com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(parameters.mode());
        GpuBuffer indices = shapeIndexBuffer.getBuffer(parameters.indexCount());
        RenderTarget framebuffer = Minecraft.getInstance().getMainRenderTarget();
        CommandEncoder encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder();
        com.mojang.blaze3d.buffers.GpuBufferSlice thunderHackUniforms = prepareThunderHackUniforms(pipeline, encoder);
        com.mojang.blaze3d.buffers.GpuBufferSlice guiProjection = prepareGuiProjectionUniforms(encoder);
        com.mojang.blaze3d.buffers.GpuBufferSlice guiTransforms = prepareGuiTransformsUniforms(encoder);
        com.mojang.blaze3d.buffers.GpuBufferSlice previousProjection =
                com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrixBuffer();
        ProjectionType previousProjectionType = com.mojang.blaze3d.systems.RenderSystem.getProjectionType();
        com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(guiProjection, ProjectionType.ORTHOGRAPHIC);

        try (RenderPass pass = encoder.createRenderPass(
                () -> "thunderhack immediate",
                framebuffer.getColorTextureView(), OptionalInt.empty(),
                framebuffer.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            thunder.hack.utility.render.compat.RenderSystem.applyScissor(pass);
            setCommonUniforms(pass, thunderHackUniforms, guiProjection, guiTransforms);
            for (int i = 0; i < pipeline.getSamplers().size(); i++) {
                GpuTextureView texture = thunder.hack.utility.render.compat.RenderSystem.getShaderTexture(i);
                var sampler = thunder.hack.utility.render.compat.RenderSystem.getShaderSampler(i);
                if (texture != null && sampler != null) {
                    pass.bindTexture(pipeline.getSamplers().get(i), texture, sampler);
                }
            }
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indices, shapeIndexBuffer.type());
            pass.drawIndexed(0, 0, parameters.indexCount(), 1);
        } finally {
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(previousProjection, previousProjectionType);
            buffer.close();
        }
    }

    private static void setCommonUniforms(
            RenderPass pass,
            com.mojang.blaze3d.buffers.GpuBufferSlice thunderHackUniforms,
            com.mojang.blaze3d.buffers.GpuBufferSlice guiProjection,
            com.mojang.blaze3d.buffers.GpuBufferSlice guiTransforms) {
        com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(pass);
        pass.setUniform("Projection", guiProjection);
        pass.setUniform("DynamicTransforms", guiTransforms);

        if (thunderHackUniforms != null) {
            pass.setUniform(THUNDERHACK_UNIFORMS, thunderHackUniforms);
        }
    }

    private static com.mojang.blaze3d.buffers.GpuBufferSlice prepareGuiProjectionUniforms(CommandEncoder encoder) {
        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        if (guiProjectionBuffer == null || guiProjectionBuffer.isClosed()) {
            guiProjectionBuffer = com.mojang.blaze3d.systems.RenderSystem.getDevice().createBuffer(
                    () -> "ThunderHack GUI projection",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    com.mojang.blaze3d.systems.RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
            guiProjectionWidth = 0;
            guiProjectionHeight = 0;
        }

        if (guiProjectionWidth != width || guiProjectionHeight != height) {
            Matrix4f projection = new Matrix4f().setOrtho(0.0f, width, height, 0.0f, -1000.0f, 1000.0f);
            GUI_PROJECTION_DATA.clear();
            ByteBuffer data = Std140Builder.intoBuffer(GUI_PROJECTION_DATA)
                    .putMat4f(projection)
                    .get();
            encoder.writeToBuffer(guiProjectionBuffer.slice(0, data.remaining()), data);
            guiProjectionWidth = width;
            guiProjectionHeight = height;
        }

        return guiProjectionBuffer.slice(0, com.mojang.blaze3d.systems.RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
    }

    private static com.mojang.blaze3d.buffers.GpuBufferSlice prepareGuiTransformsUniforms(CommandEncoder encoder) {
        if (guiTransformsBuffer == null || guiTransformsBuffer.isClosed()) {
            guiTransformsBuffer = com.mojang.blaze3d.systems.RenderSystem.getDevice().createBuffer(
                    () -> "ThunderHack GUI transforms",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    256);
            GUI_TRANSFORMS_DATA.clear();
            ByteBuffer data = Std140Builder.intoBuffer(GUI_TRANSFORMS_DATA)
                    .putMat4f(new Matrix4f())
                    .putVec4(1.0f, 1.0f, 1.0f, 1.0f)
                    .putVec3(0.0f, 0.0f, 0.0f)
                    .putMat4f(new Matrix4f())
                    .putFloat(1.0f)
                    .get();
            encoder.writeToBuffer(guiTransformsBuffer.slice(0, data.remaining()), data);
        }

        return guiTransformsBuffer.slice(0, 256);
    }

    private static com.mojang.blaze3d.buffers.GpuBufferSlice prepareThunderHackUniforms(RenderPipeline pipeline, CommandEncoder encoder) {
        for (RenderPipeline.UniformDescription uniform : pipeline.getUniforms()) {
            if (THUNDERHACK_UNIFORMS.equals(uniform.name())) {
                return uploadThunderHackUniforms(encoder);
            }
        }

        return null;
    }

    private static com.mojang.blaze3d.buffers.GpuBufferSlice uploadThunderHackUniforms(CommandEncoder encoder) {
        Map<String, Object> uniforms = thunder.hack.utility.render.compat.RenderSystem.getCurrentUniforms();

        THUNDERHACK_UNIFORM_DATA.clear();
        ByteBuffer data = Std140Builder.intoBuffer(THUNDERHACK_UNIFORM_DATA)
                .putVec4(vec(uniforms, "color1", 0, 1f), vec(uniforms, "color1", 1, 1f),
                        vec(uniforms, "color1", 2, 1f), vec(uniforms, "color1", 3, 1f))
                .putVec4(vec(uniforms, "color2", 0, 1f), vec(uniforms, "color2", 1, 1f),
                        vec(uniforms, "color2", 2, 1f), vec(uniforms, "color2", 3, 1f))
                .putVec4(vec(uniforms, "color3", 0, 1f), vec(uniforms, "color3", 1, 1f),
                        vec(uniforms, "color3", 2, 1f), vec(uniforms, "color3", 3, 1f))
                .putVec4(vec(uniforms, "color4", 0, 1f), vec(uniforms, "color4", 1, 1f),
                        vec(uniforms, "color4", 2, 1f), vec(uniforms, "color4", 3, 1f))
                .putVec2(vec(uniforms, "uSize", 0, 1f), vec(uniforms, "uSize", 1, 1f))
                .putVec2(vec(uniforms, "uSize2", 0, 1f), vec(uniforms, "uSize2", 1, 1f))
                .putVec2(vec(uniforms, "uLocation", 0, 0f), vec(uniforms, "uLocation", 1, 0f))
                .putVec2(vec(uniforms, "InputResolution", 0, 1f), vec(uniforms, "InputResolution", 1, 1f))
                .putFloat(vec(uniforms, "radius", 0, 0f))
                .putFloat(vec(uniforms, "blend", 0, 1f))
                .putFloat(vec(uniforms, "alpha", 0, 1f))
                .putFloat(vec(uniforms, "outline", 0, 0f))
                .putFloat(vec(uniforms, "glow", 0, 0f))
                .putFloat(vec(uniforms, "thickness", 0, 0f))
                .putFloat(vec(uniforms, "start", 0, 0f))
                .putFloat(vec(uniforms, "end", 0, 0f))
                .putFloat(vec(uniforms, "time", 0, 0f))
                .putFloat(vec(uniforms, "Time", 0, 0f))
                .putFloat(vec(uniforms, "Quality", 0, 1f))
                .putFloat(vec(uniforms, "Brightness", 0, 1f))
                .get();

        if (thunderHackUniformBuffer == null || thunderHackUniformBuffer.isClosed()) {
            thunderHackUniformBuffer = com.mojang.blaze3d.systems.RenderSystem.getDevice().createBuffer(
                    () -> "ThunderHack custom uniforms",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    THUNDERHACK_UNIFORM_SIZE);
        }

        int size = data.remaining();
        com.mojang.blaze3d.buffers.GpuBufferSlice slice = thunderHackUniformBuffer.slice(0, size);
        encoder.writeToBuffer(slice, data);
        return slice;
    }

    private static float vec(Map<String, Object> uniforms, String name, int index, float fallback) {
        Object value = uniforms.get(name);
        if (value instanceof float[] values && index < values.length) {
            return values[index];
        }
        if (value instanceof int[] values && index < values.length) {
            return values[index];
        }
        return fallback;
    }
}
