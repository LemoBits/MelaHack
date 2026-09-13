package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.notification.Notification;
import thunder.hack.setting.Setting;
import org.apache.commons.lang3.RandomStringUtils;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public final class AutoAuth extends Module {
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Custom);
    private final Setting<String> cpass = new Setting<>("Password", "babidjon777", v -> mode.getValue() == Mode.Custom);
    private final Setting<Boolean> show = new Setting<>("ShowPassword", true);

    public AutoAuth() {
        super("AutoAuth", Category.MISC);
    }

    private enum Mode {
        Custom, Random, Qwerty
    }

    @Override
    public void onEnable() {
        String warningMsg = isRu() ?
                ChatFormatting.RED + "Внимание! " + ChatFormatting.RESET + "Пароль сохраняется в конфиге, перед передачей конфига " + ChatFormatting.RED + " ВЫКЛЮЧИ МОДУЛЬ!" :
                ChatFormatting.RED + "Attention! " + ChatFormatting.RESET + "The passwords are stored in the config, so before sharing your configs " + ChatFormatting.RED + " TOGGLE OFF THE MODULE!";
        sendMessage(warningMsg);
    }

    @Override
    public void onDisable() {
        sendMessage("Resetting password...");
        cpass.setValue("none");
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.@NotNull Receive event) {
        if (event.getPacket() instanceof ClientboundSystemChatPacket pac && mc.getConnection() != null) {
            String password = "";
            switch (mode.getValue()) {
                case Custom -> {
                    password = cpass.getValue();
                    if (password.isEmpty()) {
                        sendMessage(ChatFormatting.RED + (isRu() ? "Ошибка регистрации: Пароль пуст!" : "Registration error: Password is empty!"));
                        return;
                    }
                }
                case Qwerty -> password = "qwerty123";
                case Random -> password = RandomStringUtils.randomAlphabetic(5) + RandomStringUtils.randomPrint(5);
            }

            String m = pac.content().getString().toLowerCase();

            if (m.contains("/reg") || m.contains("/register") || m.contains("зарегистрируйтесь")) {
                mc.getConnection().sendCommand("reg " + password + " " + password);
                if (show.getValue()) sendMessage((isRu() ? "Твой пароль: " : "Your password: ") + ChatFormatting.RED + password);
                Managers.NOTIFICATION.publicity("AutoAuth", isRu() ? "Выполнена регистрация!" : "Registration completed!", 4, Notification.Type.SUCCESS);
            } else if (m.contains("авторизуйтесь") || m.contains("/l")) {
                mc.getConnection().sendCommand("login " + password);
                Managers.NOTIFICATION.publicity("AutoAuth", isRu() ? "Выполнен вход!" : "Logged in!", 4, Notification.Type.SUCCESS);
            }
        }
    }
}
