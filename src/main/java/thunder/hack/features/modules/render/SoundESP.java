package thunder.hack.features.modules.render;

import com.google.common.collect.Lists;
import org.joml.Vector4d;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

public class SoundESP extends Module {
    public SoundESP() {
        super("SoundESP", Category.RENDER);
    }

    private final Setting<Float> scale = new Setting<>("Scale", 1f, 0.1f, 10f);
    private final Setting<ColorSetting> fillColorA = new Setting<>("Color", new ColorSetting(0x80000000));

    private List<Sound> sounds = new ArrayList<>();

    public void add(double x, double y, double z, String name) {
        sounds.add(new Sound(x, y, z, name.replace("minecraft.block.", "").replace("minecraft.entity", "").replace(".", " ")));
    }

    public void onRender2D(GuiGraphicsExtractor context) {
        for (Sound s : Lists.newArrayList(sounds)) {
            Vec3 vector = new Vec3(s.x, s.y, s.z);
            Vector4d position = null;
            vector = Render3DEngine.worldSpaceToScreenSpace(vector);
            if (vector.z > 0 && vector.z < 1) {
                position = new Vector4d(vector.x, vector.y, vector.z, 0);
                position.x = Math.min(vector.x, position.x);
                position.y = Math.min(vector.y, position.y);
                position.z = Math.max(vector.x, position.z);
            }

            if (position != null) {
                double posX = position.x;
                double posY = position.y;
                double endPosX = position.z;

                float diff = (float) (endPosX - posX) / 2;
                float textWidth = (FontRenderers.sf_bold.getStringWidth(s.name) * 1);
                float tagX = (float) ((posX + diff - textWidth / 2) * 1);

                float alpha = (float) (1f - Math.pow(1f - ((float) s.ticks / 60f), 3f));

                context.pose().pushMatrix();
                context.pose().translate((float) (tagX - 2 + (textWidth + 4) / 2f), (float) ((float) (posY - 13f) + 6.5f));
                context.pose().scale(scale.getValue(), scale.getValue());
                context.pose().translate((float) (-(tagX - 2 + (textWidth + 4) / 2f)), (float) (-(float) ((posY - 13f) + 6.5f)));
                Render2DEngine.drawRect(context.pose(), tagX - 2, (float) (posY - 13f), textWidth + 4, 11, fillColorA.getValue().withAlpha((int) (fillColorA.getValue().getAlpha() * alpha)).getColorObject());
                FontRenderers.sf_bold.drawString(context.pose(), s.name, tagX, (float) posY - 10, Render2DEngine.applyOpacity(-1, alpha));
                context.pose().popMatrix();
            }
        }
    }

    @Override
    public void onUpdate() {
        sounds.removeIf(Sound::shouldRemove);
    }

    private class Sound {
        double x, y, z;
        String name;
        int ticks;

        public Sound(double x, double y, double z, String name) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.name = name;
            this.ticks = 60;
        }

        public boolean shouldRemove() {
            return ticks-- <= 0;
        }
    }
}
