package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Blocks;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.player.MovementUtility;

import static thunder.hack.features.modules.combat.Criticals.getEntity;
import static thunder.hack.features.modules.combat.Criticals.getInteractType;

public class MoreKnockback extends Module {
    public MoreKnockback() {
        super("MoreKnockback", Category.COMBAT);
    }

    public Setting<Boolean> inMove = new Setting<>("InMove", true);
    public Setting<Integer> hurtTime = new Setting<>("HurtTime", 10, 0, 10);
    public Setting<Integer> chance = new Setting<>("Chance", 100, 0, 100);

    @EventHandler
    public void onSendPacket(PacketEvent.Send event) {
        if ((!MovementUtility.isMoving() || inMove.getValue())
                && event.getPacket() instanceof ServerboundInteractPacket
                && getInteractType(event.getPacket()) == Criticals.InteractType.ATTACK
                && !(getEntity(event.getPacket()) instanceof EndCrystal)
                && getEntity(event.getPacket()) instanceof LivingEntity lent
                && lent.hurtTime <= hurtTime.getValue()
                && MathUtility.random(0, 100) >= (100 - chance.getValue())
                && !canCrit()) {

            if (mc.player.isSprinting()) sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
            sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
            debug("wtap");
            mc.player.setSprinting(true);
            mc.player.wasSprinting = true;
        }
    }

    private boolean canCrit() {
        boolean reasonForSkipCrit =
                        mc.player.getAbilities().flying
                        || (mc.player.isFallFlying()
                        || ModuleManager.elytraPlus.isEnabled())
                        || mc.player.hasEffect(MobEffects.BLINDNESS)
                        || mc.level.getBlockState(BlockPos.containing(mc.player.position())).getBlock() == Blocks.COBWEB
                        || mc.player.isInLava()
                        || mc.player.isUnderWater();

        if (mc.player.getAttackStrengthScale(0.5f) < 0.9f)
            return false;

        if (ModuleManager.criticals.isEnabled() && !ModuleManager.criticals.mode.is(Criticals.Mode.Grim))
            return true;

        if (ModuleManager.criticals.isEnabled() && ModuleManager.criticals.mode.is(Criticals.Mode.Grim) && !mc.player.onGround())
            return true;

        if (!reasonForSkipCrit)
            return !mc.player.onGround() && mc.player.fallDistance > 0f;

        return false;
    }
}
