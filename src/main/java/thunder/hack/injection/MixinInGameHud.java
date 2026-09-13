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
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;

@Mixin(Gui.class)
public abstract class MixinInGameHud {

    @Inject(at = @At(value = "HEAD"), method = "render")
    public void renderHook(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if(Module.fullNullCheck()) return;
        Managers.MODULE.onRender2D(context);
        Managers.NOTIFICATION.onRender2D(context);
        if (ModuleManager.totemAnimation.isEnabled()) {
            ModuleManager.totemAnimation.renderFloatingItem(context, tickCounter.getGameTimeDeltaPartialTick(true));
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "renderPlayerHealth", cancellable = true)
    private void renderStatusBarsHook(GuiGraphics context, CallbackInfo ci) {
        if (mc != null && mc.screen instanceof WindowsScreen) {
            ci.cancel();
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "renderItemHotbar", cancellable = true)
    public void renderHotbarCustom(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (mc != null && mc.screen instanceof WindowsScreen)
            ci.cancel();

        if (ModuleManager.hotbar.isEnabled()) {
            ci.cancel();
            Hotbar.renderHotBarItems(tickCounter.getGameTimeDeltaPartialTick(true), context);
        }
    }


    @Inject(at = @At(value = "HEAD"), method = "renderSelectedItemName", cancellable = true)
    public void renderHeldItemTooltipHook(GuiGraphics context, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.hotbarItemName.getValue())
            ci.cancel();
    }

    @Inject(at = @At(value = "HEAD"), method = "renderEffects", cancellable = true)
    public void renderStatusEffectOverlayHook(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (ModuleManager.potionHud.isEnabled() || (ModuleManager.legacyHud.isEnabled() && ModuleManager.legacyHud.potions.getValue())) {
            ci.cancel();
        }
    }

    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V", at = @At(value = "HEAD"), cancellable = true)
    private void renderScoreboardSidebarHook(GuiGraphics context, Objective objective, CallbackInfo ci) {
        if(ModuleManager.noRender.noScoreBoard.getValue() && ModuleManager.noRender.isEnabled()){
            ci.cancel();
        }
    }

    @Inject(method = "renderVignette", at = @At(value = "HEAD"), cancellable = true)
    private void renderVignetteOverlayHook(GuiGraphics context, Entity entity, CallbackInfo ci) {
        if(ModuleManager.noRender.vignette.getValue())
            ci.cancel();
    }

    @Inject(method = "renderPortalOverlay", at = @At(value = "HEAD"), cancellable = true)
    private void renderPortalOverlayHook(GuiGraphics context, float nauseaStrength, CallbackInfo ci) {
        if(ModuleManager.noRender.portal.getValue())
            ci.cancel();
    }

    @Inject(method = "renderCrosshair", at = @At(value = "HEAD"), cancellable = true)
    public void renderCrosshair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (ModuleManager.crosshair.isEnabled())
            ci.cancel();
    }
}
