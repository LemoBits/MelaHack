package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventAfterRotate;
import thunder.hack.events.impl.EventPostSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.utility.Timer;

public final class AutoBuff extends Module {
    private final Setting<Boolean> strength = new Setting<>("Strength", true);
    private final Setting<Boolean> speed = new Setting<>("Speed", true);
    private final Setting<Boolean> fire = new Setting<>("FireResistance", true);
    private final Setting<BooleanSettingGroup> heal = new Setting<>("InstantHealing", new BooleanSettingGroup(true));
    private final Setting<Integer> healthH = new Setting<>("Health", 8, 0, 20).addToGroup(heal);
    private final Setting<BooleanSettingGroup> regen = new Setting<>("Regeneration", new BooleanSettingGroup(true));
    private final Setting<TriggerOn> triggerOn = new Setting<>("Trigger", TriggerOn.LackOfRegen).addToGroup(regen);
    private final Setting<Integer> healthR = new Setting<>("HP", 8, 0, 20, v -> triggerOn.is(TriggerOn.Health)).addToGroup(regen);
    private final Setting<Boolean> onDaGround = new Setting<>("OnlyOnGround", true);
    private final Setting<Boolean> pauseAura = new Setting<>("PauseAura", false);

    public Timer timer = new Timer();
    private boolean spoofed = false;

    public AutoBuff() {
        super("AutoBuff", Category.COMBAT);
    }

    public static int getPotionSlot(Potions potion) {
        for (int i = 0; i < 9; ++i)
            if (isStackPotion(mc.player.getInventory().getItem(i), potion)) return i;
        return -1;
    }

    public static boolean isPotionOnHotBar(Potions potions) {
        return getPotionSlot(potions) != -1;
    }

    public static boolean isStackPotion(ItemStack stack, Potions potion) {
        if (stack == null) return false;

        if (stack.getItem() instanceof SplashPotionItem) {
            PotionContents potionContentsComponent = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

            Holder<MobEffect> id = null;

            switch (potion) {
                case STRENGTH -> id = MobEffects.STRENGTH;
                case SPEED -> id = MobEffects.SPEED;
                case FIRERES -> id = MobEffects.FIRE_RESISTANCE;
                case HEAL -> id = MobEffects.INSTANT_HEALTH;
                case REGEN -> id = MobEffects.REGENERATION;
            }

            for (MobEffectInstance effect : potionContentsComponent.getAllEffects()) {
                if (effect.getEffect() == id) return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPostRotationSet(EventAfterRotate event) {
        if (Aura.target != null && mc.player.getAttackStrengthScale(1) > 0.5f) return;
        if (mc.player.tickCount > 80 && shouldThrow()) {
            mc.player.setXRot(90);
            spoofed = true;
        }
    }

    private boolean shouldThrow() {
        return (!mc.player.hasEffect(MobEffects.SPEED) && isPotionOnHotBar(Potions.SPEED) && speed.getValue()) || (!mc.player.hasEffect(MobEffects.STRENGTH) && isPotionOnHotBar(Potions.STRENGTH) && strength.getValue()) || (!mc.player.hasEffect(MobEffects.FIRE_RESISTANCE) && isPotionOnHotBar(Potions.FIRERES) && fire.getValue()) || (mc.player.getHealth() + mc.player.getAbsorptionAmount() < healthH.getValue() && isPotionOnHotBar(Potions.HEAL) && heal.getValue().isEnabled()) || (!mc.player.hasEffect(MobEffects.REGENERATION) && triggerOn.is(TriggerOn.LackOfRegen) && isPotionOnHotBar(Potions.REGEN) && regen.getValue().isEnabled()) || (mc.player.getHealth() + mc.player.getAbsorptionAmount() < healthR.getValue() && triggerOn.is(TriggerOn.Health) && isPotionOnHotBar(Potions.REGEN) && regen.getValue().isEnabled());
    }

    @EventHandler
    public void onPostSync(EventPostSync e) {
        if (Aura.target != null && mc.player.getAttackStrengthScale(1) > 0.5f) return;

        if (onDaGround.getValue() && !mc.player.onGround()) return;

        if (mc.player.tickCount > 80 && shouldThrow() && timer.passedMs(1000) && spoofed) {
            if (!mc.player.hasEffect(MobEffects.SPEED) && isPotionOnHotBar(Potions.SPEED) && speed.getValue())
                throwPotion(Potions.SPEED);

            if (!mc.player.hasEffect(MobEffects.STRENGTH) && isPotionOnHotBar(Potions.STRENGTH) && strength.getValue())
                throwPotion(Potions.STRENGTH);

            if (!mc.player.hasEffect(MobEffects.FIRE_RESISTANCE) && isPotionOnHotBar(Potions.FIRERES) && fire.getValue())
                throwPotion(Potions.FIRERES);

            if (mc.player.getHealth() + mc.player.getAbsorptionAmount() < healthH.getValue() && heal.getValue().isEnabled() && isPotionOnHotBar(Potions.HEAL))
                throwPotion(Potions.HEAL);

            if (((!mc.player.hasEffect(MobEffects.REGENERATION) && triggerOn.is(TriggerOn.LackOfRegen)) || (mc.player.getHealth() + mc.player.getAbsorptionAmount() < healthR.getValue() && triggerOn.is(TriggerOn.Health))) && isPotionOnHotBar(Potions.REGEN) && regen.getValue().isEnabled())
                throwPotion(Potions.REGEN);

            sendPacket(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
            timer.reset();
            spoofed = false;
        }
    }

    public void throwPotion(Potions potion) {
        if (pauseAura.getValue()) ModuleManager.aura.pause();
        sendPacket(new ServerboundSetCarriedItemPacket(getPotionSlot(potion)));
        sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
    }

    public enum Potions {
        STRENGTH, SPEED, FIRERES, HEAL, REGEN
    }

    public enum TriggerOn {
        LackOfRegen, Health
    }
}
