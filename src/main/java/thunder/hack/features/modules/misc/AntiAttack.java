package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import static thunder.hack.features.modules.combat.Criticals.getEntity;

public final class AntiAttack extends Module {
    private final Setting<Boolean> friend = new Setting<>("Friend", true);
    private final Setting<Boolean> zoglin = new Setting<>("Zoglin", true);
    private final Setting<Boolean> villager = new Setting<>("Villager", false);
    private final Setting<Boolean> oneHp = new Setting<>("OneHp", false);
    private final Setting<Float> hp = new Setting<>("Hp", 1f, 0f, 20f, v -> oneHp.getValue());

    public AntiAttack() {
        super("AntiAttack", Category.PLAYER);
    }

    @EventHandler
    @SuppressWarnings("unused")
    private void onPacketSend(PacketEvent.@NotNull Send e) {
        if (e.getPacket() instanceof ServerboundInteractPacket pac) {
            Entity entity = getEntity(pac);
            if (entity == null) return;
            if (Managers.FRIEND.isFriend(entity.getName().getString()) && friend.getValue())
                e.cancel();
            if (entity instanceof ZombifiedPiglin && zoglin.getValue())
                e.cancel();
            if (entity instanceof Villager && villager.getValue()) {
                e.cancel();
            } else if (oneHp.getValue() && entity instanceof LivingEntity lent) {
                if (lent.getHealth() <= hp.getValue()) {
                    e.cancel();
                }
            }
        }
    }
}
