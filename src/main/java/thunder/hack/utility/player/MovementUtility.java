package thunder.hack.utility.player;

import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventMove;
import thunder.hack.features.modules.Module;
import thunder.hack.injection.accesors.IInput;

import static thunder.hack.features.modules.Module.mc;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec2;

public final class MovementUtility {
    public static boolean isMoving() {
        return mc.player != null && mc.level != null && mc.player.input != null && (mc.player.input.getMoveVector().y != 0.0 || mc.player.input.getMoveVector().x != 0.0);
    }

    public static void setMovementInput(float x, float y) {
        if (mc.player == null || mc.player.input == null) return;
        ((IInput) mc.player.input).setMovementVector(new Vec2(x, y));
    }

    public static void clearMovementInput() {
        setMovementInput(0f, 0f);
    }

    public static void setMovementInputX(float x) {
        if (mc.player == null || mc.player.input == null) return;
        Vec2 movement = mc.player.input.getMoveVector();
        setMovementInput(x, movement.y);
    }

    public static void setMovementInputY(float y) {
        if (mc.player == null || mc.player.input == null) return;
        Vec2 movement = mc.player.input.getMoveVector();
        setMovementInput(movement.x, y);
    }

    public static void scaleMovementInput(float xScale, float yScale) {
        if (mc.player == null || mc.player.input == null) return;
        Vec2 movement = mc.player.input.getMoveVector();
        setMovementInput(movement.x * xScale, movement.y * yScale);
    }

    public static double getSpeed() {
        return Math.hypot(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().z);
    }

    public static double[] forward(final double d) {
        float f = mc.player.input.getMoveVector().y;
        float f2 = mc.player.input.getMoveVector().x;
        float f3 = mc.player.getYRot();
        if (f != 0.0f) {
            if (f2 > 0.0f) {
                f3 += ((f > 0.0f) ? -45 : 45);
            } else if (f2 < 0.0f) {
                f3 += ((f > 0.0f) ? 45 : -45);
            }
            f2 = 0.0f;
            if (f > 0.0f) {
                f = 1.0f;
            } else if (f < 0.0f) {
                f = -1.0f;
            }
        }
        final double d2 = Math.sin(Math.toRadians(f3 + 90.0f));
        final double d3 = Math.cos(Math.toRadians(f3 + 90.0f));
        final double d4 = f * d * d3 + f2 * d * d2;
        final double d5 = f * d * d2 - f2 * d * d3;
        return new double[]{d4, d5};
    }

    public static void setMotion(double speed) {
        double forward = mc.player.input.getMoveVector().y;
        double strafe = mc.player.input.getMoveVector().x;
        float yaw = mc.player.getYRot();
        if (forward == 0 && strafe == 0) {
            mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0);
        } else {
            if (forward != 0) {
                if (strafe > 0) {
                    yaw += (float) (forward > 0 ? -45 : 45);
                } else if (strafe < 0) {
                    yaw += (float) (forward > 0 ? 45 : -45);
                }
                strafe = 0;
                if (forward > 0) {
                    forward = 1;
                } else if (forward < 0) {
                    forward = -1;
                }
            }
            double sin = Mth.sin((float) Math.toRadians(yaw + 90));
            double cos = Mth.cos((float) Math.toRadians(yaw + 90));
            mc.player.setDeltaMovement(forward * speed * cos + strafe * speed * sin, mc.player.getDeltaMovement().y, forward * speed * sin - strafe * speed * cos);
        }
    }

    public static float getMoveDirection() {
        double forward = mc.player.input.getMoveVector().y;
        double strafe = mc.player.input.getMoveVector().x;

        if (strafe > 0) {
            strafe = 1;
        } else if (strafe < 0) {
            strafe = -1;
        }

        float yaw = mc.player.getYRot();
        if (forward == 0 && strafe == 0) {
            return yaw;
        } else {
            if (forward != 0) {
                if (strafe > 0)
                    yaw += forward > 0 ? -45f : -135f;
                else if (strafe < 0)
                    yaw += forward > 0 ? 45f : 135f;
                else if (forward < 0) {
                    yaw += 180f;
                }
            }
            if (forward == 0) {
                if (strafe > 0)
                    yaw -= 90f;
                else if (strafe < 0)
                    yaw += 90f;
            }
        }

        return yaw;
    }

    public static double[] forwardWithoutStrafe(final double d) {
        float f3 = mc.player.getYRot();
        final double d4 = d * Math.cos(Math.toRadians(f3 + 90.0f));
        final double d5 = d * Math.sin(Math.toRadians(f3 + 90.0f));
        return new double[]{d4, d5};
    }

    public static double getJumpSpeed() {
        double jumpSpeed = 0.3999999463558197;
        if (mc.player.hasEffect(MobEffects.JUMP_BOOST)) {
            double amplifier = mc.player.getEffect(MobEffects.JUMP_BOOST).getAmplifier();
            jumpSpeed += (amplifier + 1) * 0.1;
        }
        return jumpSpeed;
    }

    public static void modifyEventSpeed(EventMove event, double d) {
        double d2 = mc.player.input.getMoveVector().y;
        double d3 = mc.player.input.getMoveVector().x;
        float f = mc.player.getYRot();
        if (d2 == 0.0 && d3 == 0.0) {
            event.setX(0.0);
            event.setZ(0.0);
        } else {
            if (d2 != 0.0) {
                if (d3 > 0.0) {
                    f += (float) (d2 > 0.0 ? -45 : 45);
                } else if (d3 < 0.0) {
                    f += (float) (d2 > 0.0 ? 45 : -45);
                }

                d3 = 0.0;
                if (d2 > 0.0) {
                    d2 = 1.0;
                } else if (d2 < 0.0) {
                    d2 = -1.0;
                }
            }
            double sin = Math.sin(Math.toRadians(f + 90.0F));
            double cos = Math.cos(Math.toRadians(f + 90.0F));

            event.setX(d2 * d * cos + d3 * d * sin);
            event.setZ(d2 * d * sin - d3 * d * cos);
        }
    }

    public static double getBaseMoveSpeed() {
        int n;
        double d = 0.2873;

        if (Module.fullNullCheck()) return d;

        if (mc.player.hasEffect(MobEffects.SPEED)) {
            n = mc.player.getEffect(MobEffects.SPEED).getAmplifier();
            d *= 1.0 + 0.2 * (n + 1);
        }
        if (mc.player.hasEffect(MobEffects.JUMP_BOOST)) {
            n = mc.player.getEffect(MobEffects.JUMP_BOOST).getAmplifier();
            d /= 1.0 + 0.2 * (n + 1);
        }
        if (mc.player.hasEffect(MobEffects.SLOWNESS)) {
            n = mc.player.getEffect(MobEffects.SLOWNESS).getAmplifier();
            d /= 1.0 + (0.2 * (n + 1));
        }
        return d;
    }

    public static boolean sprintIsLegit(float yaw) {
        return (Math.abs(Math.abs(Mth.wrapDegrees(yaw)) - Math.abs(Mth.wrapDegrees(Managers.PLAYER.yaw))) < 40);
    }
}
