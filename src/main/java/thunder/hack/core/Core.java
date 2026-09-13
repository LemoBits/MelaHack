package thunder.hack.core;

import thunder.hack.utility.render.compat.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.features.cmd.Command;
import thunder.hack.core.manager.client.MacroManager;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.gui.notification.Notification;
import thunder.hack.gui.thundergui.ThunderGui;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.TextureStorage;
import thunder.hack.utility.render.animation.CaptureMark;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static thunder.hack.features.modules.Module.fullNullCheck;
import static thunder.hack.features.modules.Module.mc;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public final class Core {
    public static boolean lockSprint, serverSprint, hold_mouse0, showSkull;
    public static final Map<String, Identifier> HEADS = new ConcurrentHashMap<>();
    public ArrayList<Packet<?>> silentPackets = new ArrayList<>();
    private final Timer skullTimer = new Timer();
    private final Timer lastPacket = new Timer();
    private final Timer autoSave = new Timer();
    private final Timer setBackTimer = new Timer();
    private float prevHorizontalSpeed;
    private float horizontalSpeed;

    @EventHandler
    @SuppressWarnings("unused")
    public void onTick(PlayerUpdateEvent event) {
        if (fullNullCheck()) return;

        Managers.NOTIFICATION.onUpdate();
        Managers.MODULE.onUpdate();
        ThunderGui.getInstance().onTick();

        if (ModuleManager.clickGui.getBind().getKey() == -1) {
            Command.sendMessage(ChatFormatting.RED + (isRu() ? "Привязка клавиш Clickgui по умолчанию -> P" : "Default clickgui keybind --> P"));
            Command.sendMessage(ChatFormatting.RED + (isRu() ? "Вы можете получить готовую конфигурацию, выполнив следующую команду -> @cfg cloudlist." : "You can obtain a pre-built configuration by executing the following command -> @cfg cloudlist."));
            ModuleManager.clickGui.setBind(InputConstants.getKey("key.keyboard.p").getValue(), false, false);
        }

        for (Player p : mc.level.players()) {
            if (p.isDeadOrDying() || p.getHealth() == 0)
                ThunderHack.EVENT_BUS.post(new EventDeath(p));
        }

        if (!Objects.equals(Managers.COMMAND.getPrefix(), ClientSettings.prefix.getValue()))
            Managers.COMMAND.setPrefix(ClientSettings.prefix.getValue());

        new HashMap<>(InteractionUtility.awaiting).forEach((bp, time) -> {
            if (System.currentTimeMillis() - time > Managers.SERVER.getPing() * 2f)
                InteractionUtility.awaiting.remove(bp);
        });

        if (autoSave.every(600000)) {
            Managers.FRIEND.saveFriends();
            Managers.CONFIG.save(Managers.CONFIG.getCurrentConfig());
            Managers.WAYPOINT.saveWayPoints();
            Managers.MACRO.saveMacro();
            Managers.NOTIFICATION.publicity("AutoSave", isRu() ? "Сохраняю конфиг.." : "Saving config..", 3, Notification.Type.INFO);
        }

        prevHorizontalSpeed = horizontalSpeed;
        horizontalSpeed = (float) mc.player.getDeltaMovement().horizontalDistance();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.@NotNull Send e) {
        if (e.getPacket() instanceof ServerboundMovePlayerPacket && !(e.getPacket() instanceof ServerboundMovePlayerPacket.StatusOnly))
            lastPacket.reset();

        if (e.getPacket() instanceof ServerboundPlayerCommandPacket c) {
            if (c.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING || c.getAction() == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING) {
                if (lockSprint) {
                    e.cancel();
                    return;
                }

                switch (c.getAction()) {
                    case START_SPRINTING -> serverSprint = true;
                    case STOP_SPRINTING -> serverSprint = false;
                }
            }
        }
    }

    @EventHandler
    public void onSync(EventSync event) {
        if (fullNullCheck()) return;
        ModuleManager.timer.onEntitySync();
        CaptureMark.tick();
        Render3DEngine.updateTargetESP();
    }

    public void onRender2D(GuiGraphicsExtractor e) {
        drawGps(e);
        drawSkull(e);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (fullNullCheck()) return;

        if (e.getPacket() instanceof ClientboundSystemChatPacket) {
            final ClientboundSystemChatPacket packet = e.getPacket();
            if (packet.content().getString().contains("skull")) {
                showSkull = true;
                skullTimer.reset();
                mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.SKELETON_DEATH, SoundSource.BLOCKS, 1f, 1f);
            }
        }

        if (e.getPacket() instanceof ClientboundLoginPacket)
            Managers.MODULE.onLogin();

        if (e.getPacket() instanceof ClientboundPlayerPositionPacket) {
            setBackTimer.reset();
        }
    }

    /*
    @EventHandler
    @SuppressWarnings("unused")
    public void onEntitySpawn(EventEntitySpawn e) {
        new ArrayList<>(InteractionUtility.awaiting.keySet()).forEach(bp -> {
            if (e.getEntity() != null && bp.getSquaredDistance(e.getEntity().getPos()) < 4.)
                InteractionUtility.awaiting.remove(bp);
        });
    }*/

    public void drawSkull(GuiGraphicsExtractor e) {
        if (showSkull && !skullTimer.passedMs(3000) && ClientSettings.skullEmoji.getValue()) {
            int xPos = (int) (mc.getWindow().getGuiScaledWidth() / 2f - 150);
            int yPos = (int) (mc.getWindow().getGuiScaledHeight() / 2f - 150);
            float alpha = (1f - (skullTimer.getPassedTimeMs() / 3000f));
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
            e.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TextureStorage.skull, xPos, yPos, 0, 0, 300, 300, 300, 300);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        } else showSkull = false;
    }

    public void drawGps(GuiGraphicsExtractor e) {
        if (ThunderHack.gps_position != null) {
            float dst = getDistance(ThunderHack.gps_position);
            float xOffset = mc.getWindow().getGuiScaledWidth() / 2f;
            float yOffset = mc.getWindow().getGuiScaledHeight() / 2f;
            float yaw = getRotations(new Vec2(ThunderHack.gps_position.getX(), ThunderHack.gps_position.getZ())) - mc.player.getYRot();
            e.pose().translate((float) (xOffset), (float) (yOffset));
            e.pose().rotate((float) Math.toRadians(yaw));
            e.pose().translate((float) (-xOffset), (float) (-yOffset));
            Render2DEngine.drawTracerPointer(e.pose(), xOffset, yOffset - 50, 12.5f, 0.5f, 3.63f, true, true, HudEditor.getColor(1).getRGB());
            e.pose().translate((float) (xOffset), (float) (yOffset));
            e.pose().rotate((float) Math.toRadians(-yaw));
            e.pose().translate((float) (-xOffset), (float) (-yOffset));
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            FontRenderers.modules.drawCenteredString(e.pose(), "gps (" + dst + "m)", (float) (Math.sin(Math.toRadians(yaw)) * 50f) + xOffset, (float) (yOffset - (Math.cos(Math.toRadians(yaw)) * 50f)) - 23, -1);

            if (dst < 10)
                ThunderHack.gps_position = null;
        }
    }

    @EventHandler
    public void onKeyPress(EventKeyPress event) {
        if (event.getKey() == -1) return;
        for (MacroManager.Macro m : Managers.MACRO.getMacros())
            if (m.getBind() == event.getKey())
                m.runMacro();
    }

    @EventHandler
    public void onMouse(EventMouse event) {
        if (event.getAction() == 0) hold_mouse0 = false;
        if (event.getAction() == 1) hold_mouse0 = true;
    }

    public int getDistance(BlockPos bp) {
        double d0 = mc.player.getX() - bp.getX();
        double d2 = mc.player.getZ() - bp.getZ();
        return (int) (Mth.sqrt((float) (d0 * d0 + d2 * d2)));
    }

    public long getSetBackTime() {
        return setBackTimer.getPassedTimeMs();
    }

    public static float getRotations(Vec2 vec) {
        if (mc.player == null) return 0;
        double x = vec.x - mc.player.position().x;
        double z = vec.y - mc.player.position().z;
        return (float) -(Math.atan2(x, z) * (180 / Math.PI));
    }

    public void bobView(PoseStack matrices, float tickDelta) {
        if (!(mc.getCameraEntity() instanceof Player playerEntity)) {
            return;
        }

        float g = -Mth.lerp(tickDelta, prevHorizontalSpeed, horizontalSpeed);
        float h = playerEntity.walkAnimation.speed(tickDelta);
        matrices.translate(Mth.sin(g * (float) Math.PI) * h * 0.1f, -Math.abs(Mth.cos(g * (float) Math.PI) * h) * 0.3, 0.0f);
        matrices.mulPose(Axis.ZP.rotationDegrees(Mth.sin(g * (float) Math.PI) * h * 3.0f));
        matrices.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(g * (float) Math.PI - 0.2f) * h) * 0.3f));
    }
}
