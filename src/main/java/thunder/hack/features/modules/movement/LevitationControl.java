package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.effect.MobEffects;
import thunder.hack.events.impl.EventMove;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class LevitationControl extends Module {
    public LevitationControl() {
        super("LevitCtrl", Category.MOVEMENT);
    }

    private final Setting<Integer> upAmplifier = new Setting<>("Up Speed", 1, 1, 5);
    private final Setting<Integer> downAmplifier = new Setting<>("Down Speed", 1, 1, 5);

    @EventHandler
    public void onMove(EventMove e) {
        if (mc.player.hasEffect(MobEffects.LEVITATION)) {
            int amplifier = mc.player.getEffect(MobEffects.LEVITATION).getAmplifier();
            if (mc.options.keyJump.isDown()) e.setY(((0.05D * (double) (amplifier + 1) - e.getY()) * 0.2D) * upAmplifier.getValue() * 100);
            else if (mc.options.keyShift.isDown()) e.setY(-(((0.05D * (double) (amplifier + 1) - e.getY()) * 0.2D) * downAmplifier.getValue() * 100));
            else e.setY(0);
            e.cancel();
        }
    }
}
