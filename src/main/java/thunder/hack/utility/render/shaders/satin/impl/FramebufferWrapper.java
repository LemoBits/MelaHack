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

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import thunder.hack.utility.render.shaders.satin.api.managed.ManagedFramebuffer;

public final class FramebufferWrapper implements ManagedFramebuffer {
    private final String name;
    private RenderTarget wrapped;

    FramebufferWrapper(String name) {
        this.name = name;
    }

    void findTarget(PostChain shaderEffect) {
        this.wrapped = null;
    }

    public String getName() {
        return name;
    }

    @Override
    public RenderTarget getFramebuffer() {
        return wrapped;
    }

    @Override
    public void beginWrite(boolean updateViewport) {
    }

    @Override
    public void draw() {
        Window window = Minecraft.getInstance().getWindow();
        this.draw(window.getWidth(), window.getHeight(), true);
    }

    @Override
    public void draw(int width, int height, boolean disableBlend) {
        if (this.wrapped != null) {
            this.wrapped.blitAndBlendToTexture(Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTextureView(), Minecraft.getInstance().gameRenderer.mainRenderTarget().getDepthTextureView());
        }
    }

    @Override
    public void clear() {
    }

    @Override
    public void clear(boolean swallowErrors) {
    }
}
