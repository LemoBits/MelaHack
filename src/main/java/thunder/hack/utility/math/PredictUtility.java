package thunder.hack.utility.math;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static thunder.hack.features.modules.Module.mc;

public class PredictUtility {
    public static Player movePlayer(Player entity, Vec3 newPos) {
        if (entity == null || newPos == null)
            return null;
        return equipAndReturn(entity, newPos);
    }

    public static Player predictPlayer(Player entity, int ticks) {
        Vec3 posVec = predictPosition(entity, ticks);
        if (posVec == null)
            return null;
        return equipAndReturn(entity, posVec);
    }

    public static Vec3 predictPosition(Player entity, int ticks) {
        if (entity == null)
            return null;

        Vec3 posVec = new Vec3(entity.getX(), entity.getY(), entity.getZ());

        double motionX = entity.getDeltaMovement().x();
        double motionZ = entity.getDeltaMovement().z();

        for (int i = 0; i < ticks; i++) {
            float hbDeltaX = motionX > 0 ? 0.3f : -0.3f;
            float hbDeltaZ = motionZ > 0 ? 0.3f : -0.3f;

            if (!mc.level.isEmptyBlock(BlockPos.containing(posVec.add(motionX + hbDeltaX, 0.1, motionZ + hbDeltaZ))) || !mc.level.isEmptyBlock(BlockPos.containing(posVec.add(motionX + hbDeltaX, 1, motionZ + hbDeltaZ)))) {
                motionX = 0;
                motionZ = 0;
            }
            posVec = posVec.add(motionX, 0, motionZ);
        }

        return posVec;
    }

    public static AABB predictBox(Player entity, int ticks) {
        Vec3 posVec = predictPosition(entity, ticks);
        if (posVec == null)
            return null;
        return createBox(posVec, entity);
    }

    public static Player equipAndReturn(Player original, Vec3 posVec) {
        Player copyEntity = new Player(mc.level, new GameProfile(UUID.fromString("66123666-1234-5432-6666-667563866600"), "PredictEntity339")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }

            @Override
            public net.minecraft.world.level.GameType gameMode() {
                return original.gameMode();
            }
        };

        copyEntity.setPos(posVec);
        copyEntity.setHealth(original.getHealth());
        copyEntity.xo = original.xo;
        copyEntity.zo = original.zo;
        copyEntity.yo = original.yo;
        copyEntity.getInventory().replaceWith(original.getInventory());
        for (MobEffectInstance se : original.getActiveEffects()) {
            copyEntity.addEffect(se);
        }

        return copyEntity;
    }

    public static AABB createBox(Vec3 vec, Entity entity) {
        return entity.getBoundingBox().move(entity.position().vectorTo(vec));
    }
}
