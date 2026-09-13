package thunder.hack.utility.render;

import com.mojang.blaze3d.vertex.MeshData;

/**
 * Owns immediate custom meshes. Minecraft 26.2 no longer exposes its immediate
 * uploader; custom meshes are closed here rather than leaking native storage.
 */
public final class BufferRenderer {
    private BufferRenderer() {}

    public static void drawWithGlobalProgram(MeshData buffer) {
        if (buffer != null) buffer.close();
    }
}
