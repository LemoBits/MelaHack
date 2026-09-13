package thunder.hack.features.modules.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import thunder.hack.utility.render.compat.RenderSystem;
import net.minecraft.client.Camera;
import thunder.hack.utility.render.ShaderProgramKeys;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import thunder.hack.utility.render.BufferRenderer;
import net.minecraft.core.*;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import thunder.hack.core.Managers;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.injection.accesors.IAreaEffectCloudEntity;
import thunder.hack.injection.accesors.IBeaconBlockEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;

import static thunder.hack.utility.render.animation.AnimationUtility.fast;

public class ESP extends Module {
    public ESP() {
        super("ESP", Category.RENDER);
    }

    private final Setting<Boolean> lingeringPotions = new Setting<>("LingeringPotions", false);
    private final Setting<Boolean> tntFuse = new Setting<>("TNTFuse", false);
    private final Setting<Float> tntrange = new Setting<>("TNTRange", 8.0f, 0f, 8f);
    private final Setting<ColorSetting> tntFuseText = new Setting<>("TNTFuseText", new ColorSetting(new Color(-1)), v -> tntFuse.getValue());
    private final Setting<Boolean> tntRadius = new Setting<>("TNTRadius", false);
    private final Setting<ColorSetting> tntRadiusColor = new Setting<>("TNTSphereColor", new ColorSetting(new Color(-1)), v -> tntRadius.getValue());
    private final Setting<Boolean> beaconRadius = new Setting<>("BeaconRadius", false);
    private final Setting<Boolean> keepY = new Setting<>("KeepY", false, v -> beaconRadius.getValue());
    private final Setting<ColorSetting> sphereColor = new Setting<>("SphereColor", new ColorSetting(new Color(-1)), v -> beaconRadius.getValue());
    private final Setting<ColorSetting> beakonColor = new Setting<>("BeakonColor", new ColorSetting(new Color(-1)), v -> beaconRadius.getValue());
    private final Setting<Boolean> burrow = new Setting<>("Burrow", false);
    private final Setting<ColorSetting> burrowTextColor = new Setting<>("BurrowTextColor", new ColorSetting(new Color(-1)), v -> burrow.getValue());
    private final Setting<ColorSetting> burrowColor = new Setting<>("BurrowColor", new ColorSetting(new Color(-1)), v -> burrow.getValue());
    private final Setting<Boolean> pearls = new Setting<>("Pearls", false);
    private final Setting<Boolean> dizorentRadius = new Setting<>("DizorentRadius", true);
    private final Setting<ColorSetting> dizorentColor = new Setting<>("DizorentColor", new ColorSetting(new Color(0xB300F1CC, true)), v -> dizorentRadius.getValue());

    private final Setting<SettingGroup> boxEsp = new Setting<>("Box", new SettingGroup(false, 0));
    private final Setting<Boolean> players = new Setting<>("Players", true).addToGroup(boxEsp);
    private final Setting<Boolean> friends = new Setting<>("Friends", true).addToGroup(boxEsp);
    private final Setting<Boolean> crystals = new Setting<>("Crystals", true).addToGroup(boxEsp);
    private final Setting<Boolean> creatures = new Setting<>("Creatures", false).addToGroup(boxEsp);
    private final Setting<Boolean> monsters = new Setting<>("Monsters", false).addToGroup(boxEsp);
    private final Setting<Boolean> ambients = new Setting<>("Ambients", false).addToGroup(boxEsp);
    private final Setting<Boolean> others = new Setting<>("Others", false).addToGroup(boxEsp);
    private final Setting<Boolean> outline = new Setting<>("Outline", true).addToGroup(boxEsp);
    private final Setting<Colors> colorMode = new Setting<>("ColorMode", Colors.SyncColor).addToGroup(boxEsp);
    private final Setting<Boolean> renderHealth = new Setting<>("renderHealth", true).addToGroup(boxEsp);
    private final Setting<Boolean> healthOutline = new Setting<>("healthOutline", true).addToGroup(boxEsp);

    private final Setting<SettingGroup> boxColors = new Setting<>("BoxColors", new SettingGroup(false, 0));
    private final Setting<ColorSetting> boxOutline = new Setting<>("BoxOutline", new ColorSetting(new Color(0x000000))).addToGroup(boxColors);
    private final Setting<ColorSetting> playersC = new Setting<>("PlayersC", new ColorSetting(new Color(0xFF9200))).addToGroup(boxColors);
    private final Setting<ColorSetting> friendsC = new Setting<>("FriendsC", new ColorSetting(new Color(0x30FF00))).addToGroup(boxColors);
    private final Setting<ColorSetting> crystalsC = new Setting<>("CrystalsC", new ColorSetting(new Color(0x00BBFF))).addToGroup(boxColors);
    private final Setting<ColorSetting> creaturesC = new Setting<>("CreaturesC", new ColorSetting(new Color(0xA0A4A6))).addToGroup(boxColors);
    private final Setting<ColorSetting> monstersC = new Setting<>("MonstersC", new ColorSetting(new Color(0xFF0000))).addToGroup(boxColors);
    private final Setting<ColorSetting> ambientsC = new Setting<>("AmbientsC", new ColorSetting(new Color(0x7B00FF))).addToGroup(boxColors);
    private final Setting<ColorSetting> othersC = new Setting<>("OthersC", new ColorSetting(new Color(0xFF0062))).addToGroup(boxColors);
    public final Setting<ColorSetting> healthB = new Setting<>("healthB", new ColorSetting(new Color(0xff1100))).addToGroup(boxColors);
    public final Setting<ColorSetting> healthU = new Setting<>("healthU", new ColorSetting(new Color(0x2fff00))).addToGroup(boxColors);
    private final Setting<ColorSetting> healthbg = new Setting<>("healthBG", new ColorSetting(new Color(0x000000))).addToGroup(boxColors);
    private final Setting<ColorSetting> healthOutlineC = new Setting<>("healthOutlineC", new ColorSetting(new Color(0x000000))).addToGroup(boxColors);

    private float dizorentAnimation = 0f;

    public void onRender3D(PoseStack stack) {
        if(mc.options.hideGui) return;
        if (lingeringPotions.getValue()) {
            for (Entity ent : mc.level.entitiesForRendering()) {
                if (ent instanceof AreaEffectCloud aece) {
                    double x = aece.getX() - mc.getEntityRenderDispatcher().camera.position().x;
                    double y = aece.getY() - mc.getEntityRenderDispatcher().camera.position().y;
                    double z = aece.getZ() - mc.getEntityRenderDispatcher().camera.position().z;

                    float middle = aece.getRadius();

                    stack.pushPose();
                    stack.translate(x, y, z);

                    Render3DEngine.setupRender();
                    RenderSystem.disableDepthTest();
                    RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                    BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

                    for (int i = 0; i <= 360; i += 6) {
                        double v = Math.sin(Math.toRadians(i));
                        double u = Math.cos(Math.toRadians(i));
                        bufferBuilder.addVertex(stack.last().pose(), (float) u * middle, (float) 0, (float) v * middle).setColor(Render2DEngine.injectAlpha(new Color(getAreaCloudColor(aece)), 100).getRGB());
                        bufferBuilder.addVertex(stack.last().pose(), 0, 0, 0).setColor(Render2DEngine.injectAlpha(new Color(getAreaCloudColor(aece)), 0).getRGB());
                    }
                    Render2DEngine.endBuilding(bufferBuilder);

                    RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                    bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                    for (int i = 0; i <= 360; i += 6) {
                        double v = Math.sin(Math.toRadians(i));
                        double u = Math.cos(Math.toRadians(i));
                        bufferBuilder.addVertex(stack.last().pose(), (float) u * middle, (float) 0, (float) v * middle).setColor(Render2DEngine.injectAlpha(new Color(getAreaCloudColor(aece)), 255).getRGB());
                        bufferBuilder.addVertex(stack.last().pose(), (float) u * (middle - 0.04f), (float) 0, (float) v * (middle - 0.04f)).setColor(Render2DEngine.injectAlpha(new Color(getAreaCloudColor(aece)), 255).getRGB());
                    }
                    Render2DEngine.endBuilding(bufferBuilder);

                    Render3DEngine.endRender();
                    RenderSystem.enableDepthTest();
                    stack.translate(-x, -y, -z);
                    stack.popPose();

                    RenderSystem.disableDepthTest();
                    PoseStack matrices = new PoseStack();
                    Camera camera = mc.gameRenderer.getMainCamera();
                    matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
                    matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
                    matrices.translate(x, y, z);
                    matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
                    matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    matrices.translate(0, 0, 0);
                    matrices.scale(-0.05f, -0.05f, 0);
                    FontRenderers.modules.drawCenteredString(matrices, String.format("%.1f", ((aece.getRadius() * 10) - 5f)), 0, -10f, -1);
                    RenderSystem.disableBlend();
                    RenderSystem.enableDepthTest();
                }
            }
        }

        if (dizorentRadius.getValue()) {
            dizorentAnimation = fast(dizorentAnimation, mc.player.getMainHandItem().getItem() == Items.ENDER_EYE ? 10 : 0, 15f);

            if (mc.player.getMainHandItem().getItem() == Items.ENDER_EYE) {
                double x = Render2DEngine.interpolate(mc.player.xo, mc.player.getX(), Render3DEngine.getTickDelta()) - mc.getEntityRenderDispatcher().camera.position().x;
                double y = Render2DEngine.interpolate(mc.player.yo, mc.player.getY(), Render3DEngine.getTickDelta()) - mc.getEntityRenderDispatcher().camera.position().y;
                double z = Render2DEngine.interpolate(mc.player.zo, mc.player.getZ(), Render3DEngine.getTickDelta()) - mc.getEntityRenderDispatcher().camera.position().z;


                stack.pushPose();
                stack.translate(x, y, z);

                Render3DEngine.setupRender();
                RenderSystem.disableDepthTest();
                RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

                for (int i = 0; i <= 360; i += 6) {
                    double v = Math.sin(Math.toRadians(i));
                    double u = Math.cos(Math.toRadians(i));
                    bufferBuilder.addVertex(stack.last().pose(), (float) u * dizorentAnimation, (float) 0, (float) v * dizorentAnimation).setColor(Render2DEngine.injectAlpha(new Color(dizorentColor.getValue().getColor()), 100).getRGB());
                    bufferBuilder.addVertex(stack.last().pose(), 0, 0, 0).setColor(Render2DEngine.injectAlpha(new Color(dizorentColor.getValue().getColor()), 0).getRGB());
                }
                Render2DEngine.endBuilding(bufferBuilder);

                RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                for (int i = 0; i <= 360; i += 6) {
                    double v = Math.sin(Math.toRadians(i));
                    double u = Math.cos(Math.toRadians(i));
                    bufferBuilder.addVertex(stack.last().pose(), (float) u * dizorentAnimation, (float) 0, (float) v * dizorentAnimation).setColor(Render2DEngine.injectAlpha(new Color(dizorentColor.getValue().getColor()), 255).getRGB());
                    bufferBuilder.addVertex(stack.last().pose(), (float) u * (dizorentAnimation - 0.04f), (float) 0, (float) v * (dizorentAnimation - 0.04f)).setColor(Render2DEngine.injectAlpha(new Color(dizorentColor.getValue().getColor()), 255).getRGB());
                }
                Render2DEngine.endBuilding(bufferBuilder);

                Render3DEngine.endRender();
                RenderSystem.enableDepthTest();
                stack.translate(-x, -y, -z);
                stack.popPose();

                for (Player pl : Managers.ASYNC.getAsyncPlayers()) {
                    if (mc.player.distanceToSqr(pl.position()) > 100 || pl == mc.player)
                        continue;
                    Render3DEngine.drawTargetEsp(stack, pl);
                }
            }
        }

        if (beaconRadius.getValue()) {
            for (BlockEntity be : StorageEsp.getBlockEntities()) {
                if (be instanceof BeaconBlockEntity bbe) {
                    double x = be.getBlockPos().getX() - mc.getEntityRenderDispatcher().camera.position().x;
                    double y = be.getBlockPos().getY() - mc.getEntityRenderDispatcher().camera.position().y;
                    double z = be.getBlockPos().getZ() - mc.getEntityRenderDispatcher().camera.position().z;

                    Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(new AABB(be.getBlockPos()), beakonColor.getValue().getColorObject(), 2));
                    float range = ((IBeaconBlockEntity) bbe).getLevel() * 10 + 11;

                    boolean ky = keepY.getValue();

                    stack.pushPose();
                    stack.translate(x,  ky ? -10 : y, z);
                    Render3DEngine.drawCylinder(stack, range, ky ? 20 : 256, 20, ky ? 5 : 20, sphereColor.getValue().getColor());
                    stack.translate(-x, ky ? 10 : y, -z);
                    stack.popPose();
                }
            }
        }

        if (burrow.getValue()) {
            for (Player pl : mc.level.players()) {
                BlockPos blockPos = BlockPos.containing(pl.position().add(0,0.15f,0));
                Block block = mc.level.getBlockState(blockPos).getBlock();

                double x = blockPos.getX() - mc.getEntityRenderDispatcher().camera.position().x;
                double y = blockPos.getY() - mc.getEntityRenderDispatcher().camera.position().y;
                double z = blockPos.getZ() - mc.getEntityRenderDispatcher().camera.position().z;

                if (block == Blocks.OBSIDIAN
                        || block == Blocks.CRYING_OBSIDIAN
                        || block == Blocks.ANVIL
                        || block == Blocks.PLAYER_HEAD
                        || block == Blocks.SKELETON_SKULL
                        || block == Blocks.WITHER_SKELETON_SKULL) {
                    Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(new AABB(blockPos), burrowColor.getValue().getColorObject(), 2));
                    RenderSystem.disableDepthTest();
                    PoseStack matrices = new PoseStack();
                    Camera camera = mc.gameRenderer.getMainCamera();
                    matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
                    matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
                    matrices.translate(x + 0.5f, y + 0.5f, z + 0.5f);
                    matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
                    matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    matrices.translate(0, 0, 0);
                    matrices.scale(-0.025f, -0.025f, 0);
                    FontRenderers.modules.drawCenteredString(matrices, "BURROW", 0, -5, burrowTextColor.getValue().getColor());
                    RenderSystem.disableBlend();
                    RenderSystem.enableDepthTest();
                }
            }
        }

        if (tntFuse.getValue() || tntRadius.getValue()) {
            for (Entity ent : mc.level.entitiesForRendering()) {
                if (ent instanceof PrimedTnt tnt) {
                    double x = tnt.xo + (tnt.position().x - tnt.xo) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.position().x;
                    double y = tnt.yo + (tnt.position().y - tnt.yo) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.position().y;
                    double z = tnt.zo + (tnt.position().z - tnt.zo) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.position().z;

                    if (tntFuse.getValue()) {
                        RenderSystem.disableDepthTest();
                        PoseStack matrices = new PoseStack();
                        Camera camera = mc.gameRenderer.getMainCamera();
                        matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
                        matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
                        matrices.translate(x, y + 0.5f, z);
                        matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
                        matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        matrices.translate(0, 0, 0);
                        matrices.scale(-0.025f, -0.025f, 0);
                        FontRenderers.modules.drawCenteredString(matrices, String.format("%.1f", ((float) tnt.getFuse() / 20f)) + "s", 0, -5, tntFuseText.getValue().getColor());
                        RenderSystem.disableBlend();
                        RenderSystem.enableDepthTest();
                    }

                    if (tntRadius.getValue()) {
                        stack.pushPose();
                        stack.translate(x, y, z);
                        Render3DEngine.drawSphere(stack, tntrange.getValue(), 20, 20, tntRadiusColor.getValue().getColor());
                        stack.translate(-x, -y, -z);
                        stack.popPose();
                    }
                }
            }
        }
    }

    public void onRender2D(GuiGraphicsExtractor context) {
        if(mc.options.hideGui) return;
        if (pearls.getValue()) {
            for (Entity ent : mc.level.entitiesForRendering()) {
                if (ent instanceof ThrownEnderpearl pearl) {
                    float xOffset = mc.getWindow().getGuiScaledWidth() / 2f;
                    float yOffset = mc.getWindow().getGuiScaledHeight() / 2f;

                    float xPos = (float) (pearl.xo + (pearl.position().x - pearl.xo) * Render3DEngine.getTickDelta());
                    float zPos = (float) (pearl.zo + (pearl.position().z - pearl.zo) * Render3DEngine.getTickDelta());

                    float yaw = getRotations(new Vec2(xPos, zPos)) - mc.player.getYRot();
                    context.pose().translate((float) (xOffset), (float) (yOffset));
                    context.pose().rotate((float) Math.toRadians(yaw));
                    context.pose().translate((float) (-xOffset), (float) (-yOffset));
                    Render2DEngine.drawTracerPointer(context.pose(), xOffset, yOffset - 50, 12.5f, 0.5f, 3.63f, true, true, HudEditor.getColor(1).getRGB());
                    context.pose().translate((float) (xOffset), (float) (yOffset));
                    context.pose().rotate((float) Math.toRadians(-yaw));
                    context.pose().translate((float) (-xOffset), (float) (-yOffset));
                    RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
                    FontRenderers.modules.drawCenteredString(context.pose(), String.format("%.1f", mc.player.distanceTo(pearl)) + "m", (float) (Math.sin(Math.toRadians(yaw)) * 50f) + xOffset, (float) (yOffset - (Math.cos(Math.toRadians(yaw)) * 50f)) - 20, -1);
                }
            }
        }

        Matrix4f matrix = thunder.hack.utility.render.GuiMatrix.positionMatrix(context.pose());
        Render2DEngine.setupRender();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Entity ent : mc.level.entitiesForRendering())
            if (shouldRender(ent))
                drawBox(bufferBuilder, ent, matrix);

        Render2DEngine.endBuilding(bufferBuilder);
        Render2DEngine.endRender();
    }

    public boolean shouldRender(Entity entity) {
        if (entity == null)
            return false;

        if (mc.player == null)
            return false;

        if (entity instanceof Player) {
            if (entity == mc.player)
                return false;
            if (Managers.FRIEND.isFriend((Player) entity))
                return friends.getValue();
            return players.getValue();
        }

        if (entity instanceof EndCrystal)
            return crystals.getValue();

        return switch (entity.getType().getCategory()) {
            case CREATURE, WATER_CREATURE -> creatures.getValue();
            case MONSTER -> monsters.getValue();
            case AMBIENT, WATER_AMBIENT -> ambients.getValue();
            default -> others.getValue();
        };
    }

    public Color getEntityColor(Entity entity) {
        if (entity == null)
            return new Color(-1);

        if (entity instanceof Player) {
            if (Managers.FRIEND.isFriend((Player) entity))
                return friendsC.getValue().getColorObject();
            return playersC.getValue().getColorObject();
        }

        if (entity instanceof EndCrystal)
            return crystalsC.getValue().getColorObject();

        return switch (entity.getType().getCategory()) {
            case CREATURE, WATER_CREATURE -> creaturesC.getValue().getColorObject();
            case MONSTER -> monstersC.getValue().getColorObject();
            case AMBIENT, WATER_AMBIENT -> ambientsC.getValue().getColorObject();
            default -> othersC.getValue().getColorObject();
        };
    }

    public void drawBox(BufferBuilder bufferBuilder, @NotNull Entity ent, Matrix4f matrix) {
        Vec3[] vectors = getVectors(ent);

        Color col = getEntityColor(ent);

        Vector4d position = null;
        for (Vec3 vector : vectors) {
            vector = Render3DEngine.worldSpaceToScreenSpace(new Vec3(vector.x, vector.y, vector.z));
            if (vector.z > 0 && vector.z < 1) {
                if (position == null) position = new Vector4d(vector.x, vector.y, vector.z, 0);
                position.x = Math.min(vector.x, position.x);
                position.y = Math.min(vector.y, position.y);
                position.z = Math.max(vector.x, position.z);
                position.w = Math.max(vector.y, position.w);
            }
        }


        if (position != null) {
            double posX = position.x;
            double posY = position.y;
            double endPosX = position.z;
            double endPosY = position.w;

            if (outline.getValue()) {
                // ебаный пиздец
                Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 1F), (float) posY, (float) (posX + 0.5), (float) (endPosY + 0.5), boxOutline.getValue().getColorObject(), boxOutline.getValue().getColorObject(), boxOutline.getValue().getColorObject(), boxOutline.getValue().getColorObject());
                Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 1F), (float) (posY - 0.5), (float) (endPosX + 0.5), (float) (posY + 0.5 + 0.5), boxOutline.getValue().getColorObject(), boxOutline.getValue().getColorObject(), boxOutline.getValue().getColorObject(), boxOutline.getValue().getColorObject());
                Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (endPosX - 0.5 - 0.5), (float) posY, (float) (endPosX + 0.5), (float) (endPosY + 0.5), boxOutline.getValue().getColorObject(), boxOutline.getValue().getColorObject(), boxOutline.getValue().getColorObject(), boxOutline.getValue().getColorObject());
                Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 1), (float) (endPosY - 0.5 - 0.5), (float) (endPosX + 0.5), (float) (endPosY + 0.5), boxOutline.getValue().getColorObject(), boxOutline.getValue().getColorObject(), boxOutline.getValue().getColorObject(), boxOutline.getValue().getColorObject());
            }

            switch (colorMode.getValue()) {
                case Custom -> {
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 0.5f), (float) posY, (float) (posX + 0.5 - 0.5), (float) endPosY, col, col, col, col);
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) posX, (float) (endPosY - 0.5f), (float) endPosX, (float) endPosY, col, col, col, col);
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 0.5), (float) posY, (float) endPosX, (float) (posY + 0.5), col, col, col, col);
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (endPosX - 0.5), (float) posY, (float) endPosX, (float) endPosY, col, col, col, col);
                }
                case SyncColor -> {
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 0.5f), (float) posY, (float) (posX + 0.5 - 0.5), (float) endPosY, HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(0), HudEditor.getColor(270));
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) posX, (float) (endPosY - 0.5f), (float) endPosX, (float) endPosY, HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(180), HudEditor.getColor(0));
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 0.5), (float) posY, (float) endPosX, (float) (posY + 0.5), HudEditor.getColor(180), HudEditor.getColor(90), HudEditor.getColor(90), HudEditor.getColor(180));
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (endPosX - 0.5), (float) posY, (float) endPosX, (float) endPosY, HudEditor.getColor(90), HudEditor.getColor(270), HudEditor.getColor(270), HudEditor.getColor(90));
                }
            }


            if (ent instanceof LivingEntity lent && lent.getHealth() != 0 && renderHealth.getValue()) {

                if (healthOutline.getValue())
                    Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 5.5), (float) (posY - 0.5), (float) (posX - 2.5), (float) (endPosY + 0.5), healthOutlineC.getValue().getColorObject(), healthOutlineC.getValue().getColorObject(), healthOutlineC.getValue().getColorObject(), healthOutlineC.getValue().getColorObject());

                Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 5), (float) posY, (float) posX - 3, (float) endPosY, healthbg.getValue().getColorObject(), healthbg.getValue().getColorObject(), healthbg.getValue().getColorObject(), healthbg.getValue().getColorObject());

                switch (colorMode.getValue()) {
                    case Custom ->
                            Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 5), (float) (endPosY + (posY - endPosY) * lent.getHealth() / lent.getMaxHealth()), (float) posX - 3, (float) endPosY, healthB.getValue().getColorObject(), healthB.getValue().getColorObject(), healthU.getValue().getColorObject(), healthU.getValue().getColorObject());
                    case SyncColor ->
                            Render2DEngine.setRectPoints(bufferBuilder, matrix, (float) (posX - 5), (float) (endPosY + (posY - endPosY) * lent.getHealth() / lent.getMaxHealth()), (float) posX - 3, (float) endPosY, HudEditor.getColor(90), HudEditor.getColor(90), HudEditor.getColor(270), HudEditor.getColor(270));
                }
            }
        }
    }

    @NotNull
    private static Vec3[] getVectors(@NotNull Entity ent) {
        double x = ent.xo + (ent.getX() - ent.xo) * Render3DEngine.getTickDelta();
        double y = ent.yo + (ent.getY() - ent.yo) * Render3DEngine.getTickDelta();
        double z = ent.zo + (ent.getZ() - ent.zo) * Render3DEngine.getTickDelta();
        AABB axisAlignedBB2 = ent.getBoundingBox();
        AABB axisAlignedBB = new AABB(axisAlignedBB2.minX - ent.getX() + x - 0.05, axisAlignedBB2.minY - ent.getY() + y, axisAlignedBB2.minZ - ent.getZ() + z - 0.05, axisAlignedBB2.maxX - ent.getX() + x + 0.05, axisAlignedBB2.maxY - ent.getY() + y + 0.15, axisAlignedBB2.maxZ - ent.getZ() + z + 0.05);
        return new Vec3[]{new Vec3(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vec3(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)};
    }

    private int getAreaCloudColor(AreaEffectCloud ent) {
        ParticleOptions particleEffect = ent.getParticle();
        if (particleEffect instanceof ColorParticleOption effect) {
            return ((IAreaEffectCloudEntity)ent).getPotionContentsComponent().getColor();
        }
        return -1;
    }

    public static float getRotations(Vec2 vec) {
        if (mc.player == null) return 0;
        double x = vec.x - mc.player.position().x;
        double z = vec.y - mc.player.position().z;
        return (float) -(Math.atan2(x, z) * (180 / Math.PI));
    }

    public enum Colors {
        SyncColor, Custom
    }
}
