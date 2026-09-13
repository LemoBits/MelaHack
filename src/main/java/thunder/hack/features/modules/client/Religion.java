package thunder.hack.features.modules.client;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.events.impl.PacketEvent;

import static thunder.hack.features.modules.client.ClientSettings.isRu;
import static thunder.hack.features.modules.combat.Criticals.getEntity;

public class Religion extends Module {
    public Religion() { super("Religion", Category.CLIENT); }

    public final Setting<YourReligion> ReligionSetting = new Setting<>("YourReligion", YourReligion.Christianity, v -> true);

    public enum YourReligion { Christianity, Islam, Satanism, Atheism }
    public int sheepHits = 0;

    @EventHandler
    @SuppressWarnings("unused")
    private void onPacketSend(PacketEvent.@NotNull Send e) {
        if (!(e.getPacket() instanceof ServerboundInteractPacket pac)) return;

        Entity entity = getEntity(pac);
        if (entity == null) return;

        if ((entity instanceof Pig || entity instanceof Zoglin) && ReligionSetting.is(YourReligion.Islam)) e.cancel();

        if (entity instanceof Player) {
            if (ReligionSetting.is(YourReligion.Christianity) && Managers.FRIEND.isFriend(entity.getName().getString())) {
                sendMessage(isRu() ? "Люби ближнего твоего, как самого себя!" : "Love your neighbour as yourself!");
                e.cancel();
            } else if (ReligionSetting.is(YourReligion.Christianity)) {
                sendMessage(isRu() ? "Не убивай!" : "Do not kill!");
                e.cancel();
            }
        }

        if ((entity instanceof Sheep) && ReligionSetting.is(YourReligion.Satanism)) {
            sheepHits++;
            sendMessage(isRu() ? String.format("Сатана хочет больше! Ударов по овцам: %d", sheepHits) : String.format("Satan needs more! Times you hit a sheep: %d", sheepHits));
        }

    }
}
