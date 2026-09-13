package thunder.hack.utility.render.shaders.satin.impl;

import com.google.common.base.Preconditions;
import thunder.hack.utility.render.compat.RenderSystem;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.logging.LogUtils;
import thunder.hack.utility.render.shaders.satin.api.managed.ManagedFramebuffer;
import thunder.hack.utility.render.shaders.satin.api.managed.ManagedShaderEffect;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.SamplerUniformV2;
import thunder.hack.injection.accesors.AccessiblePassesShaderEffect;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceProvider;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public final class ResettableManagedShaderEffect extends ResettableManagedShaderBase<PostChain> implements ManagedShaderEffect {

    private static final Identifier BUF_IN = Identifier.fromNamespaceAndPath("thunderhack", "buf_in");
    private static final Identifier BUF_OUT = Identifier.fromNamespaceAndPath("thunderhack", "buf_out");
    private static final Set<Identifier> EXTERNAL_TARGETS = Set.of(PostChain.MAIN_TARGET_ID, BUF_IN, BUF_OUT);

    private final Consumer<ManagedShaderEffect> initCallback;
    private final Map<String, FramebufferWrapper> managedTargets;
    private final Map<String, ManagedSamplerUniformV2> managedSamplers = new HashMap<>();

    public ResettableManagedShaderEffect(Identifier location, Consumer<ManagedShaderEffect> initCallback) {
        super(location);
        this.initCallback = initCallback;
        this.managedTargets = new HashMap<>();
    }

    @Override
    public PostChain getShaderEffect() {
        return getShaderOrLog();
    }

    @Override
    protected PostChain parseShader(ResourceProvider resourceFactory, Minecraft mc, Identifier location) throws IOException {
        ShaderManager loader = mc.getShaderManager();
        return loader.getPostChain(location, EXTERNAL_TARGETS);
    }

    @Override
    public void setup(int windowWidth, int windowHeight) {
        Preconditions.checkNotNull(shader);
        for (ManagedUniformBase uniform : this.getManagedUniforms()) {
            setupUniform(uniform, shader);
        }

        for (FramebufferWrapper buf : this.managedTargets.values()) {
            buf.findTarget(this.shader);
        }

        this.initCallback.accept(this);
    }

    @Override
    public void render(float tickDelta) {
        PostChain sg = this.getShaderEffect();
        if (sg != null) {
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            Minecraft client = Minecraft.getInstance();
            Map<Identifier, com.mojang.blaze3d.pipeline.RenderTarget> externalTargets = Map.of(
                PostChain.MAIN_TARGET_ID, client.gameRenderer.mainRenderTarget(),
                BUF_IN, client.gameRenderer.mainRenderTarget(),
                BUF_OUT, client.gameRenderer.mainRenderTarget()
            );
            PostEffectRenderUtil.render(sg, client.gameRenderer.mainRenderTarget().width, client.gameRenderer.mainRenderTarget().height, externalTargets, GraphicsResourceAllocator.UNPOOLED);
            RenderSystem.disableBlend();
            RenderSystem.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableDepthTest();
        }
    }

    @Override
    public ManagedFramebuffer getTarget(String name) {
        return this.managedTargets.computeIfAbsent(name, n -> {
            FramebufferWrapper ret = new FramebufferWrapper(n);
            if (this.shader != null) {
                ret.findTarget(this.shader);
            }
            return ret;
        });
    }

    @Override
    public void setUniformValue(String uniformName, int value) {
        this.findUniform1i(uniformName).set(value);
    }

    @Override
    public void setUniformValue(String uniformName, float value) {
        this.findUniform1f(uniformName).set(value);
    }

    @Override
    public void setUniformValue(String uniformName, float value0, float value1) {
        this.findUniform2f(uniformName).set(value0, value1);
    }

    @Override
    public void setUniformValue(String uniformName, float value0, float value1, float value2) {
        this.findUniform3f(uniformName).set(value0, value1, value2);
    }

    @Override
    public void setUniformValue(String uniformName, float value0, float value1, float value2, float value3) {
        this.findUniform4f(uniformName).set(value0, value1, value2, value3);
    }

    @Override
    public SamplerUniformV2 findSampler(String samplerName) {
        return manageUniform(this.managedSamplers, ManagedSamplerUniformV2::new, samplerName, "sampler");
    }

    @Override
    protected boolean setupUniform(ManagedUniformBase uniform, PostChain shader) {
        return uniform.findUniformTargets(((AccessiblePassesShaderEffect) shader).getPasses());
    }

    @Override
    protected void logInitError(IOException e) {
        LogUtils.getLogger().error("Could not create screen shader {}", this.getLocation(), e);
    }

    private PostChain getShaderOrLog() {
        if (!this.isInitialized() && !this.isErrored()) {
            this.initializeOrLog(Minecraft.getInstance().getResourceManager());
        }
        return this.shader;
    }
}
