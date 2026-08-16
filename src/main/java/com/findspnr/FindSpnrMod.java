package com.findspnr;

import com.findspnr.config.ModConfig;
import com.findspnr.render.HUDRadarRenderer;
import com.findspnr.render.WorldRenderESP;
import com.findspnr.tracker.BaseInfo;
import com.findspnr.tracker.BaseTracker;
import com.findspnr.tracker.SpawnerInfo;
import com.findspnr.tracker.SpawnerTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * FindSpnr – Dungeon & Base Radar
 *
 * Client-only Fabric mod for Minecraft.
 * Scans loaded chunks for monster spawners (dungeons) and base treasure (Shulker Boxes & Ender Chests).
 *
 * Keybinding   : G  → toggle entire mod
 * Chat commands: /findspnr toggle | base | esp | radar | list
 */
public class FindSpnrMod implements ClientModInitializer {

    public static final String MOD_ID = "findspnr";
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        System.out.println("[FindSpnr] Initialising Dungeon & Base Radar...");

        // ── 1. Key binding (default G) ─────────────────────────────────────────
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.findspnr.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.findspnr.title"
        ));

        // ── 2. Tick listener ───────────────────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Handle toggle key press
            while (toggleKey.wasPressed()) {
                ModConfig.enabled = !ModConfig.enabled;
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("§c[FindSpnr] §f" +
                                    (ModConfig.enabled ? "§aENABLED ✔" : "§cDISABLED ✖")), true);
                }
            }
            // Run spawner & base scanners
            SpawnerTracker.tick(client);
            BaseTracker.tick(client);
        });

        // ── 3. Render hooks ────────────────────────────────────────────────────
        WorldRenderEvents.LAST.register(WorldRenderESP::render);
        HudRenderCallback.EVENT.register(HUDRadarRenderer::render);

        // ── 4. Clear cache on disconnect ───────────────────────────────────────
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SpawnerTracker.clear();
            BaseTracker.clear();
        });

        // ── 5. Chat commands ───────────────────────────────────────────────────
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("findspnr")
                        .then(ClientCommandManager.literal("toggle").executes(ctx -> {
                            ModConfig.enabled = !ModConfig.enabled;
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§c[FindSpnr] §fMod " + (ModConfig.enabled ? "§aENABLED" : "§cDISABLED")));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("base").executes(ctx -> {
                            ModConfig.renderBaseFinder = !ModConfig.renderBaseFinder;
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§c[FindSpnr] §fBase Finder (Shulker Box / Ender Chest) " +
                                            (ModConfig.renderBaseFinder ? "§aENABLED ✔" : "§cDISABLED ✖")));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("esp").executes(ctx -> {
                            ModConfig.renderWorldESP = !ModConfig.renderWorldESP;
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§c[FindSpnr] §f3-D ESP " + (ModConfig.renderWorldESP ? "§aON" : "§cOFF")));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("radar").executes(ctx -> {
                            ModConfig.renderHUDRadar = !ModConfig.renderHUDRadar;
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§c[FindSpnr] §fHUD radar " + (ModConfig.renderHUDRadar ? "§aON" : "§cOFF")));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("list").executes(ctx -> {
                            List<SpawnerInfo> spawners = SpawnerTracker.getDetectedSpawners();
                            List<BaseInfo> bases = BaseTracker.getDetectedBases();

                            if (spawners.isEmpty() && bases.isEmpty()) {
                                ctx.getSource().sendFeedback(Text.literal(
                                        "§c[FindSpnr] §7No spawners or base treasure detected in loaded chunks."));
                            } else {
                                if (!spawners.isEmpty()) {
                                    ctx.getSource().sendFeedback(Text.literal(
                                            "§c[FindSpnr] §aFound §e" + spawners.size() + " §aspawner(s):"));
                                    for (SpawnerInfo info : spawners) {
                                        BlockPos p = info.getPos();
                                        ctx.getSource().sendFeedback(Text.literal(String.format(
                                                " §e• %s §7(%.1fm) §8at [%d, %d, %d]",
                                                info.getFormattedName(), info.getDistance(),
                                                p.getX(), p.getY(), p.getZ())));
                                    }
                                }
                                if (!bases.isEmpty()) {
                                    ctx.getSource().sendFeedback(Text.literal(
                                            "§c[FindSpnr] §bFound §e" + bases.size() + " §bbase item(s):"));
                                    for (BaseInfo info : bases) {
                                        BlockPos p = info.getPos();
                                        ctx.getSource().sendFeedback(Text.literal(String.format(
                                                " §b• %s §7(%.1fm) §8at [%d, %d, %d]",
                                                info.getType(), info.getDistance(),
                                                p.getX(), p.getY(), p.getZ())));
                                    }
                                }
                            }
                            return 1;
                        }))
                )
        );

        System.out.println("[FindSpnr] Ready! Press G to toggle, use /findspnr base for Base Finder.");
    }
}
