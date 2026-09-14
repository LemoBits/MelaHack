package thunder.hack.utility.render;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import thunder.hack.utility.render.compat.RenderSystem;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Draws MelaHack's immediate mode meshes.
 *
 * <p>Minecraft 26.2 renders through a frame graph, so this class has to issue its own render
 * passes. Geometry that belongs to a screen or the HUD is recorded while vanilla extracts the
 * frame (which happens <em>before</em> the main render target is cleared) and is flushed once
 * the vanilla GUI pass finished, while world space geometry is drawn right away so that it can
 * still use the depth buffer of the level.</p>
 */
public final class BufferRenderer {
    private static final String CUSTOM_UNIFORMS = "ThunderHackCustom";
    private static final int CUSTOM_UNIFORM_SIZE = 256;

    private static final ByteBuffer CUSTOM_UNIFORM_DATA = ByteBuffer.allocateDirect(CUSTOM_UNIFORM_SIZE).order(ByteOrder.nativeOrder());
    private static final ByteBuffer GUI_PROJECTION_DATA =
            ByteBuffer.allocateDirect(RenderSystem.PROJECTION_MATRIX_UBO_SIZE).order(ByteOrder.nativeOrder());

    private static GpuBuffer guiProjectionBuffer;
    private static int guiProjectionWidth;
    private static int guiProjectionHeight;

    private static final Matrix4f worldProjectionMatrix = new Matrix4f();
    private static ProjectionMatrixBuffer worldProjectionBuffer;

    private static DynamicUniformStorage<CustomUniforms> customUniforms;

    private static final List<QueuedDraw> QUEUE = new ArrayList<>();

    private BufferRenderer() {
    }

    private record QueuedDraw(
            GpuBuffer vertices,
            int vertexCount,
            int indexCount,
            PrimitiveTopology topology,
            RenderPipeline pipeline,
            boolean customUniforms,
            float[] uniforms,
            GpuTextureView[] textures,
            GpuSampler[] samplers,
            Vector4f shaderColor,
            boolean scissored,
            int scissorX,
            int scissorY,
            int scissorWidth,
            int scissorHeight,
            Runnable capture
    ) {
    }

    public static void drawWithGlobalProgram(MeshData mesh) {
        try {
            MeshData.DrawState state = mesh.drawState();
            RenderPipeline key = RenderSystem.getCurrentPipeline();
            if (key == null) {
                key = ShaderProgramKeys.POSITION_COLOR;
            }

            RenderPipeline pipeline = ShaderProgramKeys.resolve(key, state.primitiveTopology());
            boolean custom = usesCustomUniforms(pipeline);

            QueuedDraw draw = new QueuedDraw(
                    uploadVertices(mesh.vertexBuffer()),
                    state.vertexCount(),
                    state.indexCount(),
                    state.primitiveTopology(),
                    pipeline,
                    custom,
                    custom ? snapshotUniforms() : null,
                    new GpuTextureView[]{RenderSystem.getShaderTexture(0), RenderSystem.getShaderTexture(1), RenderSystem.getShaderTexture(2)},
                    new GpuSampler[]{RenderSystem.getShaderSampler(0), RenderSystem.getShaderSampler(1), RenderSystem.getShaderSampler(2)},
                    shaderColor(),
                    RenderSystem.isScissorEnabled(),
                    RenderSystem.getScissorX(),
                    RenderSystem.getScissorY(),
                    RenderSystem.getScissorWidth(),
                    RenderSystem.getScissorHeight(),
                    RenderSystem.takePendingCapture());

            if (RenderSystem.isWorldProjection()) {
                draw(draw);
            } else {
                QUEUE.add(draw);
            }
        } finally {
            mesh.close();
        }
    }

    /** Flushes screen space geometry. Called once per frame after the vanilla GUI pass. */
    public static void flushQueue() {
        if (QUEUE.isEmpty()) {
            return;
        }

        List<QueuedDraw> pending = new ArrayList<>(QUEUE);
        QUEUE.clear();
        for (QueuedDraw draw : pending) {
            draw(draw);
        }
    }

    /** Releases any resources that were never flushed, for example when the game shuts down. */
    public static void discardQueue() {
        for (QueuedDraw draw : QUEUE) {
            draw.vertices().close();
        }
        QUEUE.clear();
    }

    private static void draw(QueuedDraw draw) {
        Minecraft client = Minecraft.getInstance();
        RenderTarget target = client.gameRenderer.mainRenderTarget();
        GpuDevice device = RenderSystem.getDevice();
        CommandEncoder encoder = device.createCommandEncoder();

        GpuBuffer indexBuffer = null;
        IndexType indexType = null;
        if (draw.indexCount() > 0) {
            AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(draw.topology());
            indexBuffer = sequential.getBuffer(draw.indexCount());
            indexType = sequential.type();
        }

        GpuBufferSlice projection = projectionBuffer(encoder);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .writeTransform(new Matrix4f(), draw.shaderColor());

        GpuBufferSlice custom = null;
        if (draw.customUniforms()) {
            custom = customUniformStorage().writeUniform(new CustomUniforms(draw.uniforms()));
        }

        if (draw.capture() != null) {
            draw.capture().run();
        }

        try (RenderPass pass = encoder.createRenderPass(
                () -> "thunderhack immediate",
                target.getColorTextureView(), Optional.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(draw.pipeline());
            pass.setUniform("Projection", projection);
            pass.setUniform("DynamicTransforms", transforms);
            if (custom != null) {
                pass.setUniform(CUSTOM_UNIFORMS, custom);
            }

            List<String> samplers = BindGroupLayout.flattenSamplers(draw.pipeline().getBindGroupLayouts());
            for (int slot = 0; slot < samplers.size() && slot < draw.textures().length; slot++) {
                GpuTextureView texture = draw.textures()[slot];
                GpuSampler sampler = draw.samplers()[slot];
                if (texture != null && sampler != null) {
                    pass.bindTexture(samplers.get(slot), texture, sampler);
                }
            }

            if (draw.scissored()) {
                // GUI coordinates can round past the framebuffer edge when its dimensions are not
                // evenly divisible by the GUI scale. RenderPass validates scissors strictly in
                // 26.2, and opening a module also briefly produces a zero-height animated clip.
                // Intersect with the framebuffer and discard draws whose clip is empty.
                int framebufferWidth = client.getWindow().getWidth();
                int framebufferHeight = client.getWindow().getHeight();
                int left = (int) Math.clamp((long) draw.scissorX(), 0L, framebufferWidth);
                int bottom = (int) Math.clamp((long) draw.scissorY(), 0L, framebufferHeight);
                int right = (int) Math.clamp((long) draw.scissorX() + draw.scissorWidth(), 0L, framebufferWidth);
                int top = (int) Math.clamp((long) draw.scissorY() + draw.scissorHeight(), 0L, framebufferHeight);

                if (right <= left || top <= bottom) {
                    return;
                }

                pass.enableScissor(left, bottom, right - left, top - bottom);
            } else {
                pass.disableScissor();
            }

            pass.setVertexBuffer(0, draw.vertices().slice());
            if (indexBuffer != null) {
                pass.setIndexBuffer(indexBuffer, indexType);
                pass.drawIndexed(draw.indexCount(), 1, 0, 0, 0);
            } else {
                pass.draw(draw.vertexCount(), 1, 0, 0);
            }
        } finally {
            draw.vertices().close();
        }
    }

    private static GpuBuffer uploadVertices(ByteBuffer data) {
        GpuDevice device = RenderSystem.getDevice();
        GpuBuffer buffer = device.createBuffer(() -> "thunderhack immediate vertices",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, data.remaining());
        device.createCommandEncoder().writeToBuffer(buffer.slice(), data);
        return buffer;
    }

    private static Vector4f shaderColor() {
        float[] color = RenderSystem.getShaderColor();
        return new Vector4f(color[0], color[1], color[2], color[3]);
    }

    private static GpuBufferSlice projectionBuffer(CommandEncoder encoder) {
        if (RenderSystem.isWorldProjection()) {
            GpuBufferSlice world = worldProjection();
            if (world != null) {
                return world;
            }
        }

        return guiProjection(encoder);
    }

    /**
     * World space geometry is drawn with the same perspective projection the level uses, taken from
     * the current camera render state.
     */
    private static GpuBufferSlice worldProjection() {
        CameraRenderState cameraState = Minecraft.getInstance().gameRenderer
                .gameRenderState().levelRenderState.cameraRenderState;
        if (cameraState == null || cameraState.projectionMatrix == null) {
            return RenderSystem.getProjectionMatrixBuffer();
        }

        worldProjectionMatrix.set(cameraState.projectionMatrix);
        if (worldProjectionBuffer == null) {
            worldProjectionBuffer = new ProjectionMatrixBuffer("thunderhack world projection");
        }

        return worldProjectionBuffer.getBuffer(worldProjectionMatrix);
    }

    private static GpuBufferSlice guiProjection(CommandEncoder encoder) {
        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        if (guiProjectionBuffer == null || guiProjectionBuffer.isClosed()) {
            guiProjectionBuffer = RenderSystem.getDevice().createBuffer(() -> "thunderhack gui projection",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
            guiProjectionWidth = 0;
            guiProjectionHeight = 0;
        }

        if (guiProjectionWidth != width || guiProjectionHeight != height) {
            Matrix4f projection = new Matrix4f().setOrtho(0.0f, width, height, 0.0f, -1000.0f, 1000.0f);
            GUI_PROJECTION_DATA.clear();
            ByteBuffer data = Std140Builder.intoBuffer(GUI_PROJECTION_DATA).putMat4f(projection).get();
            encoder.writeToBuffer(guiProjectionBuffer.slice(0, data.remaining()), data);
            guiProjectionWidth = width;
            guiProjectionHeight = height;
        }

        return guiProjectionBuffer.slice(0, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
    }

    private static boolean usesCustomUniforms(RenderPipeline pipeline) {
        for (BindGroupLayout layout : pipeline.getBindGroupLayouts()) {
            for (BindGroupLayout.UniformDescription uniform : layout.getUniforms()) {
                if (CUSTOM_UNIFORMS.equals(uniform.name())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static float[] snapshotUniforms() {
        Map<String, Object> uniforms = RenderSystem.getCurrentUniforms();
        return new float[]{
                vec(uniforms, "color1", 0, 1f), vec(uniforms, "color1", 1, 1f), vec(uniforms, "color1", 2, 1f), vec(uniforms, "color1", 3, 1f),
                vec(uniforms, "color2", 0, 1f), vec(uniforms, "color2", 1, 1f), vec(uniforms, "color2", 2, 1f), vec(uniforms, "color2", 3, 1f),
                vec(uniforms, "color3", 0, 1f), vec(uniforms, "color3", 1, 1f), vec(uniforms, "color3", 2, 1f), vec(uniforms, "color3", 3, 1f),
                vec(uniforms, "color4", 0, 1f), vec(uniforms, "color4", 1, 1f), vec(uniforms, "color4", 2, 1f), vec(uniforms, "color4", 3, 1f),
                vec(uniforms, "uSize", 0, 1f), vec(uniforms, "uSize", 1, 1f),
                vec(uniforms, "uSize2", 0, 1f), vec(uniforms, "uSize2", 1, 1f),
                vec(uniforms, "uLocation", 0, 0f), vec(uniforms, "uLocation", 1, 0f),
                vec(uniforms, "InputResolution", 0, 1f), vec(uniforms, "InputResolution", 1, 1f),
                vec(uniforms, "radius", 0, 0f),
                vec(uniforms, "blend", 0, 1f),
                vec(uniforms, "alpha", 0, 1f),
                vec(uniforms, "outline", 0, 0f),
                vec(uniforms, "glow", 0, 0f),
                vec(uniforms, "thickness", 0, 0f),
                vec(uniforms, "start", 0, 0f),
                vec(uniforms, "end", 0, 0f),
                vec(uniforms, "time", 0, 0f),
                vec(uniforms, "Time", 0, 0f),
                vec(uniforms, "Quality", 0, 1f),
                vec(uniforms, "Brightness", 0, 1f),
        };
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

    private static DynamicUniformStorage<CustomUniforms> customUniformStorage() {
        if (customUniforms == null) {
            customUniforms = new DynamicUniformStorage<>("ThunderHack custom uniforms", CUSTOM_UNIFORM_SIZE, 16);
        }
        return customUniforms;
    }

    /** Resets the custom uniform storage once all draws of a frame were issued. */
    public static void endFrame() {
        if (customUniforms != null) {
            customUniforms.endFrame();
        }
    }

    /**
     * std140 block matching {@code layout(std140) uniform ThunderHackCustom} in MelaHack's
     * fragment shaders.
     */
    private static final class CustomUniforms implements DynamicUniformStorage.DynamicUniform {
        private final float[] values;

        private CustomUniforms(float[] values) {
            this.values = values;
        }

        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder builder = Std140Builder.intoBuffer(buffer)
                    .putVec4(values[0], values[1], values[2], values[3])
                    .putVec4(values[4], values[5], values[6], values[7])
                    .putVec4(values[8], values[9], values[10], values[11])
                    .putVec4(values[12], values[13], values[14], values[15])
                    .putVec2(values[16], values[17])
                    .putVec2(values[18], values[19])
                    .putVec2(values[20], values[21])
                    .putVec2(values[22], values[23]);

            for (int i = 24; i < values.length; i++) {
                builder.putFloat(values[i]);
            }

            builder.get();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CustomUniforms uniforms && Arrays.equals(values, uniforms.values);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(values);
        }
    }
}
