package com.findspnr;

import com.findspnr.config.ModConfig;
import com.findspnr.render.HUDRadarRenderer;
import com.findspnr.render.WorldRenderESP;
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
 * FindSpnr – Dungeon Radar
 *
 * Client-only Fabric mod for Minecraft 26.2 "Chaos Cubed".
 * Scans loaded chunks for monster spawners (dungeons) and renders:
 *   • A HUD radar with red dots (top-right)
 *   • 3-D glowing outlines in the world (through walls)
 *   • A text list of the nearest spawners (top-left)
 *
 * Keybinding   : G  → toggle entire mod
 * Chat commands: /findspnr toggle | esp | radar | list
 */
public class FindSpnrMod implements ClientModInitializer {

    public static final String MOD_ID = "findspnr";
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        System.out.println("[FindSpnr] Initialising Dungeon Radar...");

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
            // Run spawner scanner
            SpawnerTracker.tick(client);
        });

        // ── 3. Render hooks ────────────────────────────────────────────────────
        WorldRenderEvents.AFTER_TRANSLUCENT.register(WorldRenderESP::render);
        HudRenderCallback.EVENT.register(HUDRadarRenderer::render);

        // ── 4. Clear cache on disconnect ───────────────────────────────────────
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SpawnerTracker.clear());

        // ── 5. Chat commands ───────────────────────────────────────────────────
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("findspnr")
                        .then(ClientCommandManager.literal("toggle").executes(ctx -> {
                            ModConfig.enabled = !ModConfig.enabled;
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§c[FindSpnr] §fMod " + (ModConfig.enabled ? "§aENABLED" : "§cDISABLED")));
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
                            List<SpawnerInfo> list = SpawnerTracker.getDetectedSpawners();
                            if (list.isEmpty()) {
                                ctx.getSource().sendFeedback(Text.literal(
                                        "§c[FindSpnr] §7No spawners detected in loaded chunks."));
                            } else {
                                ctx.getSource().sendFeedback(Text.literal(
                                        "§c[FindSpnr] §aFound §e" + list.size() + " §aspawner(s):"));
                                for (SpawnerInfo info : list) {
                                    BlockPos p = info.getPos();
                                    ctx.getSource().sendFeedback(Text.literal(String.format(
                                            " §e• %s §7(%.1fm) §8at [%d, %d, %d]",
                                            info.getFormattedName(), info.getDistance(),
                                            p.getX(), p.getY(), p.getZ())));
                                }
                            }
                            return 1;
                        }))
                )
        );

        System.out.println("[FindSpnr] Ready! Press G to toggle.");
    }
}
