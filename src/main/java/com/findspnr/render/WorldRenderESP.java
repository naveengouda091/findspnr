package com.findspnr.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.findspnr.config.ModConfig;
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
 *  1. Renders a thin red tracer thread connecting player camera directly to each spawner.
 *  2. Renders a bright red 3D bounding box outline around each spawner block.
 *  3. Uses RenderSystem.disableDepthTest() so lines & tracers render 100% THROUGH WALLS.
 */
public class WorldRenderESP {

    private static Method shaderMethod = null;
    private static boolean reflectionAttempted = false;

    public static void render(WorldRenderContext context) {
        if (!ModConfig.enabled || !ModConfig.renderWorldESP) {
            return;
        }

        List<SpawnerInfo> spawners = SpawnerTracker.getDetectedSpawners();
        if (spawners.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Vec3d cameraPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();

        matrices.push();

        // Disable depth test so tracers & boxes render see-through through all blocks
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        setLineShader();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (SpawnerInfo spawner : spawners) {
            BlockPos pos = spawner.getPos();

            // Center position of spawner relative to camera
            double targetX = pos.getX() + 0.5 - cameraPos.x;
            double targetY = pos.getY() + 0.5 - cameraPos.y;
            double targetZ = pos.getZ() + 0.5 - cameraPos.z;

            // 1. Tracer line (thin red thread from camera center (0,0,0) to spawner center)
            line(bufferBuilder, matrix, 0f, -0.1f, 0f, (float) targetX, (float) targetY, (float) targetZ, 1.0f, 0.0f, 0.0f, 1.0f);

            // 2. Outer 1x1x1 spawner box outline (Bright Red)
            drawBoxOutline(bufferBuilder, matrix, targetX - 0.5, targetY - 0.5, targetZ - 0.5,
                           targetX + 0.5, targetY + 0.5, targetZ + 0.5, 1.0f, 0.0f, 0.0f, 1.0f);

            // 3. Inner core box (Bright Yellow/Red)
            double s = 0.15;
            drawBoxOutline(bufferBuilder, matrix, targetX - s, targetY - s, targetZ - s,
                           targetX + s, targetY + s, targetZ + s, 1.0f, 0.8f, 0.0f, 1.0f);
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
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
