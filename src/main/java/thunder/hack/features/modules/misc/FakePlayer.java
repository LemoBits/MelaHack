package thunder.hack.features.modules.misc;

import com.mojang.authlib.GameProfile;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventAttack;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.TotemPopEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.world.ExplosionUtility;
import thunder.hack.utility.player.InventoryUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakePlayer extends Module {
    private final Setting<Boolean> copyInventory = new Setting<>("CopyInventory", false);

    public static RemotePlayer fakePlayer;

    public FakePlayer() {
        super("FakePlayer", Category.MISC);
    }

    private Setting<Boolean> record = new Setting<>("Record", false);
    private Setting<Boolean> play = new Setting<>("Play", false);
    private Setting<Boolean> autoTotem = new Setting<>("AutoTotem", false);
    private Setting<String> name = new Setting<>("Name", "Hell_Raider");

    private final List<PlayerState> positions = new ArrayList<>();

    int movementTick, deathTime;

    @Override
    public void onEnable() {
        fakePlayer = new RemotePlayer(mc.level, new GameProfile(UUID.fromString("66123666-6666-6666-6666-666666666600"), name.getValue()));
        fakePlayer.copyPosition(mc.player);

        if (copyInventory.getValue()) {
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().copy());
            fakePlayer.setItemInHand(InteractionHand.OFF_HAND, mc.player.getOffhandItem().copy());

            fakePlayer.getInventory().setItem(36, mc.player.getInventory().getItem(36).copy());
            fakePlayer.getInventory().setItem(37, mc.player.getInventory().getItem(37).copy());
            fakePlayer.getInventory().setItem(38, mc.player.getInventory().getItem(38).copy());
            fakePlayer.getInventory().setItem(39, mc.player.getInventory().getItem(39).copy());
        }

        mc.level.addEntity(fakePlayer);
        fakePlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 9999, 2));
        fakePlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 9999, 4));
        fakePlayer.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 9999, 1));
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof ClientboundExplodePacket explosion && fakePlayer != null && fakePlayer.hurtTime == 0) {
            fakePlayer.handleDamageEvent(mc.level.damageSources().generic());
            Vec3 center = explosion.center();
            fakePlayer.setHealth(fakePlayer.getHealth() + fakePlayer.getAbsorptionAmount() - ExplosionUtility.getAutoCrystalDamage(new Vec3(center.x, center.y, center.z), fakePlayer, 0, false));
            if (fakePlayer.isDeadOrDying()) {
                if (fakePlayer.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                    fakePlayer.getOffhandItem().shrink(1);
                    fakePlayer.setHealth(10f);


                    ThunderHack.EVENT_BUS.post(new TotemPopEvent(fakePlayer, 1));

                    //      new EntityStatusS2CPacket(fakePlayer, EntityStatuses.USE_TOTEM_OF_UNDYING).apply(mc.player.networkHandler);
                }
            }
        }
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (record.getValue()) {
            positions.add(new PlayerState(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot()));
            return;
        }
        if (fakePlayer != null) {
            if (play.getValue() && !positions.isEmpty()) {
                movementTick++;

                if (movementTick >= positions.size()) {
                    movementTick = 0;
                    return;
                }
                PlayerState p = positions.get(movementTick);
                fakePlayer.setYRot(p.yaw);
                fakePlayer.setXRot(p.pitch);
                fakePlayer.setYHeadRot(p.yaw);

                fakePlayer.setPos(p.x, p.y, p.z);
                fakePlayer.setYRot(p.yaw);
                fakePlayer.setXRot(p.pitch);
                fakePlayer.setYHeadRot(p.yaw);
            } else movementTick = 0;

            if (autoTotem.getValue() && fakePlayer.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING)
                fakePlayer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));

            if (fakePlayer.isDeadOrDying()) {
                deathTime++;
                if (deathTime > 10) disable();
            }
        }
    }

    @EventHandler
    public void onAttack(EventAttack e) {
        if (fakePlayer != null && e.getEntity() == fakePlayer && fakePlayer.hurtTime == 0 && !e.isPre()) {
            mc.level.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1f, 1f);

            if (mc.player.fallDistance > 0 || ModuleManager.criticals.isEnabled())
                mc.level.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1f);
            fakePlayer.handleDamageEvent(mc.level.damageSources().generic());
            if (ModuleManager.aura.getAttackCooldown() >= 0.85)
                fakePlayer.setHealth(fakePlayer.getHealth() + fakePlayer.getAbsorptionAmount() - InventoryUtility.getHitDamage(mc.player.getMainHandItem(), fakePlayer));
            else fakePlayer.setHealth(fakePlayer.getHealth() + fakePlayer.getAbsorptionAmount() - 1f);
            if (fakePlayer.isDeadOrDying()) {
                if (fakePlayer.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                    fakePlayer.getOffhandItem().shrink(1);
                    fakePlayer.setHealth(10f);
                    new ClientboundEntityEventPacket(fakePlayer, EntityEvent.PROTECTED_FROM_DEATH).handle(mc.player.connection);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        if (fakePlayer == null) return;
        fakePlayer.discard();
        fakePlayer.setRemoved(Entity.RemovalReason.KILLED);
        fakePlayer.onClientRemoval();
        fakePlayer = null;
        positions.clear();
        deathTime = 0;
    }

    private record PlayerState(double x, double y, double z, float yaw, float pitch) {
    }
}
