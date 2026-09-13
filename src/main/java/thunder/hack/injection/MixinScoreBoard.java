package thunder.hack.injection;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Scoreboard.class)
public abstract class MixinScoreBoard {
    @Final
    @Shadow
    private Object2ObjectMap<String, PlayerTeam> teamsByPlayer;

    @Inject(method = "removePlayerFromTeam(Ljava/lang/String;Lnet/minecraft/world/scores/PlayerTeam;)V", at = @At("HEAD"), cancellable = true)
    public void removeScoreHolderFromTeamHook(String scoreHolderName, PlayerTeam team, CallbackInfo ci) {
        ci.cancel();
        if (teamsByPlayer.get(scoreHolderName) != team) {
            //("Player is either on another team or not on any team. Cannot remove from team '" + team.getName() + "'.");
            return;
        }
        teamsByPlayer.remove(scoreHolderName);
        team.getPlayers().remove(scoreHolderName);
    }
}
