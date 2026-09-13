package thunder.hack.utility.render.shaders.satin.impl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.effect.PostEffectPass;
import net.minecraft.client.gl.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import thunder.hack.injection.accesors.IPostEffectPass;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ManagedUniform extends ManagedUniformBase implements
        Uniform1i, Uniform2i, Uniform3i, Uniform4i,
        Uniform1f, Uniform2f, Uniform3f, Uniform4f,
        UniformMat4 {

    private static final String BLOCK_NAME = "ThunderHackPost";
    private static final int BLOCK_SIZE = 256;
    private static final Map<String, Object> VALUES = new LinkedHashMap<>();

    private GpuBuffer[] targets = new GpuBuffer[0];

    public ManagedUniform(String name, int count) {
        super(name);
    }

    @Override
    public boolean findUniformTargets(List<PostEffectPass> shaders) {
        List<GpuBuffer> writableTargets = new ArrayList<>();
        for (PostEffectPass shader : shaders) {
            Map<String, GpuBuffer> uniformBuffers = ((IPostEffectPass) shader).getUniformBuffers();
            GpuBuffer buffer = uniformBuffers.get(BLOCK_NAME);
            if (buffer == null || buffer.isClosed()) {
                continue;
            }

            if ((buffer.usage() & GpuBuffer.USAGE_COPY_DST) == 0) {
                GpuBuffer replacement = RenderSystem.getDevice().createBuffer(
                        () -> "MelaHack post uniform block " + name,
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                        writeBlock());
                uniformBuffers.put(BLOCK_NAME, replacement);
                buffer.close();
                buffer = replacement;
            }

            writableTargets.add(buffer);
        }
        this.targets = writableTargets.toArray(GpuBuffer[]::new);
        syncCurrentValues();
        return this.targets.length > 0;
    }

    @Override
    public boolean findUniformTarget(ShaderProgram shader) {
        return false;
    }

    @Override public void set(int value) { put(new int[]{value}); }
    @Override public void set(int value0, int value1) { put(new int[]{value0, value1}); }
    @Override public void set(int value0, int value1, int value2) { put(new int[]{value0, value1, value2}); }
    @Override public void set(int value0, int value1, int value2, int value3) { put(new int[]{value0, value1, value2, value3}); }
    @Override public void set(float value) { put(new float[]{value}); }
    @Override public void set(float value0, float value1) { put(new float[]{value0, value1}); }
    @Override public void set(Vector2f value) { put(new float[]{value.x, value.y}); }
    @Override public void set(float value0, float value1, float value2) { put(new float[]{value0, value1, value2}); }
    @Override public void set(Vector3f value) { put(new float[]{value.x, value.y, value.z}); }
    @Override public void set(float value0, float value1, float value2, float value3) { put(new float[]{value0, value1, value2, value3}); }
    @Override public void set(Vector4f value) { put(new float[]{value.x, value.y, value.z, value.w}); }
    @Override public void set(Matrix4f value) {}
    @Override public void setFromArray(float[] values) { put(values.clone()); }

    private void put(Object value) {
        VALUES.put(name, value);
        syncCurrentValues();
    }

    private void syncCurrentValues() {
        if (targets.length == 0) {
            return;
        }

        ByteBuffer data = writeBlock();
        int size = data.remaining();
        for (GpuBuffer target : targets) {
            if (!target.isClosed()) {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(target.slice(0, size), data.duplicate());
            }
        }
    }

    private static ByteBuffer writeBlock() {
        ByteBuffer buffer = BufferUtils.createByteBuffer(BLOCK_SIZE).order(ByteOrder.nativeOrder());
        return Std140Builder.intoBuffer(buffer)
                .putVec4(f("color", 0, 1f), f("color", 1, 1f), f("color", 2, 1f), f("color", 3, 1f))
                .putVec4(f("outlinecolor", 0, 1f), f("outlinecolor", 1, 1f), f("outlinecolor", 2, 1f), f("outlinecolor", 3, 1f))
                .putVec4(f("primaryColor", 0, 1f), f("primaryColor", 1, 1f), f("primaryColor", 2, 1f), f("primaryColor", 3, 1f))
                .putVec4(f("secondaryColor", 0, 1f), f("secondaryColor", 1, 1f), f("secondaryColor", 2, 1f), f("secondaryColor", 3, 1f))
                .putVec4(f("first", 0, 1f), f("first", 1, 1f), f("first", 2, 1f), f("first", 3, 1f))
                .putVec4(f("ffirst", 0, 1f), f("ffirst", 1, 1f), f("ffirst", 2, 1f), f("ffirst", 3, 1f))
                .putVec3(f("second", 0, 1f), f("second", 1, 1f), f("second", 2, 1f))
                .putVec3(f("third", 0, 1f), f("third", 1, 1f), f("third", 2, 1f))
                .putVec3(f("fsecond", 0, 1f), f("fsecond", 1, 1f), f("fsecond", 2, 1f))
                .putVec3(f("fthird", 0, 1f), f("fthird", 1, 1f), f("fthird", 2, 1f))
                .putVec2(f("resolution", 0, 1f), f("resolution", 1, 1f))
                .putFloat(f("time", 0, 0f))
                .putFloat(f("alpha0", 0, 1f))
                .putFloat(f("alpha1", 0, 1f))
                .putFloat(f("alpha2", 0, 1f))
                .putFloat(f("factor", 0, 1f))
                .putFloat(f("moreGradient", 0, 1f))
                .putFloat(f("fillAlpha", 0, 1f))
                .putInt(i("quality", 0, 1))
                .putInt(i("lineWidth", 0, 1))
                .putInt(i("oct", 0, 1))
                .get();
    }

    private static float f(String name, int index, float fallback) {
        Object value = VALUES.get(name);
        if (value instanceof float[] values && index < values.length) {
            return values[index];
        }
        if (value instanceof int[] values && index < values.length) {
            return values[index];
        }
        return fallback;
    }

    private static int i(String name, int index, int fallback) {
        Object value = VALUES.get(name);
        if (value instanceof int[] values && index < values.length) {
            return values[index];
        }
        if (value instanceof float[] values && index < values.length) {
            return (int) values[index];
        }
        return fallback;
    }
}
