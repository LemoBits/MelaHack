package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class ReverseStep extends Module {
    public ReverseStep() {
        super("ReverseStep", Category.MOVEMENT);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Motion);
    private final Setting<Float> timer = new Setting<>("Timer", 3.0F, 1F, 10.0F, v -> mode.getValue() == Mode.Timer);
    private final Setting<Float> motion = new Setting<>("Motion", 1.0F, 0.1F, 10.0F, v -> mode.getValue() == Mode.Motion);
    private final Setting<Boolean> anyblock = new Setting<>("AnyBlock", false);
    private final Setting<Boolean> pauseIfShift = new Setting<>("PauseIfShift", false);

    private boolean disableTimer = true;
    private boolean prevGround = false;

    @EventHandler
    public void onEntitySync(EventSync eventPlayerUpdateWalking) {
        if (ModuleManager.packetFly.isEnabled()) return;

        BlockPos playerPos = BlockPos.containing(mc.player.position());

        if (pauseIfShift.getValue() && mc.options.keyShift.isDown()) {
            disableTimer();
            return;
        }

        if (mc.player.isInWater() || mc.player.isUnderWater() || mc.player.isInLava() || mc.player.isFallFlying() || mc.player.getAbilities().flying || mc.level.getBlockState(playerPos).getBlock() == Blocks.COBWEB) {
            disableTimer();
            return;
        }

        if (checkBlock(mc.level.getBlockState(playerPos.below(2))) || checkBlock(mc.level.getBlockState(playerPos.below(3))) || checkBlock(mc.level.getBlockState(playerPos.below(4))))
            doStep();

        if (disableTimer && (mc.player.onGround())) {
            disableTimer = false;
            ThunderHack.TICK_TIMER = 1.0f;
        }

        prevGround = mc.player.onGround();
    }
    
    private void disableTimer() {
        if (disableTimer)
            ThunderHack.TICK_TIMER = 1f;
    }

    private boolean checkBlock(BlockState bs) {
        return bs.getBlock() == Blocks.BEDROCK || bs.getBlock() != Blocks.OBSIDIAN || anyblock.getValue();
    }

    private void doStep() {
        if (!(mode.getValue() != Mode.Timer || !prevGround || mc.player.onGround() || !(mc.player.getDeltaMovement().y() < -0.1) || disableTimer)) {
            ThunderHack.TICK_TIMER = timer.getValue();
            disableTimer = true;
        }

        if (mc.player.onGround() && mode.getValue() == Mode.Motion)
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(0, -motion.getValue(), 0));
    }

    public enum Mode {
        Timer, Motion
    }
}
