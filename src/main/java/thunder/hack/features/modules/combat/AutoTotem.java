package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.*;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.movement.Blink;
import thunder.hack.injection.accesors.IMinecraftClient;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.Bind;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.PredictUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;
import thunder.hack.utility.world.ExplosionUtility;

public final class AutoTotem extends Module {
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Matrix);
    private final Setting<OffHand> offhand = new Setting<>("Item", OffHand.Totem);
    private final Setting<Boolean> pomoyka = new Setting<>("DoNotSwapBack", false, v -> offhand.is(OffHand.Totem));
    private final Setting<BooleanSettingGroup> bindSwap = new Setting<>("BindSwap", new BooleanSettingGroup(false), v -> offhand.is(OffHand.Totem));
    private final Setting<Bind> swapButton = new Setting<>("SwapButton", new Bind(GLFW.GLFW_KEY_CAPS_LOCK, false, false)).addToGroup(bindSwap);
    private final Setting<Swap> swapMode = new Setting<>("Swap", Swap.GappleShield).addToGroup(bindSwap);
    private final Setting<Boolean> ncpStrict = new Setting<>("NCPStrict", false);
    private final Setting<Float> healthF = new Setting<>("HP", 16f, 0f, 36f);
    private final Setting<Float> healthS = new Setting<>("ShieldGappleHp", 16f, 0f, 20f, v -> offhand.getValue() == OffHand.Shield);
    private final Setting<Boolean> calcAbsorption = new Setting<>("CalcAbsorption", true);
    private final Setting<Boolean> stopMotion = new Setting<>("StopMotion", false);
    private final Setting<Boolean> resetAttackCooldown = new Setting<>("ResetAttackCooldown", false);
    private final Setting<SettingGroup> safety = new Setting<>("Safety", new SettingGroup(false, 0));
    private final Setting<Boolean> hotbarFallBack = new Setting<>("HotbarFallback", false).addToGroup(safety);
    private final Setting<Boolean> fallBackCalc = new Setting<>("FallBackCalc", true, v -> hotbarFallBack.getValue()).addToGroup(safety);
    private final Setting<Boolean> onElytra = new Setting<>("OnElytra", true).addToGroup(safety);
    private final Setting<Boolean> onFall = new Setting<>("OnFall", true).addToGroup(safety);
    private final Setting<Boolean> onCrystal = new Setting<>("OnCrystal", true).addToGroup(safety);
    private final Setting<Boolean> onObsidianPlace = new Setting<>("OnObsidianPlace", false).addToGroup(safety);
    private final Setting<Boolean> onCrystalInHand = new Setting<>("OnCrystalInHand", false).addToGroup(safety);
    private final Setting<Boolean> onMinecartTnt = new Setting<>("OnMinecartTNT", true).addToGroup(safety);
    private final Setting<Boolean> onCreeper = new Setting<>("OnCreeper", true).addToGroup(safety);
    private final Setting<Boolean> onAnchor = new Setting<>("OnAnchor", true).addToGroup(safety);
    private final Setting<Boolean> onTnt = new Setting<>("OnTNT", true).addToGroup(safety);
    public final Setting<RCGap> rcGap = new Setting<>("RightClickGapple", RCGap.Off);
    private final Setting<Boolean> crappleSpoof = new Setting<>("CrappleSpoof", true, v -> offhand.getValue() == OffHand.GApple);

    private enum OffHand {Totem, Crystal, GApple, Shield}

    private enum Mode {Default, Alternative, Matrix, MatrixPick, NewVersion}

    private enum Swap {GappleShield, BallShield, GappleBall, BallTotem}

    public enum RCGap {Off, Always, OnlySafe}


    private int delay;

    private Timer bindDelay = new Timer();

    private Item prevItem;

    public AutoTotem() {
        super("AutoTotem", Category.COMBAT);
    }

    @Override
    protected void onSync() {
        swapTo(getItemSlot());
        HitResult crosshairTarget = mc.hitResult;
        if (crosshairTarget instanceof BlockHitResult blockHitResult) {
            BlockPos pos = blockHitResult.getBlockPos();
            if (rcGap.not(RCGap.Off)) {
                LocalPlayer clientPlayer = mc.player;
                if (clientPlayer != null && clientPlayer.getMainHandItem().is(ItemTags.SWORDS) && mc.options.keyUse.isDown() && !clientPlayer.isUsingItem() && !pomoyka.getValue()) {
                    assert mc.level != null;
                    if (!(mc.level.getBlockState(pos).getBlock() instanceof DoorBlock ||
                            mc.level.getBlockState(pos).getBlock() instanceof BedBlock ||
                            mc.level.getBlockState(pos).getBlock() instanceof TrapDoorBlock ||
                            mc.level.getBlockState(pos).getBlock() instanceof CampfireBlock ||
                            mc.level.getBlockState(pos).getBlock() instanceof CakeBlock ||
                            mc.level.getBlockState(pos).getBlock() instanceof LeverBlock ||
                            mc.level.getBlockState(pos).getBlock() instanceof ButtonBlock
                    )) {
                        ((IMinecraftClient) mc).idoItemUse();
                    }
                }
            }
        }
        delay--;
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.@NotNull Receive e) {
        if (e.getPacket() instanceof ClientboundAddEntityPacket spawn)
            if (spawn.getType() == EntityType.END_CRYSTAL)
                if (getPlayerPos().distanceToSqr(spawn.getX(), spawn.getY(), spawn.getZ()) < 36) {
                    if (hotbarFallBack.getValue()) {
                        if (fallBackCalc.getValue() && ExplosionUtility.getExplosionDamageWPredict(new Vec3(spawn.getX(), spawn.getY(), spawn.getZ()), mc.player, PredictUtility.createBox(getPlayerPos(), mc.player), false) < getTriggerHealth() + 4f)
                            return;
                        runInstant();
                    }

                    if (onCrystal.getValue()) {
                        if (getTriggerHealth() - ExplosionUtility.getExplosionDamageWPredict(new Vec3(spawn.getX(), spawn.getY(), spawn.getZ()), mc.player, PredictUtility.createBox(getPlayerPos(), mc.player), false) < 0.5) {
                            int slot = -1;
                            for (int i = 9; i < 45; i++) {
                                if (mc.player.getInventory().getItem(i >= 36 ? i - 36 : i).getItem().equals(Items.TOTEM_OF_UNDYING)) {
                                    slot = i >= 36 ? i - 36 : i;
                                    break;
                                }
                            }

                            swapTo(slot);
                            debug("spawn switch");
                        }
                    }
                }


        if (e.getPacket() instanceof ClientboundBlockUpdatePacket blockUpdate)
            if (blockUpdate.getBlockState().getBlock() == Blocks.OBSIDIAN && onObsidianPlace.getValue())
                if (getPlayerPos().distanceToSqr(blockUpdate.getPos().getCenter()) < 36 && delay <= 0)
                    runInstant();
    }

    private float getTriggerHealth() {
        return mc.player.getHealth() + (calcAbsorption.getValue() ? mc.player.getAbsorptionAmount() : 0f);
    }

    private void runInstant() {
        SearchInvResult hotbarResult = InventoryUtility.findItemInHotBar(Items.TOTEM_OF_UNDYING);
        SearchInvResult invResult = InventoryUtility.findItemInInventory(Items.TOTEM_OF_UNDYING);
        if (hotbarResult.found()) {
            hotbarResult.switchTo();
            delay = 20;
        } else if (invResult.found()) {
            int slot = invResult.slot() >= 36 ? invResult.slot() - 36 : invResult.slot();
            if (!hotbarFallBack.getValue()) swapTo(slot);
            else mc.gameMode.handleCreativeModeItemAdd(mc.player.getInventory().getItem(slot), slot);
            delay = 20;
        }
    }

    public void swapTo(int slot) {
        if (slot != -1 && delay <= 0) {
            if (mc.screen instanceof ContainerScreen) return;

            if (stopMotion.getValue()) mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y(), 0);

            int nearestSlot = findNearestCurrentItem();
            int prevCurrentItem = mc.player.getInventory().getSelectedSlot();
            if (slot >= 9) {
                switch (mode.getValue()) {
                    case Default -> {
                        if (ncpStrict.getValue())
                            sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                        clickSlot(slot);
                        clickSlot(45);
                        clickSlot(slot);
                        sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                    }
                    case Alternative -> {
                        if (ncpStrict.getValue())
                            sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                        clickSlot(slot, nearestSlot, ClickType.SWAP);
                        clickSlot(45, nearestSlot, ClickType.SWAP);
                        clickSlot(slot, nearestSlot, ClickType.SWAP);
                        sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                    }
                    case Matrix -> {
                        if (ncpStrict.getValue())
                            sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));

                        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, nearestSlot, ClickType.SWAP, mc.player);
                        debug(slot + " " + nearestSlot);

                        sendPacket(new ServerboundSetCarriedItemPacket(nearestSlot));
                        mc.player.getInventory().setSelectedSlot(nearestSlot);

                        ItemStack itemstack = mc.player.getOffhandItem();
                        mc.player.setItemInHand(InteractionHand.OFF_HAND, mc.player.getMainHandItem());
                        mc.player.setItemInHand(InteractionHand.MAIN_HAND, itemstack);
                        sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));

                        sendPacket(new ServerboundSetCarriedItemPacket(prevCurrentItem));
                        mc.player.getInventory().setSelectedSlot(prevCurrentItem);

                        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, nearestSlot, ClickType.SWAP, mc.player);

                        sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                        if (resetAttackCooldown.getValue())
                            mc.player.resetAttackStrengthTicker();
                    }
                    case MatrixPick -> {
                        debug(slot + " pick");
                        mc.gameMode.handleCreativeModeItemAdd(mc.player.getInventory().getItem(slot), slot);
                        sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
                        int prevSlot = mc.player.getInventory().getSelectedSlot();
                        Managers.ASYNC.run(() -> mc.player.getInventory().setSelectedSlot(prevSlot), 300);
                    }
                    case NewVersion -> {
                        debug(slot + " swap");
                        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, 40, ClickType.SWAP, mc.player);
                        sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                    }
                }
            } else {
                sendPacket(new ServerboundSetCarriedItemPacket(slot));
                mc.player.getInventory().setSelectedSlot(slot);
                debug(slot + " select");
                sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
                sendPacket(new ServerboundSetCarriedItemPacket(prevCurrentItem));
                mc.player.getInventory().setSelectedSlot(prevCurrentItem);
                if (resetAttackCooldown.getValue())
                    mc.player.resetAttackStrengthTicker();
            }
            delay = (int) (2 + (Managers.SERVER.getPing() / 25f));
        }
    }

    public static int findNearestCurrentItem() {
        int i = mc.player.getInventory().getSelectedSlot();
        if (i == 8) return 7;
        if (i == 0) return 1;
        return i - 1;
    }

    public int getItemSlot() {
        if (mc.player == null || mc.level == null) return -1;

        SearchInvResult gapple = InventoryUtility.findItemInInventory(Items.ENCHANTED_GOLDEN_APPLE);
        SearchInvResult crapple = InventoryUtility.findItemInInventory(Items.GOLDEN_APPLE);
        SearchInvResult shield = InventoryUtility.findItemInInventory(Items.SHIELD);
        Item offHandItem = mc.player.getOffhandItem().getItem();

        int itemSlot = -1;
        Item item = null;
        switch (offhand.getValue()) {
            case Totem -> {
                if (offHandItem != Items.TOTEM_OF_UNDYING && !mc.player.getOffhandItem().isEmpty())
                    prevItem = offHandItem;

                if (!pomoyka.getValue())
                    item = prevItem;

                if (bindSwap.getValue().isEnabled())
                    if (isKeyPressed(swapButton) && bindDelay.every(250)) {
                        switch (swapMode.getValue()) {
                            case BallShield -> {
                                if (mc.player.getOffhandItem().isEmpty() || offHandItem == Items.SHIELD)
                                    item = Items.PLAYER_HEAD;
                                else item = Items.SHIELD;
                            }
                            case GappleBall -> {
                                if (mc.player.getOffhandItem().isEmpty() || offHandItem == Items.GOLDEN_APPLE)
                                    item = Items.PLAYER_HEAD;
                                else item = Items.GOLDEN_APPLE;
                            }
                            case GappleShield -> {
                                if (mc.player.getOffhandItem().isEmpty() || offHandItem == Items.SHIELD)
                                    item = Items.GOLDEN_APPLE;
                                else item = Items.SHIELD;
                            }
                            case BallTotem -> {
                                if (mc.player.getOffhandItem().isEmpty() || offHandItem == Items.TOTEM_OF_UNDYING)
                                    item = Items.PLAYER_HEAD;
                                else item = Items.TOTEM_OF_UNDYING;
                            }
                        }

                            prevItem = item;

                    }
            }

            case Crystal -> item = Items.END_CRYSTAL;

            case GApple -> {
                if (crappleSpoof.getValue()) {
                    if (mc.player.hasEffect(MobEffects.ABSORPTION) && mc.player.getEffect(MobEffects.ABSORPTION).getAmplifier() > 2) {
                        if (crapple.found() || offHandItem == Items.GOLDEN_APPLE)
                            item = Items.GOLDEN_APPLE;
                        else if (gapple.found() || offHandItem == Items.ENCHANTED_GOLDEN_APPLE)
                            item = Items.ENCHANTED_GOLDEN_APPLE;
                    } else {
                        if (gapple.found() || offHandItem == Items.ENCHANTED_GOLDEN_APPLE)
                            item = Items.ENCHANTED_GOLDEN_APPLE;
                        else if (crapple.found() || offHandItem == Items.GOLDEN_APPLE)
                            item = Items.GOLDEN_APPLE;
                    }
                } else {
                    if (crapple.found() || offHandItem == Items.GOLDEN_APPLE)
                        item = Items.GOLDEN_APPLE;
                    else if (gapple.found() || offHandItem == Items.ENCHANTED_GOLDEN_APPLE)
                        item = Items.ENCHANTED_GOLDEN_APPLE;
                }
            }

            case Shield -> {
                if (shield.found() || offHandItem == Items.SHIELD) {
                    if (getTriggerHealth() <= healthS.getValue()) {
                        if (crapple.found() || offHandItem == Items.GOLDEN_APPLE)
                            item = Items.GOLDEN_APPLE;
                        else if (gapple.found() || offHandItem == Items.ENCHANTED_GOLDEN_APPLE)
                            item = Items.ENCHANTED_GOLDEN_APPLE;
                    } else {
                        if (!mc.player.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD))) item = Items.SHIELD;
                        else {
                            if (crapple.found() || offHandItem == Items.GOLDEN_APPLE)
                                item = Items.GOLDEN_APPLE;
                            else if (gapple.found() || offHandItem == Items.ENCHANTED_GOLDEN_APPLE)
                                item = Items.ENCHANTED_GOLDEN_APPLE;
                        }
                    }
                } else if (crapple.found() || offHandItem == Items.GOLDEN_APPLE)
                    item = Items.GOLDEN_APPLE;
            }
        }


        if (getTriggerHealth() <= healthF.getValue() && (InventoryUtility.findItemInInventory(Items.TOTEM_OF_UNDYING).found() || offHandItem == Items.TOTEM_OF_UNDYING))
            item = Items.TOTEM_OF_UNDYING;

        if (!rcGap.is(RCGap.Off) && mc.player.getMainHandItem().is(ItemTags.SWORDS) && mc.options.keyUse.isDown() && !(offHandItem instanceof ShieldItem)) {
            if (rcGap.is(RCGap.Always) || (rcGap.is(RCGap.OnlySafe) && getTriggerHealth() > healthF.getValue())) {
                if (crapple.found() || offHandItem == Items.GOLDEN_APPLE)
                    item = Items.GOLDEN_APPLE;
                if (gapple.found() || offHandItem == Items.ENCHANTED_GOLDEN_APPLE)
                    item = Items.ENCHANTED_GOLDEN_APPLE;
            }
        }

        if (onFall.getValue() && (getTriggerHealth()) - (((mc.player.fallDistance - 3) / 2F) + 3.5F) < 0.5)
            item = Items.TOTEM_OF_UNDYING;

        if (onElytra.getValue() && mc.player.isFallFlying())
            item = Items.TOTEM_OF_UNDYING;

        if (onCrystalInHand.getValue()) {
            for (Player pl : Managers.ASYNC.getAsyncPlayers()) {
                if (Managers.FRIEND.isFriend(pl)) continue;
                if (pl == mc.player) continue;
                if (getPlayerPos().distanceToSqr(pl.position()) < 36) {
                    if (pl.getMainHandItem().getItem() == Items.OBSIDIAN
                            || pl.getMainHandItem().getItem() == Items.END_CRYSTAL
                            || pl.getOffhandItem().getItem() == Items.OBSIDIAN
                            || pl.getOffhandItem().getItem() == Items.END_CRYSTAL)
                        item = Items.TOTEM_OF_UNDYING;
                }
            }
        }

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == null || !entity.isAlive()) continue;
            if (getPlayerPos().distanceToSqr(entity.position()) > 36) continue;

            if (onCrystal.getValue()) {
                if (entity instanceof EndCrystal) {
                    if (getTriggerHealth() - ExplosionUtility.getExplosionDamageWPredict(entity.position(), mc.player, PredictUtility.createBox(getPlayerPos(), mc.player), false) < 0.5) {
                        item = Items.TOTEM_OF_UNDYING;
                        break;
                    }
                }
            }

            if (onTnt.getValue()) {
                if (entity instanceof PrimedTnt) {
                    item = Items.TOTEM_OF_UNDYING;
                    break;
                }
            }

            if (onMinecartTnt.getValue()) {
                if (entity instanceof MinecartTNT) {
                    item = Items.TOTEM_OF_UNDYING;
                    break;
                }
            }

            if (onCreeper.getValue()) {
                if (entity instanceof Creeper) {
                    item = Items.TOTEM_OF_UNDYING;
                    break;
                }
            }
        }

        if (onAnchor.getValue()) {
            for (int x = -6; x <= 6; x++)
                for (int y = -6; y <= 6; y++)
                    for (int z = -6; z <= 6; z++) {
                        BlockPos bp = new BlockPos(x, y, z);
                        if (mc.level.getBlockState(bp).getBlock() == Blocks.RESPAWN_ANCHOR) {
                            item = Items.TOTEM_OF_UNDYING;
                            break;
                        }
                    }
        }

        for (int i = 9; i < 45; i++) {
            if (item != null && mc.player.getOffhandItem().getItem() == item) return -1;
            if (mc.player.getInventory().getItem(i >= 36 ? i - 36 : i).getItem().equals(item)) {
                itemSlot = i >= 36 ? i - 36 : i;
                break;
            }
        }

        if (item == mc.player.getMainHandItem().getItem() && mc.options.keyUse.isDown()) return -1;


        return itemSlot;
    }

    private Vec3 getPlayerPos() {
        return ModuleManager.blink.isEnabled() ? Blink.lastPos : mc.player.position();
    }
}
