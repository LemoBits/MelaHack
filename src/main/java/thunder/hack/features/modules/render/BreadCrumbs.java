package thunder.hack.features.modules.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import thunder.hack.utility.render.compat.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import thunder.hack.utility.render.ShaderProgramKeys;
import thunder.hack.utility.render.BufferRenderer;
import net.minecraft.world.phys.Vec3;
import thunder.hack.events.impl.EventPostSync;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BreadCrumbs extends Module {
    public BreadCrumbs() {
        super("BreadCrumbs", Category.RENDER);
    }

    private final Setting<Boolean> throughWalls = new Setting<>("ThroughWalls", true);
    private final Setting<Integer> limit = new Setting<>("ListLimit", 1000, 10, 99999);
    private final List<Vec3> positions = new CopyOnWriteArrayList<>();
    private final Setting<Mode> lmode = new Setting<>("ColorMode", Mode.Sync);
    private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(3649978), v -> lmode.getValue() == Mode.Custom);

    private enum Mode {
        Custom, Sync
    }

    public void onRender3D(PoseStack stack) {
        Render3DEngine.setupRender();

        if (throughWalls.getValue())
            RenderSystem.disableDepthTest();

        RenderSystem.disableCull();
        RenderSystem.lineWidth(1f);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < positions.size(); i++) {
            Vec3 vec1 = null;
            try {
                vec1 = positions.get(i - 1);
            } catch (Exception ignored) {
            }
            Vec3 vec2 = positions.get(i);
            if (vec1 != null && vec2 != null) {
                Color c = lmode.getValue() == Mode.Sync ? HudEditor.getColor(i) : color.getValue().getColorObject();
                if (i < 10) c = Render2DEngine.injectAlpha(c, (int) (c.getAlpha() * (i / 10f)));
                PoseStack matrices = Render3DEngine.matrixFrom(vec1.x(), vec1.y(), vec1.z());
                Render3DEngine.vertexLine(matrices, buffer, 0f, 0f, 0f, (float) (vec2.x() - vec1.x()), (float) (vec2.y() - vec1.y()), (float) (vec2.z() - vec1.z()), c);
            }
        }

        Render2DEngine.endBuilding(buffer);

        RenderSystem.enableCull();
        if (throughWalls.getValue())
            RenderSystem.enableDepthTest();
        Render3DEngine.endRender();
    }

    @EventHandler
    public void postSync(EventPostSync e) {
        if (positions.size() > limit.getValue()) positions.remove(0);
        positions.add(new Vec3(mc.player.getX(), mc.player.getBoundingBox().minY, mc.player.getZ()));
    }

    @Override
    public void onDisable() {
        positions.clear();
    }
}
