package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import thunder.hack.events.impl.EventEntitySpawn;
import thunder.hack.features.modules.Module;
import thunder.hack.utility.player.MovementUtility;

import java.util.Comparator;

public class PearlBait extends Module {
    public PearlBait() {
        super("PearlBait", Category.PLAYER);
    }

    @EventHandler
    public void onEntitySpawn(EventEntitySpawn e) {
        if (e.getEntity() instanceof ThrownEnderpearl)
            mc.level.players().stream()
                    .min(Comparator.comparingDouble((p) -> p.distanceToSqr(e.getEntity().position())))
                    .ifPresent((player) -> {
                        if (player.equals(mc.player) && mc.player.onGround()) {
                            mc.player.setDeltaMovement(0, 0, 0);
                            MovementUtility.clearMovementInput();
                            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ(), false, mc.player.horizontalCollision));
                        }
                    });
    }
}
