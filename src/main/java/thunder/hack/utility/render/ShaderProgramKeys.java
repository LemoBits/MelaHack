package thunder.hack.utility.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import thunder.hack.utility.render.compat.RenderSystem;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shader program keys for MelaHack's immediate mode renderer.
 *
 * <p>Minecraft 26.2 bakes the primitive topology, blend state and depth state into the
 * {@link RenderPipeline} object, so a single program needs one pipeline per combination.
 * Each constant below therefore only describes the shader pair and vertex format; the
 * concrete pipeline is resolved lazily by {@link #resolve(RenderPipeline, PrimitiveTopology)}.</p>
 */
public final class ShaderProgramKeys {
    private static final Map<RenderPipeline, Program> PROGRAMS = new IdentityHashMap<>();
    private static final Map<Variant, RenderPipeline> VARIANTS = new ConcurrentHashMap<>();

    public static final RenderPipeline POSITION = register("position", "core/position_color", DefaultVertexFormat.POSITION_COLOR, false);
    public static final RenderPipeline POSITION_COLOR = register("position_color", "core/position_color", DefaultVertexFormat.POSITION_COLOR, false);
    public static final RenderPipeline POSITION_TEX = register("position_tex", "core/position_tex", DefaultVertexFormat.POSITION_TEX, true);
    public static final RenderPipeline POSITION_TEX_ADDITIVE = register("position_tex_additive", "core/position_tex", DefaultVertexFormat.POSITION_TEX, true);
    public static final RenderPipeline POSITION_TEX_COLOR = register("position_tex_color", "core/position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR, true);
    public static final RenderPipeline POSITION_TEX_COLOR_ADDITIVE = register("position_tex_color_additive", "core/position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR, true);
    public static final RenderPipeline POSITION_TEX_COLOR_DST_ALPHA = register("position_tex_color_dst_alpha", "core/position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR, true);
    public static final RenderPipeline RENDERTYPE_LINES = register("lines", "core/position_color", DefaultVertexFormat.POSITION_COLOR, false);

    /** Additive blending, used when meshes are drawn with {@code blendFunc(SRC_ALPHA, ONE)}. */
    public static final BlendFunction ADDITIVE_BLEND = BlendFunction.ADDITIVE;
    /** {@code blendFunc(DST_ALPHA, ONE_MINUS_DST_ALPHA)}. */
    public static final BlendFunction DST_ALPHA_BLEND = new BlendFunction(
            BlendFactor.DST_ALPHA, BlendFactor.ONE_MINUS_DST_ALPHA, BlendFactor.ONE, BlendFactor.ZERO);

    private ShaderProgramKeys() {
    }

    public record Program(String name, String shader, VertexFormat format, boolean textured) {
    }

    private record Variant(Program program, PrimitiveTopology topology, BlendFunction blend, boolean depthTest) {
    }

    private static RenderPipeline register(String name, String shader, VertexFormat format, boolean textured) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("thunderhack", "program/" + name))
                .withVertexShader(Identifier.fromNamespaceAndPath("minecraft", shader))
                .withFragmentShader(Identifier.fromNamespaceAndPath("minecraft", shader))
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .withCull(false)
                .withVertexBinding(0, format)
                .withPrimitiveTopology(PrimitiveTopology.QUADS);

        if (textured) {
            builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        }

        RenderPipeline pipeline = builder.build();
        PROGRAMS.put(pipeline, new Program(name, shader, format, textured));
        return pipeline;
    }

    /**
     * @return the program describing the given key, or {@code null} when it is a fully specified
     *         custom pipeline (for example one of MelaHack's rounded-rectangle shaders).
     */
    public static Program program(RenderPipeline key) {
        return PROGRAMS.get(key);
    }

    /**
     * Resolves a concrete, drawable pipeline for the given program key.
     *
     * @param key      one of the constants of this class
     * @param topology primitive topology of the mesh that is about to be drawn
     */
    public static RenderPipeline resolve(RenderPipeline key, PrimitiveTopology topology) {
        Program program = PROGRAMS.get(key);
        boolean depthTest = RenderSystem.isDepthTestEnabled() && RenderSystem.isWorldProjection();
        if (program == null) {
            return key;
        }

        BlendFunction blend = blendFor(key, RenderSystem.getBlendMode());
        return VARIANTS.computeIfAbsent(new Variant(program, topology, blend, depthTest),
                ShaderProgramKeys::build);
    }

    private static BlendFunction blendFor(RenderPipeline key, RenderSystem.BlendMode mode) {
        if (key == POSITION_TEX_COLOR_DST_ALPHA) {
            return mode == RenderSystem.BlendMode.ADDITIVE ? ADDITIVE_BLEND : DST_ALPHA_BLEND;
        }

        return switch (mode) {
            case ADDITIVE -> ADDITIVE_BLEND;
            case DST_ALPHA -> DST_ALPHA_BLEND;
            default -> BlendFunction.TRANSLUCENT;
        };
    }

    private static RenderPipeline build(Variant variant) {
        Program program = variant.program();

        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("thunderhack",
                        "pipeline/" + program.name() + "_" + variant.topology().name().toLowerCase(Locale.ROOT)
                                + "_" + Integer.toHexString(variant.blend().hashCode())))
                .withVertexShader(Identifier.fromNamespaceAndPath("minecraft", program.shader()))
                .withFragmentShader(Identifier.fromNamespaceAndPath("minecraft", program.shader()))
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withColorTargetState(new ColorTargetState(variant.blend()))
                .withDepthStencilState(new DepthStencilState(
                        variant.depthTest() ? CompareOp.GREATER_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS, false))
                .withCull(false)
                .withVertexBinding(0, program.format())
                .withPrimitiveTopology(variant.topology());

        if (program.textured()) {
            builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        }

        return builder.build();
    }

    /** Layout used by MelaHack's own fragment shaders. */
    public static BindGroupLayout customUniformLayout() {
        return BindGroupLayout.builder()
                .withUniform("ThunderHackCustom", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
                .build();
    }
}
