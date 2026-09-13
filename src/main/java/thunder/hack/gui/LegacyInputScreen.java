package thunder.hack.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Adapts the pre-1.21.11 primitive input callbacks used by MelaHack's custom
 * screens to Minecraft's immutable input event API.
 */
public abstract class LegacyInputScreen extends Screen {
    protected LegacyInputScreen(Component title) {
        super(title);
    }

    @Override
    public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public final boolean mouseReleased(MouseButtonEvent event) {
        return mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public final boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public final boolean keyPressed(KeyEvent event) {
        return keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public final boolean keyReleased(KeyEvent event) {
        return keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public final boolean charTyped(CharacterEvent event) {
        return charTyped((char) event.codepoint(), 0);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) { return false; }
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) { return false; }
    public boolean charTyped(char codePoint, int modifiers) { return false; }
}
