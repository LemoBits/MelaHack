package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.core.manager.player.PlayerManager;
import thunder.hack.events.impl.EventAttackBlock;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.AutoCrystal;
import thunder.hack.injection.accesors.IInteractionManager;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.PlayerUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.world.ExplosionUtility;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;

public final class SpeedMine extends Module {
    public SpeedMine() {
        super("SpeedMine", Category.PLAYER);
    }

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.Packet);
    public final Setting<Boolean> doubleMine = new Setting<>("DoubleMine", false);
    private final Setting<StartMode> startMode = new Setting<>("StartMode", StartMode.StartAbort, v -> mode.is(Mode.Packet) && !doubleMine.getValue());
    private final Setting<SwitchMode> switchMode = new Setting<>("SwitchMode", SwitchMode.Alternative, v -> mode.not(Mode.Damage));
    private final Setting<Integer> swapDelay = new Setting<>("SwapDelay", 50, 0, 1000, v -> mode.getValue() != Mode.Damage);
    private final Setting<Float> factor = new Setting<>("Factor", 1f, 0.5f, 2f, v -> mode.getValue() != Mode.Damage);
    private final Setting<Float> speed = new Setting<>("Speed", 0.5f, 0f, 1f, v -> mode.getValue() == Mode.Damage);
    public final Setting<Float> range = new Setting<>("Range", 4.2f, 3.0f, 10.0f, v -> mode.getValue() != Mode.Damage);
    private final Setting<Boolean> rotate = new Setting<>("Rotate", false, v -> mode.getValue() != Mode.Damage);
    private final Setting<Boolean> placeCrystal = new Setting<>("PlaceCrystal", true);
    private final Setting<Boolean> resetOnSwitch = new Setting<>("ResetOnSwitch", true, v -> mode.getValue() != Mode.Damage);
    private final Setting<Integer> breakAttempts = new Setting<>("BreakAttempts", 10, 1, 50, v -> mode.getValue() == Mode.Packet);
    private final Setting<Boolean> pauseEat = new Setting<>("Pause On Eat", false);
    private final Setting<Boolean> clientRemove = new Setting<>("ClientRemove", true);

    private final Setting<SettingGroup> packets = new Setting<>("Packets", new SettingGroup(false, 0), v -> mode.is(Mode.Packet) && !doubleMine.getValue());
    private final Setting<Boolean> stop = new Setting<>("Stop", true, v -> mode.is(Mode.Packet) && !doubleMine.getValue()).addToGroup(packets);
    private final Setting<Boolean> abort = new Setting<>("Abort", true, v -> mode.is(Mode.Packet) && !doubleMine.getValue()).addToGroup(packets);
    private final Setting<Boolean> start = new Setting<>("Start", true, v -> mode.is(Mode.Packet) && !doubleMine.getValue()).addToGroup(packets);
    private final Setting<Boolean> stop2 = new Setting<>("Stop2", true, v -> mode.is(Mode.Packet) && !doubleMine.getValue()).addToGroup(packets);

    private final Setting<BooleanSettingGroup> render = new Setting<>("Render", new BooleanSettingGroup(false), v -> mode.getValue() != Mode.Damage);
    private final Setting<Boolean> smooth = new Setting<>("Smooth", true, v -> mode.getValue() != Mode.Damage).addToGroup(render);
    private final Setting<RenderMode> renderMode = new Setting<>("Render Mode", RenderMode.Shrink, v -> mode.getValue() != Mode.Damage).addToGroup(render);
    private final Setting<ColorSetting> startLineColor = new Setting<>("Start Line Color", new ColorSetting(new Color(255, 0, 0, 200)), v -> mode.getValue() != Mode.Damage).addToGroup(render);
    private final Setting<ColorSetting> endLineColor = new Setting<>("End Line Color", new ColorSetting(new Color(47, 255, 0, 200)), v -> mode.getValue() != Mode.Damage).addToGroup(render);
    private final Setting<Integer> lineWidth = new Setting<>("Line Width", 2, 1, 10, v -> mode.getValue() != Mode.Damage).addToGroup(render);
    private final Setting<ColorSetting> startFillColor = new Setting<>("Start Fill Color", new ColorSetting(new Color(255, 0, 0, 120)), v -> mode.getValue() != Mode.Damage).addToGroup(render);
    private final Setting<ColorSetting> endFillColor = new Setting<>("End Fill Color", new ColorSetting(new Color(47, 255, 0, 120)), v -> mode.getValue() != Mode.Damage).addToGroup(render);


    public ArrayList<MineAction> actions = new ArrayList<>();

    @Override
    public void onDisable() {
        actions.forEach(MineAction::reset);
        actions.clear();
    }

    @Override
    public void onEnable() {
        actions.forEach(MineAction::reset);
        actions.clear();
    }

    @Override
    public void onUpdate() {
        if (fullNullCheck() || mc.player.getAbilities().instabuild)
            return;

        if (PlayerUtility.isEating() && pauseEat.getValue()) return;

        if (mode.getValue() == Mode.Damage)
            if (((IInteractionManager) mc.gameMode).getCurBlockDamageMP() < speed.getValue())
                ((IInteractionManager) mc.gameMode).setCurBlockDamageMP(speed.getValue());

        actions.removeIf(MineAction::update);
    }

    @Override
    public void onRender3D(PoseStack stack) {
        if (mode.is(Mode.Damage) || mc.level == null)
            return;

        actions.forEach(a -> {
            if (!mc.level.isEmptyBlock(a.getPos())) {
                float noom = (float) MathUtility.clamp(Render2DEngine.interpolate(a.getPrevProgress(), a.getProgress(), Render3DEngine.getTickDelta()), 0f, 1f);
                AABB renderBox =

                        switch (renderMode.getValue()) {
                            case Block -> new AABB(a.getPos());
                            case Grow ->
                                    new AABB(a.getPos().getX(), a.getPos().getY(), a.getPos().getZ(), a.getPos().getX() + 1, a.getPos().getY() + noom, a.getPos().getZ() + 1);
                            case Shrink ->
                                    new AABB(a.getPos().getX(), a.getPos().getY(), a.getPos().getZ(), a.getPos().getX(), a.getPos().getY(), a.getPos().getZ())
                                            .contract(noom, noom, noom)
                                            .move(0.5 + noom * 0.5, 0.5 + noom * 0.5, 0.5 + noom * 0.5);
                        };

                Render3DEngine.FILLED_QUEUE.add(new Render3DEngine.FillAction(
                        renderBox,
                        Render2DEngine.getColor(startFillColor.getValue().getColorObject(), endFillColor.getValue().getColorObject(), a.getProgress(), smooth.getValue())
                ));

                Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(
                        renderBox,
                        Render2DEngine.getColor(startLineColor.getValue().getColorObject(), endLineColor.getValue().getColorObject(), a.getProgress(), smooth.getValue()),
                        lineWidth.getValue()
                ));
            }
        });
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onAttackBlock(@NotNull EventAttackBlock event) {
        if (fullNullCheck() || !canBreak(event.getBlockPos()) || mc.player.getAbilities().instabuild || mode.is(Mode.Damage))
            return;

        if (!alreadyActing(event.getBlockPos())) {
            if (!doubleMine.getValue() || actions.size() >= 2) {
                if (!actions.isEmpty())
                    actions.removeFirst().cancel();
            }

            actions.add(new MineAction(event.getBlockPos(), event.getEnumFacing()));
        }

        event.cancel();
    }

    public boolean alreadyActing(BlockPos blockPos) {
        return actions.stream().anyMatch(a -> a.pos.equals(blockPos));
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.LOW)
    private void onSync(EventSync event) {
        actions.forEach(MineAction::onSync);
    }

    @EventHandler
    @SuppressWarnings("unused")
    private void onPacketSend(PacketEvent.@NotNull SendPost e) {
        if (e.getPacket() instanceof ServerboundSetCarriedItemPacket && resetOnSwitch.getValue() && !switchMode.is(SwitchMode.Silent) && !mode.is(Mode.GrimInstant))
            actions.forEach(MineAction::reset);
    }

    private void closeScreen() {
        if (mc.player == null) return;

        sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
    }

    public float getBlockStrength(@NotNull BlockState state, BlockPos position) {
        if (state == Blocks.AIR.defaultBlockState())
            return 0.02f;

        float hardness = state.getDestroySpeed(mc.level, position);

        if (hardness < 0)
            return 0;

        return getDigSpeed(state, position) / hardness / (canBreak(position) ? 30f : 100f);
    }

    private float getDestroySpeed(BlockPos position, BlockState state) {
        float destroySpeed = 1;
        int slot = getTool(position);

        if (mc.player == null)
            return 0;
        if (slot != -1 && mc.player.getInventory().getItem(slot) != null && !mc.player.getInventory().getItem(slot).isEmpty()) {
            destroySpeed *= mc.player.getInventory().getItem(slot).getDestroySpeed(state);
        }

        return destroySpeed;
    }

    public float getDigSpeed(BlockState state, BlockPos position) {
        if (mc.player == null) return 0;
        float digSpeed = getDestroySpeed(position, state);

        if (digSpeed > 1) {
            int slot = getTool(position);
            if (slot != -1) {
                ItemStack itemstack = mc.player.getInventory().getItem(slot);
                int efficiencyModifier = EnchantmentHelper.getItemEnchantmentLevel(mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), itemstack);
                if (efficiencyModifier > 0 && !itemstack.isEmpty()) {
                    digSpeed += (float) (StrictMath.pow(efficiencyModifier, 2) + 1);
                }
            }
        }

        if (mc.player.hasEffect(MobEffects.HASTE))
            digSpeed *= 1 + (Objects.requireNonNull(mc.player.getEffect(MobEffects.HASTE)).getAmplifier() + 1) * 0.2F;


        if (mc.player.hasEffect(MobEffects.MINING_FATIGUE))
            digSpeed *= (float) Math.pow(0.3f, Objects.requireNonNull(mc.player.getEffect(MobEffects.MINING_FATIGUE)).getAmplifier() + 1);


        if (mc.player.isUnderWater())
            digSpeed *= (float) mc.player.getAttribute(Attributes.SUBMERGED_MINING_SPEED).getValue();

        if (!mc.player.onGround() && ModuleManager.freeCam.isDisabled())
            digSpeed /= 5;

        return digSpeed < 0 ? 0 : digSpeed * factor.getValue();
    }

    public int getTool(final BlockPos pos) {
        int index = -1;
        float currentFastest = 1.f;

        if (mc.level == null
                || mc.player == null
                || mc.level.getBlockState(pos).getBlock() instanceof AirBlock)
            return -1;

        for (int i = 9; i < 45; ++i) {
            final ItemStack stack = mc.player.getInventory().getItem(i >= 36 ? i - 36 : i);

            if (stack != ItemStack.EMPTY) {
                if (!(stack.getMaxDamage() - stack.getDamageValue() > 10))
                    continue;

                final float digSpeed = EnchantmentHelper.getItemEnchantmentLevel(mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), stack);
                final float destroySpeed = stack.getDestroySpeed(mc.level.getBlockState(pos));

                if (digSpeed + destroySpeed > currentFastest) {
                    currentFastest = digSpeed + destroySpeed;
                    index = i;
                }
            }
        }

        return index >= 36 ? index - 36 : index;
    }

    private boolean canBreak(BlockPos pos) {
        if (mc.level == null || PlayerUtility.squaredDistanceFromEyes(pos.getCenter()) > range.getPow2Value())
            return false;

        final BlockState blockState = mc.level.getBlockState(pos);
        final Block block = blockState.getBlock();
        return block.defaultDestroyTime() != -1;
    }

    public void placeCrystal() {
        if (AutoCrystal.target == null)
            return;

        AutoCrystal.PlaceData data = getCevData();

        if (data == null)
            data = getBestData();

        if (data != null) {
            ModuleManager.autoCrystal.placeCrystal(data.bhr(), true, false);
            debug("placing..");
            ModuleManager.autoTrap.pause();
            ModuleManager.breaker.pause();
        }
    }

    public AutoCrystal.@Nullable PlaceData getCevData() {

        for (MineAction action : actions) {
            if (mc.level.isEmptyBlock(action.getPos().below())) {
                if (ExplosionUtility.getSelfExplosionDamage(action.getPos().getCenter().add(0, 0.5, 0), 0, false) > ModuleManager.autoCrystal.maxSelfDamage.getValue())
                    return null;

                return ModuleManager.autoCrystal.getPlaceData(action.getPos(), null, mc.player.position());
            }
        }
        return null;
    }

    public AutoCrystal.@Nullable PlaceData getBestData() {
        for (MineAction action : actions) {
            BlockState prevState = mc.level.getBlockState(action.getPos());
            mc.level.setBlockAndUpdate(action.getPos(), Blocks.AIR.defaultBlockState());

            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP || dir == Direction.DOWN) continue;
                if (ExplosionUtility.getSelfExplosionDamage(action.getPos().below().relative(dir).getCenter().add(0, 0.5, 0), 0, false) > ModuleManager.autoCrystal.maxSelfDamage.getValue())
                    continue;

                AutoCrystal.PlaceData autoMineData = ModuleManager.autoCrystal.getPlaceData(action.getPos().below().relative(dir), null, mc.player.position());
                if (autoMineData != null) {
                    mc.level.setBlockAndUpdate(action.getPos(), prevState);
                    return autoMineData;
                }
            }

            float selfDmg = ExplosionUtility.getSelfExplosionDamage(action.getPos().getCenter().add(0, 0.5, 0), 0, false);
            mc.level.setBlockAndUpdate(action.getPos(), prevState);

            AutoCrystal.PlaceData autoMineData = ModuleManager.autoCrystal.getPlaceData(action.getPos(), null, mc.player.position());
            if (selfDmg > ModuleManager.autoCrystal.maxSelfDamage.getValue())
                continue;

            return autoMineData;
        }

        return null;
    }

    public boolean isBlockDrop(Entity ent) {
        if (ent instanceof ItemEntity && isOn() && ent.tickCount < 3)
            for (MineAction a : actions)
                if (a.getPos().getCenter().distanceToSqr(ent.position()) <= 1f)
                    return true;

        return false;
    }

    public class MineAction {
        @NotNull
        private final BlockPos pos;
        private float progress, prevProgress;

        private int mineBreaks;

        private final Timer attackTimer = new Timer();

        public MineAction(@NotNull BlockPos pos, Direction direction) {
            this.pos = pos;
            progress = 0;
            mineBreaks = 0;
            start(direction);
        }

        public void start(Direction direction) {
            Direction startDirection = direction == null ? mc.player.getDirection() : direction;

            if (startDirection != null)
                if (doubleMine.getValue()) {
                    sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, startDirection));
                    sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, startDirection));
                    sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, startDirection));
                } else {
                    sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, startDirection));
                    sendPacket(new ServerboundPlayerActionPacket(startMode.getValue() == StartMode.StartAbort ? ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK : ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, startDirection));
                }
        }

        public boolean update() {
            Direction dir = InteractionUtility.getStrictDirections(pos).stream().findFirst().orElse(mc.player.getDirection());

            if (mineBreaks >= breakAttempts.getValue() && mode.not(Mode.GrimInstant))
                return true;

            if (PlayerUtility.squaredDistanceFromEyes(pos.getCenter()) > range.getPow2Value()) {
                cancel();
                return true;
            }

            if (mc.level.isEmptyBlock(pos)) {
                progress = 0;
                prevProgress = -1;
                return false;
            }

            if (progress == 0 && prevProgress == -1 && mode.is(Mode.Packet) && attackTimer.every(800)) {
                start(dir);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }

            int pickSlot = getTool(pos);
            int prevSlot = mc.player.getInventory().getSelectedSlot();

            if (pickSlot == -1)
                return false;

            boolean instant = mineBreaks > 0 && mode.is(Mode.GrimInstant);

            if (progress >= 1 || instant) {
                if (placeCrystal.getValue())
                    placeCrystal();

                switchTo(pickSlot, -1);

                if (mode.getValue() == Mode.GrimInstant || doubleMine.getValue()) {
                    sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, dir));
                } else {
                    if (stop.getValue())
                        sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, dir));
                    if (abort.getValue())
                        sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, dir));
                    if (start.getValue())
                        sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, dir));
                    if (stop2.getValue())
                        sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, dir));
                }

                if (clientRemove.getValue())
                    mc.gameMode.destroyBlock(pos);

                int delay = doubleMine.getValue() ? 100 : swapDelay.getValue();

                if (delay != 0)
                    Managers.ASYNC.run(() -> switchTo(prevSlot, pickSlot), delay);
                else
                    switchTo(prevSlot, pickSlot);

                mineBreaks++;

                progress = prevProgress = 0;

                if (doubleMine.getValue() && mode.is(Mode.GrimInstant) && actions.size() >= 2)
                    return true;
            } else {
                prevProgress = progress;
                progress += getBlockStrength(mc.level.getBlockState(pos), pos);
            }

            fixMovement();

            return false;
        }

        private void switchTo(int slot, int from) {
            if (switchMode.getValue() == SwitchMode.Alternative || slot >= 9) {
                if (from == -1)
                    clickSlot(slot < 9 ? slot + 36 : slot, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP);
                else
                    clickSlot(from < 9 ? from + 36 : from, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP);
                closeScreen();
            } else if (switchMode.is(SwitchMode.Silent)) InventoryUtility.switchToSilent(slot);
            else InventoryUtility.switchTo(slot);
        }

        public void fixMovement() {
            if (rotate.getValue() && progress > 0.95)
                ModuleManager.rotations.fixRotation = PlayerManager.calcAngle(mc.player.getEyePosition(), pos.getCenter())[0];
        }

        public BlockPos getPos() {
            return pos;
        }

        public float getPrevProgress() {
            return prevProgress;
        }

        public float getProgress() {
            return progress;
        }

        public void onSync() {
            if (rotate.getValue() && progress > 0.95) {
                float[] angle = PlayerManager.calcAngle(mc.player.getEyePosition(), pos.getCenter().add(0, -0.25f, 0));
                mc.player.setYRot(angle[0]);
                mc.player.setXRot(angle[1]);
            }
        }

        public void reset() {
            if (progress == 0)
                return;

            prevProgress = progress = 0;
            Direction dir = InteractionUtility.getStrictDirections(pos).stream().findFirst().orElse(mc.player.getDirection());
            sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, dir));
            start(dir);
        }

        public void cancel() {
            if (progress != 0)
                sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN));
        }

        public boolean instantBreaking() {
            return mineBreaks > 0 && mode.is(Mode.GrimInstant);
        }
    }

    public enum Mode {
        Packet,
        GrimInstant,
        Damage
    }

    public enum RenderMode {
        Block,
        Shrink,
        Grow
    }

    public enum SwitchMode {
        Silent,
        Normal,
        Alternative
    }

    public enum StartMode {
        StartAbort,
        StartStop
    }
}
