package thunder.hack.core.manager.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.shaders.satin.api.managed.ManagedShaderEffect;
import thunder.hack.utility.render.shaders.satin.api.managed.ShaderEffectManager;
import thunder.hack.utility.render.shaders.satin.impl.PostEffectRenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL30C;
import thunder.hack.core.manager.IManager;
import thunder.hack.features.modules.render.Shaders;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShaderManager implements IManager {
    public static final Identifier BUF_IN = Identifier.of("thunderhack", "buf_in");
    public static final Identifier BUF_OUT = Identifier.of("thunderhack", "buf_out");
    private final static List<RenderTask> tasks = new ArrayList<>();
    private ThunderHackFramebuffer shaderBuffer;

    public float time = 0;

    public static ManagedShaderEffect DEFAULT_OUTLINE;
    public static ManagedShaderEffect SMOKE_OUTLINE;
    public static ManagedShaderEffect GRADIENT_OUTLINE;
    public static ManagedShaderEffect SNOW_OUTLINE;
    public static ManagedShaderEffect FADE_OUTLINE;

    public static ManagedShaderEffect DEFAULT;
    public static ManagedShaderEffect SMOKE;
    public static ManagedShaderEffect GRADIENT;
    public static ManagedShaderEffect SNOW;
    public static ManagedShaderEffect FADE;

    public void renderShader(Runnable runnable, Shader mode) {
        tasks.add(new RenderTask(runnable, mode));
    }

    public void renderShaders() {
        if (DEFAULT == null) {
            shaderBuffer = new ThunderHackFramebuffer(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
            reloadShaders();
        }

        if(shaderBuffer == null)
            return;

        tasks.forEach(t -> applyShader(t.task(), t.shader()));
        tasks.clear();
    }

    public void applyShader(Runnable runnable, Shader mode) {
        Framebuffer MCBuffer = MinecraftClient.getInstance().getFramebuffer();
        RenderSystem.assertOnRenderThreadOrInit();
        if (shaderBuffer.textureWidth != MCBuffer.textureWidth || shaderBuffer.textureHeight != MCBuffer.textureHeight)
            shaderBuffer.resize(MCBuffer.textureWidth, MCBuffer.textureHeight);
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, shaderBuffer.fbo);
        shaderBuffer.beginWrite(true);
        runnable.run();
        shaderBuffer.endWrite();
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, MCBuffer.fbo);
        MCBuffer.beginWrite(false);
        ManagedShaderEffect shader = getShader(mode);
        Framebuffer mainBuffer = MinecraftClient.getInstance().getFramebuffer();
        PostEffectProcessor effect = shader.getShaderEffect();

        setupShader(mode, shader);
        if (effect != null) {
            Map<Identifier, Framebuffer> externalTargets = new HashMap<>();
            externalTargets.put(PostEffectProcessor.MAIN, shaderBuffer);
            externalTargets.put(BUF_IN, shaderBuffer);
            externalTargets.put(BUF_OUT, shaderBuffer);
            PostEffectRenderUtil.render(effect, shaderBuffer.textureWidth, shaderBuffer.textureHeight, externalTargets, ObjectAllocator.TRIVIAL);
        }
        mainBuffer.beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        RenderSystem.backupProjectionMatrix();
        shaderBuffer.draw(shaderBuffer.textureWidth, shaderBuffer.textureHeight);
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        shaderBuffer.clear();
    }

    public ManagedShaderEffect getShader(@NotNull Shader mode) {
        return switch (mode) {
            case Gradient -> GRADIENT;
            case Smoke -> SMOKE;
            case Snow -> SNOW;
            case Fade -> FADE;
            default -> DEFAULT;
        };
    }

    public ManagedShaderEffect getShaderOutline(@NotNull Shader mode) {
        return switch (mode) {
            case Gradient -> GRADIENT_OUTLINE;
            case Smoke -> SMOKE_OUTLINE;
            case Snow -> SNOW_OUTLINE;
            case Fade -> FADE_OUTLINE;
            default -> DEFAULT_OUTLINE;
        };
    }

    public void setupShader(Shader shader, ManagedShaderEffect effect) {
        Shaders shaders = ModuleManager.shaders;
        if (shader == Shader.Gradient) {
            effect.setUniformValue("alpha0", shaders.glow.getValue() ? -1.0f : shaders.outlineColor.getValue().getAlpha() / 255.0f);
            effect.setUniformValue("alpha1", shaders.fillAlpha.getValue() / 255f);
            effect.setUniformValue("alpha2", shaders.alpha2.getValue() / 255f);
            effect.setUniformValue("lineWidth", shaders.lineWidth.getValue());
            effect.setUniformValue("oct", shaders.octaves.getValue());
            effect.setUniformValue("quality", shaders.quality.getValue());
            effect.setUniformValue("factor", shaders.factor.getValue());
            effect.setUniformValue("moreGradient", shaders.gradient.getValue());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", time);
            time += 0.008f;
        } else if (shader == Shader.Smoke) {
            effect.setUniformValue("alpha0", shaders.glow.getValue() ? -1.0f : shaders.outlineColor.getValue().getAlpha() / 255.0f);
            effect.setUniformValue("alpha1", shaders.fillAlpha.getValue() / 255f);
            effect.setUniformValue("lineWidth", shaders.lineWidth.getValue());
            effect.setUniformValue("quality", shaders.quality.getValue());
            effect.setUniformValue("first", shaders.outlineColor.getValue().getGlRed(), shaders.outlineColor.getValue().getGlGreen(), shaders.outlineColor.getValue().getGlBlue(), shaders.outlineColor.getValue().getGlAlpha());
            effect.setUniformValue("second", shaders.outlineColor1.getValue().getGlRed(), shaders.outlineColor1.getValue().getGlGreen(), shaders.outlineColor1.getValue().getGlBlue());
            effect.setUniformValue("third", shaders.outlineColor2.getValue().getGlRed(), shaders.outlineColor2.getValue().getGlGreen(), shaders.outlineColor2.getValue().getGlBlue());
            effect.setUniformValue("ffirst", shaders.fillColor1.getValue().getGlRed(), shaders.fillColor1.getValue().getGlGreen(), shaders.fillColor1.getValue().getGlBlue(), shaders.fillColor1.getValue().getGlAlpha());
            effect.setUniformValue("fsecond", shaders.fillColor2.getValue().getGlRed(), shaders.fillColor2.getValue().getGlGreen(), shaders.fillColor2.getValue().getGlBlue());
            effect.setUniformValue("fthird", shaders.fillColor3.getValue().getGlRed(), shaders.fillColor3.getValue().getGlGreen(), shaders.fillColor3.getValue().getGlBlue());
            effect.setUniformValue("oct", shaders.octaves.getValue());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", time);
            time += 0.008f;
        } else if (shader == Shader.Default) {
            effect.setUniformValue("alpha0", shaders.glow.getValue() ? -1.0f : shaders.outlineColor.getValue().getAlpha() / 255.0f);
            effect.setUniformValue("lineWidth", shaders.lineWidth.getValue());
            effect.setUniformValue("quality", shaders.quality.getValue());
            effect.setUniformValue("color", shaders.fillColor1.getValue().getGlRed(), shaders.fillColor1.getValue().getGlGreen(), shaders.fillColor1.getValue().getGlBlue(), shaders.fillColor1.getValue().getGlAlpha());
            effect.setUniformValue("outlinecolor", shaders.outlineColor.getValue().getGlRed(), shaders.outlineColor.getValue().getGlGreen(), shaders.outlineColor.getValue().getGlBlue(), shaders.outlineColor.getValue().getGlAlpha());
        } else if (shader == Shader.Snow) {
            effect.setUniformValue("color", shaders.fillColor1.getValue().getGlRed(), shaders.fillColor1.getValue().getGlGreen(), shaders.fillColor1.getValue().getGlBlue(), shaders.fillColor1.getValue().getGlAlpha());
            effect.setUniformValue("quality", shaders.quality.getValue());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", time);
            time += 0.008f;
        } else if (shader == Shader.Fade) {
            effect.setUniformValue("alpha0", shaders.glow.getValue() ? -1.0f : shaders.outlineColor.getValue().getAlpha() / 255.0f);
            effect.setUniformValue("fillAlpha", shaders.fillAlpha.getValue() / 255f);
            effect.setUniformValue("lineWidth", shaders.lineWidth.getValue());
            effect.setUniformValue("quality", shaders.quality.getValue());
            effect.setUniformValue("outlinecolor", shaders.outlineColor.getValue().getGlRed(), shaders.outlineColor.getValue().getGlGreen(), shaders.outlineColor.getValue().getGlBlue(), shaders.outlineColor.getValue().getGlAlpha());
            effect.setUniformValue("primaryColor", shaders.fillColor1.getValue().getGlRed(), shaders.fillColor1.getValue().getGlGreen(), shaders.fillColor1.getValue().getGlBlue(), shaders.fillColor1.getValue().getAlpha());
            effect.setUniformValue("secondaryColor", shaders.fillColor2.getValue().getGlRed(), shaders.fillColor2.getValue().getGlGreen(), shaders.fillColor2.getValue().getGlBlue(), shaders.fillColor1.getValue().getAlpha());
            effect.setUniformValue("time", (System.currentTimeMillis() % 100000) / 1000f);
        }
    }

    public void reloadShaders() {
        DEFAULT = ShaderEffectManager.getInstance().manage(Identifier.of("thunderhack", "outline"));
        SMOKE = ShaderEffectManager.getInstance().manage(Identifier.of("thunderhack", "smoke"));
        GRADIENT = ShaderEffectManager.getInstance().manage(Identifier.of("thunderhack", "gradient"));
        SNOW = ShaderEffectManager.getInstance().manage(Identifier.of("thunderhack", "snow"));
        FADE = ShaderEffectManager.getInstance().manage(Identifier.of("thunderhack", "fade"));

        FADE_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("thunderhack", "fade"));
        DEFAULT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("thunderhack", "outline"));
        SMOKE_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("thunderhack", "smoke"));
        GRADIENT_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("thunderhack", "gradient"));
        SNOW_OUTLINE = ShaderEffectManager.getInstance().manage(Identifier.of("thunderhack", "snow"));
    }

    public static class ThunderHackFramebuffer extends Framebuffer {
        public ThunderHackFramebuffer(int width, int height) {
            super(false);
            RenderSystem.assertOnRenderThreadOrInit();
            resize(width, height);
            setClearColor(0f, 0f, 0f, 0f);
        }
    }

    public boolean fullNullCheck() {
        if (GRADIENT == null || SMOKE == null || DEFAULT == null) {
            shaderBuffer = new ThunderHackFramebuffer(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
            reloadShaders();
            return true;
        }

        return false;
    }

    public record RenderTask(Runnable task, Shader shader) {
    }

    public enum Shader {
        Default,
        Smoke,
        Gradient,
        Snow,
        Fade
    }
}
