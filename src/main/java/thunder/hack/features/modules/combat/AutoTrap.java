package thunder.hack.features.modules.combat;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.player.CombatManager;
import thunder.hack.features.modules.base.TrapModule;
import thunder.hack.setting.Setting;

public final class AutoTrap extends TrapModule {
    private final Setting<CombatManager.TargetBy> targetBy = new Setting<>("Target By", CombatManager.TargetBy.Distance);
    private final Setting<Boolean> targetMovingPlayers = new Setting<>("MovingPlayers", false);

    public AutoTrap() {
        super("AutoTrap", Category.COMBAT);
    }

    @Override
    protected boolean needNewTarget() {
        return target == null
                || target.distanceTo(mc.player) > range.getValue()
                || target.getHealth() + target.getAbsorptionAmount() <= 0
                || target.isDeadOrDying();
    }

    @Override
    protected @Nullable Player getTarget() {
        return Managers.COMBAT.getTarget(range.getValue(), targetBy.getValue(), p -> p.getDeltaMovement().lengthSqr() < 0.08 || targetMovingPlayers.getValue());
    }
}