package thunder.hack.features.modules.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4d;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.world.WayPointManager;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.modules.Module;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.TextureStorage;

public final class WayPoints extends Module {
    public WayPoints() {
        super("WayPoints", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        sendMessage(Managers.COMMAND.getPrefix() + "waypoint add x y z name");
    }

    public void onRender2D(GuiGraphicsExtractor context) {
        if (!Managers.WAYPOINT.getWayPoints().isEmpty() && !fullNullCheck()) {
            for (WayPointManager.WayPoint wp : Managers.WAYPOINT.getWayPoints()) {
                if (wp.getName() == null) continue;
                if ((mc.isLocalServer() && wp.getServer().equals("SinglePlayer"))
                        || (mc.getConnection().getServerData() != null && !mc.getConnection().getServerData().ip.contains(wp.getServer()))) continue;
                if (!mc.level.dimension().identifier().getPath().equals(wp.getDimension())) continue;
                double difX = wp.getX() - mc.player.position().x;
                double difZ = wp.getZ() - mc.player.position().z;
                float yaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0);
                double plYaw = Mth.wrapDegrees(mc.player.getYRot());
                if (Math.abs(yaw - plYaw) > 90) continue;

                Vec3 vector = new Vec3(wp.getX(), wp.getY(), wp.getZ());
                Vector4d position = null;
                vector = Render3DEngine.worldSpaceToScreenSpace(new Vec3(vector.x, vector.y, vector.z));
                position = new Vector4d(vector.x, vector.y, vector.z, 0);
                position.x = Math.min(vector.x, position.x);
                position.y = Math.min(vector.y, position.y);
                position.z = Math.max(vector.x, position.z);

                double posX = position.x;
                double posY = position.y;
                double endPosX = position.z;

                float diff = (float) (endPosX - posX) / 2;
                float tagX = (float) ((posX + diff - FontRenderers.sf_bold_mini.getStringWidth(wp.getName()) / 2) * 1);

                String coords = wp.getX() + " " + wp.getZ();
                float tagX2 = (float) ((posX + diff - FontRenderers.sf_bold_mini.getStringWidth(coords) / 2) * 1);

                String distance = String.format("%.0f", Math.sqrt(mc.player.distanceToSqr(wp.getX(), wp.getY(), wp.getZ()))) + "m";
                float tagX3 = (float) ((posX + diff - FontRenderers.sf_bold_mini.getStringWidth(distance) / 2) * 1);

                context.pose().pushMatrix();
                context.pose().translate((float) (posX - 10), (float) ((posY - 35)));
                context.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TextureStorage.waypoint, 0, 0, 20, 20, 0, 0, 20, 20, 20, 20);
                context.pose().popMatrix();

                FontRenderers.sf_bold_mini.drawString(context.pose(), wp.getName(), tagX, (float) posY - 10, -1);
                FontRenderers.sf_bold_mini.drawString(context.pose(), ChatFormatting.GRAY + coords, tagX2, (float) posY - 2, -1);
                FontRenderers.sf_bold_mini.drawString(context.pose(), ChatFormatting.GRAY + distance, tagX3, (float) posY + 6, -1);
            }
        }
    }
}
