package thunder.hack.features.hud.impl;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import thunder.hack.utility.render.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import thunder.hack.utility.render.compat.RenderSystem;
import net.minecraft.ChatFormatting;
import thunder.hack.utility.render.ShaderProgramKeys;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import thunder.hack.utility.render.BufferRenderer;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.combat.AutoCrystal;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Collections;

public class AutoCrystalInfo extends HudElement {
    public AutoCrystalInfo() {
        super("AutoCrystalInfo", 175, 80);
    }

    private final ArrayDeque<Integer> speeds = new ArrayDeque<>(20);
    private int max, min;
    private long time;

    public void onRender2D(GuiGraphicsExtractor context) {
        super.onRender2D(context);

        Render2DEngine.drawHudBase(context.pose(), getPosX(), getPosY(), getWidth(), getHeight(), HudEditor.hudRound.getValue());

        Color c1 = HudEditor.getColor(0).darker().darker().darker();
        Color c2 = HudEditor.getColor(0);

        Render2DEngine.drawRect(context.pose(), getPosX() + 2, getPosY() + 14, 96, 64, HudEditor.hudRound.getValue(), 0.4f,
                c1, c1, c1, c1);

        FontRenderers.sf_bold.drawGradientString(context.pose(), "AutoCrystal Info", getPosX() + 2, getPosY() + 4, 10);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float offset = 0;

        for (Integer speed : speeds) {
            bufferBuilder.addVertex(getPosX() + 2 + offset, getPosY() + 80 - (55f * ((float) speed / (float) max)), 0f).setColor(c2.getRGB());
            offset += 4.8f;
        }

        Render2DEngine.endBuilding(bufferBuilder);

        BufferBuilder bufferBuilder2 = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        offset = 0;

        for (Integer speed : speeds) {
            bufferBuilder2.addVertex(getPosX() + 2 + offset, getPosY() + 80 - (55f * ((float) speed / (float) max)), 0f).setColor(Render2DEngine.applyOpacity(c2.getRGB(), 0.3f));
            bufferBuilder2.addVertex(getPosX() + 2 + offset, getPosY() + 80, 0f).setColor(Render2DEngine.applyOpacity(c2.darker().darker().getRGB(), 0f));
            offset += 4.8f;
        }

        Render2DEngine.endBuilding(bufferBuilder2);

        FontRenderers.sf_bold_mini.drawString(context.pose(), max + "", getPosX() + 100, getPosY() + 16, HudEditor.textColor.getValue().getRawColor());

        if (!speeds.isEmpty())
            FontRenderers.sf_bold_mini.drawString(context.pose(), speeds.getLast() + "", getPosX() + 100, getPosY() + 80 - (55f * ((float) speeds.getLast() / (float) max)), HudEditor.textColor.getValue().getRawColor());

        FontRenderers.sf_bold_mini.drawString(context.pose(), min + "", getPosX() + 100, getPosY() + 72, HudEditor.textColor.getValue().getRawColor());

        boolean isNull = ModuleManager.autoCrystal.getCurrentData() == null;

        FontRenderers.sf_bold_mini.drawString(context.pose(), "Target: " + ChatFormatting.GRAY + (AutoCrystal.target == null ? "null" : AutoCrystal.target.getName().getString()), getPosX() + 113, getPosY() + 16, HudEditor.textColor.getValue().getRawColor());

        int calc = (int) ModuleManager.autoCrystal.getCalcTime();
        float efficiency = (isNull ? 0 : MathUtility.round2(ModuleManager.autoCrystal.getCurrentData().damage() / ModuleManager.autoCrystal.getCurrentData().selfDamage()));

        FontRenderers.sf_bold_mini.drawString(context.pose(), "Calc delay: " + getCalcColor(calc) + calc + "ms", getPosX() + 113, getPosY() + 24, HudEditor.textColor.getValue().getRawColor());
        FontRenderers.sf_bold_mini.drawString(context.pose(), "Side: " + ChatFormatting.GRAY + (isNull ? "null" : ModuleManager.autoCrystal.getCurrentData().bhr().getDirection()), getPosX() + 113, getPosY() + 32, HudEditor.textColor.getValue().getRawColor());
        FontRenderers.sf_bold_mini.drawString(context.pose(), "Damage: " + ChatFormatting.GRAY + (isNull ? "null" : MathUtility.round2(ModuleManager.autoCrystal.getCurrentData().damage())), getPosX() + 113, getPosY() + 40, HudEditor.textColor.getValue().getRawColor());
        FontRenderers.sf_bold_mini.drawString(context.pose(), "Self: " + ChatFormatting.GRAY + (isNull ? "null" : MathUtility.round2(ModuleManager.autoCrystal.getCurrentData().selfDamage())), getPosX() + 113, getPosY() + 48, HudEditor.textColor.getValue().getRawColor());
        FontRenderers.sf_bold_mini.drawString(context.pose(), "Overr. dmg: " + ChatFormatting.GRAY + (isNull ? "null" : ModuleManager.autoCrystal.getCurrentData().overrideDamage()), getPosX() + 113, getPosY() + 56, HudEditor.textColor.getValue().getRawColor());
        FontRenderers.sf_bold_mini.drawString(context.pose(), "Efficiency: " + getEfficiencyColor(efficiency) + efficiency, getPosX() + 113, getPosY() + 64, HudEditor.textColor.getValue().getRawColor());
        FontRenderers.sf_bold_mini.drawString(context.pose(), "Pause: " + ModuleManager.autoCrystal.getPauseState(), getPosX() + 113, getPosY() + 72, HudEditor.textColor.getValue().getRawColor());

        Render2DEngine.drawRect(context.pose(), getPosX() + 110.5f, getPosY() + 12, 0.5f, 65, new Color(0x44FFFFFF, true));

        setBounds(getPosX(), getPosY(), getWidth(), getHeight());
    }

    public ChatFormatting getCalcColor(float val) {
        if (val > 20) return ChatFormatting.RED;
        else if (val > 10) return ChatFormatting.YELLOW;
        return ChatFormatting.GREEN;
    }

    public ChatFormatting getEfficiencyColor(float val) {
        if (val > 6) return ChatFormatting.GREEN;
        else if (val < 1) return ChatFormatting.RED;
        return ChatFormatting.YELLOW;
    }

    public void onSpawn() {
        if (time != 0L) {
            if (speeds.size() > 20)
                speeds.poll();

            speeds.add((int) (1000f / (float) (System.currentTimeMillis() - time)));
            max = Collections.max(speeds);
            min = Collections.min(speeds);
        }
        time = System.currentTimeMillis();
    }
}
