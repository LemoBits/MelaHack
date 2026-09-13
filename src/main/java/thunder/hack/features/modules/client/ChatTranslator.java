package thunder.hack.features.modules.client;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.world.item.Items;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

public class ChatTranslator extends Module {
    public ChatTranslator() {
        super("ChatTranslator", Category.CLIENT);
    }

    private final Setting<Lang> urLang = new Setting<>("YourLanguage", Lang.ru);
    private final Setting<Lang> outMessages = new Setting<>("OutMessages", Lang.uk);

    private List<String> exceptions = Arrays.asList("was killed by", "was destroyed by", "has joined a game", "has left the game");
    private String skip;

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (fullNullCheck()) return;

        if (e.getPacket() instanceof ClientboundSystemChatPacket mPacket) {
            String message = mPacket.content().getString();

            for (String s : exceptions)
                if (message.contains(s))
                    return;

            Managers.ASYNC.run(() -> {
                try {
                    sendMessage(ChatFormatting.WHITE + translate(message, urLang.getValue().name()));
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            });
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (fullNullCheck()) return;
        if (e.getPacket() instanceof ServerboundChatPacket pac) {
            if (Objects.equals(pac.message(), skip))
                return;
            if (mc.player.getMainHandItem().getItem() == Items.FILLED_MAP || mc.player.getOffhandItem().getItem() == Items.FILLED_MAP)
                return;
            if (pac.message().startsWith("/") || pac.message().startsWith(Managers.COMMAND.getPrefix()))
                return;

            Managers.ASYNC.run(() -> {
                try {
                    String outMessage = translate(pac.message(), outMessages.getValue().name());

                    if (Objects.equals(pac.message(), outMessage)) {
                        skip = pac.message();
                        mc.player.connection.sendChat(pac.message());
                    } else {
                        skip = outMessage;
                        mc.player.connection.sendChat(outMessage);
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            });
            e.cancel();
        }
    }

    public String translate(String text, String to) throws UnsupportedEncodingException, MalformedURLException {
        StringBuilder response = new StringBuilder();

        URL url = URI.create(String.format("https://translate.google.com/m?hl=en&sl=auto&tl=%s&ie=UTF-8&prev=_m&q=%s", to, URLEncoder.encode(text.trim(), StandardCharsets.UTF_8))).toURL();
        try {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null)
                    response.append(line + "\n");
            }
        } catch (IOException ignored) {
        }

        Matcher matcher = Pattern.compile("class=\"result-container\">([^<]*)<\\/div>", Pattern.MULTILINE).matcher(response);
        matcher.find();
        String match = matcher.group(1);
        if (match == null || match.isEmpty())
            return "translation failed";
        return unescapeHtml4(match);
    }

    public enum Lang {
        am, ar, eu, bn, bg, ca, chr, hr, cs, da, nl, en, et, fil, fi, fr, de, el, gu, iw, hi, hu, is, id, it, ja, kn, ko, lv, lt, ms, ml, mr, no, pl, ro, ru, sr, sk, sl, es, sw, sv, ta, te, th, tr, ur, uk, vi, cy, cn
    }
}
