package thunder.hack.features.modules.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import thunder.hack.utility.render.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import thunder.hack.utility.render.compat.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import thunder.hack.utility.render.ShaderProgramKeys;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.RemotePlayer;
import thunder.hack.utility.render.BufferRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector4d;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.injection.accesors.IEntity;
import thunder.hack.injection.accesors.IEntityRenderDispatcher;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.misc.FakePlayer;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class LogoutSpots extends Module {
    public LogoutSpots() {
        super("LogoutSpots", Category.RENDER);
    }

    private final Setting<RenderMode> renderMode = new Setting<>("RenderMode", RenderMode.TexturedChams);
    private final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(0x8800FF00));
    private final Setting<Boolean> notifications = new Setting<>("Notifications", true);
    private final Setting<Boolean> ignoreBots = new Setting<>("IgnoreBots", true);

    private final Map<UUID, Player> playerCache = Maps.newConcurrentMap();
    private final Map<UUID, Player> logoutCache = Maps.newConcurrentMap();

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof ClientboundPlayerInfoUpdatePacket pac) {
            if (pac.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                for (ClientboundPlayerInfoUpdatePacket.Entry ple : pac.newEntries()) {
                    for (UUID uuid : logoutCache.keySet()) {
                        if (!uuid.equals(ple.profile().id())) continue;
                        Player pl = logoutCache.get(uuid);
                        if (ignoreBots.getValue() && isABot(pl)) continue;
                        if (notifications.getValue())
                            sendMessage(pl.getName().getString() + " logged back at  X: " + (int) pl.getX() + " Y: " + (int) pl.getY() + " Z: " + (int) pl.getZ());
                        logoutCache.remove(uuid);
                    }
                }
            }
            playerCache.clear();
        }

        if (e.getPacket() instanceof ClientboundPlayerInfoRemovePacket pac) {
            for (UUID uuid2 : pac.profileIds) {
                for (UUID uuid : playerCache.keySet()) {
                    if (!uuid.equals(uuid2)) continue;
                    final Player pl = playerCache.get(uuid);
                    if (ignoreBots.getValue() && isABot(pl)) continue;
                    if (pl != null) {
                        if (notifications.getValue())
                            sendMessage(pl.getName().getString() + " logged out at  X: " + (int) pl.getX() + " Y: " + (int) pl.getY() + " Z: " + (int) pl.getZ());
                        if (!logoutCache.containsKey(uuid))
                            logoutCache.put(uuid, pl);
                    }
                }
            }
            playerCache.clear();
        }
    }

    @Override
    public void onEnable() {
        playerCache.clear();
        logoutCache.clear();
    }

    @Override
    public void onUpdate() {
        for (Player player : mc.level.players()) {
            if (player == null || player.equals(mc.player)) continue;
            playerCache.put(player.getGameProfile().id(), player);
        }
    }

    public void onRender3D(PoseStack s) {
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        if (renderMode.is(RenderMode.Box)) RenderSystem.defaultBlendFunc();
        else RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        for (UUID uuid : logoutCache.keySet()) {
            final Player data = logoutCache.get(uuid);
            if (data != null) {
                if (renderMode.is(RenderMode.Box)) {
                    Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(data.getBoundingBox(), color.getValue().getColorObject(), 2));
                } else {
                    PlayerModel modelPlayer = new PlayerModel(new EntityRendererProvider.Context(
                            mc.getEntityRenderDispatcher(), new net.minecraft.client.renderer.block.BlockModelResolver(mc.getModelManager()),
                            mc.getItemModelResolver(), mc.getMapRenderer(), mc.getResourceManager(), mc.getEntityModels(),
                            ((IEntityRenderDispatcher) mc.getEntityRenderDispatcher()).getEquipmentModelLoader(), mc.getAtlasManager(), mc.font, mc.playerSkinRenderCache()).bakeLayer(ModelLayers.PLAYER), false);
                    modelPlayer.getHead().offsetScale(new Vector3f(-0.3f, -0.3f, -0.3f));

                    renderEntity(s, data, modelPlayer, ((RemotePlayer)data).getSkin().body().texturePath(), color.getValue().getAlpha());
                }
            }
        }
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public void onRender2D(GuiGraphicsExtractor context) {
        for (UUID uuid : logoutCache.keySet()) {
            final Player data = logoutCache.get(uuid);
            if (data != null) {
                Vec3 vector = new Vec3(data.getX(), data.getY() + 2, data.getZ());
                Vector4d position = null;

                vector = Render3DEngine.worldSpaceToScreenSpace(new Vec3(vector.x, vector.y, vector.z));
                if (vector.z > 0 && vector.z < 1) {
                    position = new Vector4d(vector.x, vector.y, vector.z, 0);
                    position.x = Math.min(vector.x, position.x);
                    position.y = Math.min(vector.y, position.y);
                    position.z = Math.max(vector.x, position.z);
                }

                String string = data.getName().getString() + " " + String.format("%.1f", (data.getHealth() + data.getAbsorptionAmount())) + " X: " + (int) data.getX() + " " + " Z: " + (int) data.getZ();

                if (position != null) {
                    float diff = (float) (position.z - position.x) / 2;
                    float textWidth = (FontRenderers.sf_bold.getStringWidth(string) * 1);
                    float tagX = (float) ((position.x + diff - textWidth / 2) * 1);

                    Render2DEngine.drawRect(context.pose(), tagX - 2, (float) (position.y - 13f), textWidth + 4, 11, new Color(0x99000001, true));
                    FontRenderers.sf_bold.drawString(context.pose(), string, tagX, (float) position.y - 10, -1);
                }
            }
        }
    }

    private void renderEntity(@NotNull PoseStack matrices, @NotNull LivingEntity entity, @NotNull PlayerModel modelBase, Identifier texture, int alpha) {
        modelBase.leftPants.visible = true;
        modelBase.rightPants.visible = true;
        modelBase.leftSleeve.visible = true;
        modelBase.rightSleeve.visible = true;
        modelBase.jacket.visible = true;
        modelBase.hat.visible = true;

        double x = entity.getX() - mc.getEntityRenderDispatcher().camera.position().x;
        double y = entity.getY() - mc.getEntityRenderDispatcher().camera.position().y;
        double z = entity.getZ() - mc.getEntityRenderDispatcher().camera.position().z;
        ((IEntity) entity).setPos(entity.position());
        matrices.pushPose();
        matrices.translate((float) x, (float) y, (float) z);
        matrices.mulPose(Axis.YP.rotation(MathUtility.rad(180 - entity.yBodyRot)));
        prepareScale(matrices);
        @SuppressWarnings("unchecked")
        AvatarRenderState renderState = ((EntityRenderer<Player, AvatarRenderState>) mc.getEntityRenderDispatcher()
                .getRenderer((Player) entity))
                .createRenderState((Player) entity, Render3DEngine.getTickDelta());
        modelBase.setupAnim(renderState);
        BufferBuilder buffer;
        if (renderMode.is(RenderMode.TexturedChams)) {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
            buffer = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX);
        } else {
            RenderSystem.setShader(ShaderProgramKeys.POSITION);
            buffer = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION);
        }
        RenderSystem.setShaderColor(color.getValue().getGlRed(), color.getValue().getGlGreen(), color.getValue().getGlBlue(), alpha / 255f);
        modelBase.renderToBuffer(matrices, buffer, 10, 0, -1);
        Render2DEngine.endBuilding(buffer);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        matrices.popPose();
    }

    private static void prepareScale(@NotNull PoseStack matrixStack) {
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        matrixStack.scale(1.6f, 1.8f, 1.6f);
        matrixStack.translate(0.0F, -1.501F, 0.0F);
    }

    private boolean isABot(Player ent) {
        return !ent.getUUID().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + ent.getName().getString()).getBytes(StandardCharsets.UTF_8))) && ent instanceof RemotePlayer
                && (FakePlayer.fakePlayer == null || ent.getId() != FakePlayer.fakePlayer.getId())
                && !ent.getName().getString().contains("-");
    }

    private enum RenderMode {
        Chams, TexturedChams, Box
    }
}
