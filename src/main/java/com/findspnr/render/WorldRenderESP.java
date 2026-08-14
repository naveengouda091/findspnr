package com.findspnr.render;

import com.mojang.blaze3d.systems.RenderSystem;
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
 * Draws a glowing red 3-D bounding-box outline around every detected spawner,
 * visible through walls (depth-test disabled) so you can spot them at range.
 */
public class WorldRenderESP {

    public static void render(WorldRenderContext ctx) {
        if (!ModConfig.enabled || !ModConfig.renderWorldESP) return;

        List<SpawnerInfo> spawners = SpawnerTracker.getDetectedSpawners();
        if (spawners.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Vec3d        camera  = ctx.camera().getPos();
        MatrixStack  mstack  = ctx.matrixStack();

        mstack.push();
        mstack.translate(-camera.x, -camera.y, -camera.z);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator   tess   = Tessellator.getInstance();
        BufferBuilder buf    = tess.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        Matrix4f      matrix = mstack.peek().getPositionMatrix();

        for (SpawnerInfo spawner : spawners) {
            BlockPos p = spawner.getPos();

            // Outer box – semi-transparent bright red outline
            drawBox(buf, matrix,
                    p.getX(),       p.getY(),       p.getZ(),
                    p.getX() + 1f,  p.getY() + 1f,  p.getZ() + 1f,
                    1.0f, 0.1f, 0.1f, 0.85f);

            // Inner "core" box – solid red 0.15-block dot in the centre
            float s  = 0.15f;
            float bx = p.getX() + 0.5f;
            float by = p.getY() + 0.5f;
            float bz = p.getZ() + 0.5f;
            drawBox(buf, matrix,
                    bx - s, by - s, bz - s,
                    bx + s, by + s, bz + s,
                    1.0f, 0.0f, 0.0f, 1.0f);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        mstack.pop();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static void drawBox(BufferBuilder buf, Matrix4f m,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float r, float g, float b, float a) {
        // Bottom face
        line(buf, m, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(buf, m, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(buf, m, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(buf, m, x1, y1, z2, x1, y1, z1, r, g, b, a);
        // Top face
        line(buf, m, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(buf, m, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(buf, m, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(buf, m, x1, y2, z2, x1, y2, z1, r, g, b, a);
        // Vertical pillars
        line(buf, m, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(buf, m, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(buf, m, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(buf, m, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void line(BufferBuilder buf, Matrix4f m,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float r, float g, float b, float a) {
        buf.vertex(m, x1, y1, z1).color(r, g, b, a);
        buf.vertex(m, x2, y2, z2).color(r, g, b, a);
    }
}
