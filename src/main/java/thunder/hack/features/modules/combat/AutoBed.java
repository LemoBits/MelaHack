package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventPostSync;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.injection.accesors.IEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.world.ExplosionUtility;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.PlayerUtility;
import thunder.hack.utility.player.SearchInvResult;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.util.Objects;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

import com.mojang.blaze3d.vertex.PoseStack;

public final class AutoBed extends Module {
    private final Setting<InteractionUtility.Interact> interactMode = new Setting<>("InteractMode", InteractionUtility.Interact.Vanilla);
    public static final Setting<Float> range = new Setting<>("Range", 4f, 2f, 6.0f);
    public static final Setting<Float> wallRange = new Setting<>("WallRange", 4f, 0f, 6.0f);
    public static final Setting<Integer> placeDelay = new Setting<>("PlaceDelay", 100, 0, 1000);
    public static final Setting<Integer> explodeDelay = new Setting<>("ExplodeDelay", 100, 0, 1000);
    public static final Setting<Float> minDamage = new Setting<>("MinDamage", 8f, 0f, 25.0f);
    public static final Setting<Float> maxSelfDamage = new Setting<>("MaxSelfDamage", 4f, 0f, 25.0f);
    private final Setting<Boolean> dimCheck = new Setting<>("DimensionCheck", false);
    public final Setting<Boolean> switchToHotbar = new Setting<>("SwitchToHotbar", true);
    public final Setting<Boolean> oldPlace = new Setting<>("1.12 Place",false);
    public final Setting<Boolean> autoSwap = new Setting<>("AutoSwap", true);
    public final Setting<Boolean> autoCraft = new Setting<>("AutoCraft", true);
    public static final Setting<Integer> minBeds = new Setting<>("MinBeds", 4, 0, 10);
    public static final Setting<Integer> bedsPerCraft = new Setting<>("BedsPerCraft", 8, 1, 27);
    private final Setting<SettingGroup> renderCategory = new Setting<>("Render", new SettingGroup(false, 0));
    private final Setting<Boolean> render = new Setting<>("Render", true).addToGroup(renderCategory);
    private final Setting<Boolean> rselfDamage = new Setting<>("SelfDamage", true).addToGroup(renderCategory);
    private final Setting<Boolean> drawDamage = new Setting<>("RenderDamage", true).addToGroup(renderCategory);
    private final Setting<ColorSetting> fillColor = new Setting<>("Fill", new ColorSetting(Render2DEngine.injectAlpha(HudEditor.getColor(0), 150))).addToGroup(renderCategory);
    private final Setting<ColorSetting> lineColor = new Setting<>("Line", new ColorSetting(HudEditor.getColor(0))).addToGroup(renderCategory);
    private final Setting<ColorSetting> textColor = new Setting<>("Text", new ColorSetting(Color.WHITE)).addToGroup(renderCategory);

    private Player target;
    private BedData bestBed, bestPos;
    private float rotationYaw, rotationPitch;

    private final Timer placeTimer = new Timer();
    private final Timer explodeTimer = new Timer();

    public AutoBed() {
        super("AutoBed", Category.COMBAT);
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (bestBed != null || bestPos != null) {
            mc.player.setYRot(rotationYaw);
            mc.player.setXRot(rotationPitch);
        }
    }

    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent e) {
        target = findTarget();

        if (mc.level.dimensionType().bedWorks() && dimCheck.getValue()) {
            disable(isRu() ? "Кровати не взрываются в этом измерении!" : "Beds don't explode in this dimension!");
            return;
        }

        if (target != null && (target.isDeadOrDying() || target.getHealth() < 0)) {
            target = null;
            return;
        }

        bestBed = findBedToExplode();
        bestPos = findBlockToPlace();

        if (bestBed != null || bestPos != null) {
            float[] angle;

            angle = InteractionUtility.calculateAngle(Objects.requireNonNullElseGet(bestPos, () -> bestBed).hitResult().getLocation());

            rotationYaw = (angle[0]);
            rotationPitch = (angle[1]);
            ModuleManager.rotations.fixRotation = rotationYaw;
        }

        if (autoCraft.getValue()) {
            if (InventoryUtility.getBedsCount() <= minBeds.getValue()) {
                craftBed();
                return;
            }
            if (mc.player.containerMenu instanceof CraftingMenu) {
                sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                mc.player.clientSideCloseContainer();
            }
        }
    }

    @EventHandler
    public void onPostSync(EventPostSync e) {
        if (!(mc.player.getMainHandItem().getItem() instanceof BedItem) && autoSwap.getValue() && bestPos != null) {
            SearchInvResult hotBarResult = InventoryUtility.findBedInHotBar();
            if (hotBarResult.found()) {
                hotBarResult.switchTo();
            } else if (switchToHotbar.getValue()) {
                SearchInvResult invResult = InventoryUtility.findBed();
                if (invResult.found() && !(mc.screen instanceof CraftingScreen)) {
                    mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, invResult.slot(), mc.player.getInventory().getSelectedSlot(), ClickType.SWAP, mc.player);
                    sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                }
            }
        }

        if (bestBed != null && explodeTimer.passedMs(explodeDelay.getValue())) {
            sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, bestBed.hitResult(), id));
            mc.player.swing(InteractionHand.MAIN_HAND);
            explodeTimer.reset();
        }

        if (!(mc.player.getMainHandItem().getItem() instanceof BedItem))
            return;

        if (bestPos != null && placeTimer.passedMs(placeDelay.getValue()) && !(mc.level.getBlockState(bestPos.hitResult().getBlockPos().above()).getBlock() instanceof BedBlock)) {
            final float angle2 = InteractionUtility.calculateAngle(bestPos.hitResult.getBlockPos().getCenter(), bestPos.hitResult.getBlockPos().relative(bestPos.dir).getCenter())[0];
            sendPacket(new ServerboundMovePlayerPacket.Rot(angle2, 0, mc.player.onGround(), mc.player.horizontalCollision));
            float prevYaw = mc.player.getYRot();
            mc.player.setYRot(angle2);
            ((thunder.hack.injection.accesors.IEntity) mc.player).setLastYaw(angle2);
            ((IEntity) mc.player).setLastYaw(angle2);
            sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, bestPos.hitResult(), id));
            mc.player.swing(InteractionHand.MAIN_HAND);
            placeTimer.reset();
            mc.player.setYRot(prevYaw);
        }
    }

    @Override
    public void onRender3D(PoseStack stack) {
        if (bestPos != null && render.getValue()) {
            AABB box = new AABB(bestPos.hitResult.getBlockPos().above());
            AABB box2 = new AABB(bestPos.hitResult.getBlockPos().above().relative(bestPos.dir));

            AABB finalBox = box.minmax(box2).setMaxY(box.maxY - 0.45f);

            String dmg = MathUtility.round2(bestPos.damage()) + (rselfDamage.getValue() ? " / " + MathUtility.round2(bestPos.selfDamage()) : "");

            Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(finalBox, lineColor.getValue().getColorObject(), 2f));
            Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(finalBox, fillColor.getValue().getColorObject()));

            if (drawDamage.getValue())
                Render3DEngine.drawTextIn3D(dmg, finalBox.getCenter(), 0, 0.1, 0, textColor.getValue().getColorObject());
        }
    }

    private Player findTarget() {
        return Managers.COMBAT.getNearestTarget(12f);
    }

    private BedData findBedToExplode() {
        int intRange = (int) (Math.floor(range.getValue()) + 1);
        Iterable<BlockPos> blocks_ = BlockPos.withinManhattan(new BlockPos(BlockPos.containing(mc.player.position()).above()), intRange, intRange, intRange);

        BedData bestData = null;

        for (BlockPos b : blocks_) {
            BlockState state = mc.level.getBlockState(b);
            if (PlayerUtility.squaredDistanceFromEyes(b.getCenter()) <= range.getPow2Value()) {
                if (state.getBlock() instanceof BedBlock) {
                    BlockHitResult bhr = getInteractResult(b);

                    mc.level.removeBlock(b, false);
                    float damage = ExplosionUtility.getExplosionDamage(b.getCenter().add(0, -0.5, 0), target, false);
                    float selfDamage = ExplosionUtility.getExplosionDamage(b.getCenter().add(0, -0.5, 0), mc.player, false);
                    mc.level.setBlockAndUpdate(b, state);

                    if (damage < minDamage.getValue())
                        continue;

                    if (selfDamage > maxSelfDamage.getValue())
                        continue;

                    if (selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() + 2f)
                        continue;

                    if (bestData != null && bestData.damage > damage)
                        continue;

                    if (bhr != null)
                        bestData = new BedData(bhr, damage, selfDamage, bhr.getDirection());
                }
            }
        }
        return bestData;
    }

    private BedData findBlockToPlace() {
        int intRange = (int) (Math.floor(range.getValue()) + 1);
        Iterable<BlockPos> blocks_ = BlockPos.withinManhattan(new BlockPos(BlockPos.containing(mc.player.position()).above()), intRange, intRange, intRange);

        BedData bestData = null;

        for (BlockPos b : blocks_) {
            BlockState state = mc.level.getBlockState(b);
            BlockState state2 = mc.level.getBlockState(b.above());

            if (PlayerUtility.squaredDistanceFromEyes(b.getCenter()) <= range.getPow2Value()) {
                if (state2.getBlock() instanceof BedBlock && !placeTimer.passedMs(1500) && bestPos != null)
                    return bestPos;

                if (!state.canBeReplaced()) {
                    BlockHitResult bhr = InteractionUtility.getPlaceResult(b.above(), interactMode.getValue(), false);
                    if (bhr != null) {

                        BlockHitResult wallCheck = mc.level.clip(new ClipContext(InteractionUtility.getEyesPos(mc.player), bhr.getLocation(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                        if (wallCheck != null && wallCheck.getType() == HitResult.Type.BLOCK && wallCheck.getBlockPos() != b)
                            continue;

                        float damage = ExplosionUtility.getExplosionDamage(b.above().getCenter().add(0, -0.5, 0), target, false);
                        float selfDamage = ExplosionUtility.getExplosionDamage(b.above().getCenter().add(0, -0.5, 0), mc.player, false);

                        if (damage < minDamage.getValue())
                            continue;

                        if (selfDamage > maxSelfDamage.getValue())
                            continue;

                        if (selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount() + 2f)
                            continue;

                        if (bestData != null && bestData.damage > damage)
                            continue;


                        float bestDirdmg = 0;
                        Direction bestDir = null;
                        for (Direction dir : Direction.values()) {
                            if (dir == Direction.DOWN || dir == Direction.UP)
                                continue;
                            BlockPos offset = b.above().relative(dir);

                            if(!mc.level.getBlockState(offset).canBeReplaced())
                                continue;

                            if(oldPlace.getValue() && mc.level.getBlockState(b.relative(dir)).canBeReplaced()){
                                continue;
                            }

                            float dirdamage = ExplosionUtility.getExplosionDamage(offset.getCenter().add(0, -0.5, 0), target, false);
                            float dirSelfDamage = ExplosionUtility.getExplosionDamage(offset.getCenter().add(0, -0.5, 0), mc.player, false);
                            if (dirdamage > bestDirdmg && dirSelfDamage <= maxSelfDamage.getValue()) {
                                bestDir = dir;
                                bestDirdmg = dirdamage;
                            }
                        }

                        bestData = bestDir == null ? null : new BedData(bhr, damage, selfDamage, bestDir);
                    }
                }
            }
        }
        return bestData;
    }


    public void craftBed() {
        int intRange = (int) (Math.floor(range.getValue()) + 1);
        Iterable<BlockPos> blocks_ = BlockPos.withinManhattan(new BlockPos(BlockPos.containing(mc.player.position()).above()), intRange, intRange, intRange);

        for (BlockPos b : blocks_) {
            BlockState state = mc.level.getBlockState(b);
            if (state.getBlock() instanceof CraftingTableBlock) {
                BlockHitResult result = getInteractResult(b);
                if (result != null) {
                    if (mc.player.containerMenu instanceof CraftingMenu craft) {
                        mc.player.getRecipeBook().setOpen(craft.getRecipeBookType(), true);
                        ContextMap context = new ContextMap.Builder().create(new ContextKeySet.Builder().build());
                        for (RecipeCollection results : mc.player.getRecipeBook().getCollections()) {
                            for (RecipeDisplayEntry recipe : results.getRecipes()) {
                                ItemStack resultStack = recipe.display().result().resolveForFirstStack(context);
                                if (resultStack.getItem() instanceof BedItem) {
                                    RecipeDisplayId recipeId = recipe.id();
                                    for (int i = 0; i < bedsPerCraft.getValue(); i++)
                                        mc.gameMode.handlePlaceRecipe(mc.player.containerMenu.containerId, recipeId, false);
                                    mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 0, 0, ClickType.QUICK_MOVE, mc.player);
                                    break;
                                }
                            }
                        }
                    } else {
                        float[] angle = InteractionUtility.calculateAngle(result.getLocation());
                        mc.player.setYRot(angle[0]);
                        mc.player.setXRot(angle[1]);
                        sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, result, id));
                    }
                }
            }
        }
    }

    public BlockHitResult getInteractResult(@NotNull BlockPos bp) {
        float bestDistance = 999f;
        BlockHitResult bestResult = null;
        for (float x = 0f; x < 1f; x += 0.25f) {
            for (float y = 0f; y < 0.5f; y += 0.125f) {
                for (float z = 0f; z < 1f; z += 0.25f) {
                    Vec3 point = new Vec3(bp.getX() + x, bp.getY() + y, bp.getZ() + z);
                    float distance = PlayerUtility.squaredDistanceFromEyes(point);

                    BlockHitResult wallCheck = mc.level.clip(new ClipContext(InteractionUtility.getEyesPos(mc.player), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                    if (wallCheck != null && wallCheck.getType() == HitResult.Type.BLOCK && wallCheck.getBlockPos() != bp)
                        if (distance > wallRange.getPow2Value())
                            continue;


                    BlockHitResult result = ExplosionUtility.rayCastBlock(new ClipContext(InteractionUtility.getEyesPos(mc.player), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player), bp);
                    if (distance > range.getPow2Value())
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

        float bestDistance2 = 999f;
        Direction bestDirection = null;

        if (mc.player.getEyePosition().y() > bp.above().getY()) {
            bestDirection = Direction.UP;
        } else if (mc.player.getEyePosition().y() < bp.getY()) {
            bestDirection = Direction.DOWN;
        } else {
            for (Direction dir : Direction.values()) {
                Vec3 directionVec = new Vec3(bp.getX() + 0.5 + dir.getUnitVec3i().getX() * 0.5, bp.getY() + 0.5 + dir.getUnitVec3i().getY() * 0.5, bp.getZ() + 0.5 + dir.getUnitVec3i().getZ() * 0.5);
                float distance = PlayerUtility.squaredDistanceFromEyes(directionVec);
                if (bestDistance2 > distance) {
                    bestDirection = dir;
                    bestDistance2 = distance;
                }
            }
        }

        if(bestResult == null)
            return null;

        return new BlockHitResult(bestResult.getLocation(), bestDirection, bestResult.getBlockPos(), false);
    }

    private record BedData(BlockHitResult hitResult, float damage, float selfDamage, Direction dir) {
    }

}
