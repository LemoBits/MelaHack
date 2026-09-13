package thunder.hack.features.hud.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import thunder.hack.utility.render.compat.RenderSystem;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;

import static thunder.hack.features.modules.render.StorageEsp.getBlockEntities;

public class ChestCounter extends HudElement {
    public ChestCounter() {
        super("ChestCounter", 50, 10);
    }
    public void onRender2D(GuiGraphics context) {
        super.onRender2D(context);
        Tuple<Integer, Integer> chests = getChestCount();
        String str = "Chests: " + ChatFormatting.WHITE + "S:" + chests.getA() + " D:" + chests.getB();
        float pX = getPosX() > mc.getWindow().getGuiScaledWidth() / 2f ? getPosX() - FontRenderers.getModulesRenderer().getStringWidth(str) : getPosX();

        if(HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Render2DEngine.drawRoundedBlur(context.pose(), pX, getPosY(), FontRenderers.getModulesRenderer().getStringWidth(str) + 21, 13f, 3, HudEditor.blurColor.getValue().getColorObject());
            Render2DEngine.drawRect(context.pose(), pX + 14, getPosY() + 2, 0.5f, 8, new Color(0x44FFFFFF, true));

            Render2DEngine.setupRender();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.setShaderTexture(0, TextureStorage.chestIcon);
            Render2DEngine.renderGradientTexture(context.pose(), pX + 2, getPosY() + 1, 10, 10, 0, 0, 512, 512, 512, 512,
                    HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));
            Render2DEngine.endRender();
        }

        FontRenderers.getModulesRenderer().drawString(context.pose(), str, pX + 18, getPosY() + 5, HudEditor.getColor(1).getRGB());
        setBounds(pX, getPosY(), FontRenderers.getModulesRenderer().getStringWidth(str) + 21, 13f);
    }

    public Tuple<Integer, Integer> getChestCount() {
        int singleCount = 0;
        int doubleCount = 0;

        for (BlockEntity be : getBlockEntities()) {
            if (be instanceof ChestBlockEntity chest) {
                ChestType chestType = mc.level.getBlockState(chest.getBlockPos()).getValue(ChestBlock.TYPE);
                if (chestType == ChestType.SINGLE) {
                    singleCount++;
                } else doubleCount++;
            }
        }
        return new Tuple<>(singleCount, doubleCount / 2);
    }
}
