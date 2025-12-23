package thunder.hack.features.hud.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Formatting;
import net.minecraft.client.resource.language.LanguageManager;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;

import java.awt.*;
import java.util.Objects;

public class PotionHud extends HudElement {
    public PotionHud() {
        super("Potions", 100, 100);
    }

    private float vAnimation, hAnimation;

    private final Setting<Boolean> colored = new Setting<>("Colored", false);

    public static String getDuration(StatusEffectInstance pe) {
        if (pe.isInfinite()) {
            return "*:*";
        } else {
            int var1 = pe.getDuration();
            int mins = var1 / 1200;
            String sec = String.format("%02d", (var1 % 1200) / 20);
            return mins + ":" + sec;
        }
    }

        /*
        Render2DEngine.addWindow(context.getMatrices(), getPosX(), getPosY(), getPosX() + hAnimation, getPosY() + vAnimation, 1f);
        for (StatusEffectInstance potionEffect : effects) {
            StatusEffect potion = potionEffect.getEffectType().value();
            String power = "";
            switch (potionEffect.getAmplifier()) {
                case 0 -> power = "I";
                case 1 -> power = "II";
                case 2 -> power = "III";
                case 3 -> power = "IV";
                case 4 -> power = "V";
            }

            String s = potion.getName().getString() + " " + power;
            String s2 = getDuration(potionEffect) + "";

            Color c = new Color(potionEffect.getEffectType().value().getColor());
            FontRenderers.sf_bold_mini.drawString(context.getMatrices(), s + "  " + s2, getPosX() + 5, getPosY() + 20 + y_offset, colored.getValue() ? c.getRGB() : HudEditor.textColor.getValue().getColor());
            y_offset += 10;
        }*/

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        int y_offset1 = 0;
        float max_width = 50;

        float pointerX = 0;
        for (StatusEffectInstance potionEffect : mc.player.getStatusEffects()) {
            StatusEffect potion = potionEffect.getEffectType().value();

            if (y_offset1 == 0)
                y_offset1 += 4;

            y_offset1 += 9;

            float nameWidth = FontRenderers.source_han_sans_normal.getStringWidth(potion.getName().getString() + " " + (potionEffect.getAmplifier() + 1));
            float timeWidth = FontRenderers.source_han_sans_normal.getStringWidth(getDuration(potionEffect));
            float width = (nameWidth + timeWidth) * 1.4f;

            if (width > max_width)
                max_width = width;

            if (timeWidth > pointerX)
                pointerX = timeWidth;
        }

        // Specific configuration of HudBase
        LanguageManager languageManager = MinecraftClient.getInstance().getLanguageManager();
        String currentLanguage = languageManager.getLanguage();

        if (Objects.equals(currentLanguage, "zh_cn")) {
            vAnimation = AnimationUtility.fast(vAnimation, 14 + y_offset1, 15);
            hAnimation = AnimationUtility.fast(hAnimation + 1.5f, max_width, 15);
        }
        else {
            vAnimation = AnimationUtility.fast(vAnimation, 14 + y_offset1, 15);
            hAnimation = AnimationUtility.fast(hAnimation, max_width, 15);
        }

        Render2DEngine.drawHudBase(context.getMatrices(), getPosX(), getPosY(), hAnimation, vAnimation, HudEditor.hudRound.getValue());

        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
            FontRenderers.source_han_sans_normal.drawCenteredString(context.getMatrices(), "Potions", getPosX() + hAnimation / 2, getPosY() + 4, HudEditor.textColor.getValue().getColorObject());
        } else {
            FontRenderers.source_han_sans_normal.drawGradientCenteredString(context.getMatrices(), "Potions", getPosX() + hAnimation / 2, getPosY() + 4, 10);
        }

        if (y_offset1 > 0) {
            if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
                Render2DEngine.drawRectDumbWay(context.getMatrices(), getPosX() + 4, getPosY() + 13, getPosX() + getWidth() - 4, getPosY() + 13.5f, new Color(0x54FFFFFF, true));
            } else {
                Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + 2, getPosY() + 13.7f, getPosX() + 2 + hAnimation / 2f - 2, getPosY() + 14, Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0), HudEditor.textColor.getValue().getColorObject());
                Render2DEngine.horizontalGradient(context.getMatrices(), getPosX() + 2 + hAnimation / 2f - 2, getPosY() + 13.7f, getPosX() + 2 + hAnimation - 4, getPosY() + 14, HudEditor.textColor.getValue().getColorObject(), Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0));
            }
        }

        Render2DEngine.addWindow(context.getMatrices(), getPosX(), getPosY(), getPosX() + hAnimation, getPosY() + vAnimation, 1f);
        int y_offset = 0;
        for (StatusEffectInstance potionEffect : mc.player.getStatusEffects()) {
            StatusEffect potion = potionEffect.getEffectType().value();

            float px = getPosX() + (max_width - pointerX - 10);

            context.getMatrices().push();
            context.getMatrices().translate(getPosX() + 2, getPosY() + 16 + y_offset, 0);
            context.drawSpriteStretched(RenderLayer::getGuiTextured, mc.getStatusEffectSpriteManager().getSprite(potionEffect.getEffectType()), 0, 0, 8, 8);
            context.getMatrices().pop();

            // Position offsets for each language
            if (Objects.equals(currentLanguage, "zh_cn")) {
                FontRenderers.source_han_sans_normal.drawString(context.getMatrices(), potion.getName().getString() + " " + Formatting.RED + (potionEffect.getAmplifier() + 1), getPosX() + 12, getPosY() + 16.5 + y_offset, HudEditor.textColor.getValue().getColor());
                FontRenderers.source_han_sans_normal.drawCenteredString(context.getMatrices(), getDuration(potionEffect), px + (getPosX() + max_width - px) / 2f + 10, getPosY() + 16.5 + y_offset, HudEditor.textColor.getValue().getColor());
                Render2DEngine.drawRect(context.getMatrices(), px + 10, getPosY() + 17 + y_offset, 0.5f, 8, new Color(0x44FFFFFF, true));
                y_offset += 9;
            }
            else {
                FontRenderers.source_han_sans_normal.drawString(context.getMatrices(), potion.getName().getString() + " " + Formatting.RED + (potionEffect.getAmplifier() + 1), getPosX() + 12, getPosY() + 16.5 + y_offset, HudEditor.textColor.getValue().getColor());
                FontRenderers.source_han_sans_normal.drawCenteredString(context.getMatrices(), getDuration(potionEffect), px + (getPosX() + max_width - px) / 2f, getPosY() + 16.5 + y_offset, HudEditor.textColor.getValue().getColor());
                Render2DEngine.drawRect(context.getMatrices(), px, getPosY() + 17 + y_offset, 0.5f, 8, new Color(0x44FFFFFF, true));
                y_offset += 9;
            }
        }
        Render2DEngine.popWindow();
        setBounds(getPosX(), getPosY(), hAnimation, vAnimation);
    }
}
