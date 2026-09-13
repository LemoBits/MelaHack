package thunder.hack.injection;

import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.hud.impl.Hotbar;
import thunder.hack.gui.windows.WindowsScreen;
import thunder.hack.features.modules.Module;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static thunder.hack.core.manager.IManager.mc;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;

@Mixin(Hud.class)
public abstract class MixinInGameHud {

    @Inject(at = @At(value = "HEAD"), method = "extractRenderState")
    public void renderHook(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if(Module.fullNullCheck()) return;
        Managers.MODULE.onRender2D(context);
        Managers.NOTIFICATION.onRender2D(context);
        if (ModuleManager.totemAnimation.isEnabled()) {
            ModuleManager.totemAnimation.renderFloatingItem(context, tickCounter.getGameTimeDeltaPartialTick(true));
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "extractPlayerHealth", cancellable = true)
    private void renderStatusBarsHook(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (mc != null && mc.gui.screen() instanceof WindowsScreen) {
            ci.cancel();
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "extractItemHotbar", cancellable = true)
    public void renderHotbarCustom(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (mc != null && mc.gui.screen() instanceof WindowsScreen)
            ci.cancel();

        if (ModuleManager.hotbar.isEnabled()) {
            ci.cancel();
            Hotbar.renderHotBarItems(tickCounter.getGameTimeDeltaPartialTick(true), context);
        }
    }


    @Inject(at = @At(value = "HEAD"), method = "extractSelectedItemName", cancellable = true)
    public void renderHeldItemTooltipHook(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.hotbarItemName.getValue())
            ci.cancel();
    }

    @Inject(at = @At(value = "HEAD"), method = "extractEffects", cancellable = true)
    public void renderStatusEffectOverlayHook(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (ModuleManager.potionHud.isEnabled() || (ModuleManager.legacyHud.isEnabled() && ModuleManager.legacyHud.potions.getValue())) {
            ci.cancel();
        }
    }

    @Inject(method = "extractScoreboardSidebar", at = @At(value = "HEAD"), cancellable = true)
    private void renderScoreboardSidebarHook(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if(ModuleManager.noRender.noScoreBoard.getValue() && ModuleManager.noRender.isEnabled()){
            ci.cancel();
        }
    }

    @Inject(method = "extractVignette", at = @At(value = "HEAD"), cancellable = true)
    private void renderVignetteOverlayHook(GuiGraphicsExtractor context, Entity entity, CallbackInfo ci) {
        if(ModuleManager.noRender.vignette.getValue())
            ci.cancel();
    }

    @Inject(method = "extractPortalOverlay", at = @At(value = "HEAD"), cancellable = true)
    private void renderPortalOverlayHook(GuiGraphicsExtractor context, float nauseaStrength, CallbackInfo ci) {
        if(ModuleManager.noRender.portal.getValue())
            ci.cancel();
    }

    @Inject(method = "extractCrosshair", at = @At(value = "HEAD"), cancellable = true)
    public void renderCrosshair(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (ModuleManager.crosshair.isEnabled())
            ci.cancel();
    }
}
