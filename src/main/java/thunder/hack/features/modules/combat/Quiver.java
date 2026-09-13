package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TippedArrowItem;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;
import thunder.hack.utility.world.HoleUtility;

import java.util.Objects;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public final class Quiver extends Module {
    private final Setting<Boolean> onlyInHole = new Setting<>("Only In Hole", false);

    private int preBowSlot;
    private int count;

    public Quiver() {
        super("Quiver", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        count = 0;
        preBowSlot = mc.player.getInventory().getSelectedSlot();
    }

    @Override
    public void onDisable() {
        if (preBowSlot != -1)
            InventoryUtility.switchTo(preBowSlot);

        mc.options.keyUse.setDown(false);
    }

    @EventHandler
    @SuppressWarnings("unused")
    private void onSync(EventSync event) {
        if ((!HoleUtility.isHole(mc.player.blockPosition()) && onlyInHole.getValue()) || (mc.player.isUsingItem() && !mc.player.getMainHandItem().getItem().equals(Items.BOW)))
            return;

        SearchInvResult strength = getArrow("strength");
        SearchInvResult swiftness = getArrow("swiftness");

        boolean hasStrength = !strength.found() || (mc.player.hasEffect(MobEffects.STRENGTH) && mc.player.getEffect(MobEffects.STRENGTH).getDuration() > 100);
        boolean hasSwiftness = !swiftness.found() || (mc.player.hasEffect(MobEffects.SPEED) && mc.player.getEffect(MobEffects.SPEED).getDuration() > 100);

        if (!strength.found() && !swiftness.found()) {
            disable(isRu() ? "В интвенторе отсутствуют нужные стрелы! Отключение..." : "No arrows in hotbar! Disabling...");
            return;
        }

        if (hasSwiftness && hasStrength) {
            disable();
            return;
        }

        SearchInvResult result = InventoryUtility.findItemInHotBar(Items.BOW);
        if (!result.found()) {
            disable(isRu() ? "В хотбаре отсутствует лук! Отключение..." : "No bow in hotbar! Disabling...");
            return;
        }
        result.switchTo();

        if (BowItem.getPowerForTime(mc.player.getTicksUsingItem()) >= 0.15) {
            releaseBow();
            switchInvSlot(strength.slot(), swiftness.slot());
            return;
        }

        if (count >= (strength.found()  && swiftness.found() ? 2 : 1)) {
            disable();
            return;
        }

        mc.options.keyUse.setDown(true);
    }

    private void releaseBow() {
        sendPacket(new ServerboundMovePlayerPacket.Rot(mc.player.getYRot(), -90, mc.player.onGround(), mc.player.horizontalCollision));
        mc.options.keyUse.setDown(false);
        mc.gameMode.releaseUsingItem(mc.player);
        count++;
    }

    private SearchInvResult getArrow(String name) {
        return InventoryUtility.findInInventory(stack -> {
            if (stack.getItem() instanceof TippedArrowItem tai) {
                String key = tai.getDescriptionId();
                return key.contains("effect." + name);
            }
            return false;
        });
    }

    private void switchInvSlot(int from, int to) {
        if (from == -1 || to == -1)
            return;

        sendPacket(new ServerboundPlayerCommandPacket(Objects.requireNonNull(mc.player), ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        clickSlot(from);
        clickSlot(to);
        clickSlot(from);
        sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
    }
}
