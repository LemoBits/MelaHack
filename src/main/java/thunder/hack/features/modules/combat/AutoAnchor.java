package thunder.hack.features.modules.combat;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.core.manager.player.CombatManager;
import thunder.hack.events.impl.*;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.Bind;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.ColorSetting;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.util.Mth.wrapDegrees;

public final class AutoAnchor extends Module {
    /*   MAIN   */
    private static final Setting<Pages> page = new Setting<>("Page", Pages.Main);
    private final Setting<AK47> ak47 = new Setting<>("AK47", AK47.ON, v -> page.getValue() == Pages.Main);
    private final Setting<Timing> timing = new Setting<>("Timing", Timing.NORMAL, v -> page.getValue() == Pages.Main);
    private final Setting<Boolean> rotate = new Setting<>("Rotate", true, v -> page.getValue() == Pages.Main);
    private final Setting<BooleanSettingGroup> yawStep = new Setting<>("YawStep", new BooleanSettingGroup(false), v -> rotate.getValue() && page.getValue() == Pages.Main);
    private final Setting<Float> yawAngle = new Setting<>("YawAngle", 180.0f, 1.0f, 180.0f, v -> rotate.getValue() && page.getValue() == Pages.Main).addToGroup(yawStep);
    private final Setting<CombatManager.TargetBy> targetLogic = new Setting<>("TargetLogic", CombatManager.TargetBy.Distance, v -> page.getValue() == Pages.Main);
    private final Setting<Float> targetRange = new Setting<>("TargetRange", 10.0f, 1.0f, 15f, v -> page.getValue() == Pages.Main);
    public static final Setting<Integer> selfPredictTicks = new Setting<>("SelfPredictTicks", 3, 0, 20, v -> page.getValue() == Pages.Main);
    public final Setting<Boolean> useOptimizedCalc = new Setting<>("UseOptimizedCalc", true, v -> page.getValue() == Pages.Main);

    /*   PLACE   */
    private final Setting<InteractionUtility.Interact> interact = new Setting<>("Interact", InteractionUtility.Interact.Vanilla, v -> page.getValue() == Pages.Place);
    private final Setting<Integer> placeDelay = new Setting<>("PlaceDelay", 0, 0, 1000, v -> page.getValue() == Pages.Place);
    private final Setting<Integer> lowPlaceDelay = new Setting<>("LowPlaceDelay", 550, 0, 1000, v -> page.getValue() == Pages.Place);
    private final Setting<Float> placeRange = new Setting<>("PlaceRange", 5f, 1.0f, 6f, v -> page.getValue() == Pages.Place);
    private final Setting<Float> placeWallRange = new Setting<>("PlaceWallRange", 3.5f, 1.0f, 6f, v -> page.getValue() == Pages.Place);
    public static final Setting<Integer> predictTicks = new Setting<>("PredictTicks", 3, 0, 20, v -> page.getValue() == Pages.Place);

    /*   EXPLODE   */
    private final Setting<Integer> explodeDelay = new Setting<>("ExplodeDelay", 0, 0, 1000, v -> page.getValue() == Pages.Break);
    private final Setting<Integer> lowExplodeDelay = new Setting<>("LowExplodeDelay", 550, 0, 1000, v -> page.getValue() == Pages.Break);

    /*   PAUSE   */
    private final Setting<Boolean> mining = new Setting<>("Mining", true, v -> page.getValue() == Pages.Pause);
    private final Setting<Boolean> eating = new Setting<>("Eating", true, v -> page.getValue() == Pages.Pause);
    private final Setting<Boolean> aura = new Setting<>("Aura", false, v -> page.getValue() == Pages.Pause);
    private final Setting<Boolean> pistonAura = new Setting<>("PistonAura", true, v -> page.getValue() == Pages.Pause);
    private final Setting<Boolean> surround = new Setting<>("Surround", true, v -> page.getValue() == Pages.Pause);
    private final Setting<Boolean> middleClick = new Setting<>("MiddleClick", true, v -> page.getValue() == Pages.Pause);
    private final Setting<Float> pauseHP = new Setting<>("HP", 8.0f, 2.0f, 10f, v -> page.getValue() == Pages.Pause);
    private final Setting<BooleanSettingGroup> switchPause = new Setting<>("SwitchPause", new BooleanSettingGroup(true), v -> page.getValue() == Pages.Pause);
    private final Setting<Integer> switchDelay = new Setting<>("SwitchDelay", 100, 0, 1000, v -> page.getValue() == Pages.Pause).addToGroup(switchPause);

    /*   DAMAGES   */
    public final Setting<Sort> sort = new Setting<>("Sort", Sort.DAMAGE, v -> page.getValue() == Pages.Damages);
    public final Setting<Float> minDamage = new Setting<>("MinDamage", 6.0f, 2.0f, 20f, v -> page.getValue() == Pages.Damages);
    public final Setting<Float> maxSelfDamage = new Setting<>("MaxSelfDamage", 10.0f, 2.0f, 20f, v -> page.getValue() == Pages.Damages);
    private final Setting<Safety> safety = new Setting<>("Safety", Safety.NONE, v -> page.getValue() == Pages.Damages);
    private final Setting<Float> safetyBalance = new Setting<>("SafetyBalance", 1.1f, 0.1f, 3f, v -> page.getValue() == Pages.Damages && safety.getValue() == Safety.BALANCE);
    public final Setting<Boolean> protectFriends = new Setting<>("ProtectFriends", true, v -> page.getValue() == Pages.Damages);
    private final Setting<Boolean> overrideSelfDamage = new Setting<>("OverrideSelfDamage", true, v -> page.getValue() == Pages.Damages);
    private final Setting<Float> lethalMultiplier = new Setting<>("LethalMultiplier", 1.0f, 0.0f, 5f, v -> page.getValue() == Pages.Damages);
    private final Setting<BooleanSettingGroup> armorBreaker = new Setting<>("ArmorBreaker", new BooleanSettingGroup(true), v -> page.getValue() == Pages.Damages);
    private final Setting<Float> armorScale = new Setting<>("Armor %", 5.0f, 0.0f, 40f, v -> page.getValue() == Pages.Damages).addToGroup(armorBreaker);
    private final Setting<Float> facePlaceHp = new Setting<>("FacePlaceHp", 5.0f, 0.0f, 20f, v -> page.getValue() == Pages.Damages);
    private final Setting<Bind> facePlaceButton = new Setting<>("FacePlaceBtn", new Bind(GLFW.GLFW_KEY_LEFT_SHIFT, false, false), v -> page.getValue() == Pages.Damages);

    /*   SWITCH   */
    private final Setting<Switch> autoSwitch = new Setting<>("Switch", Switch.NORMAL, v -> page.getValue() == Pages.Switch);
    private final Setting<Switch> antiWeakness = new Setting<>("AntiWeakness", Switch.SILENT, v -> page.getValue() == Pages.Switch);

    /*   RENDER   */
    private final Setting<Boolean> render = new Setting<>("Render", true, v -> page.getValue() == Pages.Render);
    private final Setting<Render> renderMode = new Setting<>("RenderMode", Render.Fade, v -> page.getValue() == Pages.Render);
    private final Setting<Boolean> rselfDamage = new Setting<>("SelfDamage", true, v -> page.getValue() == Pages.Render);
    private final Setting<Boolean> drawDamage = new Setting<>("RenderDamage", true, v -> page.getValue() == Pages.Render);
    private final Setting<ColorSetting> fillColor = new Setting<>("Block Fill Color", new ColorSetting(HudEditor.getColor(0)), v -> page.getValue() == Pages.Render);
    private final Setting<ColorSetting> lineColor = new Setting<>("Block Line Color", new ColorSetting(HudEditor.getColor(0)), v -> page.getValue() == Pages.Render);
    private final Setting<Integer> lineWidth = new Setting<>("Block Line Width", 2, 1, 10, v -> page.getValue() == Pages.Render);
    private final Setting<Integer> slideDelay = new Setting<>("Slide Delay", 200, 1, 1000, v -> page.getValue() == Pages.Render);
    private final Setting<ColorSetting> textColor = new Setting<>("Text Color", new ColorSetting(Color.WHITE), v -> page.getValue() == Pages.Render);

    /*   INFO   */
    private final Setting<Boolean> targetName = new Setting<>("TargetName", false, v -> page.getValue() == Pages.Info);
    private final Setting<Boolean> currentSide = new Setting<>("CurrentSide", true, v -> page.getValue() == Pages.Info);
    private final Setting<Boolean> speed = new Setting<>("Speed", true, v -> page.getValue() == Pages.Info);
    private final Setting<Boolean> confirmInfo = new Setting<>("ConfirmTime", true, v -> page.getValue() == Pages.Info);
    private final Setting<Boolean> calcInfo = new Setting<>("CalcInfo", false, v -> page.getValue() == Pages.Info);

    public static Player target;
    private PlaceData bestPosition;
    private BlockHitResult bestAnchor;

    private final Timer placeTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final Timer pauseTimer = new Timer();

    private final Map<BlockPos, Long> placedAnchors = new HashMap<>();

    public float renderDamage, renderSelfDamage, rotationYaw, rotationPitch;

    private int prevAnchorAmmount, anchorSpeed, invTimer;

    private boolean rotated, facePlacing;

    private long confirmTime, calcTime;

    private BlockPos renderPos, prevRenderPos;
    private long renderMultiplier;
    private final Map<BlockPos, Long> renderPositions = new ConcurrentHashMap<>();

    public AutoAnchor() {
        super("AutoAnchor", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        resetVars();
    }

    @Override
    public void onDisable() {
        resetVars();
    }

    private void resetVars() {
        facePlacing = false;
        rotated = false;
        renderDamage = 0;
        renderSelfDamage = 0;
        renderMultiplier = 0;
        confirmTime = 0;
        renderPositions.clear();
        placedAnchors.clear();
        breakTimer.reset();
        placeTimer.reset();
        bestAnchor = null;
        bestPosition = null;
        renderPos = null;
        prevRenderPos = null;
        target = null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTick(EventTick e) {
        if (mc.player == null || mc.level == null) return;

        long currentTime = System.currentTimeMillis();
        final List<PlaceData> blocks = getPossibleBlocks(target, mc.player.position(), placeRange.getValue());
        calcPosition(blocks);
        getAnchorToExplode(blocks);
        calcTime = System.currentTimeMillis() - currentTime;

        if (timing.is(Timing.NORMAL)) {
            if (bestPosition != null && placeTimer.passedMs(facePlacing ? lowPlaceDelay.getValue() : (placeDelay.getValue())))
                placeAnchor(bestPosition, false);

            if (bestAnchor != null && breakTimer.passedMs(facePlacing ? lowExplodeDelay.getValue() : explodeDelay.getValue()))
                explodeAnchor(bestAnchor);
        }
    }

    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (rotate.getValue())
            calcRotations();
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (mc.player == null || mc.level == null) return;

        target = Managers.COMBAT.getTarget(targetRange.getValue(), targetLogic.getValue());

        if (target != null && (target.isDeadOrDying() || target.getHealth() < 0)) {
            target = null;
            return;
        }

        if (invTimer++ >= 20) {
            anchorSpeed = MathUtility.clamp(prevAnchorAmmount - InventoryUtility.getItemCount(Items.RESPAWN_ANCHOR), 0, 255);
            prevAnchorAmmount = InventoryUtility.getItemCount(Items.RESPAWN_ANCHOR);
            invTimer = 0;
        }

        // Rotate
        if (rotate.getValue() && mc.player != null && rotationYaw != mc.player.getYRot() && rotationPitch != mc.player.getXRot()) {
            mc.player.setYRot(rotationYaw);
            mc.player.setXRot(rotationPitch);
        }
    }

    @EventHandler
    public void onPostSync(EventPostSync e) {
        if (timing.is(Timing.SEQUENTIAL)) {
            if (bestPosition != null && placeTimer.passedMs(facePlacing ? lowPlaceDelay.getValue() : (placeDelay.getValue())))
                placeAnchor(bestPosition, false);

            if (bestAnchor != null && breakTimer.passedMs(facePlacing ? lowExplodeDelay.getValue() : explodeDelay.getValue()))
                explodeAnchor(bestAnchor);
        }
    }

    @Override
    public String getDisplayInfo() {
        StringBuilder info = new StringBuilder();

        Direction side = bestPosition == null ? null : bestPosition.bhr().getDirection();
        if (side != null) {
            if (targetName.getValue() && target != null) info.append(target.getName().getString()).append(" | ");
            if (speed.getValue()) info.append(anchorSpeed).append(" c/s").append(" | ");
            if (currentSide.getValue()) info.append(side.toString().toUpperCase()).append(" | ");
            if (confirmInfo.getValue()) info.append("c: ").append(confirmTime).append(" | ");
            if (calcInfo.getValue()) info.append("calc: ").append(calcTime).append(" | ");
        }
        return info.length() < 4 ? info.toString() : info.substring(0, info.length() - 3);
    }

    @EventHandler
    public void onBlockDestruct(EventSetBlockState e) {
        if (mc.player == null || mc.level == null) return;

        if (e.getPrevState() == null || e.getState() == null)
            return;

        if (target != null && target.distanceToSqr(e.getPos().getCenter()) <= 4 && e.getState().getBlock() instanceof RespawnAnchorBlock && e.getPrevState().canBeReplaced()) {
            debug("Detected change of state " + e.getPos() + ", exploding...");
            //explodeAnchor(getInteractResult(e.getPos()));
        }
    }

    public void calcRotations() {
        if (rotate.getValue() && !shouldPause() && (bestPosition != null || bestAnchor != null) && mc.player != null) {
            Vec3 vec = bestPosition == null ? bestAnchor.getLocation() : bestPosition.bhr().getLocation();

            float yawDelta = wrapDegrees((float) wrapDegrees(Math.toDegrees(Math.atan2(vec.z - mc.player.getZ(), (vec.x - mc.player.getX()))) - 90) - rotationYaw);
            float pitchDelta = ((float) (-Math.toDegrees(Math.atan2(vec.y - (mc.player.position().y + mc.player.getEyeHeight(mc.player.getPose())), Math.sqrt(Math.pow((vec.x - mc.player.getX()), 2) + Math.pow(vec.z - mc.player.getZ(), 2))))) - rotationPitch);

            yawDelta = (float) (yawDelta + Render2DEngine.interpolate(-1.2f, 1.2f, Math.sin(mc.player.tickCount % 80)) + MathUtility.random(-1.2f, 1.2f));
            pitchDelta = pitchDelta + MathUtility.random(-0.8f, 0.8f);

            if (yawDelta > 180)
                yawDelta = yawDelta - 180;

            float yawStepVal = 180f;

            if (yawStep.getValue().isEnabled())
                yawStepVal = yawAngle.getValue();

            float clampedYawDelta = Mth.clamp(Mth.abs(yawDelta), -yawStepVal, yawStepVal);
            float clampedPitchDelta = Mth.clamp(pitchDelta, -45, 45);

            rotated = Mth.abs(yawDelta) <= yawStepVal || !yawStep.getValue().isEnabled();

            float newYaw = rotationYaw + (yawDelta > 0 ? clampedYawDelta : -clampedYawDelta);
            float newPitch = Mth.clamp(rotationPitch + clampedPitchDelta, -90.0F, 90.0F);

            double gcdFix = (Math.pow(mc.options.sensitivity().get() * 0.6 + 0.2, 3.0)) * 1.2;

            rotationYaw = (float) (newYaw - (newYaw - rotationYaw) % gcdFix);
            rotationPitch = (float) (newPitch - (newPitch - rotationPitch) % gcdFix);

            ModuleManager.rotations.fixRotation = rotationYaw;
        } else {
            rotationYaw = mc.player.getYRot();
            rotationPitch = mc.player.getXRot();
        }
    }

    @Override
    public void onRender3D(PoseStack stack) {
        if (render.getValue()) {
            final Object2ObjectMap<BlockPos, Long> cache = new Object2ObjectOpenHashMap<>(renderPositions);

            cache.forEach((pos, time) -> {
                if (System.currentTimeMillis() - time > 500)
                    renderPositions.remove(pos);
            });

            String dmg = MathUtility.round2(renderDamage) + (rselfDamage.getValue() ? " / " + MathUtility.round2(renderSelfDamage) : "");

            if (renderMode.getValue() == Render.Fade) {
                cache.forEach((pos, time) -> {
                    if (System.currentTimeMillis() - time < 500) {
                        int alpha = (int) (100f * (1f - ((System.currentTimeMillis() - time) / 500f)));

                        Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(new AABB(pos), Render2DEngine.injectAlpha(fillColor.getValue().getColorObject(), alpha)));
                        Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(new AABB(pos), Render2DEngine.injectAlpha(lineColor.getValue().getColorObject(), alpha), lineWidth.getValue()));

                        if (drawDamage.getValue())
                            Render3DEngine.drawTextIn3D(dmg, pos.getCenter(), 0, 0.1, 0, Render2DEngine.applyOpacity(textColor.getValue().getColorObject(), alpha / 100f));
                    }
                });
            } else if (renderMode.getValue() == Render.Slide && renderPos != null) {
                if (prevRenderPos == null) prevRenderPos = renderPos;
                if (renderPositions.isEmpty()) return;
                float mult = MathUtility.clamp((System.currentTimeMillis() - renderMultiplier) / (float) slideDelay.getValue(), 0f, 1f);
                AABB interpolatedBox = Render3DEngine.interpolateBox(new AABB(prevRenderPos), new AABB(renderPos), mult);

                renderBox(dmg, interpolatedBox);
            } else if (renderPos != null) {
                AABB box = new AABB(renderPos);
                if (renderPositions.isEmpty())
                    return;

                renderBox(dmg, box);
            }
        }
    }

    private void renderBox(String dmg, AABB box) {
        Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(box, fillColor.getValue().getColorObject()));
        Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(box, lineColor.getValue().getColorObject(), lineWidth.getValue()));

        if (drawDamage.getValue())
            Render3DEngine.drawTextIn3D(dmg, box.getCenter(), 0, 0.1, 0, textColor.getValue().getColorObject());
    }

    public boolean shouldPause() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return true;

        boolean offhand = mc.player.getOffhandItem().getItem() == Items.RESPAWN_ANCHOR;
        boolean mainHand = mc.player.getMainHandItem().getItem() == Items.RESPAWN_ANCHOR;

        boolean offhandGlow = mc.player.getOffhandItem().getItem() == Items.GLOWSTONE;
        boolean mainHandGlow = mc.player.getMainHandItem().getItem() == Items.GLOWSTONE;

        if (!pauseTimer.passedMs(1000))
            return true;

        if (mc.gameMode.isDestroying() && !offhand && mining.getValue())
            return true;

        if (autoSwitch.is(Switch.NONE) && !offhand && !mainHand)
            return true;

        if ((autoSwitch.is(Switch.SILENT) || autoSwitch.is(Switch.NORMAL)) && !InventoryUtility.findItemInHotBar(Items.RESPAWN_ANCHOR).found() && !offhand)
            return true;

        if (autoSwitch.is(Switch.INVENTORY) && !InventoryUtility.findItemInInventory(Items.RESPAWN_ANCHOR).found() && !offhand)
            return true;

        if (autoSwitch.is(Switch.NONE) && !offhandGlow && !mainHandGlow)
            return true;

        if ((autoSwitch.is(Switch.SILENT) || autoSwitch.is(Switch.NORMAL)) && !InventoryUtility.findItemInHotBar(Items.GLOWSTONE).found() && !offhandGlow)
            return true;

        if (autoSwitch.is(Switch.INVENTORY) && !InventoryUtility.findItemInInventory(Items.GLOWSTONE).found() && !offhandGlow)
            return true;

        if (mc.player.isUsingItem() && eating.getValue())
            return true;

        if (rotationMarkedDirty())
            return true;

        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() < pauseHP.getValue())
            return true;

        boolean silentWeakness = antiWeakness.getValue() == Switch.SILENT || antiWeakness.getValue() == Switch.INVENTORY;

        boolean silent = autoSwitch.getValue() == Switch.SILENT || autoSwitch.getValue() == Switch.INVENTORY;

        return switchPause.getValue().isEnabled() && !Managers.PLAYER.switchTimer.passedMs(switchDelay.getValue()) && !silent && !silentWeakness;
    }

    public boolean rotationMarkedDirty() {
        if (ModuleManager.surround.isEnabled() && !ModuleManager.surround.inactivityTimer.passedMs(500) && surround.getValue())
            return true;

        if (ModuleManager.middleClick.isEnabled() && mc.options.keyPickItem.isDown() && middleClick.getValue())
            return true;

        if (ModuleManager.autoTrap.isEnabled() && !ModuleManager.surround.inactivityTimer.passedMs(500))
            return true;

        if (ModuleManager.blocker.isEnabled() && !ModuleManager.surround.inactivityTimer.passedMs(500))
            return true;

        if (ModuleManager.holeFill.isEnabled() && !HoleFill.inactivityTimer.passedMs(500))
            return true;

        if (ModuleManager.aura.isEnabled() && aura.getValue())
            return true;

        return ModuleManager.pistonAura.isEnabled() && pistonAura.getValue();
    }

    public void explodeAnchor(BlockHitResult bhr) {
        if (shouldPause() || mc.player == null) return;
        int prevSlot = -1;

        SearchInvResult glowResult = InventoryUtility.findItemInHotBar(Items.GLOWSTONE);
        SearchInvResult anchorResult = InventoryUtility.findInHotBar(stack -> !(stack.getItem() instanceof BlockItem));
        SearchInvResult glowInvResult = InventoryUtility.findItemInInventory(Items.GLOWSTONE);

        boolean offhand = mc.player.getOffhandItem().getItem() == Items.GLOWSTONE;
        boolean holdingAnchor = mc.player.getMainHandItem().getItem() == Items.GLOWSTONE || offhand;

        if (rotate.getValue() && !rotated)
            return;

        if (autoSwitch.getValue() != Switch.NONE && !holdingAnchor)
            prevSlot = switchTo(glowResult, glowInvResult, autoSwitch);

        if (!(mc.player.getMainHandItem().getItem() == Items.GLOWSTONE || offhand || autoSwitch.getValue() == Switch.SILENT))
            return;

        if (ak47.is(AK47.OFF)) {
            if (mc.player.isShiftKeyDown())
                sendPacket(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(false, false, false, false, false, false, false)));

            if (mc.level.getBlockState(bhr.getBlockPos()).getValue(RespawnAnchorBlock.CHARGE) == 0) {
                sendSequencedPacket(id -> new ServerboundUseItemOnPacket(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, bhr, id));
                mc.player.swing(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            } else {
                anchorResult.switchTo();
                sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, bhr, id));
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        } else {
            if (mc.player.isShiftKeyDown())
                sendPacket(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(false, false, false, false, false, false, false)));
            sendSequencedPacket(id -> new ServerboundUseItemOnPacket(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, bhr, id));
            mc.player.swing(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);

            anchorResult.switchTo();
            sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, bhr, id));
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        breakTimer.reset();

        postPlaceSwitch(prevSlot);
    }

    private int switchTo(SearchInvResult result, SearchInvResult resultInv, @NotNull Setting<Switch> switchMode) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return -1;

        int prevSlot = mc.player.getInventory().getSelectedSlot();

        switch (switchMode.getValue()) {
            case INVENTORY -> {
                if (resultInv.found()) {
                    prevSlot = resultInv.slot();
                    mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, prevSlot, mc.player.getInventory().getSelectedSlot(), ClickType.SWAP, mc.player);
                    sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                }
            }
            case NORMAL -> result.switchTo();
            case SILENT -> result.switchToSilent();
            case NONE -> {
            }
        }
        return prevSlot;
    }

    public void placeAnchor(PlaceData data, boolean instant) {
        if (shouldPause() || mc.player == null || data.anchor()) return;
        int prevSlot = -1;

        SearchInvResult anchorResult = InventoryUtility.findItemInHotBar(Items.RESPAWN_ANCHOR);
        SearchInvResult anchorInvResult = InventoryUtility.findItemInInventory(Items.RESPAWN_ANCHOR);

        boolean offhand = mc.player.getOffhandItem().getItem() == Items.RESPAWN_ANCHOR;
        boolean holdingAnchor = mc.player.getMainHandItem().getItem() == Items.RESPAWN_ANCHOR || offhand;

        if (rotate.getValue()) {
            if (instant) {
                float[] angle = InteractionUtility.calculateAngle(data.bhr().getLocation());
                sendPacket(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), angle[0], angle[1], mc.player.onGround(), mc.player.horizontalCollision));
            } else if (!rotated)
                return;
        }

        if (autoSwitch.getValue() != Switch.NONE && !holdingAnchor)
            prevSlot = switchTo(anchorResult, anchorInvResult, autoSwitch);

        if (!(mc.player.getMainHandItem().getItem() == Items.RESPAWN_ANCHOR || offhand || autoSwitch.getValue() == Switch.SILENT))
            return;

        sendSequencedPacket(id -> new ServerboundUseItemOnPacket(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, data.bhr(), id));

        mc.player.swing(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        placeTimer.reset();

        if (!data.bp().equals(renderPos)) {
            renderMultiplier = System.currentTimeMillis();
            prevRenderPos = renderPos;
            renderPos = data.bp();
        }

        //if (!placedAnchors.containsKey(data.bp()))
        //placedAnchors.put(data.bp(), System.currentTimeMillis());

        renderPositions.put(data.bp(), System.currentTimeMillis());

        if (ak47.is(AK47.ON)) {
            BlockHitResult placeResult = getInteractResult(data.bp);
            if (placeResult != null)
                explodeAnchor(placeResult);
        }

        postPlaceSwitch(prevSlot);
    }

    private void postPlaceSwitch(int slot) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (autoSwitch.getValue() == Switch.SILENT )
            InventoryUtility.switchTo(slot);

        if (autoSwitch.getValue() == Switch.INVENTORY && slot != -1) {
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, mc.player.getInventory().getSelectedSlot(), ClickType.SWAP, mc.player);
            sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
        }
    }

    public void calcPosition(List<PlaceData> blocks) {
        if (mc.player == null || mc.level == null) return;
        if (target == null) {
            renderPos = null;
            prevRenderPos = null;
            bestPosition = null;
            return;
        }
        List<PlaceData> list = blocks.stream().filter(data -> isSafe(data.damage, data.selfDamage, data.overrideDamage)).toList();
        bestPosition = list.isEmpty() ? null : filterPositions(list);
    }

    private @NotNull List<PlaceData> getPossibleBlocks(Player target, Vec3 center, float range) {
        List<PlaceData> blocks = new ArrayList<>();
        BlockPos playerPos = BlockPos.containing(center);

        for (int x = (int) Math.floor(playerPos.getX() - range); x <= Math.ceil(playerPos.getX() + range); x++) {
            for (int y = (int) Math.floor(playerPos.getY() - range); y <= Math.ceil(playerPos.getY() + range); y++) {
                for (int z = (int) Math.floor(playerPos.getZ() - range); z <= Math.ceil(playerPos.getZ() + range); z++) {
                    PlaceData data = getPlaceData(new BlockPos(x, y, z), target);
                    if (data != null)
                        blocks.add(data);
                }
            }
        }

        return blocks;
    }

    public void getAnchorToExplode(List<PlaceData> blocks) {
        if (target == null) {
            bestAnchor = null;
            return;
        }

        List<PlaceData> list = blocks.stream().filter(data -> isSafe(data.damage, data.selfDamage, data.overrideDamage) && data.anchor()).toList();
        bestAnchor = list.isEmpty() ? null : filterAnchors(list);
    }

    public boolean isSafe(float damage, float selfDamage, boolean overrideDamage) {
        if (mc.player == null || mc.level == null) return false;

        if (overrideDamage)
            return true;

        if (selfDamage + 0.5 > mc.player.getHealth() + mc.player.getAbsorptionAmount()) return false;
        else if (safety.getValue() == Safety.STABLE) return damage - selfDamage > 0;
        else if (safety.getValue() == Safety.BALANCE) return (damage * safetyBalance.getValue()) - selfDamage > 0;
        return true;
    }

    private @Nullable PlaceData filterPositions(@NotNull List<PlaceData> clearedList) {
        PlaceData bestData = null;

        float bestVal = 0f;
        for (PlaceData data : clearedList) {
            if ((shouldOverride(data.damage) || data.damage > minDamage.getValue())) {
                if (sort.getValue() == Sort.DAMAGE) {
                    if (bestVal < data.damage) {
                        bestData = data;
                        bestVal = data.damage;
                    }
                } else {
                    if (bestVal < data.damage / data.selfDamage) {
                        bestData = data;
                        bestVal = data.damage / data.selfDamage;
                    }
                }
            }
        }

        if (bestData == null) return null;
        facePlacing = bestData.damage < minDamage.getValue();
        renderDamage = bestData.damage;
        renderSelfDamage = bestData.selfDamage;
        return bestData;
    }

    public boolean shouldOverride(float damage) {
        if (target == null) return false;

        boolean override = target.getHealth() + target.getAbsorptionAmount() <= facePlaceHp.getValue();

        if (armorBreaker.getValue().isEnabled())
            for (ItemStack armor : thunder.hack.utility.player.ArmorUtility.getArmorItems(target))
                if (armor != null && !armor.getItem().equals(Items.AIR) && ((armor.getMaxDamage() - armor.getDamageValue()) / (float) armor.getMaxDamage()) * 100 < armorScale.getValue()) {
                    override = true;
                    break;
                }

        if (facePlaceButton.getValue().getKey() != -1 && InputConstants.isKeyDown(mc.getWindow().getWindow(), facePlaceButton.getValue().getKey()))
            override = true;

        if ((target.getHealth() + target.getAbsorptionAmount()) - (damage * lethalMultiplier.getValue()) < 0.5)
            override = true;

        return override;
    }

    private @Nullable BlockHitResult filterAnchors(@NotNull List<PlaceData> clearedList) {
        PlaceData bestData = null;
        float bestVal = 0f;
        for (PlaceData data : clearedList) {
            if ((shouldOverride(data.damage) || data.damage > minDamage.getValue())) {
                if (sort.getValue() == Sort.DAMAGE) {
                    if (bestVal < data.damage) {
                        bestData = data;
                        bestVal = data.damage;
                    }
                } else {
                    if (bestVal < data.damage / data.selfDamage) {
                        bestData = data;
                        bestVal = data.damage / data.selfDamage;
                    }
                }
            }
        }

        if (bestData == null) return null;
        renderDamage = bestData.damage;
        renderSelfDamage = bestData.selfDamage;
        return bestData.bhr;
    }

    public @Nullable PlaceData getPlaceData(BlockPos bp, Player target) {
        if (mc.player == null || mc.level == null)
            return null;

        if (target != null && target.position().distanceToSqr(bp.getCenter()) > 144)
            return null;

        BlockState state = mc.level.getBlockState(bp);
        boolean isAnchor = state.getBlock() instanceof RespawnAnchorBlock;

        if (!state.canBeReplaced() && !isAnchor)
            return null;

        if (isAnchor)
            mc.level.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());

        BlockHitResult placeResult = InteractionUtility.getPlaceResult(bp, interact.getValue(), false);

        if (placeResult == null) {
            if (isAnchor)
                mc.level.setBlockAndUpdate(bp, state);
            return null;
        }

        float damage = target == null ? 10f : ExplosionUtility.getAutoCrystalDamage(bp.getCenter(), target, predictTicks.getValue(), useOptimizedCalc.getValue());
        if (damage < 1.5f) {
            if (isAnchor)
                mc.level.setBlockAndUpdate(bp, state);
            return null;
        }

        float selfDamage = ExplosionUtility.getAutoCrystalDamage(bp.getCenter(), mc.player, selfPredictTicks.getValue(), useOptimizedCalc.getValue());
        boolean overrideDamage = shouldOverrideDamage(damage, selfDamage);

        if (protectFriends.getValue()) {
            List<Player> players = Lists.newArrayList(mc.level.players());
            for (Player pl : players) {
                if (!Managers.FRIEND.isFriend(pl)) continue;
                float fdamage = ExplosionUtility.getAutoCrystalDamage(bp.getCenter(), pl, selfPredictTicks.getValue(), useOptimizedCalc.getValue());
                if (fdamage > selfDamage) {
                    selfDamage = fdamage;
                }
            }
        }

        if (isAnchor) {
            mc.level.setBlockAndUpdate(bp, state);
            placeResult = getInteractResult(bp);
        }

        if (placeResult == null)
            return null;

        if (selfDamage > maxSelfDamage.getValue() && !overrideDamage) return null;

        return new PlaceData(placeResult, bp, damage, selfDamage, overrideDamage, isAnchor);
    }

    public boolean shouldOverrideDamage(float damage, float selfDamage) {
        if (overrideSelfDamage.getValue() && target != null) {
            if (mc.player == null || mc.level == null) return false;

            boolean targetSafe = (target.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING
                    || target.getMainHandItem().getItem() == Items.TOTEM_OF_UNDYING);

            boolean playerSafe = (mc.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING
                    || mc.player.getMainHandItem().getItem() == Items.TOTEM_OF_UNDYING);

            float targetHp = target.getHealth() + target.getAbsorptionAmount() - 1f;

            float playerHp = mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1f;

            boolean canPop = damage > targetHp && targetSafe;

            boolean canKill = damage > targetHp && !targetSafe;

            boolean canPopSelf = selfDamage > playerHp && playerSafe;

            boolean canKillSelf = selfDamage > playerHp && !playerSafe;

            if (canPopSelf && canKill)
                return true;

            return selfDamage > maxSelfDamage.getValue() && (canPop || canKill) && !canKillSelf && !canPopSelf;
        }
        return false;
    }

    public BlockHitResult getInteractResult(BlockPos bp) {
        BlockHitResult interactResult = null;
        switch (interact.getValue()) {
            case Vanilla, AirPlace -> interactResult = getDefaultInteract(bp);
            case Strict -> interactResult = getStrictInteract(bp);
            case Legit -> interactResult = getLegitInteract(bp);
        }
        return interactResult;
    }

    private @Nullable BlockHitResult getDefaultInteract(BlockPos bp) {
        if (mc.player == null || mc.level == null) return null;

        Vec3 vec = bp.getCenter().add(0, -0.5, 0);

        if (PlayerUtility.squaredDistanceFromEyes(vec) > placeRange.getPow2Value())
            return null;

        BlockHitResult wallCheck = mc.level.clip(new ClipContext(InteractionUtility.getEyesPos(mc.player), vec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

        if (wallCheck != null && wallCheck.getType() == HitResult.Type.BLOCK && wallCheck.getBlockPos() != bp)
            if (PlayerUtility.squaredDistanceFromEyes(vec) > placeWallRange.getPow2Value())
                return null;

        return new BlockHitResult(vec, mc.level.isInWorldBounds(bp.above()) ? Direction.UP : Direction.DOWN, bp, false);
    }

    public BlockHitResult getStrictInteract(@NotNull BlockPos bp) {
        if (mc.player == null || mc.level == null) return null;

        float bestDistance = Float.MAX_VALUE;
        Direction bestDirection = null;
        Vec3 bestVector = null;

        float upPoint = bp.above().getY();

        if (mc.player.getEyePosition().y() > upPoint) {
            bestDirection = Direction.UP;
            bestVector = new Vec3(bp.getX() + 0.5, bp.getY() + 1, bp.getZ() + 0.5);
        } else if (mc.player.getEyePosition().y() < bp.getY() && mc.level.isEmptyBlock(bp.below())) {
            bestDirection = Direction.DOWN;
            bestVector = new Vec3(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5);
        } else {
            for (Direction dir : InteractionUtility.getStrictBlockDirections(bp)) {
                if (dir == Direction.UP || dir == Direction.DOWN)
                    continue;

                Vec3 directionVec = new Vec3(bp.getX() + 0.5 + dir.getUnitVec3i().getX() * 0.5, bp.getY() + 0.9, bp.getZ() + 0.5 + dir.getUnitVec3i().getZ() * 0.5);

                if (!mc.level.isEmptyBlock(bp.relative(dir)))
                    continue;

                float distance = PlayerUtility.squaredDistanceFromEyes(directionVec);
                if (bestDistance > distance) {
                    bestDirection = dir;
                    bestVector = directionVec;
                    bestDistance = distance;
                }
            }
        }

        if (bestVector == null) return null;

        if (PlayerUtility.squaredDistanceFromEyes(bestVector) > placeRange.getPow2Value())
            return null;

        BlockHitResult wallCheck = mc.level.clip(new ClipContext(InteractionUtility.getEyesPos(mc.player), bestVector, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

        if (wallCheck != null && wallCheck.getType() == HitResult.Type.BLOCK && wallCheck.getBlockPos() != bp)
            if (PlayerUtility.squaredDistanceFromEyes(bestVector) > placeWallRange.getPow2Value())
                return null;

        return new BlockHitResult(bestVector, bestDirection, bp, false);
    }

    public BlockHitResult getLegitInteract(BlockPos bp) {
        if (mc.player == null || mc.level == null) return null;

        float bestDistance = Float.MAX_VALUE;
        BlockHitResult bestResult = null;
        for (float x = 0f; x <= 1f; x += 0.2f) {
            for (float y = 0f; y <= 1f; y += 0.2f) {
                for (float z = 0f; z <= 1f; z += 0.2f) {
                    Vec3 point = new Vec3(bp.getX() + x, bp.getY() + y, bp.getZ() + z);
                    float distance = PlayerUtility.squaredDistanceFromEyes(point);

                    BlockHitResult wallCheck = mc.level.clip(new ClipContext(InteractionUtility.getEyesPos(mc.player), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                    if (wallCheck != null && wallCheck.getType() == HitResult.Type.BLOCK && wallCheck.getBlockPos() != bp)
                        if (distance > placeWallRange.getPow2Value())
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

    public void pause() {
        pauseTimer.reset();
    }

    public record PlaceData(BlockHitResult bhr, BlockPos bp, float damage, float selfDamage, boolean overrideDamage,
                            boolean anchor) {
    }

    private enum Pages {
        Place, Break, Pause, Render, Damages, Main, Switch, Info
    }

    private enum Switch {
        NONE, NORMAL, SILENT, INVENTORY
    }

    private enum Timing {
        NORMAL, SEQUENTIAL
    }

    public enum Safety {
        BALANCE, STABLE, NONE
    }

    public enum Sort {
        SAFE, DAMAGE
    }

    public enum Render {
        Fade, Slide, Default
    }

    public enum AK47 {
        OFF, ON, SEMI
    }
}