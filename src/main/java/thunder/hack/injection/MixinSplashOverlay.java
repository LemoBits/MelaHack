package thunder.hack.injection;

import thunder.hack.utility.render.compat.RenderSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import static thunder.hack.features.modules.Module.mc;

@Mixin(LoadingOverlay.class)
public abstract class MixinSplashOverlay {
    @Final @Shadow private boolean fadeIn;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart = -1L;
    @Shadow private long fadeInStart = -1L;
    @Final @Shadow private ReloadInstance reload;
    @Final @Shadow private Consumer<Optional<Throwable>> onFinish;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ModuleManager.unHook.isEnabled() || !ClientSettings.customLoadingScreen.getValue())
            return;
        ci.cancel();
        renderCustom(context, mouseX, mouseY, delta);
    }

    public void renderCustom(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int i = mc.getWindow().getGuiScaledWidth();
        int j = mc.getWindow().getGuiScaledHeight();
        long l = Util.getMillis();
        if (fadeIn && fadeInStart == -1L) {
            fadeInStart = l;
        }

        float f = fadeOutStart > -1L ? (float) (l - fadeOutStart) / 1000.0F : -1.0F;
        float g = fadeInStart > -1L ? (float) (l - fadeInStart) / 500.0F : -1.0F;
        float h;
        int k;
        if (f >= 1.0F) {
            if (mc.gui.screen() != null)
                mc.gui.screen().extractRenderState(context, 0, 0, delta);

            k = Mth.ceil((1.0F - Mth.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
            context.fill(0, 0, i, j, withAlpha(new Color(0x070015).getRGB(), k));
            h = 1.0F - Mth.clamp(f - 1.0F, 0.0F, 1.0F);
        } else if (fadeIn) {
            if (mc.gui.screen() != null && g < 1.0F)
                mc.gui.screen().extractRenderState(context, mouseX, mouseY, delta);

            k = Mth.ceil(Mth.clamp((double) g, 0.15, 1.0) * 255.0);
            context.fill(0, 0, i, j, withAlpha(new Color(0x070015).getRGB(), k));
            h = Mth.clamp(g, 0.0F, 1.0F);
        } else {
            k = new Color(0x070015).getRGB();
            float m = (float) (k >> 16 & 255) / 255.0F;
            float n = (float) (k >> 8 & 255) / 255.0F;
            float o = (float) (k & 255) / 255.0F;
            RenderSystem.clearColor(m, n, o, 1.0F);
            RenderSystem.clear(16384);
            h = 1.0F;
        }

        k = (int) ((double) context.guiWidth() * 0.5);
        int p = (int) ((double) context.guiHeight() * 0.5);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);

        int logoAlpha = Mth.ceil(Mth.clamp(h, 0.0F, 1.0F) * 255.0F);
        context.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TextureStorage.thLogo, k - 150, p - 35, 0, 0, 300, 70, 300, 70,
                withAlpha(new Color(0x1A1A1A).getRGB(), logoAlpha));
        Render2DEngine.addWindow(context.pose(),k - 150, p - 35, k - 150 + (300 * currentProgress), p + 35, 1f);
        context.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TextureStorage.thLogo, k - 150, p - 35, 0, 0, 300, 70, 300, 70,
                withAlpha(Color.WHITE.getRGB(), logoAlpha));
        Render2DEngine.popWindow();

        float t = this.reload.getActualProgress();
        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + t * 0.050000012F, 0.0F, 1.0F);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        if (f >= 2.0F) {
            mc.gui.setOverlay(null);
        }

        if (fadeOutStart == -1L && reload.isDone() && (!fadeIn || g >= 2.0F)) {
            try {
                reload.checkExceptions();
                onFinish.accept(Optional.empty());
            } catch (Throwable var23) {
                onFinish.accept(Optional.of(var23));
            }

            fadeOutStart = Util.getMillis();
            if (mc.gui.screen() != null) {
                mc.gui.screen().init(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
        }
    }

    private static int withAlpha(int color, int alpha) {
        return color & 16777215 | alpha << 24;
    }
}
