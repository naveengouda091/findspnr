package com.findspnr.tracker;

import com.findspnr.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Separate Base & Treasure Finder Tracker:
 * Activated via command (/findspnr base).
 * Scans loaded chunks for Shulker Boxes and Ender Chests.
 */
public class BaseTracker {

    private static final ConcurrentHashMap<BlockPos, BaseInfo> detected = new ConcurrentHashMap<>();
    private static final Set<BlockPos> removedBases = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static int tickCounter = 0;

    public static void tick(MinecraftClient client) {
        if (!ModConfig.enabled || !ModConfig.renderBaseFinder || client.world == null || client.player == null) {
            return;
        }

        tickCounter++;

        // Full scan every 20 ticks (~1s)
        if (tickCounter % 20 == 0) {
            scanChunks(client.world, client.player.getPos());
        }

        // Update distances every tick & auto-remove when player arrives (distance <= 3.5m)
        Vec3d playerPos = client.player.getPos();
        detected.entrySet().removeIf(entry -> {
            BaseInfo info = entry.getValue();
            info.updateDistance(playerPos);

            if (info.getDistance() <= 3.5) {
                removedBases.add(entry.getKey());
                return true; // Auto-delete on arrival
            }
            return false;
        });
    }

    private static void scanChunks(ClientWorld world, Vec3d playerPos) {
        ChunkPos playerChunk = new ChunkPos(BlockPos.ofFloored(playerPos));
        int radius = ModConfig.scanRadiusChunks;

        // Clean up removed / broken blocks
        detected.entrySet().removeIf(entry -> {
            BlockPos p = entry.getKey();
            if (!world.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) return false;

            BlockState state = world.getBlockState(p);
            if (state.isAir() || (!isTargetBlock(state))) {
                removedBases.add(p);
                return true;
            }
            return false;
        });

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        // Scan surrounding loaded chunks
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = playerChunk.x + dx;
                int cz = playerChunk.z + dz;

                if (!world.isChunkLoaded(cx, cz)) continue;

                WorldChunk chunk = world.getChunk(cx, cz);

                // ── METHOD 1: Direct Block Entity Scan (O(1) fast lookup) ────────────
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getPos();
                    if (removedBases.contains(pos)) continue;

                    if (be instanceof ShulkerBoxBlockEntity) {
                        BaseInfo info = new BaseInfo(pos, "Shulker Box");
                        info.updateDistance(playerPos);
                        detected.put(pos, info);
                    } else if (be instanceof EnderChestBlockEntity) {
                        BaseInfo info = new BaseInfo(pos, "Ender Chest");
                        info.updateDistance(playerPos);
                        detected.put(pos, info);
                    }
                }

                // ── METHOD 2: Fast ChunkSection Filtered Scan ────────────────────────
                ChunkSection[] sections = chunk.getSectionArray();
                int startX = chunk.getPos().getStartX();
                int startZ = chunk.getPos().getStartZ();
                int worldBottomY = world.getBottomY();

                for (int i = 0; i < sections.length; i++) {
                    ChunkSection section = sections[i];
                    if (section == null || section.isEmpty()) continue;

                    // Fast check: does 16x16x16 section contain Shulker Box or Ender Chest?
                    if (!section.hasAny(BaseTracker::isTargetBlock)) {
                        continue; // Skip 4096 blocks instantly
                    }

                    int sectionBottomY = worldBottomY + (i * 16);

                    for (int y = 0; y < 16; y++) {
                        int worldY = sectionBottomY + y;

                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                mutablePos.set(startX + x, worldY, startZ + z);
                                if (removedBases.contains(mutablePos)) continue;

                                BlockState state = section.getBlockState(x, y, z);
                                if (isTargetBlock(state)) {
                                    BlockPos immutablePos = mutablePos.toImmutable();
                                    String type = (state.getBlock() instanceof ShulkerBoxBlock) ? "Shulker Box" : "Ender Chest";
                                    BaseInfo info = new BaseInfo(immutablePos, type);
                                    info.updateDistance(playerPos);
                                    detected.putIfAbsent(immutablePos, info);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isTargetBlock(BlockState state) {
        return state.isOf(Blocks.ENDER_CHEST) || (state.getBlock() instanceof ShulkerBoxBlock);
    }

    public static List<BaseInfo> getDetectedBases() {
        List<BaseInfo> list = new ArrayList<>(detected.values());
        list.sort(Comparator.comparingDouble(BaseInfo::getDistance));
        return Collections.unmodifiableList(list);
    }

    public static void clear() {
        detected.clear();
        removedBases.clear();
        tickCounter = 0;
    }
}
