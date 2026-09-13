package thunder.hack.injection;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.gui.clickui.ClickGUI;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.modules.Module;
import thunder.hack.utility.render.WindowResizeCallback;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.packs.PackResources;

import static thunder.hack.features.modules.Module.mc;

import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {

    @Shadow
    @Final
    private Window window;

    @Unique
    private String[] shittyServers = {
            "mineblaze",
            "musteryworld",
            "dexland",
            "masedworld",
            "vimeworld",
            "hypemc",
            "vimemc"
    };

    @Inject(method = "<init>", at = @At("TAIL"))
    void postWindowInit(GameConfig args, CallbackInfo ci) {
        try {
            FontRenderers.settings = FontRenderers.create(12f, "comfortaa");
            FontRenderers.modules = FontRenderers.create(15f, "comfortaa");
            FontRenderers.categories = FontRenderers.create(18f, "comfortaa");
            FontRenderers.thglitch = FontRenderers.create(36f, "glitched");
            FontRenderers.thglitchBig = FontRenderers.create(72f, "glitched");
            FontRenderers.monsterrat = FontRenderers.create(18f, "monsterrat");
            FontRenderers.sf_bold = FontRenderers.create(16f, "sf_bold");
            FontRenderers.sf_medium = FontRenderers.create(16f, "sf_medium");
            FontRenderers.sf_medium_mini = FontRenderers.create(12f, "sf_medium");
            FontRenderers.sf_medium_modules = FontRenderers.create(14f, "sf_medium");
            FontRenderers.sf_bold_mini = FontRenderers.create(14f, "sf_bold");
            FontRenderers.sf_bold_micro = FontRenderers.create(12f, "sf_bold");
            FontRenderers.profont = FontRenderers.create(16f, "profont");
            FontRenderers.icons = FontRenderers.create(20, "icons");
            FontRenderers.mid_icons = FontRenderers.create(46, "icons");
            FontRenderers.big_icons = FontRenderers.create(72, "icons");
            FontRenderers.source_han_sans_normal = FontRenderers.create(16f, "source_han_sans_normal");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    void preTickHook(CallbackInfo ci) {
        if (!Module.fullNullCheck()) ThunderHack.EVENT_BUS.post(new EventTick());
    }

    @Inject(method = "tick", at = @At("RETURN"))
    void postTickHook(CallbackInfo ci) {
        if (!Module.fullNullCheck()) ThunderHack.EVENT_BUS.post(new EventPostTick());
    }

    @Inject(method = "resizeGui", at = @At("TAIL"))
    private void captureResize(CallbackInfo ci) {
        WindowResizeCallback.EVENT.invoker().onResized((Minecraft) (Object) this, this.window);
    }


    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void doItemPickHook(CallbackInfo ci) {
        if (ModuleManager.middleClick.isEnabled() && ModuleManager.middleClick.antiPickUp.getValue())
            ci.cancel();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setIcon(Lnet/minecraft/server/packs/PackResources;Lcom/mojang/blaze3d/platform/IconSet;)V"))
    private void onChangeIcon(Window instance, PackResources resourcePack, IconSet icons) throws IOException {
        // RenderSystem.assertInInitPhase();

        if (GLFW.glfwGetPlatform() == 393218) {
            MacosUtil.loadIcon(icons.getMacIcon(resourcePack));
            return;
        }

        setWindowIcon(ThunderHack.class.getResourceAsStream("/icon.png"), ThunderHack.class.getResourceAsStream("/icon.png"));
    }

    public void setWindowIcon(InputStream img16x16, InputStream img32x32) {
        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            GLFWImage.Buffer buffer = GLFWImage.malloc(2, memorystack);
            List<InputStream> imgList = List.of(img16x16, img32x32);
            List<ByteBuffer> buffers = new ArrayList<>();

            for (int i = 0; i < imgList.size(); i++) {
                NativeImage nativeImage = NativeImage.read(imgList.get(i));
                ByteBuffer bytebuffer = MemoryUtil.memAlloc(nativeImage.getWidth() * nativeImage.getHeight() * 4);

                bytebuffer.asIntBuffer().put(nativeImage.getPixels());
                buffer.position(i);
                buffer.width(nativeImage.getWidth());
                buffer.height(nativeImage.getHeight());
                buffer.pixels(bytebuffer);

                buffers.add(bytebuffer);
            }

            try {
                if (GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_WAYLAND) {
                    GLFW.glfwSetWindowIcon(mc.getWindow().handle(), buffer);
                }
            } catch (Exception ignored) {
            }
            buffers.forEach(MemoryUtil::memFree);
        } catch (IOException ignored) {
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void doAttackHook(CallbackInfoReturnable<Boolean> cir) {
        final EventAttack event = new EventAttack(null, true);
        ThunderHack.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void handleBlockBreakingHook(boolean breaking, CallbackInfo ci) {
        EventHandleBlockBreaking event = new EventHandleBlockBreaking();
        ThunderHack.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
