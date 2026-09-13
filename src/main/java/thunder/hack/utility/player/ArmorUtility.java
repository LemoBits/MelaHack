package thunder.hack.utility.player;

import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class ArmorUtility {
    private ArmorUtility() {
    }

    public static List<ItemStack> getArmorItems(LivingEntity entity) {
        return List.of(
                entity.getItemBySlot(EquipmentSlot.HEAD),
                entity.getItemBySlot(EquipmentSlot.CHEST),
                entity.getItemBySlot(EquipmentSlot.LEGS),
                entity.getItemBySlot(EquipmentSlot.FEET)
        );
    }
}
