package thunder.hack.features.modules.render;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static thunder.hack.core.Managers.FRIEND;

public class PenisESP extends Module {
    public PenisESP() {
        super("PenisESP", Category.RENDER);
    }

    private final Setting<Boolean> onlyOwn = new Setting<>("OnlyOwn", false);
    private final Setting<Float> ballSize = new Setting<>("BallSize", 0.1f, 0.1f, 0.5f);
    private final Setting<Float> penisSize = new Setting<>("PenisSize", 1.5f, 0.1f, 3.0f);
    private final Setting<Float> friendSize = new Setting<>("FriendSize", 1.5f, 0.1f, 3.0f);
    private final Setting<Float> enemySize = new Setting<>("EnemySize", 0.5f, 0.1f, 3.0f);
    private final Setting<Integer> gradation = new Setting<>("Gradation", 30, 20, 100);
    private final Setting<Boolean> fimoz = new Setting<>("Fimoz", false);
    private final Setting<ColorSetting> penisColor = new Setting<>("PenisColor", new ColorSetting(new Color(231, 180, 122, 255)));
    private final Setting<ColorSetting> headColor = new Setting<>("HeadColor", new ColorSetting(new Color(240, 50, 180, 255)), v -> !fimoz.getValue());

    @Override
    public void onRender2D(GuiGraphics event) {
        for (Player player : mc.level.players()) {
            if (onlyOwn.getValue() && player != mc.player) continue;
            double size = (FRIEND.isFriend(player) ? friendSize.getValue() : (player != mc.player ? enemySize.getValue() : penisSize.getValue()));

            Vec3 base = getBase(player);
            Vec3 forward = base.add(0, player.getBbHeight() / 2.4, 0).add(Vec3.directionFromRotation(0, player.getYRot()).scale(0.1));

            Vec3 left = forward.add(Vec3.directionFromRotation(0, player.getYRot() - 90).scale(ballSize.getValue()));
            Vec3 right = forward.add(Vec3.directionFromRotation(0, player.getYRot() + 90).scale(ballSize.getValue()));

            drawBall(player, ballSize.getValue(), gradation.getValue(), left, penisColor.getValue().getColorObject(), 0);
            drawBall(player, ballSize.getValue(), gradation.getValue(), right, penisColor.getValue().getColorObject(), 0);
            drawPenis(player, event.pose(), size, forward);
        }
    }

    public Vec3 getBase(Entity entity) {
        double x = entity.xo + ((entity.getX() - entity.xo) * Render3DEngine.getTickDelta());
        double y = entity.yo + ((entity.getY() - entity.yo) * Render3DEngine.getTickDelta());
        double z = entity.zo + ((entity.getZ() - entity.zo) * Render3DEngine.getTickDelta());

        return new Vec3(x, y, z);
    }

    public void drawBall(Player player, double radius, int gradation, Vec3 pos, Color color, int stage) {
        float alpha, beta;

        for (alpha = 0.0f; alpha < Math.PI; alpha += Math.PI / gradation) {
            for (beta = 0.0f; beta < 2.0 * Math.PI; beta += Math.PI / gradation) {
                double x1 = (float) (pos.x() + (radius * Math.cos(beta) * Math.sin(alpha)));
                double y1 = (float) (pos.y() + (radius * Math.sin(beta) * Math.sin(alpha)));
                double z1 = (float) (pos.z() + (radius * Math.cos(alpha)));

                double sin = Math.sin(alpha + Math.PI / gradation);
                double x2 = (float) (pos.x() + (radius * Math.cos(beta) * sin));
                double y2 = (float) (pos.y() + (radius * Math.sin(beta) * sin));
                double z2 = (float) (pos.z() + (radius * Math.cos(alpha + Math.PI / gradation)));

                Vec3 base = getBase(player);
                Vec3 forward = base.add(0, player.getBbHeight() / 2.4, 0).add(Vec3.directionFromRotation(0, player.getYRot()).scale(0.1));
                Vec3 vec3d = new Vec3(x1, y1, z1);

                switch (stage) {
                    case 1 -> {
                        if (!vec3d.closerThan(forward, 0.145)) continue;
                    }
                    case 2 -> {
                        double size = (FRIEND.isFriend(player) ? friendSize.getValue() : (player != mc.player ? enemySize.getValue() : penisSize.getValue()));
                        if (vec3d.closerThan(forward, size + 0.095)) continue;
                    }
                }

                Render3DEngine.drawLine(vec3d, new Vec3(x2, y2, z2), color);
            }
        }
    }

    public void drawPenis(Player player, Object event, double size, Vec3 start) {
        Vec3 copy = start;
        start = start.add(Vec3.directionFromRotation(0, player.getYRot()).scale(0.1));
        Vec3 end = start.add(Vec3.directionFromRotation(0, player.getYRot()).scale(size));

        List<Vec3> vecs = getVec3ds(start, 0.1);
        vecs.forEach(vec3d -> {
            if (!vec3d.closerThan(copy, 0.145)) return;
            if (vec3d.closerThan(copy, 0.135)) return;
            Vec3 pos = vec3d.add(Vec3.directionFromRotation(0, player.getYRot()).scale(size));
            Render3DEngine.drawLine(vec3d, pos, penisColor.getValue().getColorObject());
        });

        drawBall(player, 0.1, gradation.getValue(), start, penisColor.getValue().getColorObject(), 1);
        if (fimoz.getValue()) {
            drawBall(player, 0.1, gradation.getValue(), end, penisColor.getValue().getColorObject(), 2);
        } else {
            drawBall(player, 0.1, gradation.getValue(), end, headColor.getValue().getColorObject(), 2);
        }
    }

    public List<Vec3> getVec3ds(Vec3 vec3d, double radius) {
        List<Vec3> vec3ds = new ArrayList<>();
        float alpha, beta;

        for (alpha = 0.0f; alpha < Math.PI; alpha += Math.PI / gradation.getValue()) {
            for (beta = 0.0f; beta < 2.01f * Math.PI; beta += Math.PI / gradation.getValue()) {
                double x1 = (float) (vec3d.x() + (radius * Math.cos(beta) * Math.sin(alpha)));
                double y1 = (float) (vec3d.y() + (radius * Math.sin(beta) * Math.sin(alpha)));
                double z1 = (float) (vec3d.z() + (radius * Math.cos(alpha)));

                Vec3 vec = new Vec3(x1, y1, z1);
                vec3ds.add(vec);
            }
        }

        return vec3ds;
    }

}
