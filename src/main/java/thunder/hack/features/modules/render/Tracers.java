package thunder.hack.features.modules.render;

import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render3DEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class Tracers extends Module {
    public Tracers() {
        super("Tracers", Category.RENDER);
    }

    private final Setting<Float> height = new Setting<>("Height", 0f, 0f, 2f);

    private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(0x93FF0000, true)));
    private final Setting<ColorSetting> friendColor = new Setting<>("Friends", new ColorSetting(new Color(0x9317DE5D, true)));

    public void onRender3D(PoseStack stack) {
        for (Player player : Managers.ASYNC.getAsyncPlayers()) {
            if (player == mc.player)
                continue;

            Color color1 = color.getValue().getColorObject();

            if (Managers.FRIEND.isFriend(player))
                color1 = friendColor.getValue().getColorObject();

            double x1 = mc.player.xo + (mc.player.getX() - mc.player.xo) * Render3DEngine.getTickDelta();
            double y1 = mc.player.getEyeHeight(mc.player.getPose()) + mc.player.yo + (mc.player.getY() - mc.player.yo) * Render3DEngine.getTickDelta();
            double z1 = mc.player.zo + (mc.player.getZ() - mc.player.zo) * Render3DEngine.getTickDelta();

            Vec3 vec2 = new Vec3(0, 0, 75)
                    .xRot(-(float) Math.toRadians(mc.gameRenderer.mainCamera().xRot()))
                    .yRot(-(float) Math.toRadians(mc.gameRenderer.mainCamera().yRot()))
                    .add(x1, y1, z1);

            double x = player.xo + (player.getX() - player.xo) * Render3DEngine.getTickDelta();
            double y = player.yo + (player.getY() - player.yo) * Render3DEngine.getTickDelta();
            double z = player.zo + (player.getZ() - player.zo) * Render3DEngine.getTickDelta();

            Render3DEngine.drawLineDebug(vec2, new Vec3(x, y + height.getValue(), z), color1);
        }
    }
}
