package thunder.hack.utility.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;

/** Vanilla-backed pipelines used by the 26.2 submit renderer compatibility layer. */
public final class ShaderProgramKeys {
    public static final RenderPipeline POSITION = RenderPipelines.DEBUG_QUADS;
    public static final RenderPipeline POSITION_COLOR = RenderPipelines.DEBUG_QUADS;
    public static final RenderPipeline POSITION_TEX = RenderPipelines.GUI_TEXTURED;
    public static final RenderPipeline POSITION_TEX_ADDITIVE = RenderPipelines.GUI_TEXTURED;
    public static final RenderPipeline POSITION_TEX_COLOR = RenderPipelines.GUI_TEXTURED;
    public static final RenderPipeline POSITION_TEX_COLOR_ADDITIVE = RenderPipelines.GUI_TEXTURED;
    public static final RenderPipeline POSITION_TEX_COLOR_DST_ALPHA = RenderPipelines.GUI_TEXTURED;
    public static final RenderPipeline RENDERTYPE_LINES = RenderPipelines.LINES;

    private ShaderProgramKeys() {}
}
