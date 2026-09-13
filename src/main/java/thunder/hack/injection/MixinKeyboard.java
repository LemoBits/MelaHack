package thunder.hack.injection;

import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventKeyPress;
import thunder.hack.events.impl.EventKeyRelease;
import thunder.hack.gui.clickui.ClickGUI;
import thunder.hack.gui.hud.HudEditorGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.features.modules.Module;

import static thunder.hack.features.modules.Module.mc;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;

@Mixin(KeyboardHandler.class)
public class MixinKeyboard {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKey(long windowPointer, int action, KeyEvent keyEvent, CallbackInfo ci) {
        int key = keyEvent.key();
        int scanCode = keyEvent.scancode();
        if(Module.fullNullCheck()) return;
        boolean whitelist = mc.gui.screen() == null || mc.gui.screen() instanceof ClickGUI || mc.gui.screen() instanceof HudEditorGui;
        if (!whitelist) return;

        if (action == 0) Managers.MODULE.onKeyReleased(key);
        if (action == 1) Managers.MODULE.onKeyPressed(key);
        if (action == 2) action = 1;

        switch (action) {
            case 0 -> {
                EventKeyRelease event = new EventKeyRelease(key, scanCode);
                ThunderHack.EVENT_BUS.post(event);
                if (event.isCancelled()) ci.cancel();
            }
            case 1 -> {
                EventKeyPress event = new EventKeyPress(key, scanCode);
                ThunderHack.EVENT_BUS.post(event);
                if (event.isCancelled()) ci.cancel();
            }
        }
    }
}