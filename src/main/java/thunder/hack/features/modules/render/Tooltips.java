package thunder.hack.features.modules.render;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class Tooltips extends Module {
    public Tooltips() {
        super("Tooltips", Category.MISC);
    }

    public static final Setting<Boolean> middleClickOpen = new Setting<>("MiddleClickOpen", true);
    public static final Setting<Boolean> storage = new Setting<>("Storage", true);
    public static final Setting<Boolean> maps = new Setting<>("Maps", true);
    public final Setting<Boolean> shulkerRegear = new Setting<>("ShulkerRegear", true);
    public final Setting<Boolean> shulkerRegearShiftMode = new Setting<>("RegearShift", true);

    public static boolean hasItems(ItemStack itemStack) {
        ItemContainerContents compoundTag = itemStack.get(DataComponents.CONTAINER);
        return compoundTag != null && !compoundTag.allItemsCopyStream().toList().isEmpty();
    }
}
