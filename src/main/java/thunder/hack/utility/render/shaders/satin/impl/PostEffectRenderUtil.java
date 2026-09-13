package thunder.hack.utility.render.shaders.satin.impl;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

public final class PostEffectRenderUtil {
    private PostEffectRenderUtil() {
    }

    public static void render(PostChain effect, int width, int height, Map<ResourceLocation, RenderTarget> externalTargets, GraphicsResourceAllocator allocator) {
        FrameGraphBuilder frameGraphBuilder = new FrameGraphBuilder();
        Map<ResourceLocation, ResourceHandle<RenderTarget>> handles = new HashMap<>(externalTargets.size());
        for (Map.Entry<ResourceLocation, RenderTarget> entry : externalTargets.entrySet()) {
            handles.put(entry.getKey(), frameGraphBuilder.importExternal(entry.getKey().toString(), entry.getValue()));
        }

        PostChain.TargetBundle framebufferSet = new PostChain.TargetBundle() {
            @Override
            public void replace(ResourceLocation id, ResourceHandle<RenderTarget> framebuffer) {
                handles.put(id, framebuffer);
            }

            @Override
            public ResourceHandle<RenderTarget> get(ResourceLocation id) {
                return handles.get(id);
            }
        };

        effect.addToFrame(frameGraphBuilder, width, height, framebufferSet);
        frameGraphBuilder.execute(allocator);
    }
}
