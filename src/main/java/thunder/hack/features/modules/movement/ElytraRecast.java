package thunder.hack.features.modules.movement;

import com.mojang.blaze3d.platform.InputConstants;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.EventTravel;
import thunder.hack.injection.accesors.ILivingEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.math.MathUtility;

//https://github.com/InLieuOfLuna/elytra-recast <-- Author of this exploit
public class ElytraRecast extends Module {
    public ElytraRecast() {
        super("ElytraRecast", Category.MOVEMENT);
    }

    public Setting<Exploit> exploit = new Setting<>("Exploit", Exploit.None);
    public Setting<Boolean> changePitch = new Setting<>("ChangePitch", true);
    public Setting<Float> pitchValue = new Setting<>("PitchValue", 55f, -90f, 90f, v -> changePitch.getValue());
    public Setting<Boolean> autoWalk = new Setting<>("AutoWalk", true);
    public Setting<Boolean> autoJump = new Setting<>("AutoJump", true);
    public Setting<Boolean> allowBroken = new Setting<>("AllowBroken", true);

    private float prevClientPitch, prevClientYaw, jitter;

    private enum Exploit {
        None, Strict, Strong
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (changePitch.getValue())
            mc.player.setXRot(pitchValue.getValue());

        switch (exploit.getValue()) {
            case None -> {}
            case Strict -> mc.player.setYRot(mc.player.getYRot() + jitter);
            case Strong -> mc.player.setXRot(pitchValue.getValue() - Math.abs(jitter / 2f));
        }
    }

    @EventHandler
    public void modifyVelocity(EventTravel e) {
        if (changePitch.getValue())
            if (e.isPre()) {
                prevClientPitch = mc.player.getXRot();
                prevClientYaw = mc.player.getYRot();
                mc.player.setXRot(pitchValue.getValue());

                switch (exploit.getValue()) {
                    case None -> {
                    }
                    case Strict -> mc.player.setYRot(mc.player.getYRot() + jitter);
                    case Strong -> mc.player.setXRot(pitchValue.getValue() - Math.abs(jitter / 2f));
                }
            } else {
                mc.player.setXRot(prevClientPitch);
                if (exploit.getValue() == Exploit.Strict)
                    mc.player.setYRot(prevClientYaw);
            }
    }

    @Override
    public void onDisable() {
        if (!InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyUp.getDefaultKey().getValue()))
            mc.options.keyUp.setDown(false);
        if (!InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getDefaultKey().getValue()))
            mc.options.keyJump.setDown(false);
    }

    @Override
    public void onUpdate() {
        if (autoJump.getValue()) mc.options.keyJump.setDown(true);
        if (autoWalk.getValue()) mc.options.keyUp.setDown(true);

        if (!mc.player.isFallFlying() && mc.player.fallDistance > 0 && checkElytra() && !mc.player.isFallFlying())
            castElytra();

        jitter = (20 * MathUtility.sin((System.currentTimeMillis() - ThunderHack.initTime) / 50f));

        ((ILivingEntity) mc.player).setLastJumpCooldown(0);
    }

    public boolean castElytra() {
        if (checkElytra() && check()) {
            sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            return true;
        }
        return false;
    }

    private boolean checkElytra() {
        if (mc.player.input.keyPresses.jump() && !mc.player.getAbilities().flying && !mc.player.isPassenger() && !mc.player.onClimbable()) {
            ItemStack is = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            return isUsableElytra(is);
        }
        return false;
    }

    private boolean check() {
        if (!mc.player.isInWater() && !mc.player.hasEffect(MobEffects.LEVITATION)) {
            ItemStack is = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            if (isUsableElytra(is)) {
                mc.player.startFallFlying();
                return true;
            }
        }
        return false;
    }

    private boolean isUsableElytra(ItemStack stack) {
        if (!stack.is(Items.ELYTRA)) {
            return false;
        }
        return allowBroken.getValue() || LivingEntity.canGlideUsing(stack, EquipmentSlot.CHEST);
    }
}
