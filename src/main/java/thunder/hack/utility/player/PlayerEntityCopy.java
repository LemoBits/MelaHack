package thunder.hack.utility.player;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.player.RemotePlayer;

import static thunder.hack.features.modules.Module.mc;

public class PlayerEntityCopy extends RemotePlayer {
    public PlayerEntityCopy() {
        super(Objects.requireNonNull(mc.level), Objects.requireNonNull(mc.player).getGameProfile());

        restoreFrom(mc.player);
        getPlayerInfo();
        entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, mc.player.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
        setUUID(UUID.randomUUID());
    }

    public void spawn() {
        if (mc.level == null) return;

        unsetRemoved();
        mc.level.addEntity(this);
    }

    public void deSpawn() {
        if (mc.level == null) return;

        mc.level.removeEntity(this.getId(), RemovalReason.DISCARDED);
    }
}
