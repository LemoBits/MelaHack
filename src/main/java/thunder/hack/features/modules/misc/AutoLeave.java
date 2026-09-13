package thunder.hack.features.modules.misc;

import org.lwjgl.glfw.GLFW;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.Bind;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InventoryUtility;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public class AutoLeave extends Module {
    public AutoLeave() {
        super("AutoLeave", Category.MISC);
    }

    private final Setting<Boolean> antiHelperLeave = new Setting<>("AntiHelperLeave", true);
    private final Setting<Boolean> antiKTLeave = new Setting<>("AntiKTLeave", true);
    private final Setting<Boolean> autoDisable = new Setting<>("AutoDisable", true);
    private final Setting<Boolean> fastLeave = new Setting<>("InstantLeave", true);
    public static Setting<String> command = new Setting<>("Command", "hub");
    private final Setting<SettingGroup> leaveIf = new Setting<>("Leave if", new SettingGroup(false, 0));
    private final Setting<Boolean> low_hp = new Setting<>("LowHp", false).addToGroup(leaveIf);
    private final Setting<Boolean> totems = new Setting<>("Totems", false).addToGroup(leaveIf);
    private final Setting<Integer> totemsCount = new Setting<>("TotemsCount", 2, 0, 10, v -> totems.getValue());
    private final Setting<Float> leaveHp = new Setting<>("HP", 8.0f, 1f, 20.0f, v -> low_hp.getValue());
    private final Setting<LeaveMode> staff = new Setting<>("Staff", LeaveMode.None).addToGroup(leaveIf);
    private final Setting<LeaveMode> players = new Setting<>("Players", LeaveMode.Leave).addToGroup(leaveIf);
    private final Setting<Integer> distance = new Setting<>("Distance", 256, 4, 256, v -> players.getValue() != LeaveMode.None).addToGroup(leaveIf);
    public final Setting<Boolean> leaveOnBind = new Setting<>("LeaveOnBind", false, v -> true).addToGroup(leaveIf);
    public final Setting<Bind> leaveBind = new Setting<>("LeaveBind", new Bind(GLFW.GLFW_KEY_N, false, false), v -> leaveOnBind.is(true)).addToGroup(leaveIf);
    public final Setting<String> leaveChat = new Setting<>("MessageOnLeave", "", v -> true);

    private final Timer chatDelay = new Timer();

    // Будет хуева если мы ливнем в кт
    private final Timer hurtTimer = new Timer();

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.level == null)
            return;

        if (mc.player.hurtTime > 0)
            hurtTimer.reset();

        if (antiKTLeave.getValue() && !hurtTimer.passedMs(30000))
            return;

        for (Player pl : mc.level.players()) {
            if (pl.getTeam() != null && antiHelperLeave.getValue()) {
                String prefix = pl.getTeam().getPlayerPrefix().getString();
                if (isStaff(ChatFormatting.stripFormatting(prefix)))
                    continue;
            }


            if (pl != mc.player && !Managers.FRIEND.isFriend(pl) && players.getValue() != LeaveMode.None && mc.player.distanceToSqr(pl.position()) <= distance.getPow2Value()) {
                switch (players.getValue()) {
                    case Command -> {
                        if (autoDisable.getValue()) disable();
                        sendMessage(isRu() ? "Ливнул т.к. рядом появился игрок!" : "Logged out because there was a player!");
                        mc.player.connection.sendCommand(command.getValue());
                        return;
                    }
                    case Leave -> {
                        leave(isRu() ? "Ливнул т.к. рядом появился игрок" : "Logged out because there was a player");
                        return;
                    }
                }
            }
        }

        if (totems.getValue() && InventoryUtility.getItemCount(Items.TOTEM_OF_UNDYING) <= totemsCount.getValue())
            leave(isRu() ? "Ливнул т.к. кончились тотемы" : "Logged out because out of totems");

        if (mc.player.getHealth() < leaveHp.getValue() && low_hp.getValue())
            leave(isRu() ? "Ливнул т.к. мало хп" : "Logged out because ur hp is low");

        if (staff.getValue() != LeaveMode.None && ModuleManager.staffBoard.isDisabled() && mc.player.tickCount % 5 == 0)
            sendMessage(isRu() ? "Включи StaffBoard!" : "Turn on StaffBoard!");

        if (isKeyPressed(leaveBind) && leaveOnBind.getValue())
            leave(isRu() ? "Ливнул т.к. прожат бинд" : "Logged out because bind is pressed");
    }

    private void leave(String message) {

        if (!chatDelay.passedMs(1000))
            return;
        chatDelay.reset();

        if (autoDisable.getValue())
            disable(message);

        if (leaveChat.getValue() != null || leaveChat.getValue() != "") {
            if (leaveChat.getValue().contains("/")) {
                mc.getConnection().sendCommand(leaveChat.getValue());
            } else {
                mc.getConnection().sendChat(leaveChat.getValue());
            }
        }

        if (fastLeave.getValue()) sendPacket(new ServerboundSetCarriedItemPacket(228));
        else mc.player.connection.getConnection().disconnect(Component.nullToEmpty("[AutoLeave] " + message));
    }

    /*public void onStaff() { todo
        if (!chatDelay.passedMs(1000))
            return;
        chatDelay.reset();

        if (hurtTimer.passedMs(30000) || !antiKTLeave.getValue()) {
            switch (staff.getValue()) {
                case Command -> {
                    sendMessage(isRu() ? "Ливнул т.к. хелпер в спеке!" : "Logged out because helper in vanish!");
                    mc.player.networkHandler.sendChatCommand(command.getValue());
                    if (autoDisable.getValue())
                        disable();
                }
                case Leave -> leave(isRu() ? "Ливнул т.к. хелпер в спеке!" : "Logged out because helper in vanish!");
            }
        }
    }*/

    public static boolean isStaff(String name) {
        if (name == null) return false;

        name = name.toLowerCase();

        return name.contains("helper") || name.contains("moder") || name.contains("admin") || name.contains("owner") || name.contains("curator") || name.contains("куратор") || name.contains("модер") || name.contains("админ") || name.contains("хелпер") || name.contains("поддержка");
    }

    private enum LeaveMode {
        None, Command, Leave
    }
}
