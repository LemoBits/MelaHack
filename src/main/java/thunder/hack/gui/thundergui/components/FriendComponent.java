package thunder.hack.gui.thundergui.components;

import thunder.hack.core.Core;
import thunder.hack.core.Managers;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.gui.thundergui.ThunderGui;
import thunder.hack.utility.ThunderUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;
import thunder.hack.utility.render.animation.AnimationUtility;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import static thunder.hack.features.modules.Module.mc;

import com.mojang.blaze3d.platform.NativeImage;

public class FriendComponent {
    float scroll_animation = 0f;
    private Identifier head = null;
    private final String name;
    private int posX;
    private int posY;
    private int progress;
    private int fade;
    private final int index;
    private boolean first_open = true;
    private float scrollPosY;
    private float prevPosY;

    public FriendComponent(String name, int posX, int posY, int index) {
        this.name = name;
        this.posX = posX;
        this.posY = posY;
        fade = 0;
        this.index = index * 5;
        loadHead(name);
        scrollPosY = posY;
        scroll_animation = 0f;
    }

    public void loadHead(String name) {
        if (Core.HEADS.containsKey(name)) head = Core.HEADS.get(name);
        net.minecraft.util.Util.backgroundExecutor().execute(() -> {
            try {
                DynamicTexture nIBT = getHeadFromURL("https://minotar.net/helm/" + name + "/22.png");
                head = ThunderUtility.registerDynamicTexture("th-heads-" + name, nIBT);
                if (head != null) Core.HEADS.put(name, head);
            } catch (Exception e) {
                head = null;
            }
        });
    }

    public static DynamicTexture getHeadFromURL(String HeadStringURL) {
        try {
            URL capeURL = URI.create(HeadStringURL).toURL();
            return getHeadFromStream(capeURL.openStream());
        } catch (IOException e) {
            return null;
        }
    }

    public static DynamicTexture getHeadFromStream(InputStream image) {
        NativeImage Head = null;
        try {
            Head = NativeImage.read(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Head != null) {
            DynamicTexture nIBT = new DynamicTexture(() -> "friend_head", parseHead(Head));
            return nIBT;
        }
        return null;
    }

    public static NativeImage parseHead(NativeImage image) {
        int imageWidth = 22;
        int imageHeight = 22;
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


    public void render(GuiGraphics context, int MouseX, int MouseY) {
        if (scrollPosY != posY) {
            scroll_animation = AnimationUtility.fast(scroll_animation, 1, 15f);
            posY = (int) Render2DEngine.interpolate(prevPosY, scrollPosY, scroll_animation);
        }
        if ((posY > ThunderGui.getInstance().main_posY + ThunderGui.getInstance().height) || posY < ThunderGui.getInstance().main_posY) {
            return;
        }
        Render2DEngine.drawRound(context.pose(), posX + 5, posY, 285, 30, 4f, Render2DEngine.applyOpacity(new Color(44, 35, 52, 255), getFadeFactor()));

        if (first_open) {
            Render2DEngine.addWindow(context.pose(), posX + 5, posY, posX + 5 + 285, posY + 30, 1f);
            Render2DEngine.drawBlurredShadow(context.pose(), MouseX - 20, MouseY - 20, 40, 40, 60, Render2DEngine.applyOpacity(new Color(0xC3555A7E, true), getFadeFactor()));
            Render2DEngine.popWindow();
            first_open = false;
        }

        if (isHovered(MouseX, MouseY)) {
            Render2DEngine.addWindow(context.pose(), posX + 5, posY, posX + 5 + 285, posY + 30, 1f);
            Render2DEngine.drawBlurredShadow(context.pose(), MouseX - 20, MouseY - 20, 40, 40, 60, Render2DEngine.applyOpacity(new Color(0xC3555A7E, true), getFadeFactor()));
            Render2DEngine.popWindow();
        }

        Render2DEngine.drawRound(context.pose(), posX + 266, posY + 8, 14, 14, 2f, Render2DEngine.applyOpacity(new Color(25, 20, 30, 255), getFadeFactor()));

        if (Render2DEngine.isHovered(MouseX, MouseY, posX + 268, posY + 10, 10, 10)) {
            Render2DEngine.drawRound(context.pose(), posX + 268, posY + 10, 10, 10, 2f, Render2DEngine.applyOpacity(new Color(65, 1, 13, 255), getFadeFactor()));
        } else {
            Render2DEngine.drawRound(context.pose(), posX + 268, posY + 10, 10, 10, 2f, Render2DEngine.applyOpacity(new Color(94, 1, 18, 255), getFadeFactor()));
        }
        FontRenderers.icons.drawString(context.pose(), "w", posX + 268, posY + 13, Render2DEngine.applyOpacity(-1, getFadeFactor()));


        context.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, Objects.requireNonNullElse(head, TextureStorage.crackedSkin), posX + 10, posY + 3, 0, 0, 22, 22, 22, 22);

        FontRenderers.modules.drawString(context.pose(), name, posX + 37, posY + 6, Render2DEngine.applyOpacity(-1, getFadeFactor()));

        boolean online = mc.player.connection.getOnlinePlayers().stream().map(p -> p.getProfile().name()).toList().contains(name);

        FontRenderers.settings.drawString(context.pose(), online ? "online" : "offline", posX + 37, posY + 17, online ? Render2DEngine.applyOpacity(new Color(0xFF0B7A00, true).getRGB(), getFadeFactor()) : Render2DEngine.applyOpacity(new Color(0xFFBDBDBD, true).getRGB(), getFadeFactor()));
    }

    private float getFadeFactor() {
        return fade / (5f + index);
    }

    public void onTick() {
        if (progress > 4) {
            progress = 0;
        }
        progress++;

        if (fade < 10 + index) {
            fade++;
        }
    }

    private boolean isHovered(int mouseX, int mouseY) {
        return mouseX > posX && mouseX < posX + 295 && mouseY > posY && mouseY < posY + 30;
    }

    public void movePosition(float deltaX, float deltaY) {
        this.posY += deltaY;
        this.posX += deltaX;
        scrollPosY = posY;
    }

    public void mouseClicked(int MouseX, int MouseY, int clickedButton) {
        if ((posY > ThunderGui.getInstance().main_posY + ThunderGui.getInstance().height) || posY < ThunderGui.getInstance().main_posY) {
            return;
        }
        if (Render2DEngine.isHovered(MouseX, MouseY, posX + 268, posY + 10, 10, 10)) {
            Managers.FRIEND.removeFriend(name);
            ThunderGui.getInstance().loadFriends();
        }
    }

    public double getPosX() {
        return this.posX;
    }

    public double getPosY() {
        return this.posY;
    }

    public void scrollElement(float deltaY) {
        scroll_animation = 0;
        prevPosY = posY;
        this.scrollPosY += deltaY;
    }
}
