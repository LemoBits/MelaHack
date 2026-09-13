package thunder.hack.features.modules.player;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import static thunder.hack.features.modules.client.ClientSettings.isRu;
import static thunder.hack.features.modules.combat.AutoTotem.findNearestCurrentItem;

import net.minecraft.world.item.ItemStack;

public class ToolSaver extends Module {
    public ToolSaver() {
        super("ToolSaver", Category.PLAYER);
    }

    private final Setting<Integer> savePercent = new Setting<>("Save %", 10, 1, 50);

    @Override
    public void onUpdate() {
        ItemStack tool = mc.player.getMainHandItem();
        if(!tool.isDamageableItem())
            return;

        float durability = tool.getMaxDamage() - tool.getDamageValue();
        int percent = (int) ((durability / (float) tool.getMaxDamage()) * 100F);

        if(percent <= savePercent.getValue()) {
            mc.player.getInventory().setSelectedSlot(findNearestCurrentItem());
            sendMessage(isRu() ? "Твой инструмент почти сломался!" : "Your tool is almost broken!");
        }
    }
}
