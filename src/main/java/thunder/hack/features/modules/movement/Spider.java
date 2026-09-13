package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

public class Spider extends Module {
    public final Setting<Integer> delay = new Setting<>("delay", 2, 1, 15);
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Matrix);

    public Spider() {
        super("Spider", Category.MOVEMENT);
    }

    public static Direction getPlaceableSide(BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbour = pos.relative(side);
            if (mc.level.isEmptyBlock(neighbour)) {
                continue;
            }
            if (!mc.level.getBlockState(neighbour).canBeReplaced()) {
                return side;
            }
        }
        return null;
    }

    @Override
    public void onUpdate() {
        if (!mc.player.horizontalCollision) return;
        if (mc.player.tickCount % 2 == 0 && mc.options.keyJump.isDown() && mode.getValue() == Mode.FunTime) {
            float pitch = mc.player.getXRot();
            mc.player.setXRot(82);
            int slot = getAtHotBar();
            if (slot != -1) {
                int originalSlot = mc.player.getInventory().getSelectedSlot();
                mc.player.getInventory().setSelectedSlot(slot);
                sendPacket(new ServerboundSetCarriedItemPacket(slot));

                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                mc.player.swing(InteractionHand.MAIN_HAND);

                mc.player.getInventory().setSelectedSlot(originalSlot);
                sendPacket(new ServerboundSetCarriedItemPacket(originalSlot));
            }

            mc.player.setXRot(pitch);
        }


        if (mode.getValue() == Mode.Default) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x(), 0.21, mc.player.getDeltaMovement().z());
        } else if (mode.getValue() == Mode.Matrix) {
            mc.player.setOnGround(mc.player.tickCount % delay.getValue() == 0);
            mc.player.yo -= 2.0E-232;
            if (mc.player.onGround())
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x(), 0.42, mc.player.getDeltaMovement().z());
        }
    }


    @EventHandler
    public void onSync(EventSync event) {
        if (mc.options.keyJump.isDown() && mc.player.getDeltaMovement().y() <= -0.3739040364667221 && mode.getValue() == Mode.MatrixNew) {
            mc.player.setOnGround(true);
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x(), 0.481145141919180, mc.player.getDeltaMovement().z());
        }
        if (mc.player.tickCount % delay.getValue() == 0 && mc.player.horizontalCollision && MovementUtility.isMoving() && mode.getValue() == Mode.Blocks) {
            int find = -2;
            for (int i = 0; i <= 8; i++)
                if (mc.player.getInventory().getItem(i).getItem() instanceof BlockItem) find = i;
            if (find == -2) return;
            BlockPos pos = BlockPos.containing(mc.player.getX(), mc.player.getY() + 2, mc.player.getZ());
            Direction side = getPlaceableSide(pos);
            if (side != null) {
                sendPacket(new ServerboundSetCarriedItemPacket(find));
                BlockPos neighbour = BlockPos.containing(mc.player.getX(), mc.player.getY() + 2, mc.player.getZ()).relative(side);
                Direction opposite = side.getOpposite();
                Vec3 hitVec = new Vec3(neighbour.getX() + 0.5, neighbour.getY() + 0.5, neighbour.getZ() + 0.5).add(new Vec3(opposite.step()).scale(0.5));
                sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, new BlockHitResult(hitVec, opposite, neighbour, false), id));
                sendPacket(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(false, false, false, false, false, true, false)));
                if (mc.level.getBlockState(BlockPos.containing(mc.player.position()).offset(0, 2, 0)).getBlock() != Blocks.AIR) {
                    sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, neighbour, opposite));
                    sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, neighbour, opposite));
                }
                sendPacket(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(false, false, false, false, false, false, false)));
            }
            mc.player.setOnGround(true);
            mc.player.jumpFromGround();
            sendPacket(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
        }
    }

    private int getAtHotBar() {
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = mc.player.getInventory().getItem(i);
            if (!(itemStack.getItem() == Items.WATER_BUCKET)) continue;
            return i;
        }
        return -1;
    }

    public enum Mode {
        Default, Matrix, MatrixNew, Blocks, FunTime
    }
}