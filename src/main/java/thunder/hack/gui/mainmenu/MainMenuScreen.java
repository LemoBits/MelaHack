package thunder.hack.gui.mainmenu;

import org.jetbrains.annotations.NotNull;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import static thunder.hack.features.modules.Module.mc;

public class MainMenuScreen extends Screen {
    private final List<MainMenuButton> buttons = new ArrayList<>();
    public boolean confirm = false;
    public static int ticksActive;

    protected MainMenuScreen() {
        super(Component.nullToEmpty("THMainMenuScreen"));
        INSTANCE = this;

        buttons.add(new MainMenuButton(-110, -70, I18n.get("menu.singleplayer").toUpperCase(Locale.ROOT), () -> mc.setScreen(new SelectWorldScreen(this))));
        buttons.add(new MainMenuButton(4, -70, I18n.get("menu.multiplayer").toUpperCase(Locale.ROOT), () -> mc.setScreen(new JoinMultiplayerScreen(this))));
        buttons.add(new MainMenuButton(-110, -29, I18n.get("menu.options")
                .toUpperCase(Locale.ROOT)
                .replace(".", ""), () -> mc.setScreen(new OptionsScreen(this, mc.options))));
        buttons.add(new MainMenuButton(4, -29, "CLICKGUI", () -> ModuleManager.clickGui.setGui()));
        buttons.add(new MainMenuButton(-110, 12, I18n.get("menu.quit").toUpperCase(Locale.ROOT), mc::stop, true));
    }

    private static MainMenuScreen INSTANCE = new MainMenuScreen();

    public static MainMenuScreen getInstance() {
        ticksActive = 0;

        if (INSTANCE == null) {
            INSTANCE = new MainMenuScreen();
        }
        return INSTANCE;
    }

    @Override
    public void tick() {
        ticksActive++;

        if (ticksActive > 400) {
            ticksActive = 0;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        float halfOfWidth = mc.getWindow().getGuiScaledWidth() / 2f;
        float halfOfHeight = mc.getWindow().getGuiScaledHeight() / 2f;

        float mainX = halfOfWidth - 120f;
        float mainY = halfOfHeight - 80f;
        float mainWidth = 240f;
        float mainHeight = 140;

        renderCustomBackground(context, halfOfWidth * 2f, halfOfHeight * 2);

        Render2DEngine.drawHudBase(context.pose(), mainX, mainY, mainWidth, mainHeight, 20);

        buttons.forEach(b -> b.onRender(context, mouseX, mouseY));

        boolean hoveredLogo = Render2DEngine.isHovered(mouseX, mouseY, (int) (halfOfWidth - 120), (int) (halfOfHeight - 130), 210, 50);

        FontRenderers.thglitchBig.drawCenteredString(context.pose(), "MELAHACK", (int) (halfOfWidth), (int) (halfOfHeight - 120), new Color(255, 255, 255, hoveredLogo ? 230 : 180).getRGB());

        boolean hovered = Render2DEngine.isHovered(mouseX, mouseY, halfOfWidth - 50, halfOfHeight + 70, 100, 10);

        FontRenderers.sf_medium.drawCenteredString(context.pose(), "<-- Back to default menu", halfOfWidth, halfOfHeight + 70, hovered ? -1 : Render2DEngine.applyOpacity(-1, 0.6f));
        //  FontRenderers.sf_medium.drawString(context.getMatrices(), "By Pan4ur & 06ED", halfOfWidth * 2 - FontRenderers.sf_medium.getStringWidth("By Pan4ur & 06ED") - 5f, halfOfHeight * 2 - 10, Render2DEngine.applyOpacity(-1, 0.4f));

        /*onlineText:
        {
            String onlineUsers = String.format("online: %s%s", Formatting.DARK_GREEN, Managers.TELEMETRY.getOnlinePlayers().size());

            FontRenderers.sf_bold.drawCenteredString(context.getMatrices(), onlineUsers, halfOfWidth, halfOfHeight * 2 - 15, Color.GREEN);

            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (halfOfWidth - 10 - FontRenderers.sf_medium.getStringWidth(onlineUsers) / 2f), (float) (halfOfHeight * 2 - 17));
            Render2DEngine.drawBloom(context.getMatrices(), Render2DEngine.applyOpacity(Color.GREEN, 0.6f), 9f);
            context.getMatrices().popMatrix();

            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) (halfOfWidth - 10 - FontRenderers.sf_medium.getStringWidth(onlineUsers) / 2f), (float) (halfOfHeight * 2 - 17));
            Render2DEngine.drawBloom(context.getMatrices(), Render2DEngine.applyOpacity(Color.GREEN, (float) (0.5f + (Math.sin((double) System.currentTimeMillis() / 500)) / 2f)), 9f);
            context.getMatrices().popMatrix();

        }*/

//        // Remove changelog cuz it's ugly
//        int offsetY = 10;
//        for (String change : ThunderUtility.changeLog) {
//            String prefix = getPrefix(change);
//            FontRenderers.sf_medium.drawString(context.getMatrices(), prefix, 10, offsetY, Render2DEngine.applyOpacity(-1, 0.4f));
//            offsetY += 10;
//        }
    }

    private void renderCustomBackground(GuiGraphics context, float width, float height) {
        if (mc.getOverlay() == null) {
            Render2DEngine.drawMainMenuShader(context.pose(), 0, 0, width, height);
        } else {
            context.fill(0, 0, Math.round(width), Math.round(height), 0xFF070015);
        }
    }

    private static @NotNull String getPrefix(@NotNull String change) {
        String prefix = "";
        if (change.contains("[+]")) {
            change = change.replace("[+] ", "");
            prefix = ChatFormatting.GREEN + "[+] " + ChatFormatting.RESET;
        } else if (change.contains("[-]")) {
            change = change.replace("[-] ", "");
            prefix = ChatFormatting.RED + "[-] " + ChatFormatting.RESET;
        } else if (change.contains("[/]")) {
            change = change.replace("[/] ", "");
            prefix = ChatFormatting.LIGHT_PURPLE + "[/] " + ChatFormatting.RESET;
        } else if (change.contains("[*]")) {
            change = change.replace("[*] ", "");
            prefix = ChatFormatting.GOLD + "[*] " + ChatFormatting.RESET;
        }
        return prefix + change;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float halfOfWidth = mc.getWindow().getGuiScaledWidth() / 2f;
        float halfOfHeight = mc.getWindow().getGuiScaledHeight() / 2f;
        buttons.forEach(b -> b.onClick((int) mouseX, (int) mouseY));

        if (Render2DEngine.isHovered(mouseX, mouseY, halfOfWidth - 50, halfOfHeight + 70, 100, 10)) {
            confirm = true;
            mc.setScreen(new TitleScreen());
            confirm = false;
        }

        if (Render2DEngine.isHovered(mouseX, mouseY, (int) (halfOfWidth - 157), (int) (halfOfHeight - 140), 300, 70))
            Util.getPlatform().openUri(URI.create("https://github.com/HundSimon/MelaHack/"));

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
