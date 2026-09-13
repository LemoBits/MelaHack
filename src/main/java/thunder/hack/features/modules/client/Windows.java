package thunder.hack.features.modules.client;

import thunder.hack.gui.windows.WindowsScreen;
import thunder.hack.gui.windows.impl.*;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.PositionSetting;

public class Windows extends Module {
    public Windows() {
        super("Windows", Category.CLIENT);
    }

    // todo size
    public final Setting<PositionSetting> macroPos = new Setting<>("macroPos", new PositionSetting(0.3f, 0.3f), v -> false);
    public final Setting<PositionSetting> configPos = new Setting<>("configPos", new PositionSetting(0.35f, 0.35f), v -> false);
    public final Setting<PositionSetting> friendPos = new Setting<>("friendPos", new PositionSetting(0.4f, 0.4f), v -> false);
    public final Setting<PositionSetting> waypointPos = new Setting<>("waypointPos", new PositionSetting(0.45f, 0.45f), v -> false);
    public final Setting<PositionSetting> proxyPos = new Setting<>("proxyPos", new PositionSetting(0.5f, 0.5f), v -> false);

    @Override
    public void onEnable() {
        mc.setScreen(new WindowsScreen(
                MacroWindow.get(macroPos.getValue().getX() * mc.getWindow().getGuiScaledWidth(), macroPos.getValue().getY() * mc.getWindow().getGuiScaledHeight(), macroPos),
                ConfigWindow.get(configPos.getValue().getX() * mc.getWindow().getGuiScaledWidth(), configPos.getValue().getY() * mc.getWindow().getGuiScaledHeight(), configPos),
                FriendsWindow.get(friendPos.getValue().getX() * mc.getWindow().getGuiScaledWidth(), friendPos.getValue().getY() * mc.getWindow().getGuiScaledHeight(), friendPos),
                WaypointWindow.get(waypointPos.getValue().getX() * mc.getWindow().getGuiScaledWidth(), waypointPos.getValue().getY() * mc.getWindow().getGuiScaledHeight(), waypointPos),
                ProxyWindow.get(proxyPos.getValue().getX() * mc.getWindow().getGuiScaledWidth(), proxyPos.getValue().getY() * mc.getWindow().getGuiScaledHeight(), proxyPos)
        ));
        disable();
    }
}
