package thunder.hack.features.modules.player;

import net.minecraft.world.effect.MobEffects;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class AntiBadEffects extends Module {
    public AntiBadEffects() {
        super("AntiBadEffects", Category.PLAYER);
    }

    private final Setting<Boolean> blindness = new Setting<>("Blindness", true);
    private final Setting<Boolean> nausea = new Setting<>("Nausea", true);
    private final Setting<Boolean> miningFatigue = new Setting<>("MiningFatigue", true);
    private final Setting<Boolean> levitation = new Setting<>("Levitation", true);
    private final Setting<Boolean> slowness = new Setting<>("Slowness", true);
    private final Setting<Boolean> jumpBoost = new Setting<>("JumpBoost", true);

    @Override
    public void onUpdate() {
        if (mc.player.hasEffect(MobEffects.BLINDNESS) && blindness.getValue()) mc.player.removeEffect(MobEffects.BLINDNESS);
        if (mc.player.hasEffect(MobEffects.NAUSEA) && nausea.getValue()) mc.player.removeEffect(MobEffects.NAUSEA);
        if (mc.player.hasEffect(MobEffects.MINING_FATIGUE) && miningFatigue.getValue()) mc.player.removeEffect(MobEffects.MINING_FATIGUE);
        if (mc.player.hasEffect(MobEffects.LEVITATION) && levitation.getValue()) mc.player.removeEffect(MobEffects.LEVITATION);
        if (mc.player.hasEffect(MobEffects.SLOWNESS) && slowness.getValue()) mc.player.removeEffect(MobEffects.SLOWNESS);
        if (mc.player.hasEffect(MobEffects.JUMP_BOOST) && jumpBoost.getValue()) mc.player.removeEffect(MobEffects.JUMP_BOOST);
    }
}