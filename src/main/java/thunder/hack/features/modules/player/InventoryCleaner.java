package thunder.hack.features.modules.player;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ItemSelectSetting;
import thunder.hack.utility.Timer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class InventoryCleaner extends Module {
    public InventoryCleaner() {
        super("InventoryCleaner", Category.PLAYER);
    }

    public final Setting<ItemSelectSetting> items = new Setting<>("Items", new ItemSelectSetting(new ArrayList<>()));
    private final Setting<DropWhen> dropWhen = new Setting<>("DropWhen", DropWhen.NotInInventory);
    private final Setting<Integer> delay = new Setting<>("Delay", 50, 0, 500);
    private final Setting<Boolean> cleanChests = new Setting<>("CleanChests", false);

    private final Timer delayTimer = new Timer();
    private boolean dirty;

    public void onRender3D(PoseStack stack) {
        boolean inInv = mc.screen instanceof InventoryScreen;

        if (mc.player.containerMenu instanceof ChestMenu chest && cleanChests.getValue())
            for (int i = 0; i < chest.getContainer().getContainerSize(); i++) {
                Slot slot = chest.getSlot(i);
                if (slot.hasItem() && dropThisShit(slot.getItem()) && !(mc.screen.getTitle().getString().contains("Аукцион") || mc.screen.getTitle().getString().contains("покупки")))
                    if (delayTimer.every(delay.getValue())) {
                        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, i, 1, ClickType.THROW, mc.player);
                        dirty = true;
                    }
            }

        if (dropWhen.getValue() == DropWhen.Inventory && !inInv) return;
        if (dropWhen.getValue() == DropWhen.NotInInventory && inInv) return;

        for (int slot = 0; slot < 36; slot++) {
            ItemStack itemFromslot = mc.player.getInventory().getItem(slot);
            if (dropThisShit(itemFromslot))
                drop(slot);
        }

        if (dirty && delayTimer.passedMs(delay.getValue() + 100)) {
            sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
            debug("after click cleaning...");
            dirty = false;
        }
    }

    private void drop(int slot) {
        if (delayTimer.every(delay.getValue())) {
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot < 9 ? slot + 36 : slot, 1, ClickType.THROW, mc.player);
            dirty = true;
        }
    }

    private boolean dropThisShit(ItemStack stack) {
        return items.getValue().getItemsById().contains(stack.getItem().getDescriptionId().replace("block.minecraft.", "").replace("item.minecraft.", ""));
    }

    public enum DropWhen {
        Inventory, Always, NotInInventory
    }
}
