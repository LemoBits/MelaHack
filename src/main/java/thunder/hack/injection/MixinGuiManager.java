package thunder.hack.injection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventScreen;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.clickui.ClickGUI;

/** Screen lifecycle hooks moved from Minecraft to the 26.2 GUI manager. */
@Mixin(Gui.class)
public abstract class MixinGuiManager {
    @Shadow @Final private Minecraft minecraft;

    private static final String[] BLOCKED_SERVERS = {
            "mineblaze", "musteryworld", "dexland", "masedworld", "vimeworld", "hypemc", "vimemc"
    };

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void beforeSetScreen(Screen screen, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        EventScreen event = new EventScreen(screen);
        ThunderHack.EVENT_BUS.post(event);
        if (event.isCancelled() || (ClickGUI.close && screen == null)) ci.cancel();
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void afterSetScreen(Screen screen, CallbackInfo ci) {
        if (Module.fullNullCheck() || !(screen instanceof JoinMultiplayerScreen multiplayer)
                || !ModuleManager.antiServerAdd.isEnabled() || multiplayer.getServers() == null) return;
        for (int i = multiplayer.getServers().size() - 1; i >= 0; i--) {
            ServerData info = multiplayer.getServers().get(i);
            if (info == null || info.ip == null) continue;
            for (String blocked : BLOCKED_SERVERS) {
                if (info.ip.toLowerCase().contains(blocked)) {
                    multiplayer.getServers().remove(info);
                    multiplayer.getServers().save();
                    break;
                }
            }
        }
    }
}
