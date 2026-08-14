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
 *  1. A text summary in the top-left corner listing detected spawners.
 *  2. A circular radar widget in the top-right corner showing red dots for
 *     each spawner, player-yaw-corrected (like SeedCracker radar).
 */
public class HUDRadarRenderer {

    // ── Colours ────────────────────────────────────────────────────────────────
    private static final int COL_HEADER    = 0xFFFF4444; // bright red
    private static final int COL_ENTRY     = 0xFFFFFFFF;
    private static final int COL_EMPTY     = 0xFFAAAAAA;
    private static final int COL_DOT       = 0xFFFF1111; // spawner dot
    private static final int COL_DOT_RING  = 0xFF880000;
    private static final int COL_PLAYER    = 0xFF00FF44; // player dot
    private static final int COL_BG        = 0xBB000000;
    private static final int COL_BORDER    = 0xFF555555;
    private static final int COL_COMPASS   = 0xFFCCCCCC;

    // ── Public entry-point ─────────────────────────────────────────────────────

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!ModConfig.enabled || !ModConfig.renderHUDRadar) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        List<SpawnerInfo> spawners = SpawnerTracker.getDetectedSpawners();
        TextRenderer tr = mc.textRenderer;

        renderTextList(ctx, tr, spawners);
        renderRadar(ctx, mc, tr, spawners);
    }

    // ── Text summary (top-left) ────────────────────────────────────────────────

    private static void renderTextList(DrawContext ctx, TextRenderer tr, List<SpawnerInfo> spawners) {
        int x = 10;
        int y = 10;

        // Header
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

    // ── Circular radar widget (top-right) ─────────────────────────────────────

    private static void renderRadar(DrawContext ctx, MinecraftClient mc,
                                     TextRenderer tr, List<SpawnerInfo> spawners) {

        int screenW   = mc.getWindow().getScaledWidth();
        int RADIUS    = 40;                        // radar radius (px)
        int cx        = screenW - RADIUS - 16;     // centre X
        int cy        = RADIUS  + 16;              // centre Y

        // Background square
        ctx.fill(cx - RADIUS - 3, cy - RADIUS - 3,
                 cx + RADIUS + 3, cy + RADIUS + 3, COL_BG);
        ctx.drawBorder(cx - RADIUS - 3, cy - RADIUS - 3,
                       (RADIUS * 2) + 6, (RADIUS * 2) + 6, COL_BORDER);

        // Compass labels
        ctx.drawTextWithShadow(tr, "N", cx - 2,  cy - RADIUS + 2,  COL_COMPASS);
        ctx.drawTextWithShadow(tr, "S", cx - 2,  cy + RADIUS - 10, COL_COMPASS);
        ctx.drawTextWithShadow(tr, "W", cx - RADIUS + 2, cy - 3,   COL_COMPASS);
        ctx.drawTextWithShadow(tr, "E", cx + RADIUS - 6, cy - 3,   COL_COMPASS);

        // Player dot (green)
        ctx.fill(cx - 2, cy - 2, cx + 2, cy + 2, COL_PLAYER);

        if (spawners.isEmpty() || mc.player == null) return;

        Vec3d playerPos = mc.player.getPos();
        float  yaw      = mc.player.getYaw();
        double maxDist  = ModConfig.scanRadiusChunks * 16.0;

        for (SpawnerInfo spawner : spawners) {
            BlockPos p  = spawner.getPos();
            double   dx = p.getX() + 0.5 - playerPos.x;
            double   dz = p.getZ() + 0.5 - playerPos.z;

            // Rotate relative-offset to player yaw so N is always "forward"
            double rad  = Math.toRadians(-yaw);
            double rotX = dx * Math.cos(rad) - dz * Math.sin(rad);
            double rotZ = dx * Math.sin(rad) + dz * Math.cos(rad);

            // Map world distance → radar pixels
            double dist  = Math.sqrt(dx * dx + dz * dz);
            double ratio = Math.min(1.0, dist / maxDist);
            double angle = Math.atan2(rotZ, rotX);

            int dotX = cx + (int) (ratio * RADIUS * Math.cos(angle));
            int dotY = cy + (int) (ratio * RADIUS * Math.sin(angle));

            // Clamp inside radar circle boundary
            dotX = Math.max(cx - RADIUS, Math.min(cx + RADIUS, dotX));
            dotY = Math.max(cy - RADIUS, Math.min(cy + RADIUS, dotY));

            // Draw red dot (3×3) with dark border
            ctx.fill(dotX - 3, dotY - 3, dotX + 3, dotY + 3, COL_DOT);
            ctx.drawBorder(dotX - 3, dotY - 3, 6, 6, COL_DOT_RING);
        }
    }
}
