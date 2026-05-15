package thunder.hack.gui.clickui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import thunder.hack.ThunderHack;
import thunder.hack.utility.render.compat.RenderSystem;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.BaritoneSettings;
import thunder.hack.features.modules.client.ClickGui;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.clickui.impl.SearchBar;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Category extends AbstractCategory {
    private final Identifier ICON;
    private static final RenderPipeline HEADER_ICON_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.of("thunderhack", "pipeline/clickgui_header_icon"))
            .withVertexShader(Identifier.of("minecraft", "core/position_tex_color"))
            .withFragmentShader(Identifier.of("minecraft", "core/position_tex_color"))
            .withSampler("Sampler0")
            .withUniform("ModelViewMat", UniformType.MATRIX4X4)
            .withUniform("ProjMat", UniformType.MATRIX4X4)
            .withUniform("ColorModulator", UniformType.VEC4)
            .withBlend(BlendFunction.ADDITIVE)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .build();

    private boolean scrollHover;
    private final List<AbstractButton> buttons;

    public float catHeight;

    public Category(Module.Category category, ArrayList<Module> features, float x, float y, float width, float height) {
        super(category.getName(), x, y, width, height);
        buttons = new ArrayList<>();
        ICON = Identifier.of("thunderhack", "textures/gui/headers/" + (Module.Category.isCustomCategory(category) ? "stock" : category.getName().toLowerCase()) + ".png");

        if (category.getName().equals("Client"))
            buttons.add(new SearchBar());

        features.forEach(feature -> {
            if (!(feature instanceof BaritoneSettings) || ThunderHack.baritone)
                buttons.add(new ModuleButton(feature));
        });
    }

    @Override
    public void init() {
        buttons.forEach(AbstractButton::init);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        setWidth(ModuleManager.clickGui.moduleWidth.getValue());

        scrollHover = Render2DEngine.isHovered(mouseX, mouseY, getX(), getY() + height, width, catHeight + 20);

        context.getMatrices().push();

        boolean popStack = false;

        float height1;
        if (ModuleManager.clickGui.scrollMode.getValue() == ClickGui.scrollModeEn.Old || getButtonsHeight() < ModuleManager.clickGui.catHeight.getValue())
            height1 = (float) getButtonsHeight();
        else
            height1 = (float) ((ModuleManager.clickGui.catHeight.getValue()));

        catHeight = AnimationUtility.fast(catHeight, height1, 30f);

        if (isOpen()) {
            Render2DEngine.drawHudBase(context.getMatrices(), getX() + 3, getY() + height - 6, width - 6, catHeight, 1, false);

            if (!(ModuleManager.clickGui.scrollMode.getValue() == ClickGui.scrollModeEn.Old || getButtonsHeight() < ModuleManager.clickGui.catHeight.getValue())) {
                Render2DEngine.addWindow(context.getMatrices(), getX() + 3, getY() + height - 6, getX() + 3 + width - 6, (getY() + height - 6) + (float) ((ModuleManager.clickGui.catHeight.getValue())), 1f);
                popStack = true;
            }

            Render2DEngine.drawBlurredShadow(context.getMatrices(), (int) getX() + 4, (int) (getY() + height - 6), (int) width - 8, 8, 7, new Color(0, 0, 0, 180));
            for (AbstractButton button : buttons) {
                if (button instanceof ModuleButton mb && SearchBar.listening && !mb.module.getName().toLowerCase().contains(SearchBar.moduleName.toLowerCase()))
                    continue;

                if (popStack && buttons.getFirst().getY() + moduleOffset < getY() + height) {
                    button.setY(getY() + height + moduleOffset);
                } else {
                    button.setY(getY() + height);
                    moduleOffset = 0f;
                }
                button.setX(getX() + 2);
                button.setWidth(width - 4);
                button.setHeight(ModuleManager.clickGui.moduleHeight.getValue());
                button.render(context, mouseX, mouseY, delta);
            }
        }

        if (popStack)
            Render2DEngine.popWindow();

        Render2DEngine.drawHudBase(context.getMatrices(), getX() + 2, getY() - 5, width - 4, height, 1, false);

        Color m1 = HudEditor.getColor(270);
        Color m2 = HudEditor.getColor(0);
        Color m3 = HudEditor.getColor(180);
        Color m4 = HudEditor.getColor(90);
        {
            RenderSystem.setShaderTexture(0, ICON);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.setShader(HEADER_ICON_PIPELINE);
            BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float clipX0 = getX() + 2;
            float clipY0 = getY() - 5;
            float clipX1 = clipX0 + width - 4;
            float clipY1 = clipY0 + height;
            renderHeaderIcon(b, context, getX() + 85, (getY() + (height - 24) / 2), 12, 12, 0, 0, 12, 12, 12, 12, clipX0, clipY0, clipX1, clipY1, m1.darker(), m2.darker(), m3.darker(), m4.darker());
            renderHeaderIcon(b, context, getX() + 75, (getY() + (height - 34) / 2), 16, 16, 0, 0, 16, 16, 16, 16, clipX0, clipY0, clipX1, clipY1, m1, m2, m3, m4);
            renderHeaderIcon(b, context, getX() + 65, (getY() + (height - 20) / 2), 12, 12, 0, 0, 12, 12, 12, 12, clipX0, clipY0, clipX1, clipY1, m1.darker().darker(), m2.darker().darker(), m3.darker().darker(), m4.darker().darker());
            renderHeaderIcon(b, context, getX() + 55, (getY() + (height - 28) / 2), 6, 6, 0, 0, 6, 6, 6, 6, clipX0, clipY0, clipX1, clipY1, m1, m2, m3, m4);
            renderHeaderIcon(b, context, getX() + 45, (getY() + (height - 17) / 2), 17, 17, 0, 0, 17, 17, 17, 17, clipX0, clipY0, clipX1, clipY1, m1, m2, m3, m4);
            renderHeaderIcon(b, context, getX() + 35, (getY() + (height - 30) / 2), 15, 15, 0, 0, 15, 15, 15, 15, clipX0, clipY0, clipX1, clipY1, m1.darker().darker().darker(), m2.darker().darker().darker(), m3.darker().darker().darker(), m4.darker().darker().darker());
            renderHeaderIcon(b, context, getX() + 25, (getY() + (height - 21) / 2), 8, 8, 0, 0, 8, 8, 8, 8, clipX0, clipY0, clipX1, clipY1, m1, m2, m3, m4);
            renderHeaderIcon(b, context, getX() + 15, (getY() + (height - 22) / 2), 12, 12, 0, 0, 12, 12, 12, 12, clipX0, clipY0, clipX1, clipY1, m1.darker().darker().darker(), m2.darker().darker().darker(), m3.darker().darker().darker(), m4.darker().darker().darker());
            renderHeaderIcon(b, context, getX() + 5, (getY() + (height - 28) / 2), 20, 20, 0, 0, 20, 20, 20, 20, clipX0, clipY0, clipX1, clipY1, m1, m2, m3, m4);
            BufferRenderer.drawWithGlobalProgram(b.end());
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }

        Render2DEngine.drawBlurredShadow(context.getMatrices(),
                ((int) getX() + (width - 4) / 2) - FontRenderers.categories.getStringWidth(getName()) / 2f, (int) getY() + (int) height / 2f - 10, FontRenderers.categories.getStringWidth(getName()) + 6, 13, 20, Render2DEngine.injectAlpha(Color.black, 170));

        FontRenderers.categories.drawCenteredString(context.getMatrices(), getName(), ((int) getX() + 2 + (width - 4) / 2), (int) getY() + (int) height / 2f - 7, new Color(-1).getRGB());
        context.getMatrices().pop();
        updatePosition();
    }

    private void renderHeaderIcon(BufferBuilder b, DrawContext context,
                                  float x, float y, float width, float height,
                                  float u, float v, float regionWidth, float regionHeight,
                                  float textureWidth, float textureHeight,
                                  float clipX0, float clipY0, float clipX1, float clipY1,
                                  Color c1, Color c2, Color c3, Color c4) {
        float x0 = Math.max(x, clipX0);
        float y0 = Math.max(y, clipY0);
        float x1 = Math.min(x + width, clipX1);
        float y1 = Math.min(y + height, clipY1);
        if (x1 <= x0 || y1 <= y0) {
            return;
        }

        float clippedU0 = u + (x0 - x) / width * regionWidth;
        float clippedU1 = u + (x1 - x) / width * regionWidth;
        float clippedV0 = v + (y0 - y) / height * regionHeight;
        float clippedV1 = v + (y1 - y) / height * regionHeight;
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        b.vertex(matrix, x0, y1, 0).texture(clippedU0 / textureWidth, clippedV1 / textureHeight).color(c1.getRGB());
        b.vertex(matrix, x1, y1, 0).texture(clippedU1 / textureWidth, clippedV1 / textureHeight).color(c2.getRGB());
        b.vertex(matrix, x1, y0, 0).texture(clippedU1 / textureWidth, clippedV0 / textureHeight).color(c3.getRGB());
        b.vertex(matrix, x0, y0, 0).texture(clippedU0 / textureWidth, clippedV0 / textureHeight).color(c4.getRGB());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {

        if (button == 1 && hovered) {
            setOpen(!isOpen());
        }
        super.mouseClicked(mouseX, mouseY, button);

        if (isOpen() && scrollHover)
            for (AbstractButton b : buttons) {
                if (b instanceof ModuleButton mb && SearchBar.listening && !mb.module.getName().toLowerCase().contains(SearchBar.moduleName.toLowerCase()))
                    continue;
                b.mouseClicked(mouseX, mouseY, button);
            }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        if (isOpen())
            for (AbstractButton b : buttons) {
                if (b instanceof ModuleButton mb && SearchBar.listening && !mb.module.getName().toLowerCase().contains(SearchBar.moduleName.toLowerCase()))
                    continue;
                b.mouseReleased(mouseX, mouseY, button);
            }
    }

    @Override
    public boolean keyTyped(int keyCode) {
        if (isOpen()) {
            for (AbstractButton button : buttons) {
                if (button instanceof ModuleButton mb && SearchBar.listening && !mb.module.getName().toLowerCase().contains(SearchBar.moduleName.toLowerCase()))
                    continue;
                button.keyTyped(keyCode);
            }
        }
        return false;
    }

    @Override
    public void charTyped(char key, int keyCode) {
        if (isOpen()) {
            for (AbstractButton button : buttons) {
                if (button instanceof ModuleButton mb && SearchBar.listening && !mb.module.getName().toLowerCase().contains(SearchBar.moduleName.toLowerCase()))
                    continue;
                button.charTyped(key, keyCode);
            }
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        buttons.forEach(AbstractButton::onGuiClosed);
    }

    @Override
    public void tick() {
        buttons.forEach(AbstractButton::tick);
    }

    private void updatePosition() {
        float offsetY = 0;
        float openY = 0;
        for (AbstractButton button : buttons) {
            if (button instanceof ModuleButton mb && SearchBar.listening && !mb.module.getName().toLowerCase().contains(SearchBar.moduleName.toLowerCase())) {
                continue;
            }
            button.setTargetOffset(offsetY);
            if (button instanceof ModuleButton mbutton) {
                if (mbutton.isOpen()) {
                    for (AbstractElement element : mbutton.getElements()) {
                        if (element.isVisible())
                            offsetY += element.getHeight();
                    }
                    offsetY += 2f;
                }
            }
            offsetY += button.getHeight() + openY;
        }
    }

    @Override
    public void hudClicked(Module module) {
        for (AbstractButton button : buttons) {
            if (button instanceof ModuleButton mbutton && mbutton.module == module)
                mbutton.setOpen(true);
        }
    }

    public double getButtonsHeight() {
        double height = 8;
        for (AbstractButton button : buttons) {
            if (button instanceof ModuleButton mb && SearchBar.listening && !mb.module.getName().toLowerCase().contains(SearchBar.moduleName.toLowerCase()))
                continue;

            if (button instanceof ModuleButton mbutton) {
                if (mbutton.isOpen())
                    height += 2f;
                height += mbutton.getElementsHeight();
            }

            height += button.getHeight();
        }
        return height;
    }
}
