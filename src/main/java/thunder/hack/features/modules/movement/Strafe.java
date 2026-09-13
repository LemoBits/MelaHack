package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.phys.AABB;
import thunder.hack.core.Core;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.features.modules.Module;
import thunder.hack.injection.accesors.ISPacketEntityVelocity;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.MovementUtility;

import static thunder.hack.utility.player.MovementUtility.isMoving;

public class Strafe extends Module {
    public Strafe() { // Outdated
        super("Strafe", Category.MOVEMENT);
    }

    private final Setting<Boost> boost = new Setting<>("Boost", Boost.None);
    private final Setting<Float> setSpeed = new Setting<>("speed", 1.3F, 0.0F, 2f, v -> boost.getValue() == Boost.Elytra);
    private final Setting<Float> velReduction = new Setting<>("Reduction", 6.0f, 0.1f, 10f, v -> boost.getValue() == Boost.Damage);
    private final Setting<Float> maxVelocitySpeed = new Setting<>("MaxVelocity", 0.8f, 0.1f, 2f, v -> boost.getValue() == Boost.Damage);
    private final Setting<Boolean> sunrise = new Setting<>("Sunrise", true, v -> boost.getValue() == Boost.Elytra);

    public static double oldSpeed, contextFriction, fovval;
    public static boolean needSwap, needSprintState, disabled;
    public static int noSlowTicks;
    static long disableTime;

    public double calculateSpeed(EventMove move) {
        float speedAttributes = getAIMoveSpeed();
        final float frictionFactor = mc.level.getBlockState(new BlockPos.MutableBlockPos().set(mc.player.getX(), getBoundingBox().min(Direction.Axis.Y) - move.getY(), mc.player.getZ())).getBlock().getFriction() * 0.91F;
        float n6 = mc.player.hasEffect(MobEffects.JUMP_BOOST) && mc.player.isUsingItem() ? 0.88f : (float) (oldSpeed > 0.32 && mc.player.isUsingItem() ? 0.88 : 0.91F);
        if (mc.player.onGround())
            n6 = frictionFactor;

        float n7 = (float) (0.1631f / Math.pow(n6, 3.0f));
        float n8;
        if (mc.player.onGround()) {
            n8 = speedAttributes * n7;
            if (move.getY() > 0)
                n8 += boost.getValue() == Boost.Elytra && InventoryUtility.getElytra() != -1 && (disabled && System.currentTimeMillis() - disableTime < 300) ? 0.65f : 0.2f;
            disabled = false;
        } else n8 = 0.0255f;

        boolean noslow = false;
        double max2 = oldSpeed + n8;
        double max = 0.0;

        if (mc.player.isUsingItem() && move.getY() <= 0 && !sunrise.getValue()) {
            double n10 = oldSpeed + n8 * 0.25;
            double motionY2 = move.getY();
            if (motionY2 != 0.0 && Math.abs(motionY2) < 0.08) {
                n10 += 0.055;
            }
            if (max2 > (max = Math.max(0.043, n10))) {
                noslow = true;
                ++noSlowTicks;
            } else {
                noSlowTicks = Math.max(noSlowTicks - 1, 0);
            }
        } else {
            noSlowTicks = 0;
        }

        if (noSlowTicks > 3) max2 = max - 0.019;
        else max2 = Math.max(noslow ? 0 : 0.25, max2) - (mc.player.tickCount % 2 == 0 ? 0.001 : 0.002);

        contextFriction = n6;
        if (!mc.player.onGround()) {
            needSprintState = !mc.player.wasSprinting;
            needSwap = true;
        } else needSprintState = false;
        return max2;
    }

    public float getAIMoveSpeed() {
        boolean prevSprinting = mc.player.isSprinting();
        mc.player.setSprinting(false);
        float speed = mc.player.getSpeed() * 1.3f;
        mc.player.setSprinting(prevSprinting);
        return speed;
    }

    public static void disabler(int elytra) {
        if (elytra == -1) return;
        if (System.currentTimeMillis() - disableTime > 190L) {
            if (elytra != -2) {
                mc.gameMode.handleContainerInput(0, elytra, 1, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(0, 6, 1, ContainerInput.PICKUP, mc.player);
            }

            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));

            if (elytra != -2) {
                mc.gameMode.handleContainerInput(0, 6, 1, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(0, elytra, 1, ContainerInput.PICKUP, mc.player);
            }
            disableTime = System.currentTimeMillis();
        }
        disabled = true;
    }


    @Override
    public void onEnable() {
        oldSpeed = 0.0;
        fovval = mc.options.fovEffectScale().get();
        mc.options.fovEffectScale().set(0d);
    }

    @Override
    public void onDisable() {
        mc.options.fovEffectScale().set(fovval);
    }

    public boolean canStrafe() {
        if (mc.player.isShiftKeyDown()) {
            return false;
        }
        if (mc.player.isInLava()) {
            return false;
        }
        if (ModuleManager.scaffold.isEnabled()) {
            return false;
        }
        if (ModuleManager.speed.isEnabled()) {
            return false;
        }
        if (mc.player.isUnderWater()) {
            return false;
        }
        return !mc.player.getAbilities().flying;
    }

    public AABB getBoundingBox() {
        return new AABB(mc.player.getX() - 0.1, mc.player.getY(), mc.player.getZ() - 0.1, mc.player.getX() + 0.1, mc.player.getY() + 1, mc.player.getZ() + 0.1);
    }

    @EventHandler
    public void onMove(EventMove event) {
        int elytraSlot = InventoryUtility.getElytra();
        if (boost.getValue() == Boost.Elytra && elytraSlot != -1) {
            if (isMoving() && !mc.player.onGround() && mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().move(0.0, event.getY(), 0.0f)).iterator().hasNext() && disabled) {
                oldSpeed = setSpeed.getValue();
            }
        }

        if (canStrafe()) {
            if (isMoving()) {
                double[] motions = MovementUtility.forward(calculateSpeed(event));

                event.setX(motions[0]);
                event.setZ(motions[1]);
            } else {
                oldSpeed = 0;
                event.setX(0);
                event.setZ(0);
            }
            event.cancel();
        } else {
            oldSpeed = 0;
        }
    }

    @EventHandler
    public void onSync(EventSync e) {
        oldSpeed = Math.hypot(mc.player.getX() - mc.player.xo, mc.player.getZ() - mc.player.zo) * contextFriction;
    }


    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof ClientboundPlayerPositionPacket) {
            oldSpeed = 0;
        }
        ClientboundSetEntityMotionPacket velocity;

        if (e.getPacket() instanceof ClientboundSetEntityMotionPacket && (velocity = e.getPacket()).id() == mc.player.getId() && boost.getValue() == Boost.Damage) {
            if (mc.player.onGround()) return;

            double vX = velocity.movement().x;
            double vZ = velocity.movement().z;

            if (vX < 0) vX *= -1;
            if (vZ < 0) vZ *= -1;

            oldSpeed = (vX + vZ) / (velReduction.getValue() * 1000f);
            oldSpeed = Math.min(oldSpeed, maxVelocitySpeed.getValue());

            ((ISPacketEntityVelocity) (Object) velocity).setMotionX(0);
            ((ISPacketEntityVelocity) (Object) velocity).setMotionY(0);
            ((ISPacketEntityVelocity) (Object) velocity).setMotionZ(0);
        }
    }

    @EventHandler
    public void actionEvent(EventSprint eventAction) {
        if (canStrafe()) {
            if (Core.serverSprint != needSprintState) {
                eventAction.setSprintState(!Core.serverSprint);
            }
        }
        if (needSwap) {
            eventAction.setSprintState(!mc.player.wasSprinting);
            needSwap = false;
        }
    }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent event) {
        if ((boost.getValue() == Boost.Elytra && InventoryUtility.getElytra() != -1 && !mc.player.onGround() && mc.player.fallDistance > 0 && !disabled)
                && (!mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -1.1f, 0.0f)).iterator().hasNext() || !sunrise.getValue())) {
            disabler(InventoryUtility.getElytra());
        }
    }

    private enum Boost {
        None, Elytra, Damage
    }
}
