package thunder.hack.features.modules.combat;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventEntityRemoved;
import thunder.hack.events.impl.EventPostSync;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.injection.accesors.IEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.world.ExplosionUtility;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.PlayerUtility;
import thunder.hack.utility.player.SearchInvResult;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import java.util.List;

import java.awt.*;
import java.util.*;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

import com.mojang.blaze3d.vertex.PoseStack;

public final class PistonAura extends Module {
    private final Setting<Integer> placeDelay = new Setting<>("Delay/Place", 1, 0, 25);
    private final Setting<Integer> blocksPerTick = new Setting<>("Block/Tick", 3, 1, 4);
    private final Setting<Pattern> patternsSetting = new Setting<>("Pattern", Pattern.All);
    private final Setting<Boolean> supportPlace = (new Setting<>("SupportPlace", false));
    private final Setting<Boolean> bypass = new Setting<>("FireBypass", false);
    private final Setting<Boolean> oldVersion = (new Setting<>("OldVersion", false));
    private final Setting<Boolean> trap = new Setting<>("Trap", false);
    private final Setting<Float> targetRange = new Setting<>("Target Range", 10.0f, 0.0f, 12.0f);
    private final Setting<Float> placeRange = new Setting<>("PlaceRange", 4.0f, 1.0f, 7.0f);
    private final Setting<Float> wallRange = new Setting<>("WallRange", 4.0f, 1.0f, 7.0f);
    private final Setting<InteractionUtility.PlaceMode> placeMode = new Setting<>("Place Mode", InteractionUtility.PlaceMode.Normal);
    private final Setting<InteractionUtility.Interact> interact = new Setting<>("Interact", InteractionUtility.Interact.Strict);
    private final Setting<InteractionUtility.Rotate> rotate = new Setting<>("Rotate", InteractionUtility.Rotate.None);

    public Player target;
    private BlockPos targetPos, pistonPos, crystalPos, redStonePos, firePos, pistonHeadPos;
    private boolean builtTrap, isFire;

    private final Timer trapTimer = new Timer();
    private final Timer attackTimer = new Timer();

    private int delay = 0;
    private Runnable postAction = null;
    private Stage stage = Stage.Searching;
    private EndCrystal lastCrystal;
    private Vec3 rotations;

    public PistonAura() {
        super("PistonAura", Category.COMBAT);
    }

    public void reset() {
        builtTrap = false;
        target = null;
        stage = Stage.Searching;
        trapTimer.reset();
        attackTimer.reset();
        rotations = Vec3.ZERO;
        pistonPos = null;
        targetPos = null;
        firePos = null;
        pistonHeadPos = null;
        lastCrystal = null;
        delay = 0;
        trapTimer.reset();
        attackTimer.reset();
        postAction = null;
    }

    @Override
    public void onEnable() {
        reset();
    }

    @EventHandler
    public void onSync(EventSync event) {
        if (delay < placeDelay.getValue()) delay++;

        if (stage == Stage.Break) {
            if (isFire) {
                reset();
                return;
            }
            breakCrystal();
            return;
        }

        if (delay < placeDelay.getValue()) return;

        handlePistonAura(false);
    }

    @EventHandler
    @SuppressWarnings("unused")
    private void onPostSync(EventPostSync event) {
        if (postAction != null) {
            delay = 0;
            postAction.run();
            postAction = null;
            int extraBlocks = 1;
            while (extraBlocks < blocksPerTick.getValue()) {
                handlePistonAura(true);
                if (postAction != null) {
                    postAction.run();
                    postAction = null;
                } else {
                    return;
                }
                extraBlocks++;
            }
        }
        postAction = null;
    }


    public void handlePistonAura(boolean extra) {
        if (!InventoryUtility.findBlockInHotBar(Blocks.OBSIDIAN).found() && (trap.getValue() || supportPlace.getValue())) {
            disable(isRu() ? "Нет обсидиана!" : "No obsidian!");
            return;
        }

        if (!InventoryUtility.findBlockInHotBar(Blocks.REDSTONE_BLOCK).found() && !InventoryUtility.findBlockInHotBar(Blocks.REDSTONE_TORCH).found()) {
            disable(isRu() ? "Нет редстоуна!" : "No redstone!");
            return;
        }

        if (!InventoryUtility.findItemInHotBar(Items.END_CRYSTAL).found() && mc.player.getOffhandItem().getItem() != Items.END_CRYSTAL) {
            disable(isRu() ? "Нет кристаллов!" : "No crystals!");
            return;
        }

        if (!(InventoryUtility.findItemInHotBar(Items.PISTON).found() || InventoryUtility.findItemInHotBar(Items.STICKY_PISTON).found())) {
            disable(isRu() ? "Нет поршней!" : "No pistons!");
            return;
        }

        switch (stage) {
            case Searching -> {
                findPos();
                stage = Stage.Trap;
            }
            case Trap -> buildTrap();
            case Piston -> placePiston(extra);
            case Fire -> placeFire(extra);
            case Crystal -> placeCrystal(extra);
            case RedStone -> placeRedStone(extra);
            case Break -> {
                if (isFire)
                    stage = Stage.Searching;
            }
        }
    }

    private void placeRedStone(boolean extra) {
        if (redStonePos == null) {
            stage = Stage.Searching;
            return;
        }

        if (mc.level.getBlockState(redStonePos).getBlock() instanceof PoweredBlock) {
            stage = Stage.Break;
        }

        if (mc.level.getBlockState(redStonePos.below()).canBeReplaced() && supportPlace.getValue()) {
            InteractionUtility.placeBlock(redStonePos.below(), rotate.getValue(), interact.getValue(), placeMode.getValue(), InventoryUtility.findBlockInHotBar(Blocks.OBSIDIAN), false, false);
            return;
        }

        final float[] angle = InteractionUtility.getPlaceAngle(redStonePos, interact.getValue(), false);
        if (angle == null) return;
        if (extra) {
            sendPacket(new ServerboundMovePlayerPacket.Rot(angle[0], angle[1], mc.player.onGround(), mc.player.horizontalCollision));
        } else {
            mc.player.setYRot(angle[0]);
            mc.player.setXRot(angle[1]);
        }

        postAction = () -> {
            int redstone_slot = -1;

            SearchInvResult redBlockResult = InventoryUtility.findBlockInHotBar(Blocks.REDSTONE_BLOCK);
            SearchInvResult redTorchResult = InventoryUtility.findBlockInHotBar(Blocks.REDSTONE_TORCH);

            if (!redBlockResult.found()) {
                if (!redTorchResult.found()) disable(isRu() ? "Нет редстоуна!" : "No redstone!");
                else redstone_slot = redTorchResult.slot();
            } else redstone_slot = redBlockResult.slot();

            InteractionUtility.placeBlock(redStonePos, InteractionUtility.Rotate.None, interact.getValue(), placeMode.getValue(), redstone_slot, false, false);
            stage = Stage.Break;
        };
    }

    private void placeCrystal(boolean extra) {
        if (crystalPos == null) {
            stage = Stage.Searching;
            return;
        }

        if (mc.level.getBlockState(crystalPos).canBeReplaced() && supportPlace.getValue()) {
            InteractionUtility.placeBlock(crystalPos, rotate.getValue(), interact.getValue(), placeMode.getValue(), InventoryUtility.findBlockInHotBar(Blocks.OBSIDIAN), false, false);
            return;
        }

        BlockHitResult result = getPlaceData(crystalPos);
        if (result == null) return;
        float[] angle = InteractionUtility.calculateAngle(rotations);
        if (extra) {
            sendPacket(new ServerboundMovePlayerPacket.Rot(angle[0] + MathUtility.random(-0.2f, 0.2f), angle[1], mc.player.onGround(), mc.player.horizontalCollision));
        } else {
            mc.player.setYRot(angle[0] + MathUtility.random(-0.2f, 0.2f));
            mc.player.setXRot(angle[1]);
        }

        postAction = () -> {
            boolean offHand = mc.player.getOffhandItem().getItem() == Items.END_CRYSTAL;
            int prev_slot = -1;
            if (!offHand) {
                int crystal_slot = InventoryUtility.findItemInHotBar(Items.END_CRYSTAL).slot();
                prev_slot = mc.player.getInventory().getSelectedSlot();
                if (crystal_slot != -1) {
                    mc.player.getInventory().setSelectedSlot(crystal_slot);
                    sendPacket(new ServerboundSetCarriedItemPacket(crystal_slot));
                }
            }

            sendSequencedPacket(id -> new ServerboundUseItemOnPacket(offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, result, id));
            sendPacket(new ServerboundSwingPacket(offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND));

            if (!offHand) {
                mc.player.getInventory().setSelectedSlot(prev_slot);
                sendPacket(new ServerboundSetCarriedItemPacket(prev_slot));
            }

            stage = Stage.RedStone;
        };
    }

    private void placeFire(boolean extra) {
        if (firePos == null) {
            stage = Stage.Searching;
            return;
        }

        if (mc.level.getBlockState(firePos).getBlock() instanceof FireBlock) stage = Stage.Crystal;

        if (mc.level.getBlockState(firePos.below()).canBeReplaced() && supportPlace.getValue()) {
            InteractionUtility.placeBlock(firePos.below(), rotate.getValue(), interact.getValue(), placeMode.getValue(), InventoryUtility.findBlockInHotBar(Blocks.OBSIDIAN), false, false);
            return;
        }

        float[] angle = InteractionUtility.getPlaceAngle(firePos, interact.getValue(), false);
        if (angle == null) return;
        if (extra) {
            sendPacket(new ServerboundMovePlayerPacket.Rot(angle[0], angle[1], mc.player.onGround(), mc.player.horizontalCollision));
        } else {
            mc.player.setYRot(angle[0]);
            mc.player.setXRot(angle[1]);
        }
        postAction = () -> {
            InteractionUtility.placeBlock(firePos, InteractionUtility.Rotate.None, interact.getValue(), placeMode.getValue(), InventoryUtility.findItemInHotBar(Items.FLINT_AND_STEEL).slot(), false, false);
            stage = Stage.Crystal;
        };
    }

    public void buildTrap() {
        if (!trap.getValue()) {
            stage = Stage.Piston;
            return;
        }
        if (mc.level.getBlockState(targetPos.offset(0, 2, 0)).getBlock() == Blocks.OBSIDIAN || pistonPos.getY() >= targetPos.offset(0, 2, 0).getY()) {
            stage = Stage.Piston;
            return;
        }

        if (!builtTrap) {
            final BlockPos offset = new BlockPos(crystalPos.getX() - targetPos.getX(), 0, crystalPos.getZ() - targetPos.getZ());
            final BlockPos trapBase = targetPos.offset(offset.getX() * -1, 0, offset.getZ() * -1);

            List<BlockPos> trapPos = new ArrayList<>();
            trapPos.add(targetPos.offset(0, 2, 0));
            trapPos.add(trapBase.offset(0, 2, 0));
            trapPos.add(trapBase.offset(0, 1, 0));

            InventoryUtility.saveSlot();
            for (BlockPos bp : trapPos) {
                if (InteractionUtility.placeBlock(bp, rotate.getValue(), interact.getValue(), placeMode.getValue(), InventoryUtility.findBlockInHotBar(Blocks.OBSIDIAN), false, false)) {
                    if (bp == targetPos.offset(0, 2, 0)) {
                        builtTrap = true;
                        stage = Stage.Piston;
                    }
                    break;
                }
            }
            InventoryUtility.returnSlot();
        }
    }

    public void placePiston(boolean extra) {
        if (pistonPos == null) {
            stage = Stage.Searching;
            return;
        }

        if (pistonHeadPos == null) {
            stage = Stage.Searching;
            return;
        }

        if (mc.level.getBlockState(pistonPos).getBlock() instanceof PistonBaseBlock) {
            stage = isFire ? Stage.Fire : Stage.Crystal;
        }

        if (mc.level.getBlockState(pistonPos.below()).canBeReplaced() && supportPlace.getValue()) {
            InteractionUtility.placeBlock(pistonPos.below(), rotate.getValue(), interact.getValue(), placeMode.getValue(), InventoryUtility.findBlockInHotBar(Blocks.OBSIDIAN), false, false);
            return;
        }

        final float[] angle = InteractionUtility.getPlaceAngle(pistonPos, interact.getValue(), false);
        if (angle == null) return;
        if (extra) {
            sendPacket(new ServerboundMovePlayerPacket.Rot(angle[0], angle[1], mc.player.onGround(), mc.player.horizontalCollision));
        } else {
            mc.player.setYRot(angle[0]);
            mc.player.setXRot(angle[1]);
        }


        postAction = () -> {
            int piston_slot;
            if (!InventoryUtility.findBlockInHotBar(Blocks.PISTON).found()) {
                if (!InventoryUtility.findBlockInHotBar(Blocks.STICKY_PISTON).found()) {
                    disable(isRu() ? "Нет поршней!" : "No pistons!");
                    return;
                } else {
                    piston_slot = InventoryUtility.findBlockInHotBar(Blocks.STICKY_PISTON).slot();
                }
            } else {
                piston_slot = InventoryUtility.findBlockInHotBar(Blocks.PISTON).slot();
            }


            final float angle2 = InteractionUtility.calculateAngle(pistonHeadPos.getCenter(), pistonPos.getCenter())[0];

            sendPacket(new ServerboundMovePlayerPacket.Rot(angle2, 0, mc.player.onGround(), mc.player.horizontalCollision));
            float prevYaw = mc.player.getYRot();
            mc.player.setYRot(angle2);
            ((thunder.hack.injection.accesors.IEntity) mc.player).setLastYaw(angle2);
            ((IEntity) mc.player).setLastYaw(angle2);
            int prevSlot = mc.player.getInventory().getSelectedSlot();
            InteractionUtility.placeBlock(pistonPos, InteractionUtility.Rotate.None, interact.getValue(), placeMode.getValue(), piston_slot, false, false);
            sendPacket(new ServerboundSetCarriedItemPacket(prevSlot));
            mc.player.getInventory().setSelectedSlot(prevSlot);
            mc.player.setYRot(prevYaw);

            stage = isFire ? Stage.Fire : Stage.Crystal;
        };
    }

    public @Nullable BlockHitResult getPlaceData(BlockPos bp) {
        Block base = mc.level.getBlockState(bp).getBlock();
        Block freeSpace = mc.level.getBlockState(bp.above()).getBlock();
        Block legacyFreeSpace = mc.level.getBlockState(bp.above().above()).getBlock();

        if (base != Blocks.OBSIDIAN && base != Blocks.BEDROCK)
            return null;

        if (!(freeSpace == Blocks.AIR && (!oldVersion.getValue() || legacyFreeSpace == Blocks.AIR)))
            return null;

        if (checkEntities(bp)) return null;

        Vec3 crystalVec = new Vec3(0.5f + bp.getX(), 1f + bp.getY(), 0.5f + bp.getZ());

        BlockHitResult interactResult = null;

        switch (interact.getValue()) {
            case Vanilla -> interactResult = getDefaultInteract(crystalVec, bp);
            case Strict -> interactResult = getStrictInteract(bp);
            case Legit -> interactResult = getLegitInteract(bp);
        }

        return interactResult;
    }

    private boolean checkEntities(@NotNull BlockPos base) {
        AABB posBoundingBox = new AABB(base.above());

        posBoundingBox = posBoundingBox.inflate(0, 1f, 0);

        for (Entity ent : mc.level.entitiesForRendering()) {
            if (ent == null) continue;
            if (ent.getBoundingBox().intersects(posBoundingBox)) {
                if (ent instanceof ExperienceOrb)
                    continue;
                if (ent instanceof EndCrystal) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private @Nullable BlockHitResult getDefaultInteract(Vec3 crystalVector, BlockPos bp) {
        if (PlayerUtility.squaredDistanceFromEyes(crystalVector) > placeRange.getPow2Value())
            return null;

        BlockHitResult wallCheck = mc.level.clip(new ClipContext(InteractionUtility.getEyesPos(mc.player), crystalVector, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

        if (wallCheck != null && wallCheck.getType() == HitResult.Type.BLOCK && wallCheck.getBlockPos() != bp)
            if (PlayerUtility.squaredDistanceFromEyes(crystalVector) > wallRange.getPow2Value())
                return null;

        return new BlockHitResult(crystalVector, Direction.DOWN, bp, false);
    }

    public @Nullable BlockHitResult getStrictInteract(@NotNull BlockPos bp) {
        float bestDistance = 999f;
        Direction bestDirection = null;
        Vec3 bestVector = null;

        if (mc.player.getEyePosition().y() > bp.above().getY()) {
            bestDirection = Direction.UP;
            bestVector = new Vec3(bp.getX() + 0.5, bp.getY() + 1, bp.getZ() + 0.5);
        } else if (mc.player.getEyePosition().y() < bp.getY()) {
            bestDirection = Direction.DOWN;
            bestVector = new Vec3(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5);
        } else {
            for (Direction dir : Direction.values()) {
                Vec3 directionVec = new Vec3(bp.getX() + 0.5 + dir.getUnitVec3i().getX() * 0.5, bp.getY() + 0.5 + dir.getUnitVec3i().getY() * 0.5, bp.getZ() + 0.5 + dir.getUnitVec3i().getZ() * 0.5);
                float distance = PlayerUtility.squaredDistanceFromEyes(directionVec);
                if (bestDistance > distance) {
                    bestDirection = dir;
                    bestVector = directionVec;
                    bestDistance = distance;
                }
            }
        }

        if (bestVector == null)
            return null;

        if (PlayerUtility.squaredDistanceFromEyes(bestVector) > placeRange.getPow2Value())
            return null;

        BlockHitResult wallCheck = mc.level.clip(new ClipContext(InteractionUtility.getEyesPos(mc.player), bestVector, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

        if (wallCheck != null && wallCheck.getType() == HitResult.Type.BLOCK && wallCheck.getBlockPos() != bp)
            if (PlayerUtility.squaredDistanceFromEyes(bestVector) > wallRange.getPow2Value())
                return null;

        return new BlockHitResult(bestVector, bestDirection, bp, false);
    }

    public BlockHitResult getLegitInteract(BlockPos bp) {
        float bestDistance = 999f;
        BlockHitResult bestResult = null;
        for (float x = 0f; x <= 1f; x += 0.2f) {
            for (float y = 0f; y <= 1f; y += 0.2f) {
                for (float z = 0f; z <= 1f; z += 0.2f) {
                    Vec3 point = new Vec3(bp.getX() + x, bp.getY() + y, bp.getZ() + z);
                    float distance = PlayerUtility.squaredDistanceFromEyes(point);

                    BlockHitResult wallCheck = mc.level.clip(new ClipContext(InteractionUtility.getEyesPos(mc.player), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                    if (wallCheck != null && wallCheck.getType() == HitResult.Type.BLOCK && wallCheck.getBlockPos() != bp)
                        if (distance > wallRange.getPow2Value())
                            continue;

                    BlockHitResult result = ExplosionUtility.rayCastBlock(new ClipContext(InteractionUtility.getEyesPos(mc.player), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player), bp);
                    if (distance > placeRange.getPow2Value())
                        continue;

                    if (distance < bestDistance) {
                        if (result != null && result.getType() == HitResult.Type.BLOCK) {
                            bestResult = result;
                            bestDistance = distance;
                        }
                    }
                }
            }
        }
        return bestResult;
    }

    public void breakCrystal() {
        for (Entity ent : mc.level.entitiesForRendering()) {
            if (!(ent instanceof EndCrystal) || target.distanceToSqr(ent.position()) > 16 || ent.tickCount < 2)
                continue;
            float[] angle = InteractionUtility.calculateAngle(ent.position());
            mc.player.setYRot(angle[0] + MathUtility.random(-3f, 3f));
            mc.player.setXRot(angle[1]);
            if (attackTimer.passedMs(200)) {
                mc.gameMode.attack(mc.player, ent);
                mc.player.swing(InteractionHand.MAIN_HAND);
                attackTimer.reset();
            }
            lastCrystal = (EndCrystal) ent;
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    private void onPacketReceive(PacketEvent.@NotNull Receive event) {
        if (event.getPacket() instanceof ClientboundSoundPacket && ((ClientboundSoundPacket) event.getPacket()).getSource().equals(SoundSource.BLOCKS) && ((ClientboundSoundPacket) event.getPacket()).getSound().value().equals(SoundEvents.GENERIC_EXPLODE)) {
            if (lastCrystal == null || !lastCrystal.isAlive())
                return;
            double soundRange = lastCrystal.distanceToSqr(((ClientboundSoundPacket) event.getPacket()).getX() + 0.5, ((ClientboundSoundPacket) event.getPacket()).getY() + 0.5, ((ClientboundSoundPacket) event.getPacket()).getZ() + 0.5);
            if (soundRange > 121)
                return;
            reset();
        }

        if (event.getPacket() instanceof ClientboundSoundEntityPacket && ((ClientboundSoundEntityPacket) event.getPacket()).getSource().equals(SoundSource.BLOCKS) && ((ClientboundSoundEntityPacket) event.getPacket()).getSound().value().equals(SoundEvents.GENERIC_EXPLODE)) {
            if (lastCrystal == null || !lastCrystal.isAlive())
                return;
            if (((ClientboundSoundEntityPacket) event.getPacket()).getId() != lastCrystal.getId())
                return;
            reset();
        }
    }


    @EventHandler
    @SuppressWarnings("unused")
    public void onEntityRemove(EventEntityRemoved e) {
        if (lastCrystal != null && lastCrystal == e.entity)
            reset();
    }


    @Override
    public @NotNull String getDisplayInfo() {
        return stage.toString() + (target != null ? " | " + target.getName().getString() : "");
    }

    @Override
    public void onRender3D(PoseStack stack) {
        if (pistonPos == null || crystalPos == null || redStonePos == null) {
            return;
        }
        Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(new AABB(pistonHeadPos.below()), Render2DEngine.injectAlpha(Color.CYAN, 100)));
        Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(new AABB(crystalPos), Render2DEngine.injectAlpha(Color.PINK, 100)));
        Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(new AABB(pistonPos.below()), Render2DEngine.injectAlpha(Color.GREEN, 100)));
        Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(new AABB(redStonePos.below()), Render2DEngine.injectAlpha(Color.RED, 100)));

        if (firePos != null)
            Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(new AABB(firePos.below()), Render2DEngine.injectAlpha(Color.yellow, 100)));
    }


    private void findPos() {
        ArrayList<Structure> list = new ArrayList<>();
        for (Player target : Objects.requireNonNull(getPlayersSorted(targetRange.getValue()))) {
            for (int i = 0; i <= 2; i++) {
                if (patternsSetting.getValue() == Pattern.Small || patternsSetting.getValue() == Pattern.All) {
                    list.add(new Structure(
                            target,
                            new BlockPos(1, i, 0),//crystal
                            new BlockPos(1, i, -1),//piston
                            new BlockPos(0, i, -1),//pistonHead
                            new BlockPos[]{new BlockPos(1, 0, -2)},//redstone
                            new BlockPos[]{new BlockPos(0, 0, 1), new BlockPos(1, i, 1), new BlockPos(0, 1 + i, 1)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(1, i, 0),//crystal
                            new BlockPos(1, i, 1),//piston
                            new BlockPos(0, i, 1),//pistonHead
                            new BlockPos[]{new BlockPos(1, i, 2)},//redstone
                            new BlockPos[]{new BlockPos(0, i, -1), new BlockPos(1, i, -1), new BlockPos(0, 1 + i, -1)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(0, i, 1),//crystal
                            new BlockPos(1, i, 1),//piston
                            new BlockPos(1, i, 0),//pistonHead
                            new BlockPos[]{new BlockPos(2, i, 1)},//redstone
                            new BlockPos[]{new BlockPos(-1, i, 1), new BlockPos(-1, i, 0), new BlockPos(-1, 1 + i, 0)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(0, i, 1),//crystal
                            new BlockPos(-1, i, 1),//piston
                            new BlockPos(-1, i, 0),//pistonHead
                            new BlockPos[]{new BlockPos(-2, i, 1)},//redstone
                            new BlockPos[]{new BlockPos(1, i, 1), new BlockPos(1, i, 0), new BlockPos(1, 1 + i, 0)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(-1, i, 0),//crystal
                            new BlockPos(-1, i, 1),//piston
                            new BlockPos(0, i, 1),//pistonHead
                            new BlockPos[]{new BlockPos(-1, i, 2)},//redstone
                            new BlockPos[]{new BlockPos(-1, i, -1), new BlockPos(0, i, -1), new BlockPos(0, 1 + i, -1)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(-1, i, 0),//crystal
                            new BlockPos(-1, i, -1),//piston
                            new BlockPos(0, i, -1),//pistonHead
                            new BlockPos[]{new BlockPos(-1, i, -2)},//redstone
                            new BlockPos[]{new BlockPos(-1, i, 1), new BlockPos(0, i, 1), new BlockPos(0, 1 + i, 1)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(0, i, -1),//crystal
                            new BlockPos(-1, i, -1),//piston
                            new BlockPos(-1, i, 0),//pistonHead
                            new BlockPos[]{new BlockPos(-2, i, -1)},//redstone
                            new BlockPos[]{new BlockPos(1, i, 0), new BlockPos(1, i, -1), new BlockPos(1, i + 1, 0)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(0, i, -1),//crystal
                            new BlockPos(1, i, -1),//piston
                            new BlockPos(1, i, 0),//pistonHead
                            new BlockPos[]{new BlockPos(2, i, -1)},//redstone
                            new BlockPos[]{new BlockPos(-1, i, 0), new BlockPos(-1, i, -1), new BlockPos(-1, 1 + i, 0)})//fire
                    );
                }
                if (patternsSetting.getValue() == Pattern.Cross || patternsSetting.getValue() == Pattern.All) {
                    list.add(new Structure(
                            target,
                            new BlockPos(1, i, 0),//crystal
                            new BlockPos(2, i, -1),//piston
                            new BlockPos(1, i, -1),//pistonHead
                            new BlockPos[]{new BlockPos(3, i, -1), new BlockPos(2, i, -2)},//redstone
                            new BlockPos[]{new BlockPos(0, i, -1), new BlockPos(1, i, 1), new BlockPos(0, i, 1), new BlockPos(0, 1 + i, 1), new BlockPos(0, 1 + i, -1)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(1, i, 0),//crystal
                            new BlockPos(2, i, 1),//piston
                            new BlockPos(1, i, 1),//pistonHead
                            new BlockPos[]{new BlockPos(3, i, 1), new BlockPos(2, i, 2)},//redstone
                            new BlockPos[]{new BlockPos(0, i, 1), new BlockPos(1, i, -1), new BlockPos(0, i, -1), new BlockPos(0, 1 + i, -1), new BlockPos(0, 1 + i, 1)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(0, i, 1),//crystal
                            new BlockPos(1, i, 2),//piston
                            new BlockPos(1, i, 1),//pistonHead
                            new BlockPos[]{new BlockPos(1, i, 3), new BlockPos(2, i, 2)},//redstone
                            new BlockPos[]{new BlockPos(1, i, 0), new BlockPos(-1, i, 0), new BlockPos(-1, i, 1), new BlockPos(-1, 1 + i, 0), new BlockPos(1, 1 + i, 0)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(0, i, 1),//crystal
                            new BlockPos(-1, i, 2),//piston
                            new BlockPos(-1, i, 1),//pistonHead
                            new BlockPos[]{new BlockPos(-1, i, 3), new BlockPos(-2, i, 2)},//redstone
                            new BlockPos[]{new BlockPos(-1, i, 0), new BlockPos(1, i, 1), new BlockPos(1, i, 0), new BlockPos(-1, 1 + i, 0), new BlockPos(1, 1 + i, 0)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(-1, i, 0),//crystal
                            new BlockPos(-2, i, 1),//piston
                            new BlockPos(-1, i, 1),//pistonHead
                            new BlockPos[]{new BlockPos(-3, i, 1), new BlockPos(-2, i, 2)},//redstone
                            new BlockPos[]{new BlockPos(0, i, 1), new BlockPos(-1, i, -1), new BlockPos(0, i, -1), new BlockPos(0, 1 + i, 1), new BlockPos(0, 1 + i, -1)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(-1, i, 0),//crystal
                            new BlockPos(-2, i, -1),//piston
                            new BlockPos(-1, i, -1),//pistonHead
                            new BlockPos[]{new BlockPos(-3, i, -1), new BlockPos(-2, i, -2)},//redstone
                            new BlockPos[]{new BlockPos(0, i, -1), new BlockPos(0, i, -1), new BlockPos(-1, i, 1), new BlockPos(0, 1 + i, 1), new BlockPos(0, 1 + i, -1)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(0, i, -1),//crystal
                            new BlockPos(-1, i, -2),//piston
                            new BlockPos(-1, i, -1),//pistonHead
                            new BlockPos[]{new BlockPos(-1, i, -3), new BlockPos(-2, i, -2)},//redstone
                            new BlockPos[]{new BlockPos(-1, i, 0), new BlockPos(1, i, 0), new BlockPos(1, i, -1), new BlockPos(-1, 1 + i, 0), new BlockPos(1, 1 + i, 0)})//fire
                    );
                }
                if (patternsSetting.getValue() == Pattern.Liner || patternsSetting.getValue() == Pattern.All) {
                    list.add(new Structure(
                            target,
                            new BlockPos(1, i, 0),//crystal
                            new BlockPos(2, i, 0),//piston
                            new BlockPos(1, i, 0),//pistonHead
                            new BlockPos[]{new BlockPos(3, i, 0)},//redstone
                            new BlockPos[]{new BlockPos(1, i, 1), new BlockPos(1, i, -1), new BlockPos(0, i, 1),
                                    new BlockPos(0, i, -1), new BlockPos(0, 1 + i, 1), new BlockPos(0, 1 + i, -1)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(-1, i, 0),//crystal
                            new BlockPos(-2, i, 0),//piston
                            new BlockPos(-1, i, 0),//pistonHead
                            new BlockPos[]{new BlockPos(-3, i, 0)},//redstone
                            new BlockPos[]{new BlockPos(-1, i, -1), new BlockPos(-1, i, 1), new BlockPos(0, i, 1),
                                    new BlockPos(0, i, -1), new BlockPos(0, 1 + i, 1), new BlockPos(0, 1 + i, -1)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(0, i, 1),//crystal
                            new BlockPos(0, i, 2),//piston
                            new BlockPos(0, i, 1),//pistonHead
                            new BlockPos[]{new BlockPos(0, i, 3)},//redstone
                            new BlockPos[]{new BlockPos(-1, i, 1), new BlockPos(1, i, 1), new BlockPos(-1, i, 0),
                                    new BlockPos(1, i, 0), new BlockPos(1, 1 + i, 0), new BlockPos(-1, 1 + i, 0)})//fire
                    );
                    list.add(new Structure(
                            target,
                            new BlockPos(0, i, -1),//crystal
                            new BlockPos(0, i, -2),//piston
                            new BlockPos(0, i, -1),//pistonHead
                            new BlockPos[]{new BlockPos(0, i, -3)},//redstone
                            new BlockPos[]{new BlockPos(-1, i, -1), new BlockPos(1, i, -1), new BlockPos(-1, i, 0),
                                    new BlockPos(1, i, 0), new BlockPos(1, 1 + i, 0), new BlockPos(-1, 1 + i, 0)})//fire
                    );
                }
            }
            this.target = target;
        }


        if (bypass.getValue() && InventoryUtility.findItemInHotBar(Items.FLINT_AND_STEEL).found()) {
            List<Structure> bestStructure = list.stream()
                    .filter(Structure::isFirePa)
                    .sorted(Comparator.comparingDouble(Structure::getMaxRange))
                    .toList();
            if (bestStructure.isEmpty()) {
                isFire = false;
                bestStructure = list.stream()
                        .filter(Structure::isNormalPa)
                        .sorted(Comparator.comparingDouble(Structure::getMaxRange))
                        .toList();
                if (!bestStructure.isEmpty()) {
                    Structure structure = bestStructure.get(0);
                    pistonPos = structure.getPistonPos();
                    crystalPos = structure.getCrystalPos();
                    redStonePos = structure.getRedStonePos();
                    pistonHeadPos = structure.getPistonHeadPos();
                    targetPos = structure.targetPos;
                    target = structure.getTarget();
                } else {
                    disable(isRu() ? "Нет цели или места!" : "No target or free space!");
                }
            } else {
                isFire = true;
                Structure structure = bestStructure.get(0);
                pistonPos = structure.getPistonPos();
                crystalPos = structure.getCrystalPos();
                redStonePos = structure.getRedStonePos();
                firePos = structure.getFirePos();
                pistonHeadPos = structure.getPistonHeadPos();
                targetPos = structure.targetPos;
                target = structure.getTarget();
            }
        } else {
            isFire = false;
            List<Structure> bestStructure = list.stream()
                    .filter(Structure::isNormalPa)
                    .sorted(Comparator.comparingDouble(Structure::getMaxRange))
                    .toList();
            if (!bestStructure.isEmpty()) {
                Structure structure = bestStructure.getFirst();
                pistonPos = structure.getPistonPos();
                crystalPos = structure.getCrystalPos();
                redStonePos = structure.getRedStonePos();
                pistonHeadPos = structure.getPistonHeadPos();
                targetPos = structure.targetPos;
                target = structure.getTarget();
            } else {
                disable(isRu() ? "Нет цели или места!" : "No target or free space!");
            }
        }
    }


    public static @NotNull List<Player> getPlayersSorted(float range) {
        synchronized (mc.level.players()) {
            List<Player> playerList = new ArrayList<>();
            for (Player player : mc.level.players()) {
                if (mc.player != player && !Managers.FRIEND.isFriend(player) && mc.player.distanceToSqr(player) <= range * range) {
                    playerList.add(player);
                }
            }
            playerList.sort(Comparator.comparing(player -> mc.player.distanceToSqr(player)));
            return playerList;
        }
    }


    public class Structure {
        private final BlockPos pistonPos;
        private BlockPos crystalPos;
        private final BlockPos targetPos;
        private BlockPos redStonePos;
        private BlockPos firePos;
        private final Player target;

        private BlockPos pistonHeadPos;

        public BlockPos getPistonHeadPos() {
            return pistonHeadPos;
        }

        public BlockPos getPistonPos() {
            return pistonPos;
        }

        public BlockPos getCrystalPos() {
            return crystalPos;
        }

        public BlockPos getRedStonePos() {
            return redStonePos;
        }

        public BlockPos getFirePos() {
            return firePos;
        }

        public Player getTarget() {
            return target;
        }

        public Structure(@NotNull Player target, @NotNull BlockPos crystalPos, @NotNull BlockPos pistonPos, @NotNull BlockPos pistonHeadPos, BlockPos[] redStonePos, BlockPos[] firePos) {
            this.target = target;
            this.targetPos = BlockPos.containing(target.position());
            this.pistonPos = canPlace(targetPos.offset(pistonPos.getX(), pistonPos.getY() + 1, pistonPos.getZ())) ? targetPos.offset(pistonPos.getX(), pistonPos.getY() + 1, pistonPos.getZ()) : null;
            this.crystalPos = getPlaceData(targetPos.offset(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ())) != null ? targetPos.offset(crystalPos.getX(), crystalPos.getY(), crystalPos.getZ()) : null;
            this.pistonHeadPos = mc.level.isEmptyBlock(targetPos.offset(pistonHeadPos.getX(), pistonHeadPos.getY() + 1, pistonHeadPos.getZ())) ? targetPos.offset(pistonHeadPos.getX(), pistonHeadPos.getY() + 1, pistonHeadPos.getZ()) : null;

            if (this.pistonHeadPos != null && !mc.level.getEntitiesOfClass(Player.class, new AABB(this.pistonHeadPos)).isEmpty()) {
                this.pistonHeadPos = null;
            }

            if (this.crystalPos != null && !mc.level.getEntitiesOfClass(Entity.class, new AABB(this.crystalPos)).isEmpty()) {
                this.crystalPos = null;
            }

            this.redStonePos = null;
            List<BlockPos> tempRed = Arrays.stream(redStonePos)
                    .map(blockPos -> targetPos.offset(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ()))
                    .toList();
            BlockState preState = mc.level.getBlockState(pistonPos);
            mc.level.setBlockAndUpdate(pistonPos, Blocks.PISTON.defaultBlockState());
            for (BlockPos pos : tempRed) {
                if (canPlace(pos)) {
                    this.redStonePos = pos;
                    break;
                }
            }
            mc.level.setBlockAndUpdate(pistonPos, preState);

            this.firePos = null;
            List<BlockPos> tempFire = Arrays.stream(firePos)
                    .map(blockPos -> targetPos.offset(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ()))
                    .toList();
            for (BlockPos pos : tempFire) {
                if (canPlace(pos)) {
                    this.firePos = pos;
                    break;
                }
            }
        }

        public boolean isNormalPa() {
            return pistonPos != null && crystalPos != null && targetPos != null && redStonePos != null && pistonHeadPos != null;
        }

        public boolean isFirePa() {
            return pistonPos != null && crystalPos != null && targetPos != null && redStonePos != null && pistonHeadPos != null && firePos != null;
        }

        private boolean canPlace(BlockPos pos) {
            if (pos == null) return false;

            BlockState prevBlockState = null;
            if (supportPlace.getValue()) {
                prevBlockState = mc.level.getBlockState(pos.below());
                if (prevBlockState.canBeReplaced()) {
                    mc.level.setBlockAndUpdate(pos.below(), Blocks.OBSIDIAN.defaultBlockState());
                } else prevBlockState = null;
            }

            boolean canPlace =  InteractionUtility.canPlaceBlock(pos, interact.getValue(), false);

            if (prevBlockState != null) {
                mc.level.setBlockAndUpdate(pos.below(), prevBlockState);
            }

            return canPlace;
        }

        public double getMaxRange() {
            if (this.pistonPos == null || this.crystalPos == null || this.redStonePos == null) return 999;
            final double piston = InteractionUtility.squaredDistanceFromEyes(this.pistonPos.getCenter());
            final double crystal = InteractionUtility.squaredDistanceFromEyes(this.crystalPos.getCenter());
            final double redStone = InteractionUtility.squaredDistanceFromEyes(this.redStonePos.getCenter());

            BlockPos firePos = this.firePos != null ? this.firePos : this.pistonPos;
            final double fire = InteractionUtility.squaredDistanceFromEyes(firePos.getCenter());
            return Math.max(Math.max(fire, crystal), Math.max(redStone, piston));
        }
    }

    public enum Stage {
        Searching,
        Trap,
        Piston,
        Fire,
        Crystal,
        RedStone,
        Break
    }

    public enum Pattern {
        Small,
        Cross,
        Liner,
        All
    }
}
