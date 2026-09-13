package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import thunder.hack.events.impl.UseTridentEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class TridentBoost extends Module {
    public TridentBoost() {
        super("TridentBoost", Category.MOVEMENT);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Motion);
    private final Setting<Float> factor = new Setting<>("Factor", 1f, 0.1f, 20f);
    public final Setting<Integer> cooldown = new Setting<>("Cooldown", 10, 0, 20);
    public final Setting<Boolean> anyWeather = new Setting<>("AnyWeather", true);

    private enum Mode {
        Motion, Factor
    }

    @EventHandler
    public void onUseTrident(UseTridentEvent e) {
        if (mc.player.getTicksUsingItem() >= cooldown.getValue()) {
            float j = EnchantmentHelper.getTridentSpinAttackStrength(mc.player.getUseItem(), mc.player);
            if (anyWeather.getValue() || mc.player.isInWaterOrRain()) {
                if (j > 0) {
                    float f = mc.player.getYRot();
                    float g = mc.player.getXRot();
                    float speedX = -Mth.sin(f * 0.017453292F) * Mth.cos(g * 0.017453292F);
                    float speedY = -Mth.sin(g * 0.017453292F);
                    float speedZ = Mth.cos(f * 0.017453292F) * Mth.cos(g * 0.017453292F);
                    float plannedSpeed = Mth.sqrt(speedX * speedX + speedY * speedY + speedZ * speedZ);

                    float n = mode.is(Mode.Factor) ? factor.getValue() * 3.0F * ((1.0F + (float) j) / 4.0F) : factor.getValue();

                    speedX *= n / plannedSpeed;
                    speedY *= n / plannedSpeed;
                    speedZ *= n / plannedSpeed;

                    mc.player.push(speedX, speedY, speedZ);
                    mc.player.startAutoSpinAttack(20, 8f, mc.player.getUseItem());

                    if (mc.player.onGround())
                        mc.player.move(MoverType.SELF, new Vec3(0.0, 1.1999999284744263, 0.0));

                    Holder<SoundEvent> registryEntry = EnchantmentHelper.pickHighestLevel(mc.player.getUseItem(), EnchantmentEffectComponents.TRIDENT_SOUND).orElse(SoundEvents.TRIDENT_THROW);
                    mc.level.playSound(null, mc.player, registryEntry.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        }
        e.cancel();
    }
}
