package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.EventTick;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.injection.accesors.IPlayerMoveC2SPacket;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

public class NoFall extends Module {
    public NoFall() {
        super("NoFall", Category.MOVEMENT);
    }

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.Rubberband);
    public final Setting<FallDistance> fallDistance = new Setting<>("FallDistance", FallDistance.Calc);
    public final Setting<Integer> fallDistanceValue = new Setting<>("FallDistanceVal", 10, 2, 100, v -> fallDistance.getValue() == FallDistance.Custom);
    private final Setting<Boolean> powderSnowBucket = new Setting<>("PowderSnowBucket", true, v -> mode.getValue() == Mode.Items);
    private final Setting<Boolean> waterBucket = new Setting<>("WaterBucket", true, v -> mode.getValue() == Mode.Items);
    private final Setting<Boolean> retrieve = new Setting<>("Retrieve", true, v -> mode.getValue() == Mode.Items && waterBucket.getValue());
    private final Setting<Boolean> enderPearl = new Setting<>("EnderPearl", true, v -> mode.getValue() == Mode.Items);
    private final Setting<Boolean> cobweb = new Setting<>("Cobweb", true, v -> mode.getValue() == Mode.Items);
    private final Setting<Boolean> twistingVines = new Setting<>("TwistingVines", true, v -> mode.getValue() == Mode.Items);

    private thunder.hack.utility.Timer pearlCooldown = new thunder.hack.utility.Timer();

    private boolean retrieveFlag;
    private boolean cancelGround = false;

    @EventHandler
    public void onSync(EventSync e) {
        if (fullNullCheck())
            return;

        if (isFalling()) {
            switch (mode.getValue()) {
                case MatrixOffGround, Vanilla -> cancelGround = true;
                case Rubberband -> sendPacket(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
                case Items -> {
                    BlockPos playerPos = BlockPos.containing(mc.player.position());

                    SearchInvResult snowResult = InventoryUtility.findItemInHotBar(Items.POWDER_SNOW_BUCKET);
                    SearchInvResult pearlResult = InventoryUtility.findItemInHotBar(Items.ENDER_PEARL);
                    SearchInvResult webResult = InventoryUtility.findItemInHotBar(Items.COBWEB);
                    SearchInvResult vinesResult = InventoryUtility.findItemInHotBar(Items.TWISTING_VINES);
                    SearchInvResult waterResult = InventoryUtility.findItemInHotBar(Items.WATER_BUCKET);

                    if (waterResult.found() && waterBucket.getValue()) {
                        mc.player.setXRot(90);
                        doWaterDrop(waterResult, playerPos);
                    } else if (pearlResult.found() && enderPearl.getValue()) {
                        mc.player.setXRot(90);
                        doPearlDrop(pearlResult);
                    } else if (webResult.found() && cobweb.getValue()) {
                        mc.player.setXRot(90);
                        doWebDrop(webResult, playerPos);
                    } else if (vinesResult.found() && twistingVines.getValue()) {
                        mc.player.setXRot(90);
                        doVinesDrop(vinesResult, playerPos);
                    } else if (snowResult.found() && powderSnowBucket.getValue()) {
                        mc.player.setXRot(90);
                        doSnowDrop(snowResult, playerPos);
                    }
                }
            }
        } else if (retrieveFlag) {
            InventoryUtility.saveSlot();
            SearchInvResult waterResult = InventoryUtility.findItemInHotBar(Items.BUCKET);
            waterResult.switchTo();
            mc.player.setXRot(90);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
            InventoryUtility.returnSlot();
            retrieveFlag = false;
        }
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (mode.is(Mode.Grim2b2t) && isFalling()) {
            sendPacket(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY() + 0.000000001, mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), false, mc.player.horizontalCollision));
            mc.player.resetFallDistance();
        }
    }

    private void doWaterDrop(SearchInvResult waterResult, BlockPos playerPos) {
        if (mc.level.getBlockState(playerPos.below()).isRedstoneConductor(mc.level, playerPos.below()) || mc.level.getBlockState(playerPos.below().below()).isRedstoneConductor(mc.level, playerPos.below().below())) {
            InventoryUtility.saveSlot();
            waterResult.switchTo();
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
            InventoryUtility.returnSlot();
            retrieveFlag = retrieve.getValue();
        }
    }

    private void doPearlDrop(SearchInvResult pearlResult) {
        if (pearlCooldown.passedMs(5000)) {
            InventoryUtility.saveSlot();
            pearlResult.switchTo();
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
            InventoryUtility.returnSlot();
            pearlCooldown.reset();
        }
    }

    private void doWebDrop(SearchInvResult webResult, BlockPos playerPos) {
        if (mc.level.getBlockState(playerPos.below()).isRedstoneConductor(mc.level, playerPos.below()) || mc.level.getBlockState(playerPos.below().below()).isRedstoneConductor(mc.level, playerPos.below().below())) {
            InventoryUtility.saveSlot();
            if (mc.level.getBlockState(playerPos.below()).isRedstoneConductor(mc.level, playerPos.below()))
                InteractionUtility.placeBlock(playerPos, InteractionUtility.Rotate.None, InteractionUtility.Interact.Vanilla, InteractionUtility.PlaceMode.Normal, webResult.slot(), false, true);
            else
                InteractionUtility.placeBlock(playerPos.below(), InteractionUtility.Rotate.None, InteractionUtility.Interact.Vanilla, InteractionUtility.PlaceMode.Normal, webResult.slot(), false, true);
            mc.player.swing(InteractionHand.MAIN_HAND);
            InventoryUtility.returnSlot();
        }
    }

    private void doVinesDrop(SearchInvResult vinesResult, BlockPos playerPos) {
        if (mc.level.getBlockState(playerPos.below()).isRedstoneConductor(mc.level, playerPos.below()) || mc.level.getBlockState(playerPos.below().below()).isRedstoneConductor(mc.level, playerPos.below().below())) {
            InventoryUtility.saveSlot();
            if (mc.level.getBlockState(playerPos.below()).isRedstoneConductor(mc.level, playerPos.below()))
                InteractionUtility.placeBlock(playerPos, InteractionUtility.Rotate.None, InteractionUtility.Interact.Vanilla, InteractionUtility.PlaceMode.Normal, vinesResult.slot(), false, true);
            else
                InteractionUtility.placeBlock(playerPos.below(), InteractionUtility.Rotate.None, InteractionUtility.Interact.Vanilla, InteractionUtility.PlaceMode.Normal, vinesResult.slot(), false, true);
            mc.player.swing(InteractionHand.MAIN_HAND);
            InventoryUtility.returnSlot();
        }
    }

    private void doSnowDrop(SearchInvResult snowResult, BlockPos playerPos) {
        if (mc.level.getBlockState(playerPos.below()).isRedstoneConductor(mc.level, playerPos.below()) || mc.level.getBlockState(playerPos.below().below()).isRedstoneConductor(mc.level, playerPos.below().below())) {
            InventoryUtility.saveSlot();
            if (mc.level.getBlockState(playerPos.below()).isRedstoneConductor(mc.level, playerPos.below()))
                InteractionUtility.placeBlock(playerPos, InteractionUtility.Rotate.None, InteractionUtility.Interact.Vanilla, InteractionUtility.PlaceMode.Normal, snowResult.slot(), false, true);
            else
                InteractionUtility.placeBlock(playerPos.below(), InteractionUtility.Rotate.None, InteractionUtility.Interact.Vanilla, InteractionUtility.PlaceMode.Normal, snowResult.slot(), false, true);
            mc.player.swing(InteractionHand.MAIN_HAND);
            InventoryUtility.returnSlot();
        }
    }

    public boolean isFalling() {
        if (mc == null || mc.player == null || mc.level == null)
            return false;

        if (mc.player.isFallFlying())
            return false;

        if (mode.is(Mode.Grim2b2t))
            return mc.player.fallDistance > 3f;

        switch (fallDistance.getValue()) {
            case Custom -> {
                return mc.player.fallDistance > fallDistanceValue.getValue();
            }
            case Calc -> {
                return (((mc.player.fallDistance - 3) / 2F) + 3.5F) > mc.player.getHealth() / 3f;
            }
        }
        return false;
    }

    @Override
    public String getDisplayInfo() {
        return mode.getValue().toString() + " " + (isFalling() ? "Ready" : "");
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (e.getPacket() instanceof ServerboundMovePlayerPacket pac) {
            if (cancelGround)
                ((IPlayerMoveC2SPacket) pac).setOnGround(false);
        }
    }

    @Override
    public void onEnable() {
        cancelGround = false;
    }

    private enum Mode {
        Rubberband, Items, MatrixOffGround, Vanilla, Grim2b2t
    }

    private enum FallDistance {
        Calc, Custom
    }
}
