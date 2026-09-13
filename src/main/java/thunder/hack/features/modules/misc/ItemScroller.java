package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import org.lwjgl.glfw.GLFW;
import thunder.hack.events.impl.EventClickSlot;
import thunder.hack.injection.MixinClientPlayerInteractionManager;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class ItemScroller extends Module {
    public ItemScroller() {
        super("ItemScroller", Category.MISC);
    }

    public Setting<Integer> delay = new Setting<>("Delay",80,0,500);

    private boolean pauseListening = false;

    @EventHandler
    public void onClick(EventClickSlot e) {
        if ((isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GLFW.GLFW_KEY_RIGHT_SHIFT))
                && (isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL))
                && e.getSlotActionType() == ContainerInput.THROW
                && !pauseListening) {
            Item copy = mc.player.containerMenu.slots.get(e.getSlot()).getItem().getItem();
            pauseListening = true;
            for (int i2 = 0; i2 < mc.player.containerMenu.slots.size(); ++i2) {
                if (mc.player.containerMenu.slots.get(i2).getItem().getItem() == copy)
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, i2, 1, ContainerInput.THROW, mc.player);
            }
            pauseListening = false;
        }
    }
}
