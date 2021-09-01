/*
 * MIT License
 *
 * Copyright (c) 2021 OroArmor (Eli Orona), Blaze4D
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oroarmor.bakedminecraftmodels.model;

import com.mojang.blaze3d.platform.GlStateManager;
import com.oroarmor.bakedminecraftmodels.BakedMinecraftModelsRenderLayerManager;
import com.oroarmor.bakedminecraftmodels.BakedMinecraftModelsVertexFormats;
import com.oroarmor.bakedminecraftmodels.access.RenderLayerCreatedVertexConsumer;
import com.oroarmor.bakedminecraftmodels.data.BakingData;
import com.oroarmor.bakedminecraftmodels.mixin.buffer.BufferBuilderAccessor;
import com.oroarmor.bakedminecraftmodels.mixin.buffer.SpriteTexturedVertexConsumerAccessor;
import com.oroarmor.bakedminecraftmodels.mixin.renderlayer.MultiPhaseParametersAccessor;
import com.oroarmor.bakedminecraftmodels.mixin.renderlayer.MultiPhaseRenderPassAccessor;
import com.oroarmor.bakedminecraftmodels.ssbo.SectionedPbo;
import com.oroarmor.bakedminecraftmodels.ssbo.SectionedSyncObjects;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.SpriteTexturedVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryUtil;

public class GlobalModelUtils {

    public static final int MODEL_STRUCT_SIZE = (4 * Float.BYTES) + (2 * Integer.BYTES) + (2 * Integer.BYTES) + (4 * Float.BYTES);
    public static final int PART_STRUCT_SIZE = 16 * Float.BYTES;

    public static final MatrixStack.Entry IDENTITY_STACK_ENTRY = new MatrixStack().peek();

    public static final int BUFFER_CREATION_FLAGS = GL30C.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT;

    public static final int BUFFER_MAP_FLAGS = GL30C.GL_MAP_WRITE_BIT | GL30C.GL_MAP_FLUSH_EXPLICIT_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT;

    public static final int BUFFER_SECTIONS = 3;

    public static final int ENTITY_LIMIT = 8192;

    public static final long PART_PBO_SIZE = ENTITY_LIMIT * 16 * PART_STRUCT_SIZE; // assume each entity has on average 16 parts
    public static final long MODEL_PBO_SIZE = ENTITY_LIMIT * MODEL_STRUCT_SIZE;

    public static final BakingData bakingData = new BakingData();

    // TODO: MOVE THESE AS SOON AS POSSIBLE FOR ABSTRACTION!!!
    private static SectionedPbo PART_PBO;
    private static SectionedPbo MODEL_PBO;
    public static SectionedSyncObjects SYNC_OBJECTS = new SectionedSyncObjects(GlobalModelUtils.BUFFER_SECTIONS);

    public static SectionedPbo getOrCreatePartPbo() {
        if (PART_PBO == null) {
            PART_PBO = createSsboPbo(PART_PBO_SIZE);
        }
        return PART_PBO;
    }

    public static SectionedPbo getOrCreateModelPbo() {
        if (MODEL_PBO == null) {
            MODEL_PBO = createSsboPbo(MODEL_PBO_SIZE);
        }
        return MODEL_PBO;
    }

    private static SectionedPbo createSsboPbo(long ssboSize) {
        int name = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, name);
        long fullSize = ssboSize * GlobalModelUtils.BUFFER_SECTIONS;
        ARBBufferStorage.nglBufferStorage(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, fullSize, MemoryUtil.NULL, GlobalModelUtils.BUFFER_CREATION_FLAGS);
        return new SectionedPbo(
                GL30C.glMapBufferRange(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, 0, fullSize, GlobalModelUtils.BUFFER_MAP_FLAGS),
                name,
                GlobalModelUtils.BUFFER_SECTIONS,
                ssboSize
        );
    }

    public static BufferBuilder getNestedBufferBuilder(VertexConsumer consumer) { // TODO: add more possibilities with this method, ex outline consumers
        return consumer instanceof SpriteTexturedVertexConsumer ?
                (BufferBuilder) ((SpriteTexturedVertexConsumerAccessor) consumer).getParent() :
                (BufferBuilder) consumer;
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean isSmartBufferBuilder(BufferBuilder nestedBuilder) {
        if (((BufferBuilderAccessor) nestedBuilder).getFormat().equals(BakedMinecraftModelsVertexFormats.SMART_ENTITY_FORMAT)) {
            if (nestedBuilder instanceof RenderLayerCreatedVertexConsumer renderLayerCreatedVertexConsumer) {
                if (renderLayerCreatedVertexConsumer.getRenderLayer() instanceof MultiPhaseRenderPassAccessor multiPhaseRenderPassAccessor) {
                    return ((MultiPhaseParametersAccessor) (Object) multiPhaseRenderPassAccessor.getPhases()).getShader().equals(BakedMinecraftModelsRenderLayerManager.SMART_ENTITY_CUTOUT_NO_CULL_PHASE);
                }
            }
        }
        return false;
    }

}
