package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.core.manager.player.CombatManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.player.SpeedMine;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.world.ExplosionUtility;
import thunder.hack.utility.world.HoleUtility;

import java.util.ArrayList;
import java.util.Comparator;

public final class Breaker extends Module {
    private final Setting<Target> targetMode = new Setting<>("Target", Target.AutoCrystal);
    private final Setting<Boolean> onlyIfHole = new Setting<>("OnlyIfHole", false);
    private final Setting<CombatManager.TargetBy> targetBy = new Setting<>("TargetBy", CombatManager.TargetBy.Distance, v -> targetMode.is(Target.Breaker));
    private final Setting<Integer> range = new Setting<>("Range", 5, 1, 7, v -> targetMode.is(Target.Breaker));
    private final Setting<Float> minDamage = new Setting<>("MinDamage", 7f, 0f, 36f);
    private final Setting<Float> maxSelfDamage = new Setting<>("MaxSelfDamage", 4f, 0f, 36f);
    private final Setting<Boolean> cevPriority = new Setting<>("CevPriority", true);
    private final Setting<Boolean> antiShulker = new Setting<>("AntiShulker", true);

    private enum Target {
        AutoCrystal, Breaker
    }

    private BlockPos blockPos;

    private final Timer pause = new Timer();

    public Breaker() {
        super("Breaker", Category.COMBAT);
    }

    @EventHandler
    @SuppressWarnings("unused")
    private void onSync(EventSync event) {
        Player target;

        if (targetMode.is(Target.Breaker))
            target = Managers.COMBAT.getTarget(range.getValue(), targetBy.getValue());
        else
            target = AutoCrystal.target;

        if (target == null)
            return;

        BlockPos burrow = BlockPos.containing(target.position());
        BlockState burrowState = mc.level.getBlockState(burrow);

        if (!pause.passedMs(600))
            return;

        if (blockPos != null) {
            if (mc.level.isEmptyBlock(blockPos) || mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(blockPos)) > (ModuleManager.speedMine.isEnabled() ? ModuleManager.speedMine.range.getPow2Value() : ModuleManager.reach.isEnabled() ? ModuleManager.reach.blocksRange.getPow2Value() : 9)) {
                blockPos = null;
                return;
            }

            if (ModuleManager.speedMine.isEnabled()) {
                //   if (SpeedMine.minePosition == blockPos || (SpeedMine.minePosition != null && !mc.world.isAir(SpeedMine.minePosition)))
                //                    return;

                if (ModuleManager.speedMine.alreadyActing(blockPos))
                    return;

                for (SpeedMine.MineAction action : ModuleManager.speedMine.actions)
                    if (action.instantBreaking())
                        return;

                mc.gameMode.startDestroyBlock(blockPos, Direction.UP);
            } else mc.gameMode.continueDestroyBlock(blockPos, Direction.UP);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        ArrayList<BreakData> list = new ArrayList<>();

        if (cevPriority.getValue()) {
            for (int y = 2; y <= 3; y++) {
                BlockPos bp = BlockPos.containing(target.getX(), target.getY() + y, target.getZ());
                if (mc.level.getBlockState(bp).getBlock() == Blocks.OBSIDIAN
                        && !bp.equals(BlockPos.containing(target.position()).below())) {
                    if (ModuleManager.autoCrystal.getInteractResult(bp, new Vec3(0.5f + bp.getX(), 1f + bp.getY(), 0.5f + bp.getZ())) == null)
                        continue;
                    BlockState currentState = mc.level.getBlockState(bp);
                    mc.level.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
                    float damage = ExplosionUtility.getExplosionDamage(net.minecraft.world.phys.Vec3.atBottomCenterOf(bp).add(0, -0.5, 0), target, false);
                    float selfDamage = ExplosionUtility.getExplosionDamage(net.minecraft.world.phys.Vec3.atBottomCenterOf(bp).add(0, -0.5, 0), mc.player, false);
                    mc.level.setBlockAndUpdate(bp, currentState);
                    if ((Float.isNaN(ModuleManager.autoCrystal.renderDamage) || ModuleManager.autoCrystal.renderDamage < damage) && selfDamage < maxSelfDamage.getValue() && damage >= minDamage.getValue())
                        list.add(new BreakData(bp, damage));
                }
            }

            BreakData best = list.stream().max(Comparator.comparing(BreakData::damage)).orElse(null);

            if (best != null && cevPriority.getValue())
                list.add(new BreakData(best.blockPos, 999));
        }

        boolean inBurrow = burrowState.getBlock() == Blocks.OBSIDIAN || burrowState.getBlock() == Blocks.ENDER_CHEST;

        if (inBurrow) {
            list.add(new BreakData(burrow, 995));
            mc.level.setBlockAndUpdate(burrow, Blocks.AIR.defaultBlockState());
        } else if (onlyIfHole.getValue() && !HoleUtility.isHole(BlockPos.containing(target.position())))
            return;

        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (y > 1 && (x == -2 || z == -2 || x == 2 || z == 2))
                        continue;
                    BlockPos bp = BlockPos.containing(target.getX() + x, target.getY() + y, target.getZ() + z);

                    if (mc.level.getBlockState(bp).getBlock() instanceof ShulkerBoxBlock && antiShulker.getValue())
                        list.add(new BreakData(burrow, 990));

                    if ((mc.level.getBlockState(bp).getBlock() == Blocks.OBSIDIAN || mc.level.getBlockState(bp).getBlock() == Blocks.ENDER_CHEST)
                            && (mc.level.getBlockState(bp.below()).getBlock() == Blocks.OBSIDIAN || mc.level.getBlockState(bp.below()).getBlock() == Blocks.BEDROCK)
                            && !bp.equals(BlockPos.containing(target.position()).below())
                    ) {
                        if (ModuleManager.autoCrystal.getInteractResult(bp, new Vec3(0.5f + bp.getX(), 1f + bp.getY(), 0.5f + bp.getZ())) == null)
                            continue;

                        BlockState currentState = mc.level.getBlockState(bp);
                        mc.level.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
                        float damage = ExplosionUtility.getExplosionDamage(net.minecraft.world.phys.Vec3.atBottomCenterOf(bp).add(0, -0.5, 0), target, false);
                        float selfDamage = ExplosionUtility.getExplosionDamage(net.minecraft.world.phys.Vec3.atBottomCenterOf(bp).add(0, -0.5, 0), mc.player, false);
                        mc.level.setBlockAndUpdate(bp, currentState);

                        if (ModuleManager.autoCrystal.renderDamage < damage && selfDamage <= maxSelfDamage.getValue() && damage >= minDamage.getValue() && bp != blockPos)
                            list.add(new BreakData(bp, damage));
                    }
                }
            }
        }

        if (inBurrow)
            mc.level.setBlockAndUpdate(burrow, burrowState);

        BreakData best = list.stream().max(Comparator.comparing(BreakData::damage)).orElse(null);
        BreakData secondBest = ModuleManager.speedMine.doubleMine.getValue() ? list.stream().sorted(Comparator.comparing(BreakData::damage).reversed()).skip(1).findFirst().orElse(null) : null;

        blockPos = best == null ? null : (!ModuleManager.speedMine.alreadyActing(best.blockPos()) || secondBest == null ? best.blockPos() : secondBest.blockPos());
    }

    public void pause() {
        pause.reset();
    }

    private record BreakData(BlockPos blockPos, float damage) {
    }
}
