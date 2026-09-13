/*
 * Satin
 * Copyright (C) 2019-2024 Ladysnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; If not, see <https://www.gnu.org/licenses>.
 */
package thunder.hack.utility.render.shaders.satin.impl;

import thunder.hack.utility.render.shaders.satin.api.managed.uniform.SamplerUniformV2;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.function.IntSupplier;
import net.minecraft.client.renderer.texture.AbstractTexture;

public final class ManagedSamplerUniformV2 extends ManagedSamplerUniformBase implements SamplerUniformV2 {
    public ManagedSamplerUniformV2(String name) {
        super(name);
    }

    @Override
    public void set(AbstractTexture texture) {
        set((Object) texture);
    }

    @Override
    public void set(RenderTarget textureFbo) {
        set((Object) textureFbo);
    }

    @Override
    public void set(int textureName) {
        set((Object) textureName);
    }

    @Override
    protected void set(Object value) {
        this.cachedValue = value;
    }

    @Override
    public void set(IntSupplier value) {
        this.cachedValue = value;
    }
}
