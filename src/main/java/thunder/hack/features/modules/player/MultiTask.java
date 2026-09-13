package thunder.hack.features.modules.player;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import thunder.hack.features.modules.Module;

public class MultiTask extends Module {
    public MultiTask() {
        super("MultiTask", Category.PLAYER);
    }

    @Override
    public void onUpdate() {
        if (mc.hitResult instanceof BlockHitResult crossHair && crossHair.getBlockPos() != null && mc.options.keyAttack.isDown() && !mc.level.getBlockState(crossHair.getBlockPos()).isAir()) {
            mc.gameMode.startDestroyBlock(crossHair.getBlockPos(), crossHair.getDirection());
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() != null && mc.options.keyAttack.isDown() && mc.player.getAttackStrengthScale(0.5f) > 0.9f) {
            mc.gameMode.attack(mc.player, ehr.getEntity());
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
