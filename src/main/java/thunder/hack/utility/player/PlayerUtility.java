package thunder.hack.utility.player;

import org.jetbrains.annotations.NotNull;
import thunder.hack.utility.world.ExplosionUtility;

import java.util.Objects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import static thunder.hack.features.modules.Module.mc;

public final class PlayerUtility {
    public static boolean isInHell() {
        if (mc.level == null) return false;
        return Objects.equals(mc.level.dimension().identifier().getPath(), "the_nether");
    }

    public static boolean isInEnd() {
        if (mc.level == null) return false;
        return Objects.equals(mc.level.dimension().identifier().getPath(), "the_end");
    }

    public static boolean isInOver() {
        if (mc.level == null) return false;
        return Objects.equals(mc.level.dimension().identifier().getPath(), "overworld");
    }

    public static boolean isEating() {
        if (mc.player == null) return false;

        return (mc.player.getMainHandItem().getComponents().has(DataComponents.FOOD)
                || mc.player.getOffhandItem().getComponents().has(DataComponents.FOOD))
                && mc.player.isUsingItem();
    }

    public static boolean isMining() {
        if (mc.gameMode == null) return false;

        return mc.gameMode.isDestroying();
    }

    public static float squaredDistanceFromEyes(@NotNull Vec3 targetPos) {
        if (mc.player == null) return 0.0f;

        double dx = targetPos.x - mc.player.getX();
        double dy = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = targetPos.z - mc.player.getZ();

        return (float) (dx * dx + dy * dy + dz * dz);
    }


    public static float squaredDistance2d(@NotNull Vec2 point) {
        if (mc.player == null) return 0f;

        double d = mc.player.getX() - point.x;
        double f = mc.player.getZ() - point.y;
        return (float) (d * d + f * f);
    }

    public static LocalPlayer getPlayer() {
        return mc.player;
    }

    public static float calculatePercentage(@NotNull ItemStack stack) {
        float durability = stack.getMaxDamage() - stack.getDamageValue();
        return (durability / (float) stack.getMaxDamage()) * 100F;
    }

    public static float fixAngle(float angle) {
        return Math.round(angle / ((float) (getGCD() * 0.15D))) * (float) (getGCD() * 0.15D);
    }

    public static float getGCD() {
        double sensitivity = mc.options.sensitivity().get();
        double value = sensitivity * 0.6 + 0.2;
        double result = Math.pow(value, 3) * 8.0;

        return (float) result;
    }


    public static float squaredDistance2d(double x, double z) {
        if (mc.player == null) return 0f;

        double d = mc.player.getX() - x;
        double f = mc.player.getZ() - z;
        return (float) (d * d + f * f);
    }

    public static float getSquaredDistance2D(Vec3 vec) {
        double d0 = mc.player.getX() - vec.x();
        double d2 = mc.player.getZ() - vec.z();
        return (float) (d0 * d0 + d2 * d2);
    }

    public static boolean canSee(Vec3 pos) {
        Vec3 vec3d = new Vec3(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
        if (pos.distanceTo(vec3d) > 128.0)
            return false;
        else
            return ExplosionUtility.raycast(vec3d, pos, false) == HitResult.Type.MISS;
    }

    public static boolean isFalling() {
        if (mc.player == null) {
            return false;
        }

        return !mc.player.onGround() && !mc.player.isCreative() && mc.player.getDeltaMovement().y < 0;
    }
}
