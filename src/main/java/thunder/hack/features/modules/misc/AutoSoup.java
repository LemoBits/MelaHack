package thunder.hack.features.modules.misc;

import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

public class AutoSoup extends Module {
    public AutoSoup() {
        super("AutoSoup", Category.MISC);
    }

    private final Setting<Float> health = new Setting<>("TriggerHealth", 7f, 1f, 20f);

    @Override
    public void onUpdate() {
        if (mc.player.getHealth() <= health.getValue()) {
            SearchInvResult result = InventoryUtility.findItemInHotBar(Items.MUSHROOM_STEW);
            int prevSlot = mc.player.getInventory().getSelectedSlot();
            if (result.found()) {
                result.switchTo();
                sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
                InventoryUtility.switchTo(prevSlot);
            }
        }
    }
}
