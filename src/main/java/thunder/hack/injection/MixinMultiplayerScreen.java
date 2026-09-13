package thunder.hack.injection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.windows.WindowsScreen;
import thunder.hack.gui.windows.impl.*;

import static thunder.hack.core.manager.IManager.mc;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MixinMultiplayerScreen extends Screen {
    public MixinMultiplayerScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void initHook(CallbackInfo ci) {
        Button.Builder builder = Button.builder(Component.literal("⚡"), button -> mc.gui.setScreen(
                new WindowsScreen(
                        MacroWindow.get(ModuleManager.windows.macroPos.getValue().getX() * mc.getWindow().getGuiScaledWidth(), ModuleManager.windows.macroPos.getValue().getY() * mc.getWindow().getGuiScaledHeight(), ModuleManager.windows.macroPos),
                        ConfigWindow.get(ModuleManager.windows.configPos.getValue().getX() * mc.getWindow().getGuiScaledWidth(), ModuleManager.windows.configPos.getValue().getY() * mc.getWindow().getGuiScaledHeight(), ModuleManager.windows.configPos),
                        FriendsWindow.get(ModuleManager.windows.friendPos.getValue().getX() * mc.getWindow().getGuiScaledWidth(), ModuleManager.windows.friendPos.getValue().getY() * mc.getWindow().getGuiScaledHeight(), ModuleManager.windows.friendPos),
                        WaypointWindow.get(ModuleManager.windows.waypointPos.getValue().getX() * mc.getWindow().getGuiScaledWidth(), ModuleManager.windows.waypointPos.getValue().getY() * mc.getWindow().getGuiScaledHeight(), ModuleManager.windows.waypointPos),
                        ProxyWindow.get(ModuleManager.windows.proxyPos.getValue().getX() * mc.getWindow().getGuiScaledWidth(), ModuleManager.windows.proxyPos.getValue().getY() * mc.getWindow().getGuiScaledHeight(), ModuleManager.windows.proxyPos)
                ))).size(60, 20);
        if (!ModuleManager.unHook.isEnabled())
            addRenderableWidget(builder.pos(width - 65, height - 25).build());
    }
}