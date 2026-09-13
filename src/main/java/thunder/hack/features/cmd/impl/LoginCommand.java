package thunder.hack.features.cmd.impl;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.jetbrains.annotations.NotNull;
import thunder.hack.features.cmd.Command;
import thunder.hack.injection.accesors.IMinecraftClient;

import java.util.Optional;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.UUIDUtil;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class LoginCommand extends Command {
    public LoginCommand() {
        super("login");
    }

    @Override
    public void executeBuild(@NotNull LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(arg("name", StringArgumentType.word()).executes(context -> {
            login(context.getArgument("name", String.class));
            sendMessage((isRu() ? "Аккаунт изменен на: " : "Switched account to: ") + mc.getUser().getName());

            return SINGLE_SUCCESS;
        }));

        builder.executes(context -> {
            sendMessage(isRu() ? "Использование: .login <nickname>" : "Usage: .login <nickname>");

            return SINGLE_SUCCESS;
        });
    }

    public void login(String name) {
        try {
            setSession(new User(name, UUIDUtil.createOfflinePlayerUUID(name), "", Optional.empty(), Optional.empty(), User.Type.MOJANG));
        } catch (Exception exception) {
            sendMessage((isRu() ? "Неверное имя! " : "Incorrect username! ") + exception);
        }
    }

    public void setSession(User session) {
        IMinecraftClient mca = (IMinecraftClient) mc;
        mca.setSessionT(session);
        mc.getGameProfile().getProperties().clear();
        UserApiService apiService;
        apiService = UserApiService.OFFLINE;
        mca.setUserApiService(apiService);
        mca.setSocialInteractionsManagerT(new PlayerSocialManager(mc, apiService));
        mca.setProfileKeys(ProfileKeyPairManager.create(apiService, session, mc.gameDirectory.toPath()));
        mca.setAbuseReportContextT(ReportingContext.create(ReportEnvironment.local(), apiService));
    }
}
