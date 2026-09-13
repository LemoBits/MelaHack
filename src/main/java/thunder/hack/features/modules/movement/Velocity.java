package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.phys.Vec3;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.injection.accesors.IEntity;
import thunder.hack.injection.accesors.IExplosionS2CPacket;
import thunder.hack.injection.accesors.ISPacketEntityVelocity;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

import java.util.Optional;
//TY <3
//https://github.com/SkidderMC/FDPClient/blob/main/src/main/java/net/ccbluex/liquidbounce/features/module/modules/combat/velocitys/vanilla/JumpVelocity.kt

public class Velocity extends Module {
    public Velocity() {
        super("Velocity", Category.MOVEMENT);
    }

    public Setting<Boolean> onlyAura = new Setting<>("OnlyDuringAura", false);
    public Setting<Boolean> pauseInWater = new Setting<>("PauseInLiquids", false);
    public Setting<Boolean> explosions = new Setting<>("Explosions", true);
    public Setting<Boolean> cc = new Setting<>("PauseOnFlag", false);
    public Setting<Boolean> fire = new Setting<>("PauseOnFire", false);
    private final Setting<modeEn> mode = new Setting<>("Mode", modeEn.Matrix);
    public Setting<Float> vertical = new Setting<>("Vertical", 0.0f, 0.0f, 100.0f, v -> mode.getValue() == modeEn.Custom);
    private final Setting<jumpModeEn> jumpMode = new Setting<>("JumpMode", jumpModeEn.Jump, v -> mode.getValue() == modeEn.Jump);
    public Setting<Float> horizontal = new Setting<>("Horizontal", 0.0f, 0.0f, 100.0f, v -> mode.getValue() == modeEn.Custom || mode.getValue() == modeEn.Jump);
    public Setting<Float> motion = new Setting<>("Motion", .42f, 0.4f, 0.5f, v -> mode.getValue() == modeEn.Jump);
    public Setting<Boolean> fail = new Setting<>("SmartFail", true, v -> mode.getValue() == modeEn.Jump);
    public Setting<Float> failRate = new Setting<>("FailRate", 0.3f, 0.0f, 1.0f, v -> mode.getValue() == modeEn.Jump && fail.getValue());
    public Setting<Float> jumpRate = new Setting<>("FailJumpRate", 0.25f, 0.0f, 1.0f, v -> mode.getValue() == modeEn.Jump && fail.getValue());

    private boolean doJump, failJump, skip, flag;
    private int grimTicks, ccCooldown;


    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (fullNullCheck()) return;

        if (mc.player != null && (mc.player.isInWater() || mc.player.isUnderWater() || mc.player.isInLava()) && pauseInWater.getValue())
            return;

        if (mc.player != null && mc.player.isOnFire() && fire.getValue() && (mc.player.hurtTime > 0)) {
            return;
        }

        if (ccCooldown > 0) {
            ccCooldown--;
            return;
        }

        // MAIN VELOCITY
        if (e.getPacket() instanceof ClientboundSetEntityMotionPacket pac) {
            if (pac.id() == mc.player.getId() && (!onlyAura.getValue() || ModuleManager.aura.isEnabled())) {
                switch (mode.getValue()) {
                    case Matrix -> {
                        if (!flag) {
                            e.cancel();
                            flag = true;
                        } else {
                            flag = false;
                            ((ISPacketEntityVelocity) (Object) pac).setMotionX(pac.movement().x * -0.1);
                            ((ISPacketEntityVelocity) (Object) pac).setMotionZ(pac.movement().z * -0.1);
                        }
                    }
                    case Redirect -> {
                        double vX = Math.abs(pac.movement().x);
                        double vZ = Math.abs(pac.movement().z);
                        double[] motion = MovementUtility.forward(vX + vZ);
                        ((ISPacketEntityVelocity) (Object) pac).setMotionX(motion[0]);
                        ((ISPacketEntityVelocity) (Object) pac).setMotionY(0);
                        ((ISPacketEntityVelocity) (Object) pac).setMotionZ(motion[1]);
                    }
                    case Custom -> {
                        ((ISPacketEntityVelocity) (Object) pac).setMotionX(pac.movement().x * horizontal.getValue() / 100f);
                        ((ISPacketEntityVelocity) (Object) pac).setMotionY(pac.movement().y * vertical.getValue() / 100f);
                        ((ISPacketEntityVelocity) (Object) pac).setMotionZ(pac.movement().z * horizontal.getValue() / 100f);
                    }
                    case Sunrise -> {
                        e.cancel();
                        sendPacket(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), -999.0, mc.player.getZ(), true, mc.player.horizontalCollision));
                    }
                    case Cancel -> e.cancel();
                    case Jump -> {
                        ((ISPacketEntityVelocity) (Object) pac).setMotionX(pac.movement().x * horizontal.getValue() / 100f);
                        ((ISPacketEntityVelocity) (Object) pac).setMotionZ(pac.movement().z * horizontal.getValue() / 100f);
                    }
                    case OldGrim -> {
                        e.cancel();
                        grimTicks = 6;
                    }
                    case GrimNew -> {
                        e.cancel();
                        flag = true;
                    }
                }
            }
        }

        // EXPLOSION
        if (e.getPacket() instanceof ClientboundExplodePacket explosion && explosions.getValue()) {
            Optional<Vec3> knockback = ((IExplosionS2CPacket) (Object) explosion).getPlayerKnockback();
            if (knockback.isEmpty()) {
                return;
            }
            Vec3 playerKnockback = knockback.get();
            switch (mode.getValue()) {
                case Cancel -> {
                    ((IExplosionS2CPacket) (Object) explosion).setPlayerKnockback(Optional.of(Vec3.ZERO));
                }
                case Custom -> {
                    Vec3 scaled = new Vec3(
                        playerKnockback.x * horizontal.getValue() / 100f,
                        playerKnockback.y * vertical.getValue() / 100f,
                        playerKnockback.z * horizontal.getValue() / 100f
                    );
                    ((IExplosionS2CPacket) (Object) explosion).setPlayerKnockback(Optional.of(scaled));
                }
                case GrimNew -> {
                    ((IExplosionS2CPacket) (Object) explosion).setPlayerKnockback(Optional.of(Vec3.ZERO));
                    flag = true;
                }
            }
        }

        // PING
        if (mode.getValue() == modeEn.OldGrim) {
            if (e.getPacket() instanceof ClientboundPingPacket && grimTicks > 0) {
                e.cancel();
                grimTicks--;
            }
        }

        // LAGBACK
        if (e.getPacket() instanceof ClientboundPlayerPositionPacket) {
            if (cc.getValue() || mode.getValue() == modeEn.GrimNew)
                ccCooldown = 5;
        }
    }


    @Override
    public void onUpdate() {
        if (mc.player != null && (mc.player.isInWater() || mc.player.isUnderWater()) && pauseInWater.getValue())
            return;

        switch (mode.getValue()) {
            case Matrix -> {
                if (mc.player.hurtTime > 0 && !mc.player.onGround()) {
                    double var3 = mc.player.getYRot() * 0.017453292F;
                    double var5 = Math.sqrt(mc.player.getDeltaMovement().x * mc.player.getDeltaMovement().x + mc.player.getDeltaMovement().z * mc.player.getDeltaMovement().z);
                    mc.player.setDeltaMovement(-Math.sin(var3) * var5, mc.player.getDeltaMovement().y, Math.cos(var3) * var5);
                    mc.player.setSprinting(mc.player.tickCount % 2 != 0);
                }
            }
            case Jump -> {
                if ((failJump || mc.player.hurtTime > 6) && mc.player.onGround()) {
                    if (failJump) failJump = false;
                    if (!doJump) skip = true;
                    if (Math.random() <= failRate.getValue() && fail.getValue()) {
                        if (Math.random() <= jumpRate.getValue()) {
                            doJump = true;
                            failJump = true;
                        } else {
                            doJump = false;
                            failJump = false;
                        }
                    } else {
                        doJump = true;
                        failJump = false;
                    }
                    if (skip) {
                        skip = false;
                        return;
                    }
                    switch (jumpMode.getValue()) {
                        case Jump -> mc.player.jumpFromGround();
                        case Motion ->
                                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x(), motion.getValue(), mc.player.getDeltaMovement().z());
                        case Both -> {
                            mc.player.jumpFromGround();
                            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x(), motion.getValue(), mc.player.getDeltaMovement().z());
                        }
                    }
                }
            }
            case GrimNew -> {
                if (flag) {
                    if (ccCooldown <= 0) {
                        sendPacket(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), ((IEntity) mc.player).getLastYaw(), ((IEntity) mc.player).getLastPitch(), mc.player.onGround(), mc.player.horizontalCollision));
                        sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, BlockPos.containing(mc.player.position()), Direction.DOWN));
                    }
                    flag = false;
                }
            }
        }
        if (grimTicks > 0)
            grimTicks--;
    }

    private boolean isValidMotion(double motion, double min, double max) {
        return Math.abs(motion) > min && Math.abs(motion) < max;
    }

    @Override
    public void onEnable() {
        grimTicks = 0;
    }

    public enum modeEn {
        Matrix, Cancel, Sunrise, Custom, Redirect, OldGrim, Jump, GrimNew
    }

    public enum jumpModeEn {
        Motion, Jump, Both
    }
}
