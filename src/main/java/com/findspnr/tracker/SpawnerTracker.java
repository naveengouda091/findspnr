package com.findspnr.tracker;

import com.findspnr.config.ModConfig;
import net.minecraft.block.BlockState;
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
 * Dual-Mode Spawner & Dungeon Tracker:
 *
 * Mode 1: Direct MobSpawnerBlockEntity & BlockState scanner (Singleplayer & Vanilla servers).
 * Mode 2: SeedCracker Dungeon Structure Detector (bypasses Paper/Spigot Anti-Xray Engine Mode 2).
 *
 * Feature: Remembers destroyed/mined spawners so once you destroy a dungeon,
 * it disappears FOREVER and never shows up on your radar again!
 */
public class SpawnerTracker {

    private static final ConcurrentHashMap<BlockPos, SpawnerInfo> detected = new ConcurrentHashMap<>();
    private static final Set<BlockPos> destroyedSpawners = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static int tickCounter = 0;

    public static void tick(MinecraftClient client) {
        if (!ModConfig.enabled || client.world == null || client.player == null) return;

        tickCounter++;

        // Full scan every 10 ticks (~0.5s)
        if (tickCounter % 10 == 0) {
            scanChunks(client.world, client.player.getPos());
            mergeDuplicates();
        }

        // Update distances every tick for smooth radar rendering
        Vec3d playerPos = client.player.getPos();
        for (SpawnerInfo info : detected.values()) {
            info.updateDistance(playerPos);
        }
    }

    private static void scanChunks(ClientWorld world, Vec3d playerPos) {
        ChunkPos playerChunk = new ChunkPos(BlockPos.ofFloored(playerPos));
        int radius = ModConfig.scanRadiusChunks;

        // Clean up destroyed / mined spawners
        detected.entrySet().removeIf(entry -> {
            BlockPos p = entry.getKey();
            if (!world.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) return false;
            
            BlockState state = world.getBlockState(p);
            // If the block is now air or no longer a spawner/dungeon floor, mark as destroyed!
            if (state.isAir()) {
                destroyedSpawners.add(p);
                return true; // Remove from radar immediately
            }
            return false;
        });

        // Scan surrounding loaded chunks
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = playerChunk.x + dx;
                int cz = playerChunk.z + dz;

                if (!world.isChunkLoaded(cx, cz)) continue;

                WorldChunk chunk = world.getChunk(cx, cz);

                // ── METHOD 1: Direct Block Entity Scan ─────────────────────────────────
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof MobSpawnerBlockEntity spawnerBe) {
                        BlockPos pos = spawnerBe.getPos();
                        if (destroyedSpawners.contains(pos)) continue;

                        String entityType = "unknown";
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

                        removeNearbyApproximate(pos);
                        detected.put(pos, info);
                    }
                }

                // ── METHOD 2 & 3: Anti-Xray Block & Dungeon Floor Structure Scan ─────
                int startX = chunk.getPos().getStartX();
                int startZ = chunk.getPos().getStartZ();

                for (int y = -64; y <= 128; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                            if (destroyedSpawners.contains(pos)) continue;

                            BlockState state = world.getBlockState(pos);

                            if (state.isOf(Blocks.SPAWNER)) {
                                removeNearbyApproximate(pos);
                                SpawnerInfo info = new SpawnerInfo(pos, "Monster");
                                info.updateDistance(playerPos);
                                detected.putIfAbsent(pos, info);
                            } else if (state.isOf(Blocks.MOSSY_COBBLESTONE)) {
                                checkDungeonFloor(world, pos, playerPos);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * SeedCracker Dungeon Structure Detector:
     * Checks if a mossy cobblestone block belongs to an underground dungeon floor.
     * Skips any dungeon location that has been destroyed/mined by the player.
     */
    private static void checkDungeonFloor(ClientWorld world, BlockPos mossyPos, Vec3d playerPos) {
        int y = mossyPos.getY();
        if (y > 128 || y < -64) return;

        BlockPos spawnerPos = new BlockPos(mossyPos.getX(), y + 1, mossyPos.getZ());
        if (destroyedSpawners.contains(spawnerPos) || destroyedSpawners.contains(mossyPos)) return;

        // If the spawner position itself is already Air, do NOT add as new dungeon!
        BlockState above = world.getBlockState(spawnerPos);
        if (above.isAir()) {
            // Check if there is an actual spawner block nearby
            boolean hasSpawner = false;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (world.getBlockState(new BlockPos(mossyPos.getX() + dx, y + 1, mossyPos.getZ() + dz)).isOf(Blocks.SPAWNER)) {
                        hasSpawner = true;
                        break;
                    }
                }
            }
            if (!hasSpawner) {
                // If spawner is missing/air, do not add fake dungeon entry
                return;
            }
        }

        int cobbleCount = 0;
        int mossyCount = 0;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockState s = world.getBlockState(new BlockPos(mossyPos.getX() + dx, y, mossyPos.getZ() + dz));
                if (s.isOf(Blocks.MOSSY_COBBLESTONE)) mossyCount++;
                else if (s.isOf(Blocks.COBBLESTONE)) cobbleCount++;
            }
        }

        // Dungeon floor requires 8+ cobblestone/mossy blocks in 5x5 floor area
        if (cobbleCount + mossyCount >= 8 && mossyCount >= 1) {
            boolean isDuplicate = false;
            for (BlockPos existing : detected.keySet()) {
                if (Math.abs(existing.getX() - spawnerPos.getX()) <= 4 &&
                    Math.abs(existing.getZ() - spawnerPos.getZ()) <= 4 &&
                    Math.abs(existing.getY() - spawnerPos.getY()) <= 2) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                SpawnerInfo info = new SpawnerInfo(spawnerPos, "Dungeon");
                info.updateDistance(playerPos);
                detected.put(spawnerPos, info);
            }
        }
    }

    private static void removeNearbyApproximate(BlockPos exactPos) {
        detected.entrySet().removeIf(entry -> {
            BlockPos p = entry.getKey();
            SpawnerInfo info = entry.getValue();
            return info.getEntityType().equals("Dungeon") &&
                    Math.abs(p.getX() - exactPos.getX()) <= 4 &&
                    Math.abs(p.getZ() - exactPos.getZ()) <= 4 &&
                    Math.abs(p.getY() - exactPos.getY()) <= 2;
        });
    }

    private static void mergeDuplicates() {
        List<BlockPos> keys = new ArrayList<>(detected.keySet());
        for (int i = 0; i < keys.size(); i++) {
            BlockPos p1 = keys.get(i);
            SpawnerInfo info1 = detected.get(p1);
            if (info1 == null) continue;

            for (int j = i + 1; j < keys.size(); j++) {
                BlockPos p2 = keys.get(j);
                SpawnerInfo info2 = detected.get(p2);
                if (info2 == null) continue;

                if (Math.abs(p1.getX() - p2.getX()) <= 4 &&
                    Math.abs(p1.getZ() - p2.getZ()) <= 4 &&
                    Math.abs(p1.getY() - p2.getY()) <= 2) {

                    if (!info1.getEntityType().equals("Dungeon")) {
                        detected.remove(p2);
                    } else if (!info2.getEntityType().equals("Dungeon")) {
                        detected.remove(p1);
                        break;
                    } else {
                        detected.remove(p2);
                    }
                }
            }
        }
    }

    public static List<SpawnerInfo> getDetectedSpawners() {
        List<SpawnerInfo> list = new ArrayList<>(detected.values());
        list.sort(Comparator.comparingDouble(SpawnerInfo::getDistance));
        return Collections.unmodifiableList(list);
    }

    public static void clear() {
        detected.clear();
        destroyedSpawners.clear();
        tickCounter = 0;
    }
}
