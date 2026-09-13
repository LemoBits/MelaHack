package thunder.hack.gui.misc;

import org.lwjgl.glfw.GLFW;
import thunder.hack.features.modules.render.Tooltips;

import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;

public class PeekScreen extends ShulkerBoxScreen {
    private static final ItemStack[] ITEMS = new ItemStack[27];

    public PeekScreen(ShulkerBoxMenu handler, Inventory inventory, Component title, Block block) {
        super(handler, inventory, title);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && hoveredSlot != null && !hoveredSlot.getItem().isEmpty() && minecraft.player.inventoryMenu.getCarried().isEmpty()) {
            ItemStack itemStack = hoveredSlot.getItem();

            if (Tooltips.hasItems(itemStack) && Tooltips.middleClickOpen.getValue()) {

                Arrays.fill(ITEMS, ItemStack.EMPTY);
                ItemContainerContents nbt = itemStack.get(DataComponents.CONTAINER);
                if (nbt != null) {
                    List<ItemStack> list = nbt.stream().toList();
                    for (int i = 0; i < list.size(); i++)
                        ITEMS[i] = list.get(i);
                }


                minecraft.setScreen(new PeekScreen(new ShulkerBoxMenu(0, minecraft.player.getInventory(), new SimpleContainer(ITEMS)), minecraft.player.getInventory(), hoveredSlot.getItem().getHoverName(), ((BlockItem) hoveredSlot.getItem().getItem()).getBlock()));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return false;
    }
}