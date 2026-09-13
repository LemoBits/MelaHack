package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.TotemPopEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.AntiBot;
import thunder.hack.gui.notification.Notification;
import thunder.hack.setting.Setting;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class TotemPopCounter extends Module {
    public TotemPopCounter() {
        super("TotemPopCounter", Category.MISC);
    }

    public Setting<Boolean> notification = new Setting<>("Notification", true);

    @EventHandler
    public void onTotemPop(@NotNull TotemPopEvent event) {
        if (event.getEntity() == mc.player) return;

        String s;
        if (isRu()) s = ChatFormatting.GREEN + event.getEntity().getName().getString() + ChatFormatting.WHITE + " попнул " + ChatFormatting.AQUA + (event.getPops() > 1 ? event.getPops() + "" + ChatFormatting.WHITE + " тотемов!" : ChatFormatting.WHITE + "тотем!");
        else s = ChatFormatting.GREEN + event.getEntity().getName().getString() + ChatFormatting.WHITE + " popped " + ChatFormatting.AQUA + (event.getPops() > 1 ? event.getPops() + "" + ChatFormatting.WHITE + " totems!" : ChatFormatting.WHITE + " a totem!");

        sendMessage(s);
        if (notification.getValue())
            Managers.NOTIFICATION.publicity("TotemPopCounter", s, 2, Notification.Type.INFO);
    }

    @Override
    public void onUpdate() {
        for (Player player : mc.level.players()) {
            if (player == mc.player || AntiBot.bots.contains(player) || player.getHealth() > 0 || !Managers.COMBAT.popList.containsKey(player.getName().getString()))
                continue;

            String s;
            if (isRu()) s = ChatFormatting.GREEN + player.getName().getString() + ChatFormatting.WHITE + " попнул " + (Managers.COMBAT.popList.get(player.getName().getString()) > 1 ? Managers.COMBAT.popList.get(player.getName().getString()) + "" + ChatFormatting.WHITE + " тотемов и сдох!" : ChatFormatting.WHITE + "тотем и сдох!");
            else s = ChatFormatting.GREEN + player.getName().getString() + ChatFormatting.WHITE + " popped " + (Managers.COMBAT.popList.get(player.getName().getString()) > 1 ? Managers.COMBAT.popList.get(player.getName().getString()) + "" + ChatFormatting.WHITE + " totems and died EZ LMAO!" : ChatFormatting.WHITE + "totem and died EZ LMAO!");

            sendMessage(s);
            if (notification.getValue())
                Managers.NOTIFICATION.publicity("TotemPopCounter", s, 2, Notification.Type.INFO);
        }
    }
}
