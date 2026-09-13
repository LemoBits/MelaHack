package thunder.hack.injection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.client.ClientSettings;

import java.util.Comparator;
import java.util.List;
import net.minecraft.Optionull;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

import static thunder.hack.features.modules.Module.mc;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerListHud {
    private static final Comparator<Object> ENTRY_ORDERING = Comparator.comparingInt((entry) -> ((PlayerInfo) entry).getGameMode() == GameType.SPECTATOR ? 1 : 0)
            .thenComparing((entry) -> Optionull.mapOrDefault(((PlayerInfo) entry).getTeam(), PlayerTeam::getName, ""))
            .thenComparing((entry) -> ((PlayerInfo) entry).getProfile().getName(), String::compareToIgnoreCase);

    @Inject(method = "getPlayerInfos", at = @At("HEAD"), cancellable = true)
    private void collectPlayerEntriesHook(CallbackInfoReturnable<List<PlayerInfo>> cir) {
        if (ClientSettings.futureCompatibility.getValue())
            return;

        if (ThunderHack.isFuturePresent())
            return;

        if (ModuleManager.extraTab.isEnabled())
            cir.setReturnValue(mc.player.connection.getListedOnlinePlayers().stream().sorted(ENTRY_ORDERING).limit(1000).toList());
        else
            cir.setReturnValue(mc.player.connection.getListedOnlinePlayers().stream().sorted(ENTRY_ORDERING).limit(80).toList());
    }
}