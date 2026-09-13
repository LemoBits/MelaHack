package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventPlayerTravel;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.player.MovementUtility;

import java.util.ArrayList;

public class BoatFly extends Module {
    public BoatFly() {
        super("BoatFly", Category.MOVEMENT);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Packet);

    private final Setting<Boolean> phase = new Setting<>("Phase", false);
    private final Setting<Boolean> gravity = new Setting<>("Gravity", false);
    private final Setting<Boolean> automount = new Setting<>("AutoMount", true);
    public final Setting<Boolean> allowShift = new Setting<>("AllowShift", true);
    private final Setting<Float> speed = new Setting<>("Speed", 2f, 0.0f, 25f);
    private final Setting<Float> yspeed = new Setting<>("YSpeed", 1f, 0.0f, 10f);

    // For pro players
    public final Setting<SettingGroup> advanced = new Setting<>("Advanced", new SettingGroup(false, 0));
    private final Setting<Float> glidespeed = new Setting<>("GlideSpeed", 0f, 0f, 10f).addToGroup(advanced);
    private final Setting<Boolean> slotClick = new Setting<>("ClickSlot", false).addToGroup(advanced);
    private final Setting<Boolean> limit = new Setting<>("Limit", true).addToGroup(advanced);
    private final Setting<Boolean> ongroundpacket = new Setting<>("OnGroundPacket", false).addToGroup(advanced);
    private final Setting<Boolean> spoofpackets = new Setting<>("SpoofPackets", false).addToGroup(advanced);
    private final Setting<Float> jitter = new Setting<>("Jitter", 0.1f, 0.0f, 10f, v -> spoofpackets.getValue()).addToGroup(advanced);
    private final Setting<Boolean> cancelrotations = new Setting<>("CancelRotations", true).addToGroup(advanced);
    private final Setting<Boolean> cancel = new Setting<>("Cancel", true).addToGroup(advanced);
    private final Setting<Boolean> pause = new Setting<>("Pause", false).addToGroup(advanced);
    private final Setting<Integer> enableticks = new Setting<>("EnableTicks", 10, 1, 100, v -> pause.getValue()).addToGroup(advanced);
    private final Setting<Integer> waitTicks = new Setting<>("WaitTicks", 10, 1, 100, v -> pause.getValue()).addToGroup(advanced);
    private final Setting<Boolean> stopunloaded = new Setting<>("StopUnloaded", true).addToGroup(advanced);
    private final Setting<Float> timer = new Setting<>("Timer", 1f, 0.1f, 5f).addToGroup(advanced);
    public final Setting<Boolean> hideBoat = new Setting<>("HideBoat", true).addToGroup(advanced);

    private final ArrayList<ServerboundMoveVehiclePacket> vehiclePackets = new ArrayList<>();
    private int ticksEnabled = 0;
    private int enableDelay = 0;
    private boolean waitedCooldown = false;
    private boolean returnGravity = false;
    private boolean jitterSwitch = false;

    @Override
    public void onEnable() {
        if (fullNullCheck()) {
            disable();
            return;
        }

        if (automount.getValue()) mountToBoat();
    }

    @Override
    public void onDisable() {
        ThunderHack.TICK_TIMER = 1.0f;
        vehiclePackets.clear();
        waitedCooldown = false;

        if (mc.player == null) return;

        if ((phase.getValue()) && mode.getValue() == Mode.Motion) {
            if (mc.player.getControlledVehicle() != null) mc.player.getControlledVehicle().noPhysics = false;
            mc.player.noPhysics = false;
        }
        if (mc.player.getControlledVehicle() != null) mc.player.getControlledVehicle().setNoGravity(false);
        mc.player.setNoGravity(false);
    }


    private float randomizeYOffset() {
        jitterSwitch = !jitterSwitch;
        return jitterSwitch ? jitter.getValue() : -jitter.getValue();
    }

    private void sendMovePacket(ServerboundMoveVehiclePacket pac) {
        vehiclePackets.add(pac);
        sendPacket(pac);
    }

    private void teleportToGround(Entity boat) {
        BlockPos blockPos = BlockPos.containing(boat.position());
        for (int i = 0; i < 255; ++i) {
            if (!mc.level.getBlockState(blockPos).canBeReplaced() || mc.level.getBlockState(blockPos).getBlock() == Blocks.WATER) {
                boat.setPos(boat.getX(), blockPos.getY() + 1, boat.getZ());
                sendMovePacket(ServerboundMoveVehiclePacket.fromEntity(boat));
                boat.setPos(boat.getX(), boat.getY(), boat.getZ());
                break;
            }
            blockPos = blockPos.below();
        }
    }

    private void mountToBoat() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Boat) || mc.player.distanceToSqr(entity) > 25.0f) continue;
            sendPacket(new ServerboundInteractPacket(entity.getId(), InteractionHand.MAIN_HAND, null, false));
            break;
        }
    }

    @EventHandler
    public void onPlayerTravel(@NotNull EventPlayerTravel ev) {
        if (!ev.isPre()) return;
        if (fullNullCheck()) return;


        if (mc.player.getControlledVehicle() == null) {
            if (automount.getValue())
                mountToBoat();
            return;
        }

        if (phase.getValue() && mode.getValue() == Mode.Motion) {
            mc.player.getControlledVehicle().noPhysics = true;
            mc.player.getControlledVehicle().setNoGravity(true);
            mc.player.noPhysics = true;
        }

        if (!returnGravity) {
            mc.player.getControlledVehicle().setNoGravity(!gravity.getValue());
            mc.player.setNoGravity(!gravity.getValue());
        }

        if (pause.getValue()) {
            if (ticksEnabled > enableticks.getValue() && !waitedCooldown) {
                ticksEnabled = 0;
                waitedCooldown = true;
                enableDelay = waitTicks.getValue();
            }

            if (enableDelay > 0 && waitedCooldown) {
                --enableDelay;
                return;
            }

            if (enableDelay <= 0) waitedCooldown = false;
        }

        Entity entity = mc.player.getControlledVehicle();


        if ((!mc.level.hasChunk((int) entity.position().x >> 4, (int) entity.position().z >> 4) || entity.position().y < -60) && stopunloaded.getValue()) {
            returnGravity = true;
            return;
        }

        if (timer.getValue() != 1.0f) ThunderHack.TICK_TIMER = (timer.getValue());

        entity.setYRot(mc.player.getYRot());

        double[] boatMotion = MovementUtility.forward(speed.getValue());
        double predictedX = entity.getX() + boatMotion[0];
        double predictedZ = entity.getZ() + boatMotion[1];
        double predictedY = entity.getY();

        if ((!mc.level.hasChunk((int) predictedX >> 4, (int) predictedZ >> 4) || entity.position().y < -60) && stopunloaded.getValue()) {
            returnGravity = true;
            return;
        }

        returnGravity = false;

        entity.setDeltaMovement(entity.getDeltaMovement().x(), -glidespeed.getValue() / 100.0f, entity.getDeltaMovement().z());

        if (mode.getValue() == Mode.Motion)
            entity.setDeltaMovement(boatMotion[0], entity.getDeltaMovement().y(), boatMotion[1]);

        if (mc.options.keyJump.isDown()) {
            if (mode.getValue() == Mode.Motion)
                entity.setDeltaMovement(entity.getDeltaMovement().x(), entity.getDeltaMovement().y() + yspeed.getValue(), entity.getDeltaMovement().z());
            else predictedY += yspeed.getValue();
        } else if (mc.options.keyShift.isDown()) {
            if (mode.getValue() == Mode.Motion)
                entity.setDeltaMovement(entity.getDeltaMovement().x(), entity.getDeltaMovement().y() - yspeed.getValue(), entity.getDeltaMovement().z());
            else predictedY -= yspeed.getValue();
        }

        if (!MovementUtility.isMoving()) entity.setDeltaMovement(0, entity.getDeltaMovement().y(), 0);

        if (ongroundpacket.getValue()) teleportToGround(entity);

        if (mode.getValue() == Mode.Packet) {
            entity.setPos(predictedX, predictedY, predictedZ);
            sendMovePacket(ServerboundMoveVehiclePacket.fromEntity(entity));
        }

        if (slotClick.getValue())
            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 0, 0, ContainerInput.CLONE, mc.player);

        if (spoofpackets.getValue()) {
            Vec3 vec3d = entity.position().add(0.0, randomizeYOffset(), 0.0);
            Boat entityBoat = new Boat(EntityTypes.OAK_BOAT, mc.level, () -> Items.OAK_BOAT);
            entityBoat.setPos(vec3d);
            entityBoat.setYRot(entity.getYRot());
            entityBoat.setXRot(entity.getXRot());
            sendMovePacket(ServerboundMoveVehiclePacket.fromEntity(entityBoat));
        }

        ev.cancel();
        ++ticksEnabled;
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (fullNullCheck()) return;

        if (event.getPacket() instanceof ClientboundDisconnectPacket) disable();

        if (!mc.player.isHandsBusy() || returnGravity || waitedCooldown) return;

        if (cancel.getValue()) {
            if (event.getPacket() instanceof ClientboundMoveVehiclePacket) event.cancel();
            if (event.getPacket() instanceof ClientboundPlayerPositionPacket) event.cancel();
            if (event.getPacket() instanceof ClientboundMoveEntityPacket) event.cancel();
            if (event.getPacket() instanceof ClientboundSetEntityLinkPacket) event.cancel();
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (fullNullCheck()) return;

        if ((event.getPacket() instanceof ServerboundMovePlayerPacket.Rot && (cancelrotations.getValue()) || event.getPacket() instanceof ServerboundPlayerInputPacket) && mc.player.isHandsBusy())
            event.cancel();

        if (returnGravity && event.getPacket() instanceof ServerboundMoveVehiclePacket) event.cancel();

        if (event.getPacket() instanceof ServerboundPlayerInputPacket && allowShift.getValue()) {
            event.cancel();
        }

        if (mc.player.getControlledVehicle() == null || returnGravity || waitedCooldown)
            return;

        Vec3 boatPos = mc.player.getControlledVehicle().position();
        if ((!mc.level.hasChunk((int) boatPos.x() >> 4, (int) boatPos.z() >> 4) || boatPos.y() < -60) && stopunloaded.getValue())
            return;

        if (event.getPacket() instanceof ServerboundMoveVehiclePacket pac && limit.getValue() && mode.getValue() == Mode.Packet)
            if (vehiclePackets.contains(pac)) vehiclePackets.remove(pac);
            else event.cancel();
    }

    public enum Mode {
        Packet, Motion
    }
}
