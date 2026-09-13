package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventMove;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.injection.accesors.IPlayerPositionLookS2CPacket;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.utility.player.MovementUtility;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class PacketFly extends Module {
    public PacketFly() {
        super("PacketFly", Category.MOVEMENT);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Fast);
    private final Setting<Type> type = new Setting<>("Type", Type.Preserve);
    private final Setting<Phase> phase = new Setting<>("Phase", Phase.Full);
    private final Setting<Boolean> limit = new Setting<>("Limit", true);
    private final Setting<BooleanSettingGroup> antiKick = new Setting<>("AntiKick", new BooleanSettingGroup(true));
    private final Setting<Integer> interval = new Setting<>("Interval", 4, 1, 50).addToGroup(antiKick);
    private final Setting<Integer> upInterval = new Setting<>("UpInterval", 20, 1, 50).addToGroup(antiKick);
    private final Setting<Float> anticKickOffset = new Setting<>("anticKickOffset", 0.04f, 0.008f, 1f).addToGroup(antiKick);
    private final Setting<Float> speed = new Setting<>("Speed", 1.0f, 0.0f, 10.0f);
    private final Setting<Float> upSpeed = new Setting<>("UpSpeed", 0.062f, 0.001f, 0.1f);
    private final Setting<Float> timer = new Setting<>("Timer", 1f, 0.1f, 5f);
    private final Setting<Integer> increaseTicks = new Setting<>("IncreaseTicks", 1, 1, 20);
    private final Setting<Float> factor = new Setting<>("Factor", 1f, 1f, 10f);
    private final Setting<Float> offset = new Setting<>("Offset", 1337f, 1f, 1337f, v-> type.is(Type.Up) || type.is(Type.Down));

    private final ConcurrentHashMap<Integer,Teleport> teleports = new ConcurrentHashMap<>();
    private final ArrayList<ServerboundMovePlayerPacket> movePackets = new ArrayList<>();
    private int ticks, factorTicks, teleportId = -1;
    private boolean flip = false;

    @Override
    public void onEnable() {
        teleportId = -1;
        if (fullNullCheck() && mc.player != null) {
            ticks = 0;
            teleportId = 0;
            movePackets.clear();
            teleports.clear();
        }
        factorTicks = 0;
    }

    @Override
    public void onDisable() {
        ThunderHack.TICK_TIMER = 1.0f;
    }

    public boolean getTickCounter(int n) {
        ++ticks;
        if (ticks >= n) {
            ticks = 0;
            return true;
        }
        return false;
    }

    private int getWorldBorder() {
        if (mc.isLocalServer()) {
            return 1;
        }
        int n = ThreadLocalRandom.current().nextInt(29000000);
        if (ThreadLocalRandom.current().nextBoolean()) {
            return n;
        }
        return -n;
    }

    public Vec3 getVectorByMode(@NotNull Vec3 vec3d, Vec3 vec3d2) {
        Vec3 vec3d3 = vec3d.add(vec3d2);
        switch (type.getValue()) {
            case Preserve -> vec3d3 = vec3d3.add(getWorldBorder(), 0.0, getWorldBorder());
            case Up -> vec3d3 = vec3d3.add(0.0, offset.getValue(), 0.0);
            case Down -> vec3d3 = vec3d3.add(0.0, -offset.getValue(), 0.0);
            case Bounds -> vec3d3 = new Vec3(vec3d3.x, mc.player.getY() <= 10.0 ? 255.0 : 1.0, vec3d3.z);
        }
        return vec3d3;
    }

    public void sendPackets(Vec3 vec3d, boolean confirm) {
        Vec3 motion = mc.player.position().add(vec3d);
        Vec3 rubberBand = getVectorByMode(vec3d, motion);

        ServerboundMovePlayerPacket motionPacket =  new ServerboundMovePlayerPacket.Pos(motion.x, motion.y, motion.z, mc.player.onGround(), mc.player.horizontalCollision);
        movePackets.add(motionPacket);
        sendPacket(motionPacket);

        ServerboundMovePlayerPacket rubberBandPacket = new ServerboundMovePlayerPacket.Pos(rubberBand.x, rubberBand.y, rubberBand.z, mc.player.onGround(), mc.player.horizontalCollision);
        movePackets.add(rubberBandPacket);
        sendPacket(rubberBandPacket);

        if (confirm) {
            sendPacket(new ServerboundAcceptTeleportationPacket(++teleportId));
            teleports.put(teleportId, new Teleport(motion.x, motion.y, motion.z, System.currentTimeMillis()));
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (fullNullCheck()) return;
        if (mc.player != null && event.getPacket() instanceof ClientboundPlayerPositionPacket pac) {
            Teleport teleport = teleports.remove(pac.id());
            Vec3 pos = pac.change().position();
            if (
                    mc.player.isAlive()
                    && mc.level.hasChunk((int) mc.player.getX() >> 4, (int) mc.player.getZ() >> 4)
                    && !(mc.screen instanceof ReceivingLevelScreen)
                    && mode.getValue() != Mode.Rubber
                    && teleport != null
                    && teleport.x == pos.x
                    && teleport.y == pos.y
                    && teleport.z == pos.z
            ) {
                event.cancel();
                return;
            }
            PositionMoveRotation change = pac.change();
            PositionMoveRotation updated = new PositionMoveRotation(change.position(), change.deltaMovement(), mc.player.getYRot(), mc.player.getXRot());
            ((IPlayerPositionLookS2CPacket) (Object) pac).setChange(updated);
            teleportId = pac.id();
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.@NotNull Send event) {
        if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
            if (movePackets.contains((ServerboundMovePlayerPacket) event.getPacket())) {
                movePackets.remove((ServerboundMovePlayerPacket) event.getPacket());
                return;
            }
            event.cancel();
        }
    }

    @Override
    public void onUpdate() {
        teleports.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue().time > 30000);
    }

    @EventHandler
    public void onMove(@NotNull EventMove event) {
        if (!event.isCancelled()) {
            if (mode.getValue() != Mode.Rubber && teleportId == 0)
                return;

            event.cancel();
            event.setX(mc.player.getDeltaMovement().x);
            event.setY(mc.player.getDeltaMovement().y);
            event.setZ(mc.player.getDeltaMovement().z);
            if (phase.getValue() != Phase.Off && (phase.getValue() == Phase.Semi || mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().inflate(-0.0625, -0.0625, -0.0625)).iterator().hasNext())) {
                mc.player.noPhysics = true;
            }
        }
    }

    @EventHandler
    public void onSync(EventSync eventPlayerUpdateWalking) {
        if (timer.getValue() != 1.0)
            ThunderHack.TICK_TIMER = timer.getValue();

        mc.player.setDeltaMovement(0.0, 0.0, 0.0);

        if (mode.getValue() != Mode.Rubber && teleportId == 0) {
            if (getTickCounter(4))
                sendPackets(Vec3.ZERO, false);
            return;
        }

        boolean insideBlock = mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().inflate(-0.0625, -0.0625, -0.0625)).iterator().hasNext();

        double upMotion = 0;

        if (mc.options.keyJump.isDown() && (insideBlock || !MovementUtility.isMoving())) {
            if (antiKick.getValue().isEnabled() && !insideBlock)
                upMotion = getTickCounter(mode.is(Mode.Rubber) ? upInterval.getValue() / 2 : upInterval.getValue()) ? -upSpeed.getValue() / 2f : upSpeed.getValue();
            else
                upMotion = upSpeed.getValue();
        } else if (mc.options.keyShift.isDown())
            upMotion = -upSpeed.getValue();
        else if(antiKick.getValue().isEnabled() && !insideBlock)
            upMotion = getTickCounter(interval.getValue()) ? -anticKickOffset.getValue() : 0.0;

        if (phase.is(Phase.Full) && insideBlock && MovementUtility.isMoving() && upMotion != 0.0)
            upMotion = mc.options.keyJump.isDown()  ? upMotion / 2.5 : upMotion / 1.5;

        double[] motion = MovementUtility.forward(phase.is(Phase.Full) && insideBlock ? 0.034444444444444444 : (double) (speed.getValue()) * 0.26);

        int factorInt = 1;
        if (mode.getValue() == Mode.Factor && mc.player.tickCount % increaseTicks.getValue() == 0) {
            factorInt = (int) Math.floor(factor.getValue());
            factorTicks++;
            if (factorTicks > (int) (20D / ((factor.getValue() - factorInt) * 20D))) {
                factorInt += 1;
                factorTicks = 0;
            }
        }

        for (int i = 1; i <= factorInt; ++i) {
            if (mode.getValue() == Mode.Limit) {
                if (mc.player.tickCount % 2 == 0) {
                    if (flip && upMotion >= 0.0) {
                        flip = false;
                        upMotion = -upSpeed.getValue() / 2f;
                    }
                    mc.player.setDeltaMovement(motion[0] * i,upMotion * i,motion[1] * i);
                    sendPackets(mc.player.getDeltaMovement(), !limit.getValue());
                    continue;
                }
                if (!(upMotion < 0.0)) continue;
                flip = true;
                continue;
            }
            mc.player.setDeltaMovement(motion[0] * i,upMotion * i,motion[1] * i);
            sendPackets(mc.player.getDeltaMovement(), !mode.is(Mode.Rubber));
        }
    }

    public enum Mode {
        Fast, Factor, Rubber, Limit
    }

    public enum Phase {
        Full, Off, Semi
    }

    public enum Type {
        Preserve, Up, Down, Bounds
    }

    public record Teleport(double x, double y, double z, long time){}
}
