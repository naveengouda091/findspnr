package com.findspnr.render;

import com.findspnr.config.ModConfig;
import com.findspnr.tracker.SpawnerInfo;
import com.findspnr.tracker.SpawnerTracker;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Draws a glowing red 3-D bounding-box outline around every detected spawner.
 *
 * Updated for Minecraft 26.2 "Chaos Cubed":
 *  - Uses VertexConsumerProvider / VertexConsumer instead of raw RenderSystem calls
 *    to stay compatible with both the legacy OpenGL and experimental Vulkan backends.
 *  - Lines are drawn via RenderLayer.LINES which Blaze3D abstracts over the backend.
 */
public class WorldRenderESP {

    public static void render(WorldRenderContext ctx) {
        if (!ModConfig.enabled || !ModConfig.renderWorldESP) return;

        List<SpawnerInfo> spawners = SpawnerTracker.getDetectedSpawners();
        if (spawners.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Vec3d       camera  = ctx.camera().getPos();
        MatrixStack mstack  = ctx.matrixStack();

        // Use the immediate VertexConsumerProvider from the render context
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer lines = immediate.getBuffer(RenderLayer.getLines());

        mstack.push();
        mstack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = mstack.peek().getPositionMatrix();
        Matrix4f normalMatrix = new Matrix4f(mstack.peek().getNormalMatrix());

        for (SpawnerInfo spawner : spawners) {
            BlockPos p = spawner.getPos();

            // Outer box – semi-transparent bright red
            drawBox(lines, matrix, normalMatrix,
                    p.getX(),      p.getY(),      p.getZ(),
                    p.getX() + 1f, p.getY() + 1f, p.getZ() + 1f,
                    1.0f, 0.1f, 0.1f, 0.85f);

            // Inner core dot at the centre
            float s  = 0.15f;
            float bx = p.getX() + 0.5f;
            float by = p.getY() + 0.5f;
            float bz = p.getZ() + 0.5f;
            drawBox(lines, matrix, normalMatrix,
                    bx - s, by - s, bz - s,
                    bx + s, by + s, bz + s,
                    1.0f, 0.0f, 0.0f, 1.0f);
        }

        immediate.draw(RenderLayer.getLines());
        mstack.pop();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static void drawBox(VertexConsumer vc, Matrix4f m, Matrix4f nm,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float r, float g, float b, float a) {
        // Bottom face
        line(vc, m, nm, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(vc, m, nm, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(vc, m, nm, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(vc, m, nm, x1, y1, z2, x1, y1, z1, r, g, b, a);
        // Top face
        line(vc, m, nm, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(vc, m, nm, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(vc, m, nm, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(vc, m, nm, x1, y2, z2, x1, y2, z1, r, g, b, a);
        // Vertical pillars
        line(vc, m, nm, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(vc, m, nm, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(vc, m, nm, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(vc, m, nm, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    /**
     * Emits a single line segment using the modern VertexConsumer API.
     * Each vertex carries position + color + a flat normal (required by RenderLayer.LINES).
     */
    private static void line(VertexConsumer vc, Matrix4f m, Matrix4f nm,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float r, float g, float b, float a) {
        // Direction vector for the normal (unit vector along the segment)
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;

        vc.vertex(m, x1, y1, z1).color(r, g, b, a).normal(nm, nx, ny, nz);
        vc.vertex(m, x2, y2, z2).color(r, g, b, a).normal(nm, nx, ny, nz);
    }
}
