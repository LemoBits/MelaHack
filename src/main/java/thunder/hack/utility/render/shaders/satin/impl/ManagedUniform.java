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

import thunder.hack.utility.render.shaders.satin.api.managed.uniform.Uniform1f;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.Uniform1i;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.Uniform2f;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.Uniform2i;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.Uniform3f;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.Uniform3i;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.Uniform4f;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.Uniform4i;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.UniformMat4;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gl.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public final class ManagedUniform extends ManagedUniformBase implements
        Uniform1i, Uniform2i, Uniform3i, Uniform4i,
        Uniform1f, Uniform2f, Uniform3f, Uniform4f,
        UniformMat4 {

    private static final GlUniform[] NO_TARGETS = new GlUniform[0];

    private final int count;

    private GlUniform[] targets = NO_TARGETS;
    private final int[] intValues = new int[4];
    private final float[] floatValues = new float[4];
    private boolean firstUpload = true;

    public ManagedUniform(String name, int count) {
        super(name);
        this.count = count;
    }

    @Override
    public boolean findUniformTargets(List<PostEffectPass> shaders) {
        this.targets = NO_TARGETS;
        return false;
    }

    @Override
    public boolean findUniformTarget(ShaderProgram shader) {
        GlUniform uniform = shader.getUniform(this.name);
        if (uniform != null) {
            UniformType type = uniform.getType();
            if (type.count() != this.count) {
                throw new IllegalStateException("Mismatched number of values, expected " + this.count + " but shader declares " + type.count());
            }
            this.targets = new GlUniform[] {uniform};
            this.syncCurrentValues();
            return true;
        } else {
            this.targets = NO_TARGETS;
            return false;
        }
    }

    private void syncCurrentValues() {
        if (!this.firstUpload) {
            for (GlUniform target : this.targets) {
                if (target.getType().isIntegerData()) {
                    if (this.count == 1) {
                        target.set(intValues[0]);
                    } else if (this.count == 3) {
                        target.set(intValues[0], intValues[1], intValues[2]);
                    } else {
                        target.set(intValues);
                    }
                } else {
                    if (this.count == 1) {
                        target.set(floatValues[0]);
                    } else if (this.count == 2) {
                        target.set(floatValues[0], floatValues[1]);
                    } else if (this.count == 3) {
                        target.set(floatValues[0], floatValues[1], floatValues[2]);
                    } else if (this.count == 4) {
                        target.setAndFlip(floatValues[0], floatValues[1], floatValues[2], floatValues[3]);
                    } else {
                        target.set(floatValues);
                    }
                }
            }
        }
    }

    @Override
    public void set(int value) {
        GlUniform[] targets = this.targets;
        int nbTargets = targets.length;
        if (nbTargets > 0) {
            if (firstUpload || intValues[0] != value) {
                for (GlUniform target : targets) {
                    target.set(value);
                }
                intValues[0] = value;
                firstUpload = false;
            }
        }
    }

    @Override
    public void set(int value0, int value1) {
        GlUniform[] targets = this.targets;
        int nbTargets = targets.length;
        if (nbTargets > 0) {
            if (firstUpload || intValues[0] != value0 || intValues[1] != value1) {
                for (GlUniform target : targets) {
                    target.set(new int[] {value0, value1});
                }
                intValues[0] = value0;
                intValues[1] = value1;
                firstUpload = false;
            }
        }
    }

    @Override
    public void set(int value0, int value1, int value2) {
        GlUniform[] targets = this.targets;
        int nbTargets = targets.length;
        if (nbTargets > 0) {
            if (firstUpload || intValues[0] != value0 || intValues[1] != value1 || intValues[2] != value2) {
                for (GlUniform target : targets) {
                    target.set(value0, value1, value2);
                }
                intValues[0] = value0;
                intValues[1] = value1;
                intValues[2] = value2;
                firstUpload = false;
            }
        }
    }

    @Override
    public void set(int value0, int value1, int value2, int value3) {
        GlUniform[] targets = this.targets;
        int nbTargets = targets.length;
        if (nbTargets > 0) {
            if (firstUpload || intValues[0] != value0 || intValues[1] != value1 || intValues[2] != value2 || intValues[3] != value3) {
                for (GlUniform target : targets) {
                    target.set(new int[] {value0, value1, value2, value3});
                }
                intValues[0] = value0;
                intValues[1] = value1;
                intValues[2] = value2;
                intValues[3] = value3;
                firstUpload = false;
            }
        }
    }

    @Override
    public void set(float value) {
        GlUniform[] targets = this.targets;
        int nbTargets = targets.length;
        if (nbTargets > 0) {
            if (firstUpload || floatValues[0] != value) {
                for (GlUniform target : targets) {
                    target.set(value);
                }
                floatValues[0] = value;
                firstUpload = false;
            }
        }
    }

    @Override
    public void set(float value0, float value1) {
        GlUniform[] targets = this.targets;
        int nbTargets = targets.length;
        if (nbTargets > 0) {
            if (firstUpload || floatValues[0] != value0 || floatValues[1] != value1) {
                for (GlUniform target : targets) {
                    target.set(value0, value1);
                }
                floatValues[0] = value0;
                floatValues[1] = value1;
                firstUpload = false;
            }
        }
    }

    @Override
    public void set(Vector2f value) {
        set(value.x(), value.y());
    }

    @Override
    public void set(float value0, float value1, float value2) {
        GlUniform[] targets = this.targets;
        int nbTargets = targets.length;
        if (nbTargets > 0) {
            if (firstUpload || floatValues[0] != value0 || floatValues[1] != value1 || floatValues[2] != value2) {
                for (GlUniform target : targets) {
                    target.set(value0, value1, value2);
                }
                floatValues[0] = value0;
                floatValues[1] = value1;
                floatValues[2] = value2;
                firstUpload = false;
            }
        }
    }

    @Override
    public void set(Vector3f value) {
        set(value.x(), value.y(), value.z());
    }

    @Override
    public void set(float value0, float value1, float value2, float value3) {
        GlUniform[] targets = this.targets;
        int nbTargets = targets.length;
        if (nbTargets > 0) {
            if (firstUpload || floatValues[0] != value0 || floatValues[1] != value1 || floatValues[2] != value2 || floatValues[3] != value3) {
                for (GlUniform target : targets) {
                    target.setAndFlip(value0, value1, value2, value3);
                }
                floatValues[0] = value0;
                floatValues[1] = value1;
                floatValues[2] = value2;
                floatValues[3] = value3;
                firstUpload = false;
            }
        }
    }

    @Override
    public void set(Vector4f value) {
        set(value.x(), value.y(), value.z(), value.w());
    }

    @Override
    public void set(Matrix4f value) {
        GlUniform[] targets = this.targets;
        int nbTargets = targets.length;
        if (nbTargets > 0) {
            for (GlUniform target : targets) {
                target.set(value);
            }
        }
    }

    @Override
    public void setFromArray(float[] values) {
        if (this.count != values.length) {
            throw new IllegalArgumentException("Mismatched values size, expected " + count + " but got " + values.length);
        }

        GlUniform[] targets = this.targets;
        int nbTargets = targets.length;
        if (nbTargets > 0) {
            for (GlUniform target : targets) {
                target.set(values);
            }
        }
    }
}
