package com.findspnr.render;

import com.findspnr.config.ModConfig;
import com.findspnr.tracker.BaseInfo;
import com.findspnr.tracker.BaseTracker;
import com.findspnr.tracker.BastionInfo;
import com.findspnr.tracker.BastionTracker;
import com.findspnr.tracker.FreecamController;
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
 * Free Fire / Tactical Style Minimap & HUD (1.21.11 Compatible):
 *  1. Top-left text summary listing detected spawners, Shulker Boxes, and Bastions.
 *  2. Middle-top player coordinate overlay (XYZ: X / Y / Z).
 *  3. Top-right minimap radar:
 *     • Red dots = Monster Spawners
 *     • Yellow dots = Shulker Boxes
 *     • Orange dots = Nether Bastions
 *     • Fixed North-Up orientation with rotating Green Player Arrow.
 */
public class HUDRadarRenderer {

    private static final int COL_HEADER     = 0xFFFF3333; // bright red
    private static final int COL_BASE_HEAD  = 0xFFFFD700; // yellow for shulkers
    private static final int COL_BAST_HEAD  = 0xFFFF9900; // orange
    private static final int COL_ENTRY      = 0xFFFFFFFF;
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
        List<BaseInfo> bases = ModConfig.renderBaseFinder ? BaseTracker.getDetectedBases() : List.of();
        List<BastionInfo> bastions = ModConfig.renderBastionFinder ? BastionTracker.getDetectedBastions() : List.of();

        TextRenderer tr = mc.textRenderer;

        renderTextList(ctx, tr, spawners, bases, bastions);
        renderMiddleTopCoords(ctx, mc, tr);
        renderRadar(ctx, mc, tr, spawners, bases, bastions);
    }

    private static void renderTextList(DrawContext ctx, TextRenderer tr, List<SpawnerInfo> spawners, List<BaseInfo> bases, List<BastionInfo> bastions) {
        int x = 10;
        int y = 10;

        String title;
        if (ModConfig.findSpawners && ModConfig.findDungeons) {
            title = "§c§l[FindSpnr] §fTargets: §e" + spawners.size();
        } else if (ModConfig.findSpawners) {
            title = "§c§l[FindSpnr] §fOnly Spawners: §e" + spawners.size();
        } else if (ModConfig.findDungeons) {
            title = "§c§l[FindSpnr] §fOnly Dungeons: §e" + spawners.size();
        } else {
            title = "§c§l[FindSpnr] §7(Filters OFF)";
        }
        ctx.drawTextWithShadow(tr, title, x, y, COL_HEADER);
        y += 12;

        if (!spawners.isEmpty()) {
            int limit = Math.min(spawners.size(), 4);
            for (int i = 0; i < limit; i++) {
                SpawnerInfo info = spawners.get(i);
                BlockPos p = info.getPos();
                String line = String.format("§e• %s §7(%.1fm) §8[%d/%d/%d]",
                        info.getFormattedName(), info.getDistance(), p.getX(), p.getY(), p.getZ());
                ctx.drawTextWithShadow(tr, line, x, y, COL_ENTRY);
                y += 10;
            }
        }

        if (ModConfig.renderBaseFinder) {
            y += 4;
            ctx.drawTextWithShadow(tr, "§e§l[Shulker Finder] §fTargets: §e" + bases.size(), x, y, COL_BASE_HEAD);
            y += 12;

            if (!bases.isEmpty()) {
                int limit = Math.min(bases.size(), 4);
                for (int i = 0; i < limit; i++) {
                    BaseInfo info = bases.get(i);
                    BlockPos p = info.getPos();
                    String line = String.format("§e• %s §7(%.1fm) §8[%d/%d/%d]",
                            info.getType(), info.getDistance(), p.getX(), p.getY(), p.getZ());
                    ctx.drawTextWithShadow(tr, line, x, y, COL_ENTRY);
                    y += 10;
                }
            }
        }

        if (ModConfig.renderBastionFinder) {
            y += 4;
            ctx.drawTextWithShadow(tr, "§6§l[Bastion Finder] §fTargets: §e" + bastions.size(), x, y, COL_BAST_HEAD);
            y += 12;

            if (!bastions.isEmpty()) {
                int limit = Math.min(bastions.size(), 4);
                for (int i = 0; i < limit; i++) {
                    BastionInfo info = bastions.get(i);
                    BlockPos p = info.getPos();
                    String line = String.format("§6• %s §7(%.1fm) §8[%d/%d/%d]",
                            info.getType(), info.getDistance(), p.getX(), p.getY(), p.getZ());
                    ctx.drawTextWithShadow(tr, line, x, y, COL_ENTRY);
                    y += 10;
                }
            }
        }

        if (ModConfig.freecamEnabled) {
            y += 4;
            ctx.drawTextWithShadow(tr, "§c§l[Freecam Mode Active] §7(Press K to Exit)", x, y, 0xFFFF3333);
        }
    }

    private static void renderMiddleTopCoords(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
        if (mc.player == null) return;

        BlockPos p = ModConfig.freecamEnabled ? BlockPos.ofFloored(FreecamController.getFreecamPos()) : mc.player.getBlockPos();
        String text = String.format("§7XYZ: §e%d §7/ §e%d §7/ §e%d %s",
                p.getX(), p.getY(), p.getZ(), ModConfig.freecamEnabled ? "§c(Freecam)" : "");
        int textWidth = tr.getWidth(text);
        int screenW = mc.getWindow().getScaledWidth();

        int x = (screenW - textWidth) / 2;
        int y = 10;

        ctx.fill(x - 6, y - 3, x + textWidth + 6, y + 11, 0xAA000000);
        drawBorder(ctx, x - 6, y - 3, textWidth + 12, 14, 0xFF555555);

        ctx.drawTextWithShadow(tr, text, x, y, 0xFFFFFFFF);
    }

    private static void renderRadar(DrawContext ctx, MinecraftClient mc, TextRenderer tr, List<SpawnerInfo> spawners, List<BaseInfo> bases, List<BastionInfo> bastions) {
        int screenW = mc.getWindow().getScaledWidth();
        int RADIUS = 44;                             // radar radius (px)
        int cx = screenW - RADIUS - 18;             // center X
        int cy = RADIUS + 18;                        // center Y

        // 1. Draw background box
        ctx.fill(cx - RADIUS - 3, cy - RADIUS - 3, cx + RADIUS + 3, cy + RADIUS + 3, COL_BG);
        drawBorder(ctx, cx - RADIUS - 3, cy - RADIUS - 3, (RADIUS * 2) + 6, (RADIUS * 2) + 6, COL_BORDER);

        // 2. Draw crosshair grid
        ctx.fill(cx, cy - RADIUS, cx + 1, cy + RADIUS, 0x33FFFFFF);
        ctx.fill(cx - RADIUS, cy, cx + RADIUS, cy + 1, 0x33FFFFFF);

        // 3. Fixed Compass labels (North is ALWAYS UP!)
        ctx.drawTextWithShadow(tr, "N", cx - 2, cy - RADIUS + 2, COL_COMPASS);
        ctx.drawTextWithShadow(tr, "S", cx - 2, cy + RADIUS - 10, COL_COMPASS);
        ctx.drawTextWithShadow(tr, "W", cx - RADIUS + 2, cy - 3, COL_COMPASS);
        ctx.drawTextWithShadow(tr, "E", cx + RADIUS - 6, cy - 3, COL_COMPASS);

        if (mc.player == null) return;

        Vec3d playerPos = ModConfig.freecamEnabled ? FreecamController.getFreecamPos() : new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        double maxDist = ModConfig.scanRadiusChunks * 16.0;
        double scale = RADIUS / maxDist;

        // 4. Draw Spawner Red Dots
        for (SpawnerInfo spawner : spawners) {
            BlockPos p = spawner.getPos();

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
            int dotY = cy + (int) projZ;

            dotX = Math.max(cx - RADIUS + 2, Math.min(cx + RADIUS - 2, dotX));
            dotY = Math.max(cy - RADIUS + 2, Math.min(cy + RADIUS - 2, dotY));

            ctx.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, COL_DOT);
            drawBorder(ctx, dotX - 2, dotY - 2, 4, 4, COL_DOT_BORDER);
        }

        // 5. Draw Base Finder Dots (Yellow = Shulker Boxes)
        if (ModConfig.renderBaseFinder) {
            for (BaseInfo base : bases) {
                BlockPos p = base.getPos();

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
                int dotY = cy + (int) projZ;

                dotX = Math.max(cx - RADIUS + 2, Math.min(cx + RADIUS - 2, dotX));
                dotY = Math.max(cy - RADIUS + 2, Math.min(cy + RADIUS - 2, dotY));

                int dotColor = 0xFFFFD700; // Bright Gold/Yellow
                ctx.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, dotColor);
                drawBorder(ctx, dotX - 2, dotY - 2, 4, 4, COL_DOT_BORDER);
            }
        }

        // 6. Draw Bastion Dots (Orange)
        if (ModConfig.renderBastionFinder) {
            for (BastionInfo bastion : bastions) {
                BlockPos p = bastion.getPos();

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
                int dotY = cy + (int) projZ;

                dotX = Math.max(cx - RADIUS + 2, Math.min(cx + RADIUS - 2, dotX));
                dotY = Math.max(cy - RADIUS + 2, Math.min(cy + RADIUS - 2, dotY));

                ctx.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, 0xFFFF9900);
                drawBorder(ctx, dotX - 2, dotY - 2, 4, 4, COL_DOT_BORDER);
            }
        }

        // 7. Draw Player Rotating Green Arrow
        float yaw = ModConfig.freecamEnabled ? FreecamController.getFreecamYaw() : mc.player.getYaw();
        renderPlayerArrow(ctx, cx, cy, yaw);
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int width, int height, int color) {
        ctx.fill(x, y, x + width, y + 1, color);
        ctx.fill(x, y + height - 1, x + width, y + height, color);
        ctx.fill(x, y + 1, x + 1, y + height - 1, color);
        ctx.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    private static void renderPlayerArrow(DrawContext ctx, int cx, int cy, float yaw) {
        double rad = Math.toRadians(yaw);

        double fwdX = -Math.sin(rad);
        double fwdY = Math.cos(rad);

        int tipX = cx + (int) (fwdX * 7);
        int tipY = cy + (int) (fwdY * 7);

        int leftX = cx + (int) (-fwdX * 3 + fwdY * 3);
        int leftY = cy + (int) (-fwdY * 3 - fwdX * 3);

        int rightX = cx + (int) (-fwdX * 3 - fwdY * 3);
        int rightY = cy + (int) (-fwdY * 3 + fwdX * 3);

        ctx.fill(cx - 2, cy - 2, cx + 2, cy + 2, COL_PLAYER);
        ctx.fill(tipX - 1, tipY - 1, tipX + 1, tipY + 1, COL_PLAYER);
        ctx.fill(leftX - 1, leftY - 1, leftX + 1, leftY + 1, COL_PLAYER);
        ctx.fill(rightX - 1, rightY - 1, rightX + 1, rightY + 1, COL_PLAYER);

        ctx.fill(Math.min(cx, tipX), Math.min(cy, tipY), Math.max(cx, tipX) + 1, Math.max(cy, tipY) + 1, COL_PLAYER);
    }
}
