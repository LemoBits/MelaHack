package thunder.hack.injection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventEntityRemoved;
import thunder.hack.events.impl.EventEntitySpawn;
import thunder.hack.events.impl.EventEntitySpawnPost;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.render.WorldTweaks;
import thunder.hack.setting.impl.ColorSetting;

import static thunder.hack.features.modules.Module.mc;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(ClientLevel.class)
public class MixinClientWorld {
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    public void addEntityHook(Entity entity, CallbackInfo ci) {
        if(Module.fullNullCheck()) return;
        EventEntitySpawn ees = new EventEntitySpawn(entity);
        ThunderHack.EVENT_BUS.post(ees);
        if (ees.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "addEntity", at = @At("RETURN"), cancellable = true)
    public void addEntityHookPost(Entity entity, CallbackInfo ci) {
        if(Module.fullNullCheck()) return;
        EventEntitySpawnPost ees = new EventEntitySpawnPost(entity);
        ThunderHack.EVENT_BUS.post(ees);
        if (ees.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    public void removeEntityHook(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
        if(Module.fullNullCheck()) return;
        EventEntityRemoved eer = new EventEntityRemoved(mc.level.getEntity(entityId));
        ThunderHack.EVENT_BUS.post(eer);
    }

    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void getSkyColorHook(Vec3 cameraPos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        if (ModuleManager.worldTweaks.isEnabled() && WorldTweaks.fogModify.getValue().isEnabled()) {
            ColorSetting c = WorldTweaks.fogColor.getValue();
            cir.setReturnValue(0xFF000000 | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue());
        }
    }

    @Inject(method = "playSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZJ)V", at = @At("HEAD"))
    private void playSoundHoof(double x, double y, double z, SoundEvent event, SoundSource category, float volume, float pitch, boolean useDistance, long seed, CallbackInfo ci) {
        if (ModuleManager.soundESP.isEnabled()) {
            ResourceLocation id = BuiltInRegistries.SOUND_EVENT.getKey(event);
            ModuleManager.soundESP.add(x, y, z, id != null ? id.toLanguageKey() : "unknown");
        }
    }
}
