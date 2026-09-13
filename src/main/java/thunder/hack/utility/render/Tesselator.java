package thunder.hack.utility.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

/** Immediate-mode buffer builder retained for the mod's custom geometry. */
public final class Tesselator {
    private static final Tesselator INSTANCE = new Tesselator();
    private final ByteBufferBuilder allocator = new ByteBufferBuilder(2 * 1024 * 1024);

    private Tesselator() {}

    public static Tesselator getInstance() {
        return INSTANCE;
    }

    public BufferBuilder begin(PrimitiveTopology topology, VertexFormat format) {
        return new BufferBuilder(allocator, topology, format);
    }
}
