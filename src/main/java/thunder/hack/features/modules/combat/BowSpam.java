package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.PlayerUtility;

public class BowSpam extends Module {
    private final Setting<Integer> ticks = new Setting<>("Delay", 3, 0, 20);

    public BowSpam() {
        super("BowSpam", Category.COMBAT);
    }

    @EventHandler
    public void onSync(EventSync event) {
        if ((mc.player.getOffhandItem().getItem() == Items.BOW || mc.player.getMainHandItem().getItem() == Items.BOW) && mc.player.isUsingItem()) {
            if (mc.player.getTicksUsingItem() >= this.ticks.getValue()) {
                sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, mc.player.getDirection()));
                sendSequencedPacket(id -> new ServerboundUseItemPacket(mc.player.getOffhandItem().getItem() == Items.BOW ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                mc.player.releaseUsingItem();
            }
        }
    }
}
