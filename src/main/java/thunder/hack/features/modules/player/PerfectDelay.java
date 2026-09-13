package thunder.hack.features.modules.player;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class PerfectDelay extends Module {
    public PerfectDelay() {
        super("PerfectDelay", Category.PLAYER);
    }

    private final Setting<HorseJump> horse = new Setting<>("Horse", HorseJump.Legit);
    private final Setting<Boolean> bow = new Setting<>("Bow", true);
    private final Setting<Boolean> crossbow = new Setting<>("Crossbow", true);
    private final Setting<Boolean> trident = new Setting<>("Trident", true);

    private float getEnchantLevel(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.QUICK_CHARGE), stack);
    }

    @Override
    public void onUpdate() {
        if (mc.player.getUseItem().getItem() instanceof TridentItem && trident.getValue()) {
            if (mc.player.getTicksUsingItem() > (ModuleManager.tridentBoost.isEnabled() ? ModuleManager.tridentBoost.cooldown.getValue() : 9))
                mc.gameMode.releaseUsingItem(mc.player);
        }

        if (mc.player.getUseItem().getItem() instanceof CrossbowItem && crossbow.getValue()) {
            if (mc.player.getTicksUsingItem() >= 25 - (0.25 * getEnchantLevel(mc.player.getUseItem()) * 20))
                mc.gameMode.releaseUsingItem(mc.player);
        }

        if (mc.player.getUseItem().getItem() instanceof BowItem && bow.getValue()) {
            if (mc.player.getTicksUsingItem() > 19)
                mc.gameMode.releaseUsingItem(mc.player);
        }

        if (mc.player.getControlledVehicle() != null && mc.player.getControlledVehicle() instanceof Horse && horse.is(HorseJump.Rage)) {
            ((IClientPlayerEntity) mc.player).setMountJumpStrength(1f);
        }

        if (mc.player.getControlledVehicle() != null && mc.player.getControlledVehicle() instanceof Horse && horse.is(HorseJump.Legit) && mc.player.getJumpRidingScale() >= 1) {
            mc.options.keyJump.setDown(false);
        }
    }

    private enum HorseJump {
        Legit, Rage, Off
    }
}
