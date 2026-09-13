package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventEntityRemoved;
import thunder.hack.events.impl.EventEntitySpawn;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.notification.Notification;
import thunder.hack.setting.Setting;

import java.util.ArrayList;

public class VisualRange extends Module {
    private static final ArrayList<String> entities = new ArrayList<>();
    private final Setting<Boolean> leave = new Setting<>("Leave", true);
    private final Setting<Boolean> enter = new Setting<>("Enter", true);
    private final Setting<Boolean> friends = new Setting<>("Friends", true);
    private final Setting<Boolean> soundpl = new Setting<>("Sound", true);
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Notification);

    public VisualRange() {
        super("VisualRange", Category.MISC);
    }

    @EventHandler
    public void onEntityAdded(EventEntitySpawn event) {
        if (!isValid(event.getEntity())) return;

        if (!entities.contains(event.getEntity().getName().getString()))
            entities.add(event.getEntity().getName().getString());
        else return;

        if (enter.getValue()) notify(event.getEntity(), true);
    }

    @EventHandler
    public void onEntityRemoved(EventEntityRemoved event) {
        if (!isValid(event.entity)) return;

        if (entities.contains(event.entity.getName().getString())) entities.remove(event.entity.getName().getString());
        else return;

        if (leave.getValue()) notify(event.entity, false);
    }

    public void notify(Entity entity, boolean enter) {
        String message = "";
        if (ModuleManager.nameProtect.isEnabled() && NameProtect.hideFriends.getValue()) {
            message = ChatFormatting.AQUA + NameProtect.newName.getValue();
        }
        if (Managers.FRIEND.isFriend(entity.getName().getString()))
            message = ChatFormatting.AQUA + entity.getName().getString();
        else message = ChatFormatting.GRAY + entity.getName().getString();


        if (enter) message += ChatFormatting.GREEN + " was found!";
        else message += ChatFormatting.RED + " left to X:" + (int) entity.getX() + " Z:" + (int) entity.getZ();

        if (mode.is(Mode.Chat) || mode.is(Mode.Both)) sendMessage(message);

        if (mode.is(Mode.Notification) || mode.is(Mode.Both))
            Managers.NOTIFICATION.publicity("VisualRange", message, 2, Notification.Type.WARNING);

        if (soundpl.getValue()) {
            try {
                if (enter)
                    mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1f, 1f);
                else
                    mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1f, 1f);
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isValid(Entity entity) {
        if (!(entity instanceof Player)) return false;
        return entity != mc.player && (!Managers.FRIEND.isFriend(entity.getName().getString()) || friends.getValue());
    }

    public enum Mode {
        Chat, Notification, Both
    }
}
