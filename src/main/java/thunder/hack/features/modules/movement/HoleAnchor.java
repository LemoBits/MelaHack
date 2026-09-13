package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import static thunder.hack.utility.world.HoleUtility.validIndestructible;
import static thunder.hack.utility.world.HoleUtility.validTwoBlockIndestructible;

public class HoleAnchor extends Module {
    public HoleAnchor() {
        super("HoleAnchor", Category.MOVEMENT);
    }

    private final Setting<Integer> pitch = new Setting<>("Pitch", 60, 0, 90);
    private final Setting<Boolean> pull = new Setting<>("Pull", true);

    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent e) {
        if (mc.player.getXRot() > pitch.getValue()) {
            if (
                    validIndestructible(BlockPos.containing(mc.player.position()).below(1))
                            || validIndestructible(BlockPos.containing(mc.player.position()).below(2))
                            || validIndestructible(BlockPos.containing(mc.player.position()).below(3))
                            || validTwoBlockIndestructible(BlockPos.containing(mc.player.position()).below(1))
                            || validTwoBlockIndestructible(BlockPos.containing(mc.player.position()).below(2))
                            || validTwoBlockIndestructible(BlockPos.containing(mc.player.position()).below(3))
            ) {
                if (!pull.getValue()) {
                    mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y(), 0);
                } else {
                    Vec3 center = new Vec3(Math.floor(mc.player.getX()) + 0.5, Math.floor(mc.player.getY()), Math.floor(mc.player.getZ()) + 0.5);

                    if (Math.abs(center.x - mc.player.getX()) > 0.1 || Math.abs(center.z - mc.player.getZ()) > 0.1) {
                        double d3 = center.x - mc.player.getX();
                        double d4 = center.z - mc.player.getZ();
                        mc.player.setDeltaMovement(Math.min(d3 / 2.0, 0.2), mc.player.getDeltaMovement().y(), Math.min(d4 / 2.0, 0.2));
                    }
                }
            }
        }
    }
}
