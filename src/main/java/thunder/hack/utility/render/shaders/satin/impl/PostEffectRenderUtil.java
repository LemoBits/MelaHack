package thunder.hack.utility.render.shaders.satin.impl;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class PostEffectRenderUtil {
    private PostEffectRenderUtil() {
    }

    public static void render(PostEffectProcessor effect, int width, int height, Map<Identifier, Framebuffer> externalTargets, ObjectAllocator allocator) {
        FrameGraphBuilder frameGraphBuilder = new FrameGraphBuilder();
        Map<Identifier, Handle<Framebuffer>> handles = new HashMap<>(externalTargets.size());
        for (Map.Entry<Identifier, Framebuffer> entry : externalTargets.entrySet()) {
            handles.put(entry.getKey(), frameGraphBuilder.createObjectNode(entry.getKey().toString(), entry.getValue()));
        }

        PostEffectProcessor.FramebufferSet framebufferSet = new PostEffectProcessor.FramebufferSet() {
            @Override
            public void set(Identifier id, Handle<Framebuffer> framebuffer) {
                handles.put(id, framebuffer);
            }

            @Override
            public Handle<Framebuffer> get(Identifier id) {
                return handles.get(id);
            }
        };

        effect.render(frameGraphBuilder, width, height, framebufferSet);
        frameGraphBuilder.run(allocator);
    }
}
