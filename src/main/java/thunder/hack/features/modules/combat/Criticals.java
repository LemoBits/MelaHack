package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.jetbrains.annotations.NotNull;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.injection.accesors.IPlayerInteractEntityC2SPacket;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.injection.accesors.IEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public final class Criticals extends Module {
    public final Setting<Mode> mode = new Setting<>("Mode", Mode.UpdatedNCP);

    public static boolean cancelCrit;

    public Criticals() {
        super("Criticals", Category.COMBAT);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.@NotNull Send event) {
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket && getInteractType(event.getPacket()) == InteractType.ATTACK) {
            Entity ent = getEntity(event.getPacket());
            if (ent == null || ent instanceof EndCrystalEntity || cancelCrit)
                return;
            doCrit();
        }
    }

    public void doCrit() {
        if (isDisabled() || mc.player == null || mc.world == null)
            return;
        if ((mc.player.isOnGround() || mc.player.getAbilities().flying || mode.is(Mode.Grim)) && !mc.player.isInLava() && !mc.player.isSubmergedInWater()) {
            switch (mode.getValue()) {
                case OldNCP -> {
                    critPacket(0.00001058293536, false);
                    critPacket(0.00000916580235, false);
                    critPacket(0.00000010371854, false);
                }
                case Ncp -> {
                    critPacket(0.0625D, false);
                    critPacket(0., false);
                }
                case UpdatedNCP -> {
                    critPacket(0.000000271875, false);
                    critPacket(0., false);
                }
                case Strict -> {
                    critPacket(0.062600301692775, false);
                    critPacket(0.07260029960661, false);
                    critPacket(0., false);
                    critPacket(0., false);
                }
                case Grim -> {
                    if (!mc.player.isOnGround())
                        critPacket(-0.000001, true);

                }
            }
        }
    }

    private void critPacket(double yDelta, boolean full) {
        if (!full)
            sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + yDelta, mc.player.getZ(), false, mc.player.horizontalCollision));
        else
            sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + yDelta, mc.player.getZ(), ((IEntity) mc.player).getLastYaw(), ((IEntity) mc.player).getLastPitch(), false, mc.player.horizontalCollision));
    }

    public static Entity getEntity(@NotNull PlayerInteractEntityC2SPacket packet) {
        return mc.world.getEntityById(((IPlayerInteractEntityC2SPacket) packet).getEntityId());
    }

    public static InteractType getInteractType(@NotNull PlayerInteractEntityC2SPacket packet) {
        return InteractType.valueOf(((IPlayerInteractEntityC2SPacket) packet).getType().getType().name());
    }

    public enum InteractType {
        INTERACT, ATTACK, INTERACT_AT
    }

    public enum Mode {
        Ncp, Strict, OldNCP, UpdatedNCP, Grim
    }
}
