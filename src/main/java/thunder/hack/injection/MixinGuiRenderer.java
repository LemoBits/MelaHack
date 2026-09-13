package thunder.hack.injection;

import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.injection.accesors.IGuiRenderer;
import thunder.hack.utility.render.BufferRenderer;

import static thunder.hack.features.modules.Module.mc;

/**
 * MelaHack records its immediate mode screen geometry while vanilla extracts the frame, which is
 * before the main render target is cleared. The recorded draws are replayed here so that they end
 * up between the vanilla GUI layers:
 *
 * <ul>
 *     <li>above the (possibly blurred) screen backdrop,</li>
 *     <li>below the vanilla widgets, which vanilla draws after the blur pass.</li>
 * </ul>
 */
@Mixin(GuiRenderer.class)
public class MixinGuiRenderer {
    @Inject(method = "draw", at = @At("HEAD"))
    private void thunderhack$flushBeforeGuiElements(CallbackInfo ci) {
        if (mc.gui == null || mc.gui.screen() == null) {
            // HUD only content is replayed at the end of the frame so that it sits on top of the
            // vanilla HUD.
            return;
        }

        if (thunderhack$willBlur()) {
            // Wait for the blur pass, otherwise the backdrop would blur MelaHack's own panels.
            return;
        }

        BufferRenderer.flushQueue();
    }

    @Inject(method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V", shift = At.Shift.AFTER))
    private void thunderhack$flushAfterBlur(CallbackInfo ci) {
        BufferRenderer.flushQueue();
    }

    private boolean thunderhack$willBlur() {
        IGuiRenderer accessor = (IGuiRenderer) this;
        return accessor.thunderhack$getFirstDrawIndexAfterBlur() < accessor.thunderhack$getDraws().size();
    }
}
