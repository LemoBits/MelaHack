package thunder.hack.features.hud.impl;

import thunder.hack.utility.render.compat.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import thunder.hack.features.hud.HudElement;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;

public class ArmorHud extends HudElement {
    public ArmorHud() {
        super("ArmorHud", 60, 25);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.V2);

    private enum Mode {
        V1, V2
    }

    public void onRender2D(GuiGraphics context) {
        super.onRender2D(context);
        float xItemOffset = getPosX();
        for (ItemStack itemStack : thunder.hack.utility.player.ArmorUtility.getArmorItems(mc.player).reversed()) {
            if (itemStack.isEmpty()) continue;

            if (mode.is(Mode.V1)) {
                context.renderItem(itemStack, (int) xItemOffset, (int) getPosY());
                context.renderItemDecorations(mc.font,itemStack,  (int) xItemOffset, (int) getPosY());
            } else {
                RenderSystem.setShaderColor(0.4f,0.4f,0.4f,0.35f);
                context.renderItem(itemStack, (int) xItemOffset, (int) getPosY());
                RenderSystem.setShaderColor(1f,1f,1f,1f);
                float offset = mc.player.getEquipmentSlotForItem(itemStack) == EquipmentSlot.HEAD ? -4 : 0;
                Render2DEngine.addWindow(context.pose(), (int) xItemOffset, getPosY() + offset + (15 - offset) * ((float) itemStack.getDamageValue() / (float) itemStack.getMaxDamage()), xItemOffset + 15, getPosY() + 15, 1f);
                context.renderItem(itemStack, (int) xItemOffset, (int) getPosY());
                Render2DEngine.popWindow();
            }
            xItemOffset += 20;
        }

        setBounds(getPosX(), getPosY(), 60, 25);
    }
}
