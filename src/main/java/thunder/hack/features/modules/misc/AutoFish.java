package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.player.InventoryUtility;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class AutoFish extends Module {
    public AutoFish() {
        super("AutoFish", Category.MISC);
    }

    private final Setting<DetectMode> detectMode = new Setting<>("DetectMode", DetectMode.DataTracker);
    private final Setting<Boolean> rodSave = new Setting<>("RodSave", true);
    private final Setting<Boolean> changeRod = new Setting<>("ChangeRod", false);
    private final Setting<Boolean> autoSell = new Setting<>("AutoSell", false);

    private boolean flag = false;
    private final Timer timeout = new Timer();
    private final Timer cooldown = new Timer();

    private enum DetectMode {
        Sound, DataTracker
    }

    @Override
    public void onEnable() {
        if (fullNullCheck()) disable("NPE protection");
    }

    @Override
    public void onDisable() {
        flag = false;
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof ClientboundSoundPacket sound && detectMode.getValue() == DetectMode.Sound)
            if (sound.getSound().value().equals(SoundEvents.FISHING_BOBBER_SPLASH) && mc.player.fishing != null && mc.player.fishing.distanceToSqr(sound.getX(), sound.getY(), sound.getZ()) < 4f)
                catchFish();
    }

    @Override
    public void onUpdate() {
        if (mc.player.getMainHandItem().getItem() instanceof FishingRodItem) {
            if (mc.player.getMainHandItem().getDamageValue() > 52) {
                if (rodSave.getValue() && !changeRod.getValue()) {
                    disable(isRu() ? "Удочка почти сломалась!" : "Saving the rod...");
                } else if (changeRod.getValue() && getRodSlot() != -1) {
                    sendMessage(isRu() ? "Свапнулся на новую удочку" : "Swapped to a new rod");
                    InventoryUtility.switchTo(getRodSlot());
                    cooldown.reset();
                } else disable(isRu() ? "Удочка почти сломалась!" : "Saving the rod...");
            }
        }

        if (!cooldown.passedMs(1000)) return;

        if (timeout.passedMs(45000) && mc.player.getMainHandItem().getItem() instanceof FishingRodItem) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            timeout.reset();
            cooldown.reset();
        }

        if (mc.player.fishing != null && detectMode.getValue() == DetectMode.DataTracker) {
            boolean caughtFish = mc.player.fishing.getEntityData().get(FishingHook.DATA_BITING);
            if (!flag && caughtFish) {
                catchFish();
                flag = true;
            } else if (!caughtFish) flag = false;
        }
    }

    private void catchFish() {
        Managers.ASYNC.run(() -> {

            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

            if (autoSell.getValue() && timeout.passedMs(1000)) mc.player.connection.sendCommand("sellfish");

            try {
                Thread.sleep((int) MathUtility.random(899, 1399));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            timeout.reset();
        }, (int) MathUtility.random(199, 349));
    }

    private int getRodSlot() {
        for (int i = 0; i < 9; i++) {
            final ItemStack item = mc.player.getInventory().getItem(i);
            if (item.getItem() == Items.FISHING_ROD && item.getDamageValue() < 52) return i;
        }
        return -1;
    }
}
