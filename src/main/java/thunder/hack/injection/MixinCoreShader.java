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
package thunder.hack.injection;

import net.minecraft.client.gl.ShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import thunder.hack.utility.render.shaders.satin.impl.SamplerAccess;

import java.util.List;

@Mixin(ShaderProgram.class)
public abstract class MixinCoreShader implements SamplerAccess {
    @Override
    public boolean hasSampler(String name) {
        return false;
    }

    @Override
    public List<String> getSamplerNames() {
        return List.of();
    }

    @Override
    public List<Integer> getSamplerShaderLocs() {
        return List.of();
    }
}
