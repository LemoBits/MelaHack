package thunder.hack.utility.world;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableInt;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.utility.math.PredictUtility;

import java.util.Objects;

import static thunder.hack.features.modules.Module.mc;

public final class ExplosionUtility {

    public static boolean terrainIgnore = false;
    private static DamageSource getExplosionDamageSource() {
        return Explosion.getDefaultDamageSource(mc.level, mc.player);
    }

    /**
     * Calculate target damage based on crystal position and target. Uses AutoCrystal settings so use only in AutoCrystal
     *
     * @param crystalPos the position of the crystal whose damage is to be calculated
     * @param target     the damage will be calculated on this entity
     * @param optimized  use light calculate
     * @return damage value in Float format
     */
    public static float getAutoCrystalDamage(Vec3 crystalPos, Player target, int predictTicks, boolean optimized) {
        if (predictTicks == 0) return getExplosionDamage(crystalPos, target, optimized);
        else
            return getExplosionDamageWPredict(crystalPos, target, PredictUtility.predictBox(target, predictTicks), optimized);
    }

    /**
     * Calculate self damage based on crystal position and self extrapolation (predict).
     *
     * @param explosionPos the position of the explosion whose damage is to be calculated
     * @param predictTicks the number of game ticks for which the player's position should be predicted
     * @param optimized    use light calculate
     * @return damage value in Float format
     */
    public static float getSelfExplosionDamage(Vec3 explosionPos, int predictTicks, boolean optimized) {
        return getAutoCrystalDamage(explosionPos, mc.player, predictTicks, optimized);
    }

    /**
     * Calculate target damage based on crystal position and target.
     *
     * @param explosionPos the position of the explosion whose damage is to be calculated
     * @param target       the damage will be calculated on this entity
     * @param optimized    use light calculate
     * @return damage value in Float format
     */
    public static float getExplosionDamage(Vec3 explosionPos, Player target, boolean optimized) {
        if (mc.level.getDifficulty() == Difficulty.PEACEFUL || target == null) return 0f;

        DamageSource damageSource = getExplosionDamageSource();

        if (!new AABB(Mth.floor(explosionPos.x - 11), Mth.floor(explosionPos.y - 11), Mth.floor(explosionPos.z - 11), Mth.floor(explosionPos.x + 13), Mth.floor(explosionPos.y + 13), Mth.floor(explosionPos.z + 13)).intersects(target.getBoundingBox()))
            return 0f;

        if (!target.isInvulnerable()) {
            double distExposure = (float) target.distanceToSqr(explosionPos) / 144.;
            if (distExposure <= 1.0) {
                terrainIgnore = ModuleManager.autoCrystal.ignoreTerrain.getValue();
                double exposure = getExposure(explosionPos, target.getBoundingBox(), optimized);
                terrainIgnore = false;
                double finalExposure = (1.0 - distExposure) * exposure;

                float toDamage = (float) Math.floor((finalExposure * finalExposure + finalExposure) / 2. * 7. * 12. + 1.);

                if (mc.level.getDifficulty() == Difficulty.EASY) toDamage = Math.min(toDamage / 2f + 1f, toDamage);
                else if (mc.level.getDifficulty() == Difficulty.HARD) toDamage = toDamage * 3f / 2f;

                toDamage = CombatRules.getDamageAfterAbsorb(target, toDamage, damageSource, target.getArmorValue(), (float) target.getAttribute(Attributes.ARMOR_TOUGHNESS).getValue());

                if (target.hasEffect(MobEffects.RESISTANCE)) {
                    int resistance = 25 - (target.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
                    float resistance_1 = toDamage * resistance;
                    toDamage = Math.max(resistance_1 / 25f, 0f);
                }

                if (toDamage <= 0f) toDamage = 0f;
                else {
                    float protAmount = ModuleManager.autoCrystal.assumeBestArmor.getValue() ? 32f : getProtectionAmount(thunder.hack.utility.player.ArmorUtility.getArmorItems(target));

                    if (protAmount > 0)
                        toDamage = CombatRules.getDamageAfterMagicAbsorb(toDamage, protAmount);
                }
                return toDamage;
            }
        }
        return 0f;
    }

    /**
     * Calculate target damage based on crystal position, target and predicted copy of target.
     *
     * @param explosionPos the position of the explosion whose damage is to be calculated
     * @param target       the damage will be calculated on this entity
     * @param predict      predicted copy of target
     * @return damage value in Float format
     */
    public static float getExplosionDamageWPredict(Vec3 explosionPos, Player target, AABB predict, boolean optimized) {
        if (mc.level.getDifficulty() == Difficulty.PEACEFUL) return 0f;

        if (target == null || predict == null) return 0f;

        DamageSource damageSource = getExplosionDamageSource();

        if (!new AABB(Mth.floor(explosionPos.x - 11d), Mth.floor(explosionPos.y - 11d), Mth.floor(explosionPos.z - 11d), Mth.floor(explosionPos.x + 13d), Mth.floor(explosionPos.y + 13d), Mth.floor(explosionPos.z + 13d)).intersects(predict))
            return 0f;

        if (!target.isInvulnerable()) {
            double distExposure = predict.getCenter().add(0, -0.9, 0).distanceToSqr(explosionPos) / 144.;
            if (distExposure <= 1.0) {
                terrainIgnore = ModuleManager.autoCrystal.ignoreTerrain.getValue();
                double exposure = getExposure(explosionPos, predict, optimized);
                terrainIgnore = false;
                double finalExposure = (1.0 - distExposure) * exposure;

                float toDamage = (float) Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * 12d + 1.0);

                if (mc.level.getDifficulty() == Difficulty.EASY) toDamage = Math.min(toDamage / 2f + 1f, toDamage);
                else if (mc.level.getDifficulty() == Difficulty.HARD) toDamage = toDamage * 3f / 2f;

                toDamage = CombatRules.getDamageAfterAbsorb(target, toDamage, damageSource, target.getArmorValue(), (float) Objects.requireNonNull(target.getAttribute(Attributes.ARMOR_TOUGHNESS)).getValue());

                if (target.hasEffect(MobEffects.RESISTANCE)) {
                    int resistance = 25 - (Objects.requireNonNull(target.getEffect(MobEffects.RESISTANCE)).getAmplifier() + 1) * 5;
                    float resistance_1 = toDamage * resistance;
                    toDamage = Math.max(resistance_1 / 25f, 0f);
                }

                if (toDamage <= 0f) toDamage = 0f;
                else {
                    float protAmount = ModuleManager.autoCrystal.assumeBestArmor.getValue() ? 32f : getProtectionAmount(thunder.hack.utility.player.ArmorUtility.getArmorItems(target));

                    if (protAmount > 0) toDamage = CombatRules.getDamageAfterMagicAbsorb(toDamage, protAmount);
                }
                return toDamage;
            }
        }
        return 0f;
    }

    /**
     * Returns the BlockHitResult of the block without considering other blocks
     *
     * @param context context with point of player's eyes and final aiming point
     * @param block   position of block
     * @return BlockHitResult
     */
    public static BlockHitResult rayCastBlock(ClipContext context, BlockPos block) {
        return BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (raycastContext, blockPos) -> {
            BlockState blockState;

            if (!blockPos.equals(block)) blockState = Blocks.AIR.defaultBlockState();
            else blockState = Blocks.OBSIDIAN.defaultBlockState();

            Vec3 vec3d = raycastContext.getFrom();
            Vec3 vec3d2 = raycastContext.getTo();
            VoxelShape voxelShape = raycastContext.getBlockShape(blockState, mc.level, blockPos);
            BlockHitResult blockHitResult = mc.level.clipWithInteractionOverride(vec3d, vec3d2, blockPos, voxelShape, blockState);
            VoxelShape voxelShape2 = Shapes.empty();
            BlockHitResult blockHitResult2 = voxelShape2.clip(vec3d, vec3d2, blockPos);

            double d = blockHitResult == null ? Double.MAX_VALUE : raycastContext.getFrom().distanceToSqr(blockHitResult.getLocation());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : raycastContext.getFrom().distanceToSqr(blockHitResult2.getLocation());

            return d <= e ? blockHitResult : blockHitResult2;
        }, (raycastContext) -> {
            Vec3 vec3d = raycastContext.getFrom().subtract(raycastContext.getTo());
            return BlockHitResult.miss(raycastContext.getTo(), Direction.getApproximateNearest(vec3d.x, vec3d.y, vec3d.z), BlockPos.containing(raycastContext.getTo()));
        });
    }

    /**
     * Calculate target damage based on explosion position, target and blockpos which, regardless of state, will be counted as obsidian
     *
     * @param explosionPos the position of the explosion whose damage is to be calculated
     * @param target       the damage will be calculated on this entity
     * @param bp           blockpos which, regardless of state, will be counted as obsidian
     * @return damage value in Float format
     */
    public static float getDamageOfGhostBlock(Vec3 explosionPos, Player target, BlockPos bp) {

        if (mc.level.getDifficulty() == Difficulty.PEACEFUL) return 0f;

        DamageSource damageSource = getExplosionDamageSource();

        double maxDist = 12;
        if (!new AABB(Mth.floor(explosionPos.x - maxDist - 1.0), Mth.floor(explosionPos.y - maxDist - 1.0), Mth.floor(explosionPos.z - maxDist - 1.0), Mth.floor(explosionPos.x + maxDist + 1.0), Mth.floor(explosionPos.y + maxDist + 1.0), Mth.floor(explosionPos.z + maxDist + 1.0)).intersects(target.getBoundingBox())) {
            return 0f;
        }

        if (!target.isInvulnerable()) {
            double distExposure = target.distanceToSqr(explosionPos) / 144.;
            if (distExposure <= 1.0) {
                terrainIgnore = ModuleManager.autoCrystal.ignoreTerrain.getValue();
                double exposure = getExposureGhost(explosionPos, target, bp);
                terrainIgnore = false;
                double finalExposure = (1.0 - distExposure) * exposure;

                float toDamage = (float) Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * maxDist + 1.0);

                if (mc.level.getDifficulty() == Difficulty.EASY) {
                    toDamage = Math.min(toDamage / 2f + 1f, toDamage);
                } else if (mc.level.getDifficulty() == Difficulty.HARD) {
                    toDamage = toDamage * 3f / 2f;
                }

                toDamage = CombatRules.getDamageAfterAbsorb(target, toDamage, damageSource, target.getArmorValue(), (float) target.getAttribute(Attributes.ARMOR_TOUGHNESS).getValue());

                if (target.hasEffect(MobEffects.RESISTANCE)) {
                    int resistance = 25 - (target.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
                    float resistance_1 = toDamage * resistance;
                    toDamage = Math.max(resistance_1 / 25f, 0f);
                }

                if (toDamage <= 0f) toDamage = 0f;
                else {
                    float protAmount = ModuleManager.autoCrystal.assumeBestArmor.getValue() ? 32f : getProtectionAmount(thunder.hack.utility.player.ArmorUtility.getArmorItems(target));

                    if (protAmount > 0) toDamage = CombatRules.getDamageAfterMagicAbsorb(toDamage, protAmount);
                }
                return toDamage;
            }
        }
        return 0f;
    }

    private static float getExposureGhost(Vec3 source, Entity entity, BlockPos pos) {
        AABB box = entity.getBoundingBox();
        double d = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double e = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double f = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double g = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
        double h = (1.0 - Math.floor(1.0 / f) * f) / 2.0;

        if (d < 0.0 || e < 0.0 || f < 0.0) {
            return 0.0f;
        }

        int i = 0;
        int j = 0;

        for (double k = 0.0; k <= 1.0; k += d) {
            for (double l = 0.0; l <= 1.0; l += e) {
                for (double m = 0.0; m <= 1.0; m += f) {
                    double n = Mth.lerp(k, box.minX, box.maxX);
                    double o = Mth.lerp(l, box.minY, box.maxY);
                    double p = Mth.lerp(m, box.minZ, box.maxZ);
                    Vec3 vec3d = new Vec3(n + g, o, p + h);
                    if (raycastGhost(new ClipContext(vec3d, source, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity), pos).getType() == HitResult.Type.MISS)
                        ++i;
                    ++j;
                }
            }
        }

        return (float) i / (float) j;
    }

    public static float getExposure(Vec3 source, AABB box, boolean optimized) {
        if (!optimized) return getExposure(source, box);

        int miss = 0;
        int hit = 0;

        for (int k = 0; k <= 1; k += 1) {
            for (int l = 0; l <= 1; l += 1) {
                for (int m = 0; m <= 1; m += 1) {
                    double n = Mth.lerp(k, box.minX, box.maxX);
                    double o = Mth.lerp(l, box.minY, box.maxY);
                    double p = Mth.lerp(m, box.minZ, box.maxZ);
                    Vec3 vec3d = new Vec3(n, o, p);
                    if (raycast(vec3d, source, ModuleManager.autoCrystal.ignoreTerrain.getValue()) == HitResult.Type.MISS)
                        ++miss;
                    ++hit;
                }
            }
        }
        return (float) miss / (float) hit;
    }

    public static float getExposure(Vec3 source, AABB box) {
        double d = 0.4545454446934474;
        double e = 0.21739130885479366;
        double f = 0.4545454446934474;

        int i = 0;
        int j = 0;

        for (double k = 0.0; k <= 1.0; k += d)
            for (double l = 0.0; l <= 1.0; l += e)
                for (double m = 0.0; m <= 1.0; m += f) {
                    double n = Mth.lerp(k, box.minX, box.maxX);
                    double o = Mth.lerp(l, box.minY, box.maxY);
                    double p = Mth.lerp(m, box.minZ, box.maxZ);
                    Vec3 vec3d = new Vec3(n + 0.045454555306552624, o, p + 0.045454555306552624);
                    if (raycast(vec3d, source, ModuleManager.autoCrystal.ignoreTerrain.getValue()) == HitResult.Type.MISS)
                        ++i;
                    ++j;
                }

        return (float) i / (float) j;
    }

    private static BlockHitResult raycastGhost(ClipContext context, BlockPos bPos) {
        return BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (innerContext, pos) -> {
            Vec3 vec3d = innerContext.getFrom();
            Vec3 vec3d2 = innerContext.getTo();

            BlockState blockState;

            if (!pos.equals(bPos)) blockState = mc.level.getBlockState(bPos);
            else blockState = Blocks.OBSIDIAN.defaultBlockState();

            VoxelShape voxelShape = innerContext.getBlockShape(blockState, mc.level, pos);
            BlockHitResult blockHitResult = mc.level.clipWithInteractionOverride(vec3d, vec3d2, pos, voxelShape, blockState);
            BlockHitResult blockHitResult2 = Shapes.empty().clip(vec3d, vec3d2, pos);
            double d = blockHitResult == null ? Double.MAX_VALUE : innerContext.getFrom().distanceToSqr(blockHitResult.getLocation());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : innerContext.getFrom().distanceToSqr(blockHitResult2.getLocation());
            return d <= e ? blockHitResult : blockHitResult2;
        }, innerContext -> {
            Vec3 vec3d = innerContext.getFrom().subtract(innerContext.getTo());
            return BlockHitResult.miss(innerContext.getTo(), Direction.getApproximateNearest(vec3d.x, vec3d.y, vec3d.z), BlockPos.containing(innerContext.getTo()));
        });
    }

    public static HitResult.Type raycast(Vec3 start, Vec3 end, boolean ignoreTerrain) {
        return BlockGetter.traverseBlocks(start, end, null, (innerContext, blockPos) -> {
            BlockState blockState = mc.level.getBlockState(blockPos);
            if (blockState.getBlock().getExplosionResistance() < 600 && ignoreTerrain) return null;
            BlockHitResult hitResult = blockState.getCollisionShape(mc.level, blockPos).clip(start, end, blockPos);
            return hitResult == null ? null : hitResult.getType();
        }, (innerContext) -> HitResult.Type.MISS);
    }


    public static int getProtectionAmount(Iterable<ItemStack> equipment) {
        MutableInt mutableInt = new MutableInt();
        equipment.forEach(i -> mutableInt.add(getProtectionAmount(i)));
        return mutableInt.intValue();
    }

    public static int getProtectionAmount(ItemStack stack) {
        var enchantments = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int modifierBlast = EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.BLAST_PROTECTION), stack);
        int modifier = EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.PROTECTION), stack);
        return modifierBlast * 2 + modifier;
    }
}
