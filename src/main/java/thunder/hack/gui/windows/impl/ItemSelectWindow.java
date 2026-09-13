package thunder.hack.gui.windows.impl;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import com.google.common.collect.Lists;
import thunder.hack.utility.render.compat.RenderSystem;
import net.minecraft.ChatFormatting;
import thunder.hack.utility.render.ShaderProgramKeys;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import thunder.hack.utility.render.BufferRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.lwjgl.glfw.GLFW;
import thunder.hack.core.Managers;
import thunder.hack.gui.clickui.ClickGUI;
import thunder.hack.gui.clickui.impl.SliderElement;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.gui.windows.WindowBase;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ItemSelectSetting;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;

import static thunder.hack.features.modules.Module.mc;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class ItemSelectWindow extends WindowBase {
    private Setting<ItemSelectSetting> itemSetting;
    private ArrayList<ItemPlate> itemPlates = new ArrayList<>();
    private ArrayList<ItemPlate> allItems = new ArrayList<>();

    private boolean allTab = true, listening = false;
    private String search = "Search";

    public ItemSelectWindow(Setting<ItemSelectSetting> itemSetting) {
        this(mc.getWindow().getGuiScaledWidth() / 2f - 100, mc.getWindow().getGuiScaledHeight() / 2f - 150, 200, 300, itemSetting);
    }

    public ItemSelectWindow(float x, float y, float width, float height, Setting<ItemSelectSetting> itemSetting) {
        super(x, y, width, height, "Items / " + ChatFormatting.GRAY + itemSetting.getModule().getName(), null, null);
        this.itemSetting = itemSetting;
        refreshItemPlates();

        int id1 = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            allItems.add(new ItemPlate(id1, id1 * 20, block.asItem(), block.getDescriptionId()));
            id1++;
        }

        for (Item item : BuiltInRegistries.ITEM) {
            allItems.add(new ItemPlate(id1, id1 * 20, item, item.getDescriptionId()));
            id1++;
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.render(context, mouseX, mouseY);
        boolean hover1 = Render2DEngine.isHovered(mouseX, mouseY, getX() + getWidth() - 90, getY() + 3, 70, 10);

        Render2DEngine.drawRect(context.pose(), getX() + getWidth() - 90, getY() + 3, 70, 10, hover1 ? new Color(0xC5838383, true) : new Color(0xC5575757, true));
        FontRenderers.sf_medium_mini.drawString(context.pose(), search, getX() + getWidth() - 86, getY() + 7, new Color(0xD5D5D5).getRGB());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        int tabColor1 = allTab ? new Color(0xD5D5D5).getRGB() : Color.GRAY.getRGB();
        int tabColor2 = allTab ? Color.GRAY.getRGB() : new Color(0xBDBDBD).getRGB();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.addVertex(getX() + 1.5f, getY() + 29, 0f).setColor(Color.DARK_GRAY.getRGB());
        bufferBuilder.addVertex(getX() + 8, getY() + 29, 0f).setColor(tabColor1);
        bufferBuilder.addVertex(getX() + 8, getY() + 19, 0f).setColor(tabColor1);
        bufferBuilder.addVertex(getX() + 48, getY() + 19, 0f).setColor(tabColor1);
        bufferBuilder.addVertex(getX() + 54, getY() + 29, 0f).setColor(tabColor1);
        bufferBuilder.addVertex(getX() + 52, getY() + 25, 0f).setColor(tabColor2);
        bufferBuilder.addVertex(getX() + 52, getY() + 19, 0f).setColor(tabColor2);
        bufferBuilder.addVertex(getX() + 92, getY() + 19, 0f).setColor(tabColor2);
        bufferBuilder.addVertex(getX() + 100, getY() + 29, 0f).setColor(Color.GRAY.getRGB());
        bufferBuilder.addVertex(getX() + getWidth() - 1, getY() + 29, 0f).setColor(Color.DARK_GRAY.getRGB());
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.buildOrThrow());

        FontRenderers.sf_medium_mini.drawString(context.pose(), "All", getX() + 25, getY() + 25, tabColor1);
        FontRenderers.sf_medium_mini.drawString(context.pose(), "Selected", getX() + 60, getY() + 25, tabColor2);

        if (!allTab && itemPlates.isEmpty()) {
            FontRenderers.sf_medium.drawCenteredString(context.pose(), isRu() ? "Тут пока пусто" : "It's empty here yet",
                    getX() + getWidth() / 2f, getY() + getHeight() / 2f, new Color(0xBDBDBD).getRGB());
        }

        Render2DEngine.addWindow(context.pose(), getX(), getY() + 30, getX() + getWidth(), getY() + getHeight() - 1, 1f);

        for (ItemPlate itemPlate : (allTab ? allItems : itemPlates)) {
            if (itemPlate.offset + getY() + 25 + getScrollOffset() > getY() + getHeight() || itemPlate.offset + getScrollOffset() + getY() + 10 < getY())
                continue;

            context.pose().pushMatrix();
            context.pose().translate((float) (getX() + 6), (float) (itemPlate.offset + getY() + 32 + getScrollOffset()));
            context.item(itemPlate.item().getDefaultInstance(), 0, 0);
            context.pose().popMatrix();

            FontRenderers.sf_medium.drawString(context.pose(), I18n.get(itemPlate.key()), getX() + 26, itemPlate.offset + getY() + 38 + getScrollOffset(), new Color(0xBDBDBD).getRGB());

            boolean hover2 = Render2DEngine.isHovered(mouseX, mouseY, getX() + getWidth() - 20, itemPlate.offset + getY() + 35 + getScrollOffset(), 11, 11);

            Render2DEngine.drawRect(context.pose(), getX() + getWidth() - 20, itemPlate.offset + getY() + 35 + getScrollOffset(), 11, 11,
                    hover2 ? new Color(0xC57A7A7A, true) : new Color(0xC5575757, true));

            boolean selected = itemPlates.stream().anyMatch(sI -> Objects.equals(sI.key, itemPlate.key));

            if (allTab && !selected) {
                FontRenderers.categories.drawString(context.pose(), "+", getX() + getWidth() - 17, itemPlate.offset + getY() + 39 + getScrollOffset(), -1);
            } else {
                FontRenderers.icons.drawString(context.pose(), "w", getX() + getWidth() - 19.5f, itemPlate.offset + getY() + 39 + getScrollOffset(), -1);
            }
        }
        setMaxElementsHeight((allTab ? allItems : itemPlates).size() * 20);
        Render2DEngine.popWindow();
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);

        if (Render2DEngine.isHovered(mouseX, mouseY, getX() + 8, getY() + 19, 52, 19)) {
            allTab = true;
            resetScroll();
            Managers.SOUND.playBoolean();
        }

        if (Render2DEngine.isHovered(mouseX, mouseY, getX() + 54, getY() + 19, 70, 19)) {
            allTab = false;
            resetScroll();
            Managers.SOUND.playBoolean();
        }

        if (Render2DEngine.isHovered(mouseX, mouseY, getX() + getWidth() - 90, getY() + 3, 70, 10)) {
            listening = true;
            search = "";
        }

        if (Render2DEngine.isHovered(mouseX, mouseY, getX() + getWidth() - 15, getY() + 3, 10, 10))
            mc.setScreen(ClickGUI.getClickGui());

        ArrayList<ItemPlate> copy = Lists.newArrayList(allTab ? allItems : itemPlates);
        for (ItemPlate itemPlate : copy) {
            if ((int) (itemPlate.offset + getY() + 50) + getScrollOffset() > getY() + getHeight())
                continue;

            String name = itemPlate.key().replace("item.minecraft.", "").replace("block.minecraft.", "");

            if (Render2DEngine.isHovered(mouseX, mouseY, getX() + getWidth() - 20, itemPlate.offset + getY() + 35 + getScrollOffset(), 10, 10)) {
                boolean selected = itemPlates.stream().anyMatch(sI -> Objects.equals(sI.key(), itemPlate.key));

                if (allTab && !selected) {
                    if (itemSetting.getValue().getItemsById().contains(name))
                        continue;
                    itemSetting.getValue().getItemsById().add(name);
                    refreshItemPlates();
                } else {
                    itemSetting.getValue().getItemsById().remove(name);
                    refreshItemPlates();
                }
                Managers.SOUND.playScroll();
            }
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F && (InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL))) {
            listening = !listening;
            return;
        }

        if (listening) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_ESCAPE -> {
                    listening = false;
                    search = "Search";
                    refreshAllItems();
                }

                case GLFW.GLFW_KEY_BACKSPACE -> {
                    search = SliderElement.removeLastChar(search);
                    refreshAllItems();

                    if (Objects.equals(search, "")) {
                        listening = false;
                        search = "Search";
                    }
                }

                case GLFW.GLFW_KEY_SPACE -> search = search + " ";
            }
        }
    }

    @Override
    public void charTyped(char key, int keyCode) {
        if (StringUtil.isAllowedChatCharacter(key) && listening) {
            search = search + key;
            refreshAllItems();
        }
    }


    private void refreshItemPlates() {
        itemPlates.clear();

        int id = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (itemSetting.getValue().getItemsById().contains(block.getDescriptionId().replace("block.minecraft.", ""))) {
                itemPlates.add(new ItemPlate(id, id * 20, block.asItem(), block.getDescriptionId()));
                id++;
            }
        }

        for (Item item : BuiltInRegistries.ITEM)
            if (itemSetting.getValue().getItemsById().contains(item.getDescriptionId().replace("item.minecraft.", ""))) {
                itemPlates.add(new ItemPlate(id, id * 20, item, item.getDescriptionId()));
                id++;
            }
    }

    private void refreshAllItems() {
        allItems.clear();
        resetScroll();
        int id1 = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (search.equals("Search") || search.isEmpty() || block.getDescriptionId().contains(search) || I18n.get(block.getDescriptionId()).toLowerCase().contains(search.toLowerCase())) {
                allItems.add(new ItemPlate(id1, id1 * 20, block.asItem(), block.getDescriptionId()));
                id1++;
            }
        }

        for (Item item : BuiltInRegistries.ITEM) {
            if (search.equals("Search") || search.isEmpty() || item.getDescriptionId().contains(search) || item.getName(item.getDefaultInstance()).getString().toLowerCase().contains(search.toLowerCase())) {
                allItems.add(new ItemPlate(id1, id1 * 20, item, item.getDescriptionId()));
                id1++;
            }
        }
    }

    private record ItemPlate(float id, float offset, Item item, String key) {
    }
}
