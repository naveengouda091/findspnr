package com.findspnr.tracker;

import com.findspnr.config.ModConfig;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scans loaded chunks every 10 ticks (~0.5 s) for MobSpawnerBlockEntities
 * and keeps a live {@link ConcurrentHashMap} of what was found.
 */
public class SpawnerTracker {

    private static final ConcurrentHashMap<BlockPos, SpawnerInfo> detected = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    // ── Called from the tick event ─────────────────────────────────────────────

    public static void tick(MinecraftClient client) {
        if (!ModConfig.enabled || client.world == null || client.player == null) return;

        tickCounter++;

        // Full chunk scan every 10 ticks
        if (tickCounter % 10 == 0) {
            scanChunks(client.world, client.player.getPos());
        }

        // Update distances every tick so the radar is smooth
        Vec3d playerPos = client.player.getPos();
        for (SpawnerInfo info : detected.values()) {
            info.updateDistance(playerPos);
        }
    }

    // ── Chunk scanner ──────────────────────────────────────────────────────────

    private static void scanChunks(ClientWorld world, Vec3d playerPos) {
        ChunkPos playerChunk = new ChunkPos(BlockPos.ofFloored(playerPos));
        int radius = ModConfig.scanRadiusChunks;

        // Remove stale entries (block is no longer a spawner in a loaded chunk)
        detected.entrySet().removeIf(entry -> {
            BlockPos p = entry.getKey();
            // Keep if chunk is unloaded – avoids false negatives on boundary
            if (!world.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) return false;
            return world.getBlockState(p).getBlock() != Blocks.SPAWNER;
        });

        // Scan surrounding loaded chunks
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = playerChunk.x + dx;
                int cz = playerChunk.z + dz;

                if (!world.isChunkLoaded(cx, cz)) continue;

                WorldChunk chunk = world.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof MobSpawnerBlockEntity spawnerBe)) continue;

                    BlockPos pos = spawnerBe.getPos();
                    String entityType = "unknown";

                    // Try to read what entity the spawner contains
                    try {
                        var logic = spawnerBe.getLogic();
                        if (logic != null) {
                            var entity = logic.getRenderedEntity(world, pos);
                            if (entity != null) {
                                entityType = entity.getType().getUntranslatedName();
                            }
                        }
                    } catch (Exception ignored) {}

                    SpawnerInfo info = new SpawnerInfo(pos, entityType);
                    info.updateDistance(playerPos);
                    detected.put(pos, info);
                }
            }
        }
    }

    // ── Public accessors ───────────────────────────────────────────────────────

    /** Returns all detected spawners sorted by distance (closest first). */
    public static List<SpawnerInfo> getDetectedSpawners() {
        List<SpawnerInfo> list = new ArrayList<>(detected.values());
        list.sort(Comparator.comparingDouble(SpawnerInfo::getDistance));
        return Collections.unmodifiableList(list);
    }

    /** Wipes all cached spawners (e.g. on world disconnect). */
    public static void clear() {
        detected.clear();
        tickCounter = 0;
    }
}
