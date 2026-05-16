package thunder.hack.injection;

import thunder.hack.utility.render.compat.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;
import java.util.Optional;
import java.util.function.Consumer;

import static thunder.hack.features.modules.Module.mc;

@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay {
    @Final @Shadow private boolean reloading;
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime = -1L;
    @Shadow private long reloadStartTime = -1L;
    @Final @Shadow private ResourceReload reload;
    @Final @Shadow private Consumer<Optional<Throwable>> exceptionHandler;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ModuleManager.unHook.isEnabled())
            return;
        ci.cancel();
        renderCustom(context, mouseX, mouseY, delta);
    }

    public void renderCustom(DrawContext context, int mouseX, int mouseY, float delta) {
        int i = mc.getWindow().getScaledWidth();
        int j = mc.getWindow().getScaledHeight();
        long l = Util.getMeasuringTimeMs();
        if (reloading && reloadStartTime == -1L) {
            reloadStartTime = l;
        }

        float f = reloadCompleteTime > -1L ? (float) (l - reloadCompleteTime) / 1000.0F : -1.0F;
        float g = reloadStartTime > -1L ? (float) (l - reloadStartTime) / 500.0F : -1.0F;
        float h;
        int k;
        if (f >= 1.0F) {
            if (mc.currentScreen != null)
                mc.currentScreen.render(context, 0, 0, delta);

            k = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
            context.fill(0, 0, i, j, withAlpha(new Color(0x070015).getRGB(), k));
            h = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
        } else if (reloading) {
            if (mc.currentScreen != null && g < 1.0F)
                mc.currentScreen.render(context, mouseX, mouseY, delta);

            k = MathHelper.ceil(MathHelper.clamp((double) g, 0.15, 1.0) * 255.0);
            context.fill(0, 0, i, j, withAlpha(new Color(0x070015).getRGB(), k));
            h = MathHelper.clamp(g, 0.0F, 1.0F);
        } else {
            k = new Color(0x070015).getRGB();
            float m = (float) (k >> 16 & 255) / 255.0F;
            float n = (float) (k >> 8 & 255) / 255.0F;
            float o = (float) (k & 255) / 255.0F;
            RenderSystem.clearColor(m, n, o, 1.0F);
            RenderSystem.clear(16384);
            h = 1.0F;
        }

        k = (int) ((double) context.getScaledWindowWidth() * 0.5);
        int p = (int) ((double) context.getScaledWindowHeight() * 0.5);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);

        int logoAlpha = MathHelper.ceil(MathHelper.clamp(h, 0.0F, 1.0F) * 255.0F);
        context.drawTexture(RenderLayer::getGuiTextured, TextureStorage.thLogo, k - 150, p - 35, 0, 0, 300, 70, 300, 70,
                withAlpha(new Color(0x1A1A1A).getRGB(), logoAlpha));
        Render2DEngine.addWindow(context.getMatrices(),k - 150, p - 35, k - 150 + (300 * progress), p + 35, 1f);
        context.drawTexture(RenderLayer::getGuiTextured, TextureStorage.thLogo, k - 150, p - 35, 0, 0, 300, 70, 300, 70,
                withAlpha(Color.WHITE.getRGB(), logoAlpha));
        Render2DEngine.popWindow();

        float t = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + t * 0.050000012F, 0.0F, 1.0F);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        if (f >= 2.0F) {
            mc.setOverlay(null);
        }

        if (reloadCompleteTime == -1L && reload.isComplete() && (!reloading || g >= 2.0F)) {
            try {
                reload.throwException();
                exceptionHandler.accept(Optional.empty());
            } catch (Throwable var23) {
                exceptionHandler.accept(Optional.of(var23));
            }

            reloadCompleteTime = Util.getMeasuringTimeMs();
            if (mc.currentScreen != null) {
                mc.currentScreen.init(mc, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
            }
        }
    }

    private static int withAlpha(int color, int alpha) {
        return color & 16777215 | alpha << 24;
    }
}
