package com.findspnr.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.findspnr.config.ModConfig;
import com.findspnr.tracker.BaseInfo;
import com.findspnr.tracker.BaseTracker;
import com.findspnr.tracker.BastionInfo;
import com.findspnr.tracker.BastionTracker;
import com.findspnr.tracker.FreecamController;
import com.findspnr.tracker.SpawnerInfo;
import com.findspnr.tracker.SpawnerTracker;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 3D World ESP & Tracer Lines Renderer:
 *  1. Renders thin tracer threads connecting camera crosshair directly to target blocks.
 *  2. Renders 3D bounding box outlines around targets:
 *     • Red = Monster Spawners
 *     • Yellow = Shulker Boxes
 *     • Cyan = Ender Chests
 *     • Orange = Nether Bastion Remnants
 *  3. Uses direct GlStateManager._disableDepthTest() so lines & tracers render 100% THROUGH ALL BLOCKS!
 */
public class WorldRenderESP {

    private static Method shaderMethod = null;
    private static boolean reflectionAttempted = false;

    public static void render(WorldRenderContext context) {
        if (!ModConfig.enabled || !ModConfig.renderWorldESP) {
            return;
        }

        List<SpawnerInfo> spawners = SpawnerTracker.getDetectedSpawners();
        List<BaseInfo> bases = ModConfig.renderBaseFinder ? BaseTracker.getDetectedBases() : List.of();
        List<BastionInfo> bastions = ModConfig.renderBastionFinder ? BastionTracker.getDetectedBastions() : List.of();

        if (spawners.isEmpty() && bases.isEmpty() && bastions.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Vec3d cameraPos = ModConfig.freecamEnabled ? FreecamController.getFreecamPos() : context.camera().getPos();
        MatrixStack matrices = context.matrixStack();

        matrices.push();

        // Direct low-level OpenGL state overrides to force see-through lines everywhere
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._enableBlend();
        GlStateManager._blendFunc(GlStateManager.SrcFactor.SRC_ALPHA.value, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA.value);

        setLineShader();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // 1. Render Spawner Targets (Bright Red)
        for (SpawnerInfo spawner : spawners) {
            BlockPos pos = spawner.getPos();

            double targetX = pos.getX() + 0.5 - cameraPos.x;
            double targetY = pos.getY() + 0.5 - cameraPos.y;
            double targetZ = pos.getZ() + 0.5 - cameraPos.z;

            line(bufferBuilder, matrix, 0f, 0f, 0f, (float) targetX, (float) targetY, (float) targetZ, 1.0f, 0.0f, 0.0f, 1.0f);

            drawBoxOutline(bufferBuilder, matrix, targetX - 0.5, targetY - 0.5, targetZ - 0.5,
                           targetX + 0.5, targetY + 0.5, targetZ + 0.5, 1.0f, 0.0f, 0.0f, 1.0f);

            double s = 0.15;
            drawBoxOutline(bufferBuilder, matrix, targetX - s, targetY - s, targetZ - s,
                           targetX + s, targetY + s, targetZ + s, 1.0f, 0.8f, 0.0f, 1.0f);
        }

        // 2. Render Base Targets (Shulker Box = Yellow, Ender Chest = Cyan)
        if (ModConfig.renderBaseFinder) {
            for (BaseInfo base : bases) {
                BlockPos pos = base.getPos();

                double targetX = pos.getX() + 0.5 - cameraPos.x;
                double targetY = pos.getY() + 0.5 - cameraPos.y;
                double targetZ = pos.getZ() + 0.5 - cameraPos.z;

                float r = base.isShulkerBox() ? 1.0f : 0.0f;
                float g = base.isShulkerBox() ? 0.85f : 0.95f;
                float b = base.isShulkerBox() ? 0.0f : 1.0f;

                line(bufferBuilder, matrix, 0f, 0f, 0f, (float) targetX, (float) targetY, (float) targetZ, r, g, b, 1.0f);

                drawBoxOutline(bufferBuilder, matrix, targetX - 0.5, targetY - 0.5, targetZ - 0.5,
                               targetX + 0.5, targetY + 0.5, targetZ + 0.5, r, g, b, 1.0f);
            }
        }

        // 3. Render Bastion Targets (Orange)
        if (ModConfig.renderBastionFinder) {
            for (BastionInfo bastion : bastions) {
                BlockPos pos = bastion.getPos();

                double targetX = pos.getX() + 0.5 - cameraPos.x;
                double targetY = pos.getY() + 0.5 - cameraPos.y;
                double targetZ = pos.getZ() + 0.5 - cameraPos.z;

                float r = 1.0f;
                float g = 0.5f;
                float b = 0.0f;

                line(bufferBuilder, matrix, 0f, 0f, 0f, (float) targetX, (float) targetY, (float) targetZ, r, g, b, 1.0f);

                drawBoxOutline(bufferBuilder, matrix, targetX - 1.5, targetY - 1.5, targetZ - 1.5,
                               targetX + 1.5, targetY + 1.5, targetZ + 1.5, r, g, b, 1.0f);
            }
        }

        // FORCE OpenGL depth test disabled RIGHT BEFORE actual GPU draw call
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        // Restore OpenGL depth state
        GlStateManager._depthMask(true);
        GlStateManager._enableDepthTest();
        matrices.pop();
    }

    private static void setLineShader() {
        if (!reflectionAttempted) {
            reflectionAttempted = true;
            for (Method m : GameRenderer.class.getDeclaredMethods()) {
                if (m.getParameterCount() == 0 && ShaderProgram.class.isAssignableFrom(m.getReturnType())) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("line") || name.contains("positioncolor")) {
                        shaderMethod = m;
                        break;
                    }
                }
            }
        }

        if (shaderMethod != null) {
            try {
                ShaderProgram program = (ShaderProgram) shaderMethod.invoke(null);
                if (program != null) {
                    RenderSystem.setShader(program);
                }
            } catch (Exception ignored) {}
        }
    }

    private static void drawBoxOutline(BufferBuilder builder, Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float r, float g, float b, float a) {
        float x1 = (float) minX;
        float y1 = (float) minY;
        float z1 = (float) minZ;
        float x2 = (float) maxX;
        float y2 = (float) maxY;
        float z2 = (float) maxZ;

        // Bottom square
        line(builder, matrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(builder, matrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(builder, matrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(builder, matrix, x1, y1, z2, x1, y1, z1, r, g, b, a);

        // Top square
        line(builder, matrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(builder, matrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(builder, matrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(builder, matrix, x1, y2, z2, x1, y2, z1, r, g, b, a);

        // Vertical pillars
        line(builder, matrix, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(builder, matrix, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(builder, matrix, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(builder, matrix, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void line(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = len > 0 ? dx / len : 0f;
        float ny = len > 0 ? dy / len : 1f;
        float nz = len > 0 ? dz / len : 0f;

        builder.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(nx, ny, nz);
        builder.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(nx, ny, nz);
    }
}
