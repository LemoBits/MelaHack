package thunder.hack.utility.render.shaders.satin.impl;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;

public final class PostEffectRenderUtil {
    private PostEffectRenderUtil() {
    }

    public static void render(PostChain effect, int width, int height, Map<Identifier, RenderTarget> externalTargets, GraphicsResourceAllocator allocator) {
        FrameGraphBuilder frameGraphBuilder = new FrameGraphBuilder();
        Map<Identifier, ResourceHandle<RenderTarget>> handles = new HashMap<>(externalTargets.size());
        for (Map.Entry<Identifier, RenderTarget> entry : externalTargets.entrySet()) {
            handles.put(entry.getKey(), frameGraphBuilder.importExternal(entry.getKey().toString(), entry.getValue()));
        }

        PostChain.TargetBundle framebufferSet = new PostChain.TargetBundle() {
            @Override
            public void replace(Identifier id, ResourceHandle<RenderTarget> framebuffer) {
                handles.put(id, framebuffer);
            }

            @Override
            public ResourceHandle<RenderTarget> get(Identifier id) {
                return handles.get(id);
            }
        };

        effect.addToFrame(frameGraphBuilder, width, height, framebufferSet);
        frameGraphBuilder.execute(allocator);
    }
}
