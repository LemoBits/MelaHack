package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.lwjgl.glfw.GLFW;
import thunder.hack.events.impl.EventClickSlot;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.Bind;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.MovementUtility;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

import com.mojang.blaze3d.platform.InputConstants;

public class GuiMove extends Module {
    public GuiMove() {
        super("GuiMove", Category.MOVEMENT);
    }

    private final Setting<Bypass> clickBypass = new Setting<>("Bypass", Bypass.None);
    private final Setting<Boolean> rotateOnArrows = new Setting<>("RotateOnArrows", true);
    private final Setting<Boolean> sneak = new Setting<>("sneak", false);
    private final Setting<BooleanSettingGroup> closeWithoutPacketGroup = new Setting<>("CloseWithoutPacket", new BooleanSettingGroup(false), v -> true);
    private final Setting<Bind> closeBind = new Setting<>("CloseAndReopenBind", new Bind(GLFW.GLFW_KEY_B, false, false), v -> true).addToGroup(closeWithoutPacketGroup);

    private Screen screen;
    private AbstractContainerMenu screenHandler;
    private Timer bindDelay = new Timer();

    private final Queue<ServerboundContainerClickPacket> storedClicks = new LinkedList<>();
    private AtomicBoolean pause = new AtomicBoolean();

    @Override
    public void onUpdate() {
        if (mc.screen != null && !(mc.screen instanceof ChatScreen)) {
            for (KeyMapping k : new KeyMapping[]{mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight, mc.options.keyJump, mc.options.keySprint})
                k.setDown(isKeyPressed(InputConstants.getKey(k.saveString()).getValue()));

            float deltaX = 0;
            float deltaY = 0;

            if (rotateOnArrows.getValue()) {
                if (isKeyPressed(264))
                    deltaY += 30f;

                if (isKeyPressed(265))
                    deltaY -= 30f;

                if (isKeyPressed(262))
                    deltaX += 30f;

                if (isKeyPressed(263))
                    deltaX -= 30f;

                if (deltaX != 0 || deltaY != 0)
                    mc.player.turn(deltaX, deltaY);
            }

            if (sneak.getValue())
                mc.options.keyShift.setDown(isKeyPressed(InputConstants.getKey(mc.options.keyShift.saveString()).getValue()));

        }

        if (closeWithoutPacketGroup.getValue().isEnabled()) {
            closeWithoutPacket();
        }
    }

    // thanks ui utils for this!
    public void closeWithoutPacket() {
        if (isKeyPressed(closeBind) && bindDelay.every(250)) {

            if (mc.screen instanceof ChatScreen) {
                return;
            }

            if (mc.screen != null) {
                screen = mc.screen;
                screenHandler = mc.player.containerMenu;
                mc.setScreen(null);
                if (mc.screen != screen) sendMessage(isRu() ? "Интерфейс сохранен! Нажмите еще раз чтобы открыть" : "GUI have been saved! Press again to open.");
            } else {
                mc.setScreen(screen);
                mc.player.containerMenu = screenHandler;
                sendMessage(isRu() ? "Интерфейс открыт." : "GUI Opened.");
            }

        }
    }

    @EventHandler
    public void onClickSlot(EventClickSlot e) {
        if (clickBypass.is(Bypass.DisableClicks) && (MovementUtility.isMoving() || mc.options.keyJump.isDown()))
            e.cancel();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (!MovementUtility.isMoving() || !mc.options.keyJump.isDown() || pause.get())
            return;

        if (e.getPacket() instanceof ServerboundContainerClickPacket click) {
            switch (clickBypass.getValue()) {
                case GrimSwap -> {
                    if (click.clickType() != ClickType.PICKUP && click.clickType() != ClickType.PICKUP_ALL)
                        sendPacket(new ServerboundContainerClosePacket(0));
                }

                case StrictNCP -> {
                    if (mc.player.onGround() && !mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().move(0.0, 0.0656, 0.0)).iterator().hasNext()) {
                        if (mc.player.isSprinting())
                            sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                        sendPacket(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + 0.0656, mc.player.getZ(), false, mc.player.horizontalCollision));
                    }
                }

                case StrictNCP2 -> {
                    if (mc.player.onGround() && !mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().move(0.0, 0.000000271875, 0.0)).iterator().hasNext()) {
                        if (mc.player.isSprinting())
                            sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                        sendPacket(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + 0.000000271875, mc.player.getZ(), false, mc.player.horizontalCollision));
                    }
                }

                case MatrixNcp -> {
                    sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                    mc.options.keyUp.setDown(false);
                    MovementUtility.setMovementInputY(0f);
                }

                case Delay -> {
                    storedClicks.add(click);
                    e.cancel();
                }
            }
        }

        if (e.getPacket() instanceof ServerboundContainerClosePacket) {
            if (clickBypass.is(Bypass.Delay)) {
                pause.set(true);
                while (!storedClicks.isEmpty())
                    sendPacket(storedClicks.poll());
                pause.set(false);
            }
        }
    }

    @EventHandler
    public void onPacketSendPost(PacketEvent.SendPost e) {
        if (e.getPacket() instanceof ServerboundContainerClickPacket) {
            if (mc.player.isSprinting() && clickBypass.is(Bypass.StrictNCP))
                sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        }
    }

    private enum Bypass {
        DisableClicks, None, StrictNCP, GrimSwap, MatrixNcp, Delay, StrictNCP2
    }
}
