package com.findspnr.render;

import com.findspnr.config.ModConfig;
import com.findspnr.tracker.SpawnerInfo;
import com.findspnr.tracker.SpawnerTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Renders two HUD elements:
 *  1. Top-left text summary listing detected spawners with coords and distance.
 *  2. Top-right minimap radar widget showing player position (green dot)
 *     and all nearby spawners (bright red dots) projected relative to player yaw (SeedCracker style).
 */
public class HUDRadarRenderer {

    private static final int COL_HEADER    = 0xFFFF3333; // bright red
    private static final int COL_ENTRY     = 0xFFFFFFFF;
    private static final int COL_EMPTY     = 0xFFAAAAAA;
    private static final int COL_DOT       = 0xFFFF0000; // bright spawner red dot
    private static final int COL_DOT_BORDER= 0xFFFFFFFF; // white outline for dot visibility
    private static final int COL_PLAYER    = 0xFF00FF00; // green player dot
    private static final int COL_BG        = 0xCC000000; // dark background
    private static final int COL_BORDER    = 0xFF888888;
    private static final int COL_COMPASS   = 0xFFFFD700; // gold compass

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!ModConfig.enabled || !ModConfig.renderHUDRadar) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        List<SpawnerInfo> spawners = SpawnerTracker.getDetectedSpawners();
        TextRenderer tr = mc.textRenderer;

        renderTextList(ctx, tr, spawners);
        renderRadar(ctx, mc, tr, spawners);
    }

    private static void renderTextList(DrawContext ctx, TextRenderer tr, List<SpawnerInfo> spawners) {
        int x = 10;
        int y = 10;

        ctx.drawTextWithShadow(tr, "§c§l[FindSpnr] §fDetected: §e" + spawners.size(), x, y, COL_HEADER);
        y += 13;

        if (spawners.isEmpty()) {
            ctx.drawTextWithShadow(tr, "§7No spawners in loaded chunks", x, y, COL_EMPTY);
            return;
        }

        int limit = Math.min(spawners.size(), 6);
        for (int i = 0; i < limit; i++) {
            SpawnerInfo info = spawners.get(i);
            BlockPos p = info.getPos();
            String line = String.format("§e• %s §7(%.1fm) §8[%d/%d/%d]",
                    info.getFormattedName(), info.getDistance(), p.getX(), p.getY(), p.getZ());
            ctx.drawTextWithShadow(tr, line, x, y, COL_ENTRY);
            y += 10;
        }

        if (spawners.size() > limit) {
            ctx.drawTextWithShadow(tr,
                    "§8  … and " + (spawners.size() - limit) + " more", x, y, COL_EMPTY);
        }
    }

    private static void renderRadar(DrawContext ctx, MinecraftClient mc, TextRenderer tr, List<SpawnerInfo> spawners) {
        int screenW = mc.getWindow().getScaledWidth();
        int RADIUS = 42;                             // radar radius (px)
        int cx = screenW - RADIUS - 18;             // center X
        int cy = RADIUS + 18;                        // center Y

        // Draw background box
        ctx.fill(cx - RADIUS - 3, cy - RADIUS - 3, cx + RADIUS + 3, cy + RADIUS + 3, COL_BG);
        ctx.drawBorder(cx - RADIUS - 3, cy - RADIUS - 3, (RADIUS * 2) + 6, (RADIUS * 2) + 6, COL_BORDER);

        // Draw crosshair axes
        ctx.fill(cx, cy - RADIUS, cx + 1, cy + RADIUS, 0x44FFFFFF);
        ctx.fill(cx - RADIUS, cy, cx + RADIUS, cy + 1, 0x44FFFFFF);

        // Draw Compass N, S, W, E labels
        ctx.drawTextWithShadow(tr, "N", cx - 2, cy - RADIUS + 2, COL_COMPASS);
        ctx.drawTextWithShadow(tr, "S", cx - 2, cy + RADIUS - 10, COL_COMPASS);
        ctx.drawTextWithShadow(tr, "W", cx - RADIUS + 2, cy - 3, COL_COMPASS);
        ctx.drawTextWithShadow(tr, "E", cx + RADIUS - 6, cy - 3, COL_COMPASS);

        // Draw player dot in center (Green 3x3)
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, COL_PLAYER);

        if (mc.player == null) return;

        Vec3d playerPos = mc.player.getPos();
        float yaw = mc.player.getYaw();
        double maxDist = ModConfig.scanRadiusChunks * 16.0;

        for (SpawnerInfo spawner : spawners) {
            BlockPos p = spawner.getPos();
            double dx = p.getX() + 0.5 - playerPos.x;
            double dz = p.getZ() + 0.5 - playerPos.z;

            // Convert world offset (dx, dz) to player-heading relative offset
            // MC yaw: 0 = South (+Z), 90 = West (-X), 180 = North (-Z), 270 = East (+X)
            double rad = Math.toRadians(yaw);
            double rotX = dx * Math.cos(rad) - dz * Math.sin(rad);
            double rotZ = dx * Math.sin(rad) + dz * Math.cos(rad);

            // Scale distance to radar bounds
            double dist = Math.sqrt(rotX * rotX + rotZ * rotZ);
            double scale = RADIUS / maxDist;
            
            double projX = rotX * scale;
            double projZ = rotZ * scale;

            // Clamp inside radar radius
            if (dist > maxDist) {
                double factor = maxDist / dist;
                projX *= factor;
                projZ *= factor;
            }

            int dotX = cx + (int) projX;
            int dotY = cy + (int) projZ;

            // Ensure dot stays strictly inside radar window
            dotX = Math.max(cx - RADIUS + 2, Math.min(cx + RADIUS - 2, dotX));
            dotY = Math.max(cy - RADIUS + 2, Math.min(cy + RADIUS - 2, dotY));

            // Draw bright red spawner dot (4x4 square with white outline for maximum contrast)
            ctx.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, COL_DOT);
            ctx.drawBorder(dotX - 2, dotY - 2, 4, 4, COL_DOT_BORDER);
        }
    }
}
