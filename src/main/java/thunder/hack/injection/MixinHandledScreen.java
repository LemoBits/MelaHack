package thunder.hack.injection;
import thunder.hack.utility.render.compat.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.core.Core;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.misc.PeekScreen;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.render.Tooltips;
import thunder.hack.utility.Timer;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;
import java.util.List;

import java.awt.*;
import java.util.*;

import static thunder.hack.features.modules.Module.mc;
import static thunder.hack.features.modules.render.Tooltips.hasItems;

import com.mojang.blaze3d.platform.InputConstants;
@Mixin(value = {AbstractContainerScreen.class})
public abstract class MixinHandledScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {

    @Unique
    private final Timer delayTimer = new Timer();

    @Unique
    private Runnable postRender;

    protected MixinHandledScreen(Component title) {
        super(title);
    }

    @Shadow
    protected abstract boolean isHovering(Slot slotIn, double mouseX, double mouseY);

    @Shadow
    protected abstract void slotClicked(Slot slotIn, int slotId, int mouseButton, ContainerInput type);

    @Inject(method = "render", at = @At("HEAD"))
    private void drawScreenHook(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        for (int i1 = 0; i1 < mc.player.containerMenu.slots.size(); ++i1) {
            Slot slot = mc.player.containerMenu.slots.get(i1);
            if (isHovering(slot, mouseX, mouseY) && slot.isActive()) {
                if (ModuleManager.itemScroller.isEnabled() && shit() && attack() && delayTimer.passedMs(ModuleManager.itemScroller.delay.getValue())) {
                    this.slotClicked(slot, slot.index, 0, ContainerInput.QUICK_MOVE);
                    delayTimer.reset();
                }
            }
        }
    }

    private boolean shit() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), 340) || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), 344);
    }

    private boolean attack() {
        return Core.hold_mouse0;
    }

    @Shadow
    @Nullable
    protected Slot hoveredSlot;
    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;

    private static final ItemStack[] ITEMS = new ItemStack[27];

    private Map<Render2DEngine.Rectangle, Integer> clickableRects = new HashMap<>();

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        if (hoveredSlot != null && !hoveredSlot.getItem().isEmpty() && minecraft.player.inventoryMenu.getCarried().isEmpty()) {
            if (hasItems(hoveredSlot.getItem()) && Tooltips.storage.getValue()) {
                renderShulkerToolTip(context, mouseX, mouseY, 0, 0, hoveredSlot.getItem());
            } else if (hoveredSlot.getItem().getItem() == Items.FILLED_MAP && Tooltips.maps.getValue()) {
                drawMapPreview(context, hoveredSlot.getItem(), mouseX, mouseY);
            }
        }
        int xOffset = 0;
        int yOffset = 20;
        int stage = 0;

        if (ModuleManager.tooltips.isEnabled() && ModuleManager.tooltips.shulkerRegear.getValue()) {
            clickableRects.clear();
            for (int i1 = 0; i1 < mc.player.containerMenu.slots.size(); ++i1) {
                Slot slot = mc.player.containerMenu.slots.get(i1);
                if (slot.getItem().isEmpty()) continue;

                if (slot.getItem().getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
                    if (!renderShulkerToolTip(context, xOffset, yOffset + 67, mouseX, mouseY, slot.getItem()))
                        continue;
                    clickableRects.put(new Render2DEngine.Rectangle(xOffset, yOffset, xOffset + 176, yOffset + 67), slot.index);
                    yOffset += 67;
                    if (stage == 0) {
                        if (yOffset + 67 >= mc.getWindow().getGuiScaledHeight()) {
                            yOffset = 20;
                            xOffset = mc.getWindow().getGuiScaledWidth() - 176;
                            stage = 1;
                        }
                    } else if (stage == 1) {
                        if (yOffset + 67 >= mc.getWindow().getGuiScaledHeight()) {
                            yOffset = 20;
                            xOffset = 170;
                            stage = 2;
                        }
                    } else {
                        if (yOffset + 67 >= mc.getWindow().getGuiScaledHeight()) {
                            yOffset = 20;
                            xOffset = mc.getWindow().getGuiScaledWidth() - 352;
                            stage = 0;
                        }
                    }
                }
            }

            if (postRender != null) {
                postRender.run();
                postRender = null;
            }
        }
    }

    @Inject(method = "drawSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/inventory/Slot;)V", at = @At("TAIL"))
    protected void drawSlotHook(GuiGraphicsExtractor context, Slot slot, CallbackInfo ci) {
        if (ModuleManager.serverHelper.isEnabled() && ModuleManager.serverHelper.aucHelper.getValue())
            ModuleManager.serverHelper.onRenderChest(context, slot);
    }

    public boolean renderShulkerToolTip(GuiGraphicsExtractor context, int offsetX, int offsetY, int mouseX, int mouseY, ItemStack stack) {
        try {
            ItemContainerContents compoundTag = stack.get(DataComponents.CONTAINER);
            if (compoundTag == null)
                return false;

            float[] colors = new float[]{1F, 1F, 1F};
            Item focusedItem = stack.getItem();
            if (focusedItem instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
                try {
                    Color c = new Color(Objects.requireNonNull(((ShulkerBoxBlock) bi.getBlock()).getColor()).getTextureDiffuseColor());
                    colors = new float[]{c.getRed() / 255f, c.getGreen() / 255f, c.getRed() / 255f, c.getAlpha() / 255f};
                } catch (NullPointerException npe) {
                    colors = new float[]{1F, 1F, 1F};
                }
            }
            draw(context, compoundTag.allItemsCopyStream().toList(), offsetX, offsetY, mouseX, mouseY, colors);
        } catch (Exception ignore) {
            return false;
        }
        return true;
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawMouseoverTooltip(GuiGraphicsExtractor context, int x, int y, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        if (hoveredSlot != null && !hoveredSlot.getItem().isEmpty() && minecraft.player.inventoryMenu.getCarried().isEmpty()) {
            if (hoveredSlot.getItem().getItem() == Items.FILLED_MAP && Tooltips.maps.getValue()) ci.cancel();
        }
    }

    @Unique
    private void draw(GuiGraphicsExtractor context, List<ItemStack> itemStacks, int offsetX, int offsetY, int mouseX, int mouseY, float[] colors) {
        RenderSystem.disableDepthTest();
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        offsetX += 8;
        offsetY -= 82;

        drawBackground(context, offsetX, offsetY, colors);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int row = 0;
        int i = 0;
        for (ItemStack itemStack : itemStacks) {
            context.item(itemStack, offsetX + 8 + i * 18, offsetY + 7 + row * 18);
            context.itemDecorations(mc.font, itemStack, offsetX + 8 + i * 18, offsetY + 7 + row * 18);

            if (mouseX > offsetX + 8 + i * 18 && mouseX < offsetX + 28 + i * 18 && mouseY > offsetY + 7 + row * 18 && mouseY < offsetY + 27 + row * 18)
                postRender = () -> context.setTooltipForNextFrame(font, getTooltipFromItem(mc, itemStack), itemStack.getTooltipImage(), mouseX, mouseY);

            i++;
            if (i >= 9) {
                i = 0;
                row++;
            }
        }
        RenderSystem.enableDepthTest();
    }

    private void drawBackground(GuiGraphicsExtractor context, int x, int y, float[] colors) {
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(colors[0], colors[1], colors[2], 1F);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        context.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TextureStorage.container, x, y, 0, 0, 176, 67, 176, 67);
        RenderSystem.enableBlend();
    }

    private void drawMapPreview(GuiGraphicsExtractor context, ItemStack stack, int x, int y) {
        RenderSystem.enableBlend();
        context.pose().pushMatrix();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int y1 = y - 12;
        int x1 = x + 8;
        int z = 300;

        MapItemSavedData mapState = MapItem.getSavedData(stack, minecraft.level);

        if (mapState != null) {
            mapState.getHoldingPlayer(minecraft.player);

            x1 += 8;
            y1 += 8;
            z = 310;
            double scale = (double) (100 - 16) / 128.0D;
            context.pose().translate((float) (x1), (float) (y1));
            context.pose().scale((float) scale, (float) scale);
            MapRenderState renderState = new MapRenderState();
            minecraft.getMapRenderer().extractRenderState((MapId) stack.get(DataComponents.MAP_ID), mapState, renderState);
            context.map(renderState);
        }
        context.pose().popMatrix();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (Module.fullNullCheck()) return;
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && hoveredSlot != null && !hoveredSlot.getItem().isEmpty() && minecraft.player.inventoryMenu.getCarried().isEmpty()) {
            ItemStack itemStack = hoveredSlot.getItem();

            if (hasItems(itemStack) && Tooltips.middleClickOpen.getValue()) {

                Arrays.fill(ITEMS, ItemStack.EMPTY);
                ItemContainerContents nbt = itemStack.get(DataComponents.CONTAINER);

                if (nbt != null) {
                    List<ItemStack> list = nbt.allItemsCopyStream().toList();
                    for (int i = 0; i < list.size(); i++)
                        ITEMS[i] = list.get(i);
                }

                minecraft.setScreen(new PeekScreen(new ShulkerBoxMenu(0, minecraft.player.getInventory(), new SimpleContainer(ITEMS)), minecraft.player.getInventory(), hoveredSlot.getItem().getHoverName(), ((BlockItem) hoveredSlot.getItem().getItem()).getBlock()));
                cir.setReturnValue(true);
            }
        }
        for (Render2DEngine.Rectangle rect : clickableRects.keySet()) {
            if (rect.contains(mouseX, mouseY)) {
                if (ModuleManager.tooltips.shulkerRegearShiftMode.getValue()) {
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, clickableRects.get(rect), 0, ContainerInput.QUICK_MOVE, mc.player);
                } else {
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, clickableRects.get(rect), 0, ContainerInput.PICKUP, mc.player);
                }
            }
        }
    }
}
