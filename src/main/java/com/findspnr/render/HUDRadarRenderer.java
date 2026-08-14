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
 * Free Fire / Tactical Style Minimap:
 *  - Fixed North-Up Map (North is always UP, never spins around).
 *  - Player represented by a rotating Green Directional Arrow at the center.
 *  - Spawners shown as bright Red Dots (with white border) moving smoothly as you walk.
 *  - Top-left text overlay showing nearest spawner coordinates and distance.
 */
public class HUDRadarRenderer {

    private static final int COL_HEADER     = 0xFFFF3333; // bright red
    private static final int COL_ENTRY      = 0xFFFFFFFF;
    private static final int COL_EMPTY      = 0xFFAAAAAA;
    private static final int COL_DOT        = 0xFFFF0000; // bright spawner red dot
    private static final int COL_DOT_BORDER = 0xFFFFFFFF; // white outline
    private static final int COL_PLAYER     = 0xFF00FF00; // green player arrow
    private static final int COL_BG         = 0xCC000000; // dark background
    private static final int COL_BORDER     = 0xFF888888;
    private static final int COL_COMPASS    = 0xFFFFD700; // gold compass

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
        int RADIUS = 44;                             // radar radius (px)
        int cx = screenW - RADIUS - 18;             // center X
        int cy = RADIUS + 18;                        // center Y

        // 1. Draw background box
        ctx.fill(cx - RADIUS - 3, cy - RADIUS - 3, cx + RADIUS + 3, cy + RADIUS + 3, COL_BG);
        ctx.drawBorder(cx - RADIUS - 3, cy - RADIUS - 3, (RADIUS * 2) + 6, (RADIUS * 2) + 6, COL_BORDER);

        // 2. Draw crosshair grid
        ctx.fill(cx, cy - RADIUS, cx + 1, cy + RADIUS, 0x33FFFFFF);
        ctx.fill(cx - RADIUS, cy, cx + RADIUS, cy + 1, 0x33FFFFFF);

        // 3. Fixed Compass labels (North is ALWAYS UP!)
        ctx.drawTextWithShadow(tr, "N", cx - 2, cy - RADIUS + 2, COL_COMPASS);
        ctx.drawTextWithShadow(tr, "S", cx - 2, cy + RADIUS - 10, COL_COMPASS);
        ctx.drawTextWithShadow(tr, "W", cx - RADIUS + 2, cy - 3, COL_COMPASS);
        ctx.drawTextWithShadow(tr, "E", cx + RADIUS - 6, cy - 3, COL_COMPASS);

        if (mc.player == null) return;

        Vec3d playerPos = mc.player.getPos();
        double maxDist = ModConfig.scanRadiusChunks * 16.0;
        double scale = RADIUS / maxDist;

        // 4. Draw Spawner Red Dots (North-Up fixed map projection)
        for (SpawnerInfo spawner : spawners) {
            BlockPos p = spawner.getPos();

            // World offset: +X = East (Right), +Z = South (Down)
            double dx = (p.getX() + 0.5) - playerPos.x;
            double dz = (p.getZ() + 0.5) - playerPos.z;

            double projX = dx * scale;
            double projZ = dz * scale;

            double dist = Math.sqrt(projX * projX + projZ * projZ);
            if (dist > RADIUS) {
                double factor = RADIUS / dist;
                projX *= factor;
                projZ *= factor;
            }

            int dotX = cx + (int) projX;
            int dotY = cy + (int) projZ; // +Z is South (Down on screen)

            // Clamp inside minimap box
            dotX = Math.max(cx - RADIUS + 2, Math.min(cx + RADIUS - 2, dotX));
            dotY = Math.max(cy - RADIUS + 2, Math.min(cy + RADIUS - 2, dotY));

            // Draw bright red spawner dot (4x4 square with white outline)
            ctx.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, COL_DOT);
            ctx.drawBorder(dotX - 2, dotY - 2, 4, 4, COL_DOT_BORDER);
        }

        // 5. Draw Player Rotating Green Arrow at center (Free Fire style)
        float yaw = mc.player.getYaw();
        renderPlayerArrow(ctx, cx, cy, yaw);
    }

    /**
     * Renders a crisp green directional player arrow pointing in the direction of the player's yaw.
     */
    private static void renderPlayerArrow(DrawContext ctx, int cx, int cy, float yaw) {
        // In MC: 0 = South (+Z, Down), 90 = West (-X, Left), 180 = North (-Z, Up), 270 = East (+X, Right)
        double rad = Math.toRadians(yaw);

        // Forward unit vector
        double fwdX = -Math.sin(rad); // East/West
        double fwdY = Math.cos(rad);  // South/North

        // Tip of the arrow
        int tipX = cx + (int) (fwdX * 7);
        int tipY = cy + (int) (fwdY * 7);

        // Left base corner
        int leftX = cx + (int) (-fwdX * 3 + fwdY * 3);
        int leftY = cy + (int) (-fwdY * 3 - fwdX * 3);

        // Right base corner
        int rightX = cx + (int) (-fwdX * 3 - fwdY * 3);
        int rightY = cy + (int) (-fwdY * 3 + fwdX * 3);

        // Draw arrow body & lines (Green player indicator)
        ctx.fill(cx - 2, cy - 2, cx + 2, cy + 2, COL_PLAYER);
        ctx.fill(tipX - 1, tipY - 1, tipX + 1, tipY + 1, COL_PLAYER);
        ctx.fill(leftX - 1, leftY - 1, leftX + 1, leftY + 1, COL_PLAYER);
        ctx.fill(rightX - 1, rightY - 1, rightX + 1, rightY + 1, COL_PLAYER);

        // Pointer line from center to tip
        ctx.fill(Math.min(cx, tipX), Math.min(cy, tipY), Math.max(cx, tipX) + 1, Math.max(cy, tipY) + 1, COL_PLAYER);
    }
}
