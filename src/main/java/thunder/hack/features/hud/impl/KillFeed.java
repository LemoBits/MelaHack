package thunder.hack.features.hud.impl;

import com.google.common.collect.Lists;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.features.modules.combat.AutoCrystal;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KillFeed extends HudElement {
    public KillFeed() {
        super("KillFeed", 50, 50);
    }

    private Setting<Boolean> resetOnDeath = new Setting<>("ResetOnDeath", true);

    private final List<KillComponent> players = new ArrayList<>();

    private float vAnimation, hAnimation;

    public void onRender2D(GuiGraphics context) {
        super.onRender2D(context);
        int y_offset1 = 0;
        float scale_x = 30;

        for (KillComponent kc : Lists.newArrayList(players)) {
            if (FontRenderers.modules.getStringWidth(kc.getString()) > scale_x)
                scale_x = FontRenderers.modules.getStringWidth(kc.getString());
            y_offset1 += 15;
        }

        vAnimation = AnimationUtility.fast(vAnimation, 14 + y_offset1, 15);
        hAnimation = AnimationUtility.fast(hAnimation, scale_x + 10, 15);

        Render2DEngine.drawHudBase(context.pose(), getPosX(), getPosY(), hAnimation, vAnimation, HudEditor.hudRound.getValue());

        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
            FontRenderers.sf_bold.drawCenteredString(context.pose(), "KillFeed", getPosX() + hAnimation / 2, getPosY() + 4, HudEditor.textColor.getValue().getColorObject());
        } else {
            FontRenderers.sf_bold.drawGradientCenteredString(context.pose(), "KillFeed", getPosX() + hAnimation / 2, getPosY() + 4, 10);
        }

        if (y_offset1 > 0) {
            if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
                Render2DEngine.horizontalGradient(context.pose(), getPosX() + 2, getPosY() + 13.7f, getPosX() + 2 + hAnimation / 2f - 2, getPosY() + 14, Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0), HudEditor.textColor.getValue().getColorObject());
                Render2DEngine.horizontalGradient(context.pose(), getPosX() + 2 + hAnimation / 2f - 2, getPosY() + 13.7f, getPosX() + 2 + hAnimation - 4, getPosY() + 14, HudEditor.textColor.getValue().getColorObject(), Render2DEngine.injectAlpha(HudEditor.textColor.getValue().getColorObject(), 0));
            } else {
                Render2DEngine.drawRectDumbWay(context.pose(), getPosX() + 4, getPosY() + 13, getPosX() + getWidth() - 4, getPosY() + 13.5f, new Color(0x54FFFFFF, true));
            }
        }

        Render2DEngine.addWindow(context.pose(), getPosX(), getPosY(), getPosX() + hAnimation, getPosY() + vAnimation, 1f);
        int y_offset = 3;
        for (KillComponent kc : Lists.newArrayList(players)) {
            FontRenderers.modules.drawString(context.pose(), kc.getString(), getPosX() + 5, getPosY() + 18 + y_offset, -1);
            y_offset += 10;
        }
        Render2DEngine.popWindow();
        setBounds(getPosX(), getPosY(), hAnimation, vAnimation);
    }

    @EventHandler
    public void onPacket(PacketEvent.@NotNull Receive e) {
        if (!(e.getPacket() instanceof ClientboundEntityEventPacket pac)) return;
        if (pac.getEventId() == EntityEvent.DEATH && pac.getEntity(mc.level) instanceof Player pl) {

            if(pl == mc.player && resetOnDeath.getValue()) {
                players.clear();
                return;
            }

            if ((Aura.target != null && Aura.target == pac.getEntity(mc.level)) || (AutoCrystal.target != null && AutoCrystal.target == pac.getEntity(mc.level))) {
                for (KillComponent kc : Lists.newArrayList(players))
                    if (Objects.equals(kc.getName(), pl.getName().getString())) {
                        kc.increase();
                        return;
                    }
                players.add(new KillComponent(pl.getName().getString()));
            }
        }
    }

    @Override
    public void onDisable() {
        players.clear();
    }

    private class KillComponent {
        private String name;
        private int count;

        public KillComponent(String name) {
            this.name = name;
            this.count = 1;
        }

        public void increase() {
            count++;
        }

        public String getName() {
            return name;
        }

        public String getString() {
            return ChatFormatting.RED + "EZ - " + ChatFormatting.RESET + name + (count > 1 ?  (" [" + ChatFormatting.GRAY + "x" + count + ChatFormatting.RESET + "]") : "");
        }
    }
}
