package thunder.hack.utility;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.client.Capes;
import thunder.hack.utility.ThunderUtility;

import javax.net.ssl.HttpsURLConnection;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CapeHandler {
    /**
     * author: @dragonostic
     * of-capes
     */

    public interface ReturnCapeTexture {
        void response(Identifier id);
    }

    public static void loadPlayerCape(GameProfile player, ReturnCapeTexture response) {
        try {
            String uuid = player.id().toString();
            DynamicTexture optifineCape = getCapeFromURL(String.format("http://s.optifine.net/capes/%s.png", player.name()));
            DynamicTexture minecraftcapesCape = getCapeFromURL(String.format("https://api.minecraftcapes.net/profile/%s/cape/map", player.id().toString().replace("-","")));
            DynamicTexture minecraftcapesCapeCrack = getCapeFromURL(String.format("https://api.minecraftcapes.net/profile/%s/cape/map", getUUID(player)));
            switch (ModuleManager.capes.priority.getValue()) {
                case Capes.capePriority.Optifine:
                    if (optifineCape != null && ModuleManager.capes.optifineCapes.getValue()) {
                        Identifier capeTexture = ThunderUtility.registerDynamicTexture("th-cape-" + uuid, optifineCape);
                        if (capeTexture != null) response.response(capeTexture);
                    } else if (ModuleManager.capes.minecraftcapesCapes.getValue() && minecraftcapesCape != null) {
                        Identifier capeTexture = ThunderUtility.registerDynamicTexture("th-cape-" + uuid, minecraftcapesCape);
                        if (capeTexture != null) response.response(capeTexture);
                    } else if(ModuleManager.capes.minecraftcapesCapes.getValue()) {
                        Identifier capeTexture = ThunderUtility.registerDynamicTexture("th-cape-" + uuid, minecraftcapesCapeCrack);
                        if (capeTexture != null) response.response(capeTexture);
                    }
                    break;
                case Capes.capePriority.Minecraftcapes:
                    if (minecraftcapesCape != null && ModuleManager.capes.minecraftcapesCapes.getValue()) {
                        Identifier capeTexture = ThunderUtility.registerDynamicTexture("th-cape-" + uuid, minecraftcapesCape);
                        if (capeTexture != null) response.response(capeTexture);
                    } else if (minecraftcapesCapeCrack != null && ModuleManager.capes.minecraftcapesCapes.getValue()) {
                        Identifier capeTexture = ThunderUtility.registerDynamicTexture("th-cape-" + uuid, minecraftcapesCapeCrack);
                        if (capeTexture != null) response.response(capeTexture);
                    } else if (ModuleManager.capes.optifineCapes.getValue()) {
                        Identifier capeTexture = ThunderUtility.registerDynamicTexture("th-cape-" + uuid, optifineCape);
                        if (capeTexture != null) response.response(capeTexture);
                    }
                    break;
            }
        } catch (Exception ignored) {
        }
    }

    public static String getUUID(GameProfile player) {
        StringBuffer content = null;
        try {
            URL request = new URL(String.format("https://api.mojang.com/users/profiles/minecraft/%s", player.name()));
            HttpsURLConnection connection = (HttpsURLConnection) request.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            String inputLine;
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            content = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            connection.disconnect();
        } catch (Exception ignored) {
        }
        // ЭТО САМЫЙ СЕКСУАЛЬНЫЙ JSON ПАРСЕР В ИСТОРИИ ЧЕЛОВЕЧЕСТВА
        // надо потом переписать
        Pattern uuidPattern = Pattern.compile("id");
        Matcher uuidMatch = uuidPattern.matcher(content.toString());
        if (!uuidMatch.find())
            return null;
        String[] parsin = content.toString().split("\"");
        return parsin[3];
    }

    public static DynamicTexture getCapeFromURL(String capeStringURL) {
        try {
            URL capeURL = new URL(capeStringURL);
            return getCapeFromStream(capeURL.openStream());
        } catch (IOException e) {
            return null;
        }
    }

    public static DynamicTexture getCapeFromStream(InputStream image) {
        NativeImage cape = null;
        try {
            cape = NativeImage.read(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (cape != null) {
            return new DynamicTexture(() -> "cape_texture", parseCape(cape));
        }
        return null;
    }

    public static NativeImage parseCape(NativeImage image) {
        int imageWidth = 64;
        int imageHeight = 32;
        int imageSrcWidth = image.getWidth();
        int srcHeight = image.getHeight();
        for (int imageSrcHeight = image.getHeight(); imageWidth < imageSrcWidth || imageHeight < imageSrcHeight; imageHeight *= 2) {
            imageWidth *= 2;
        }

        NativeImage imgNew = new NativeImage(imageWidth, imageHeight, true);
        for (int x = 0; x < imageSrcWidth; x++) {
            for (int y = 0; y < srcHeight; y++) {
                imgNew.setPixel(x, y, image.getPixel(x, y));
            }
        }
        image.close();
        return imgNew;
    }
}
