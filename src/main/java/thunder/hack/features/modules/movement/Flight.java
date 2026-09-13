package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class Flight extends Module {
    public Flight() {
        super("Flight", Category.MOVEMENT);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Vanilla);
    private final Setting<Float> hSpeed = new Setting<>("Horizontal", 1f, 0.0f, 10.0f, v -> !mode.is(Mode.StormBreak));
    private final Setting<Float> vSpeed = new Setting<>("Vertical", 0.78F, 0.0F, 5F, v -> !mode.is(Mode.StormBreak));
    private final Setting<Float> boostValue = new Setting<>("Boost", 1f, 0.1F, 1f, v -> mode.is(Mode.StormBreak));
    private final Setting<Boolean> autoToggle = new Setting<>("AutoToggle", false, v -> mode.is(Mode.MatrixJump));
    private final Setting<Integer> boostTicks = new Setting<>("Ticks", 8, 0, 40, v -> mode.is(Mode.Damage));
    private final Setting<Boolean> antiKick = new Setting<>("AntiKick", false, v -> mode.is(Mode.Creative) || mode.is(Mode.Vanilla));

    private double prevX, prevY, prevZ, velocityMotion;
    public boolean onPosLook = false;
    private int flyTicks = 0;


    @EventHandler
    public void onEventSync(EventSync event) {
        switch (mode.getValue()) {
            case Vanilla -> {
                if (MovementUtility.isMoving()) {
                    final double[] dir = MovementUtility.forward(hSpeed.getValue());
                    mc.player.setDeltaMovement(dir[0], 0, dir[1]);
                } else mc.player.setDeltaMovement(0, 0, 0);

                if (mc.options.keyJump.isDown())
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(0, vSpeed.getValue(), 0));
                if (mc.options.keyShift.isDown())
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(0, -vSpeed.getValue(), 0));
            }

            case AirJump -> {
                if (MovementUtility.isMoving() && mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().inflate(0.5, 0.0, 0.5).move(0.0, -1.0, 0.0)).iterator().hasNext()) {
                    mc.player.setOnGround(true);
                    mc.player.jumpFromGround();
                }
            }

            case MatrixGlide -> {
                if (mc.player.onGround()) {
                    mc.player.jumpFromGround();
                    flyTicks = 5;
                } else if (flyTicks > 0) {
                    if (MovementUtility.isMoving()) {
                        final double[] dir = MovementUtility.forward(hSpeed.getValue());
                        mc.player.setDeltaMovement(dir[0], -0.04, dir[1]);
                    } else mc.player.setDeltaMovement(0, -0.04, 0);
                    flyTicks--;
                }
            }

            case StormBreak -> {
                if (mc.player.tickCount % 60 == 0)
                    sendMessage(ChatFormatting.RED + (isRu() ? "В этом режиме нужно ломать блоки!" : "In this mode you need to break blocks!"));
            }
        }

        if (antiKick.getValue() && (mode.is(Mode.Creative) || mode.is(Mode.Vanilla)))
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(0, -0.08, 0));
    }

    @Override
    public void onUpdate() {
        if (mode.is(Mode.Damage))
            if (flyTicks-- > boostTicks.getValue())
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, velocityMotion, mc.player.getDeltaMovement().z());

        if (mode.is(Mode.MatrixJump)) {
            if (mc.player.fallDistance == 0)
                return;

            mc.player.getAbilities().flying = false;
            mc.player.setDeltaMovement(0.0, 0.0, 0.0);

            if (mc.options.keyJump.isDown())
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(0, vSpeed.getValue(), 0));

            if (mc.options.keyShift.isDown())
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(0, -vSpeed.getValue(), 0));

            final double[] dir = MovementUtility.forward(hSpeed.getValue());
            mc.player.setDeltaMovement(dir[0], mc.player.getDeltaMovement().y(), dir[1]);
        }

        if (mode.is(Mode.Creative)) {
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlyingSpeed(hSpeed.getValue() / 10f);
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (mode.is(Mode.MatrixJump)) {
            if (fullNullCheck()) return;
            if (e.getPacket() instanceof ClientboundPlayerPositionPacket) {
                onPosLook = true;
                prevX = mc.player.getDeltaMovement().x();
                prevY = mc.player.getDeltaMovement().y();
                prevZ = mc.player.getDeltaMovement().z();
            }
        }

        if (mode.is(Mode.Damage))
            if (e.getPacket() instanceof ClientboundSetEntityMotionPacket v)
                if (v.getMovement().y > 0.2) {
                    velocityMotion = v.getMovement().y;
                    flyTicks = boostTicks.getValue();
                }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (mode.is(Mode.MatrixJump)) {
            if (e.getPacket() instanceof ServerboundMovePlayerPacket.PosRot) {
                if (onPosLook) {
                    mc.player.setDeltaMovement(prevX, prevY, prevZ);
                    onPosLook = false;
                    if (autoToggle.getValue()) disable();
                }
            }
        }

        if (mode.is(Mode.StormBreak) && e.getPacket() instanceof ServerboundPlayerActionPacket pac && (pac.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
                || pac.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK && mc.level.getBlockState(pac.getPos()).canBeReplaced())) {
            final double[] dir = MovementUtility.forward(2.0f * boostValue.getValue());
            mc.player.setDeltaMovement(dir[0], 3f * boostValue.getValue(), dir[1]);
        }
    }

    @Override
    public void onDisable() {
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().setFlyingSpeed(0.05f);
    }

    private enum Mode {
        Vanilla, MatrixJump, AirJump, MatrixGlide, StormBreak, Damage, Creative
    }
}