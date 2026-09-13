package thunder.hack.injection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventMouse;

import static thunder.hack.features.modules.Module.mc;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;

@Mixin(MouseHandler.class)
public class MixinMouse {
    @Inject(method = "onButton", at = @At("HEAD"))
    public void onMouseButtonHook(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        int button = info.button();
        if (window == mc.getWindow().handle()) {
            if (action == 0) Managers.MODULE.onMoseKeyReleased(button);
            if (action == 1) Managers.MODULE.onMoseKeyPressed(button);

            ThunderHack.EVENT_BUS.post(new EventMouse(button, action));
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void onMouseScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window == mc.getWindow().handle()) {
            ThunderHack.EVENT_BUS.post(new EventMouse((int) vertical, 2));
        }
    }
}