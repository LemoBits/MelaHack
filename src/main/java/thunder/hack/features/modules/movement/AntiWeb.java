package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WebBlock;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventCollision;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

public class AntiWeb extends Module {
    public AntiWeb() {
        super("AntiWeb", Category.MOVEMENT);
    }

    public static final Setting<Mode> mode = new Setting<>("Mode", Mode.Solid);
    public static final Setting<Boolean> grim = new Setting<>("Grim", false, v -> mode.is(Mode.Ignore));
    public static final Setting<Float> timer = new Setting<>("Timer", 20f, 1f, 50f, v -> mode.getValue() == Mode.Timer);
    public Setting<Float> speed = new Setting<>("Speed", 0.3f, 0.0f, 10.0f, v -> mode.getValue() == Mode.Fly);

    private boolean timerEnabled = false;

    public enum Mode {
        Timer, Solid, Ignore, Fly
    }

    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent e) {
        if (Managers.PLAYER.isInWeb()) {
            if (mode.getValue() == Mode.Timer) {
                if (mc.player.onGround()) {
                    ThunderHack.TICK_TIMER = 1f;
                } else {
                    ThunderHack.TICK_TIMER = timer.getValue();
                    timerEnabled = true;
                }
            }
            if (mode.getValue() == Mode.Fly) {
                final double[] dir = MovementUtility.forward(speed.getValue());
                mc.player.setDeltaMovement(dir[0], 0, dir[1]);
                if (mc.options.keyJump.isDown())
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(0, speed.getValue(), 0));
                if (mc.options.keyShift.isDown())
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(0, -speed.getValue(), 0));
            }
        } else if (timerEnabled) {
            timerEnabled = false;
            ThunderHack.TICK_TIMER = 1f;
        }
    }

    @EventHandler
    public void onCollide(EventCollision e) {
        if (e.getState().getBlock() instanceof WebBlock && mode.getValue() == Mode.Solid)
            e.setState(Blocks.DIRT.defaultBlockState());
    }
}
