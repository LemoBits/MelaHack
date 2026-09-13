package thunder.hack.features.hud.impl;

import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PVPResources extends HudElement {
    public PVPResources() {
        super("PVPResources", 50, 50);
    }

    public void onRender2D(GuiGraphics context) {
        super.onRender2D(context);
        Render2DEngine.drawHudBase(context.pose(), getPosX(), getPosY(), 50, 50, HudEditor.hudRound.getValue());

        setBounds(getPosX(), getPosY(), 50, 50);

        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Render2DEngine.drawRectDumbWay(context.pose(), getPosX(), getPosY() + 24.5f, getPosX() + 50, getPosY() + 25, new Color(0x54FFFFFF, true));
            Render2DEngine.drawRectDumbWay(context.pose(), getPosX() + 24.5f, getPosY() - 1, getPosX() + 25, getPosY() + 49, new Color(0x54FFFFFF, true));
        } else {
            Render2DEngine.horizontalGradient(context.pose(), getPosX() + 2, getPosY() + 24.5f, getPosX() + 26, getPosY() + 25, Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0), HudEditor.textColor.getValue().getColorObject());
            Render2DEngine.horizontalGradient(context.pose(), getPosX() + 26, getPosY() + 24.5f, getPosX() + 48, getPosY() + 25, HudEditor.textColor.getValue().getColorObject(), Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0));
            Render2DEngine.verticalGradient(context.pose(), getPosX() + 25.5f, getPosY() + 2, getPosX() + 26, getPosY() + 23, Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0), HudEditor.textColor.getValue().getColorObject());
            Render2DEngine.verticalGradient(context.pose(), getPosX() + 25.5f, getPosY() + 23, getPosX() + 26, getPosY() + 48, HudEditor.textColor.getValue().getColorObject(), Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0));
        }

        int totemCount = getItemCount(Items.TOTEM_OF_UNDYING);
        int xpCount = getItemCount(Items.EXPERIENCE_BOTTLE);
        int crystalCount = getItemCount(Items.END_CRYSTAL);
        int gappleCount = getItemCount(Items.ENCHANTED_GOLDEN_APPLE);

        List<ItemStack> list = new ArrayList<>();

        if (totemCount > 0) list.add(new ItemStack(Items.TOTEM_OF_UNDYING, totemCount));
        if (xpCount > 0) list.add(new ItemStack(Items.EXPERIENCE_BOTTLE, xpCount));
        if (crystalCount > 0) list.add(new ItemStack(Items.END_CRYSTAL, crystalCount));
        if (gappleCount > 0) list.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, gappleCount));

        for (int i = 0; i < list.size(); ++i) {
            int offsetX = i % 2 * 25;
            int offsetY = i / 2 * 25;
            context.renderItem(list.get(i), (int) (getPosX() + offsetX + 4), (int) (getPosY() + offsetY + 4));
            context.pose().pushMatrix();
            context.pose().translate((float) (0), (float) (0));
            Render2DEngine.drawBlurredShadow(context.pose(), getPosX() + offsetX + 8, getPosY() + offsetY + 8, 9, 9, 12, Color.BLACK);
            FontRenderers.sf_medium.drawCenteredString(context.pose(), String.valueOf(list.get(i).getCount()), (int) (getPosX() + offsetX + 12), (int) (getPosY() + offsetY + 11f), HudEditor.textColor.getValue().getColor());
            context.pose().popMatrix();
        }
    }

    public int getItemCount(Item item) {
        if (mc.player == null) return 0;
        int n = 0;
        int n2 = 44;
        for (int i = 0; i <= n2; ++i) {
            ItemStack itemStack = mc.player.getInventory().getItem(i);
            if (itemStack.getItem() != item) continue;
            n += itemStack.getCount();
        }
        return n;
    }
}