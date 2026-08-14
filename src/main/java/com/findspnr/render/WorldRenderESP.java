package com.findspnr.render;

import com.findspnr.config.ModConfig;
import com.findspnr.tracker.SpawnerInfo;
import com.findspnr.tracker.SpawnerTracker;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

public class WorldRenderESP {

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
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        if (context.consumers() != null) {
            VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getLines());
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            for (SpawnerInfo spawner : spawners) {
                BlockPos pos = spawner.getPos();
                drawBoxOutline(consumer, matrix, pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, 1.0f, 0.1f, 0.1f, 0.9f);
                
                // Draw inner red center dot box
                double cx = pos.getX() + 0.5;
                double cy = pos.getY() + 0.5;
                double cz = pos.getZ() + 0.5;
                double s = 0.15;
                drawBoxOutline(consumer, matrix, cx - s, cy - s, cz - s, cx + s, cy + s, cz + s, 1.0f, 0.0f, 0.0f, 1.0f);
            }
        }

        matrices.pop();
    }

    private static void drawBoxOutline(VertexConsumer consumer, Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float r, float g, float b, float a) {
        float x1 = (float) minX;
        float y1 = (float) minY;
        float z1 = (float) minZ;
        float x2 = (float) maxX;
        float y2 = (float) maxY;
        float z2 = (float) maxZ;

        // Bottom square
        line(consumer, matrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(consumer, matrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(consumer, matrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(consumer, matrix, x1, y1, z2, x1, y1, z1, r, g, b, a);

        // Top square
        line(consumer, matrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(consumer, matrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(consumer, matrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(consumer, matrix, x1, y2, z2, x1, y2, z1, r, g, b, a);

        // Vertical pillars
        line(consumer, matrix, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(consumer, matrix, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(consumer, matrix, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(consumer, matrix, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void line(VertexConsumer consumer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
    }
}
