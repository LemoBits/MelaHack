package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventPlayerTravel;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

public class EntitySpeed extends Module {
    public EntitySpeed() {
        super("EntitySpeed", Category.MOVEMENT);
    }

    private final Setting<Boolean> accelerate = new Setting<>("Accelerate", true);
    private final Setting<Float> accelerateFactor = new Setting<>("AccelerateFactor", 9f, 0f, 20f, v -> accelerate.getValue());
    private final Setting<Boolean> stopunloaded = new Setting<>("StopUnloaded", true);
    private final Setting<Float> speed = new Setting<>("Speed", 0.77f, 0.1f, 5f);
    private final Setting<Float> timer = new Setting<>("Timer", 1f, 0.1f, 5f);
    private final Setting<Float> jitter = new Setting<>("Jitter", 0.05f, 0.0f, 0.5f);

    private int ticks;
    private float acceleration;

    @Override
    public void onEnable() {
        ticks = 0;
        acceleration = 0f;
    }

    @Override
    public void onDisable() {
        ThunderHack.TICK_TIMER = 1f;
    }

    @EventHandler
    public void onPlayerTravel(@NotNull EventPlayerTravel ev) {
        if (!ev.isPre()) return;
        if (fullNullCheck()) return;

        Entity entity = mc.player.getControlledVehicle();

        if (entity == null) return;
        if ((!mc.level.hasChunk((int) entity.position().x >> 4, (int) entity.position().z >> 4) || entity.position().y < -60) && stopunloaded.getValue())
            return;

        if (entity.horizontalCollision || mc.player.horizontalCollision)
            acceleration = 0;

        if (timer.getValue() != 1.0f)
            ThunderHack.TICK_TIMER = timer.getValue();

        double[] motion = MovementUtility.forward(getSpeed());
        double predictedX = entity.getX() + motion[0];
        double predictedZ = entity.getZ() + motion[1];

        if ((!mc.level.hasChunk((int) predictedX >> 4, (int) predictedZ >> 4) || entity.position().y < -60) && stopunloaded.getValue())
            return;

        if (MovementUtility.isMoving()) entity.setDeltaMovement(motion[0], entity.getDeltaMovement().y(), motion[1]);
        else entity.setDeltaMovement(0, entity.getDeltaMovement().y(), 0);

        if (ticks++ > 50)
            ticks = 0;

        ev.cancel();
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof ClientboundPlayerPositionPacket)
            acceleration = 0;
    }

    private float getSpeed() {
        float baseSpeed = Math.min((acceleration = (acceleration + (20f - accelerateFactor.getValue()) / speed.getValue())) / 100.0F, speed.getValue());
        if (!accelerate.getValue())
            baseSpeed = speed.getValue();
        baseSpeed += (ticks > 25 ? jitter.getValue() : 0);
        return baseSpeed;
    }
}
