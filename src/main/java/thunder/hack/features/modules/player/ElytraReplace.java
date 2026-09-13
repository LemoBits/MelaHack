package thunder.hack.features.modules.player;

import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.notification.Notification;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ElytraReplace extends Module {
    public ElytraReplace() {
        super("ElytraReplace", Category.PLAYER);
    }

    private final Setting<Integer> durability = new Setting<>("Durability", 5, 0, 100);

    @Override
    public void onUpdate() {
        ItemStack is = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if(is.is(Items.ELYTRA) && ((100f - ((float) is.getDamageValue() / (float) is.getMaxDamage()) * 100f) <= durability.getValue())){

            SearchInvResult result = InventoryUtility.findInInventory(stack -> {
                if (stack.is(Items.ELYTRA))
                    return (100f - ((float) stack.getDamageValue() / (float) stack.getMaxDamage()) * 100f) > durability.getValue();
                return false;
            });

            if (result.found()) {
                clickSlot(result.slot());
                clickSlot(6);
                clickSlot(result.slot());
                sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                Managers.NOTIFICATION.publicity("ElytraReplace", isRu() ? "Меняем элитру на новую!" : "Swapping the old elytra for a new one!",2, Notification.Type.SUCCESS);
                sendMessage(isRu() ? "Меняем элитру на новую!" : "Swapping the old elytra for a new one!");
            }
        }
    }
}
