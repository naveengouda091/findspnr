package com.findspnr.render;

import com.findspnr.config.ModConfig;
import com.findspnr.tracker.BaseInfo;
import com.findspnr.tracker.BaseTracker;
import com.findspnr.tracker.BastionInfo;
import com.findspnr.tracker.BastionTracker;
import com.findspnr.tracker.FreecamController;
import com.findspnr.tracker.SpawnerInfo;
import com.findspnr.tracker.SpawnerTracker;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * 3D World ESP & Tracer Lines Renderer for Minecraft 1.21.11:
 *  1. Renders thin tracer threads connecting camera crosshair directly to target blocks.
 *  2. Renders 3D bounding box outlines around targets:
 *     • Red = Monster Spawners
 *     • Yellow = Shulker Boxes
 *     • Orange = Nether Bastion Remnants
 *  3. Uses RenderLayers.LINES directly via WorldRenderContext with 1.21.11 lineWidth support.
 */
public class WorldRenderESP {

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

        Vec3d cameraPos = ModConfig.freecamEnabled ? FreecamController.getFreecamPos() : client.player.getEyePos();
        MatrixStack matrices = context.matrices();
        VertexConsumer buffer = context.consumers().getBuffer(RenderLayers.LINES);

        matrices.push();
        MatrixStack.Entry entry = matrices.peek();

        // 1. Render Spawner Targets (Bright Red)
        for (SpawnerInfo spawner : spawners) {
            BlockPos pos = spawner.getPos();

            double targetX = pos.getX() + 0.5 - cameraPos.x;
            double targetY = pos.getY() + 0.5 - cameraPos.y;
            double targetZ = pos.getZ() + 0.5 - cameraPos.z;

            line(buffer, entry, 0f, 0f, 0f, (float) targetX, (float) targetY, (float) targetZ, 1.0f, 0.0f, 0.0f, 1.0f);

            drawBoxOutline(buffer, entry, targetX - 0.5, targetY - 0.5, targetZ - 0.5,
                           targetX + 0.5, targetY + 0.5, targetZ + 0.5, 1.0f, 0.0f, 0.0f, 1.0f);

            double s = 0.15;
            drawBoxOutline(buffer, entry, targetX - s, targetY - s, targetZ - s,
                           targetX + s, targetY + s, targetZ + s, 1.0f, 0.8f, 0.0f, 1.0f);
        }

        // 2. Render Base Targets (Shulker Box = Bright Yellow)
        if (ModConfig.renderBaseFinder) {
            for (BaseInfo base : bases) {
                BlockPos pos = base.getPos();

                double targetX = pos.getX() + 0.5 - cameraPos.x;
                double targetY = pos.getY() + 0.5 - cameraPos.y;
                double targetZ = pos.getZ() + 0.5 - cameraPos.z;

                float r = 1.0f;
                float g = 0.85f;
                float b = 0.0f;

                line(buffer, entry, 0f, 0f, 0f, (float) targetX, (float) targetY, (float) targetZ, r, g, b, 1.0f);

                drawBoxOutline(buffer, entry, targetX - 0.5, targetY - 0.5, targetZ - 0.5,
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

                line(buffer, entry, 0f, 0f, 0f, (float) targetX, (float) targetY, (float) targetZ, r, g, b, 1.0f);

                drawBoxOutline(buffer, entry, targetX - 1.5, targetY - 1.5, targetZ - 1.5,
                               targetX + 1.5, targetY + 1.5, targetZ + 1.5, r, g, b, 1.0f);
            }
        }

        matrices.pop();
    }

    private static void drawBoxOutline(VertexConsumer builder, MatrixStack.Entry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float r, float g, float b, float a) {
        float x1 = (float) minX;
        float y1 = (float) minY;
        float z1 = (float) minZ;
        float x2 = (float) maxX;
        float y2 = (float) maxY;
        float z2 = (float) maxZ;

        // Bottom square
        line(builder, entry, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(builder, entry, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(builder, entry, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(builder, entry, x1, y1, z2, x1, y1, z1, r, g, b, a);

        // Top square
        line(builder, entry, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(builder, entry, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(builder, entry, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(builder, entry, x1, y2, z2, x1, y2, z1, r, g, b, a);

        // Vertical pillars
        line(builder, entry, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(builder, entry, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(builder, entry, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(builder, entry, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void line(VertexConsumer builder, MatrixStack.Entry entry, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = len > 0 ? dx / len : 0f;
        float ny = len > 0 ? dy / len : 1f;
        float nz = len > 0 ? dz / len : 0f;

        builder.vertex(entry, x1, y1, z1).color(r, g, b, a).normal(entry, nx, ny, nz).lineWidth(2.0f);
        builder.vertex(entry, x2, y2, z2).color(r, g, b, a).normal(entry, nx, ny, nz).lineWidth(2.0f);
    }
}
