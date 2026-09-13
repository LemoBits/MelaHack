package thunder.hack.injection.accesors;

import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface IMinecraftClient {

    @Accessor("rightClickDelay")
    int getUseCooldown();

    @Accessor("rightClickDelay")
    void setUseCooldown(int val);

    @Invoker("startUseItem")
    void idoItemUse();

    @Invoker("startAttack")
    boolean idoAttack();

    @Mutable
    @Accessor("profileKeyPairManager")
    void setProfileKeys(ProfileKeyPairManager keys);

    @Mutable
    @Accessor("user")
    void setSessionT(User session);

    @Mutable
    @Accessor
    void setUserApiService(UserApiService apiService);

    @Mutable
    @Accessor("playerSocialManager")
    void setSocialInteractionsManagerT(PlayerSocialManager socialInteractionsManager);

    @Mutable
    @Accessor("reportingContext")
    void setAbuseReportContextT(ReportingContext abuseReportContext);
}