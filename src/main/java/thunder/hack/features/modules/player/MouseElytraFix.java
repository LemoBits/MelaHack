package thunder.hack.features.modules.player;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import thunder.hack.features.modules.Module;
import thunder.hack.utility.Timer;

public class MouseElytraFix extends Module {
    public MouseElytraFix() {
        super("MouseElytraFix", Category.PLAYER);
    }

    private final Timer delay = new Timer();

    @Override
    public void onUpdate() {
        if (mc.player.getEquipmentSlotForItem(mc.player.containerMenu.getCarried()).isArmor() && !ElytraSwap.swapping) {
            if (delay.every(300) && mc.player.getEquipmentSlotForItem(mc.player.containerMenu.getCarried()) == EquipmentSlot.CHEST)
                if (mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                    mc.gameMode.handleInventoryMouseClick(0, 6, 1, ClickType.PICKUP, mc.player);
                    int empty = findEmptySlot();
                    boolean needDrop = (empty == 999);
                    if (needDrop)
                        empty = 9;
                    mc.gameMode.handleInventoryMouseClick(0, empty, 1, ClickType.PICKUP, mc.player);
                    if (needDrop)
                        mc.gameMode.handleInventoryMouseClick(0, -999, 1, ClickType.PICKUP, mc.player);
                }
        }
    }

    public static int findEmptySlot() {
        for (int i = 0; i < 36; i++)
            if (mc.player.getInventory().getItem(i).isEmpty()) return i < 9 ? i + 36 : i;
        return 999;
    }
}
