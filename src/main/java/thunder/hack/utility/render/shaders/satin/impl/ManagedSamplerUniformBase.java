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

import thunder.hack.utility.render.shaders.satin.api.managed.uniform.SamplerUniform;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.ShaderProgram;

import java.util.List;

public abstract class ManagedSamplerUniformBase extends ManagedUniformBase implements SamplerUniform {
    protected SamplerAccess[] targets = new SamplerAccess[0];
    protected Object cachedValue;

    public ManagedSamplerUniformBase(String name) {
        super(name);
    }

    @Override
    public boolean findUniformTargets(List<PostEffectPass> shaders) {
        this.targets = new SamplerAccess[0];
        return false;
    }

    @Override
    public boolean findUniformTarget(ShaderProgram shader) {
        return findUniformTarget1(((SamplerAccess) shader));
    }

    private boolean findUniformTarget1(SamplerAccess access) {
        if (access.hasSampler(this.name)) {
            this.targets = new SamplerAccess[] {access};
            this.syncCurrentValues();
            return true;
        }
        return false;
    }

    private void syncCurrentValues() {
        Object value = this.cachedValue;
        if (value != null) { // after the first upload
            this.cachedValue = null;
            this.set(value);
        }
    }

    protected abstract void set(Object value);

}
