package thunder.hack.features.modules.misc;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thunder.hack.features.modules.Module;
import thunder.hack.injection.accesors.IMinecraftClient;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.MathUtility;

public class TapeMouse extends Module {
    public TapeMouse() {
        super("TapeMouse", Category.MISC);
    }

    private final Setting<Integer> delay = new Setting<>("Delay", 600, 0, 10000);
    private final Setting<BooleanSettingGroup> randomize = new Setting<>("Randomize", new BooleanSettingGroup(false));
    private final Setting<Integer> randomizeValue = new Setting<>("Value", 600, 0, 10000).addToGroup(randomize);
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Left);
    private final Setting<Boolean> legit = new Setting<>("Legit", false, v -> mode.getValue() == Mode.Left);

    private final Timer timer = new Timer();

    private enum Mode {Right, Left}

    @Override
    public void onUpdate() {
        if (timer.every((long) (delay.getValue() + (randomize.getValue().isEnabled() ? MathUtility.random(0, randomizeValue.getValue()) : 0))))
            if (mode.getValue() == Mode.Left) {
                if (!legit.getValue()) {
                    HitResult hr = mc.hitResult;
                    if (hr != null) {
                        if (hr instanceof EntityHitResult ehr && ehr.getEntity() != null) {
                            mc.gameMode.attack(mc.player, ehr.getEntity());
                            mc.player.swing(InteractionHand.MAIN_HAND);
                        } else if (hr instanceof BlockHitResult bhr && bhr.getBlockPos() != null && bhr.getDirection() != null && !mc.level.isEmptyBlock(bhr.getBlockPos())) {
                            mc.gameMode.startDestroyBlock(bhr.getBlockPos(), bhr.getDirection());
                            mc.player.swing(InteractionHand.MAIN_HAND);
                        }
                    }
                } else ((IMinecraftClient) mc).idoAttack();
            } else ((IMinecraftClient) mc).idoItemUse();
    }
}
