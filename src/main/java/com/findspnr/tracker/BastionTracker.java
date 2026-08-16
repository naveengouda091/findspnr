package com.findspnr.tracker;

import com.findspnr.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
 * Bastion Remnant Tracker:
 * Scans loaded Nether chunks for Gilded Blackstone and Polished Blackstone Bricks
 * (which naturally generate exclusively in Nether Bastions).
 * Activated via command (/findspnr bastion).
 */
public class BastionTracker {

    private static final ConcurrentHashMap<BlockPos, BastionInfo> detected = new ConcurrentHashMap<>();
    private static final Set<BlockPos> removedBastions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static int tickCounter = 0;

    public static void tick(MinecraftClient client) {
        if (!ModConfig.enabled || !ModConfig.renderBastionFinder || client.world == null || client.player == null) {
            return;
        }

        tickCounter++;

        // Full scan every 20 ticks (~1s)
        if (tickCounter % 20 == 0) {
            scanChunks(client.world, client.player.getPos());
            mergeDuplicates();
        }

        // Update distances every tick & auto-remove when player arrives (distance <= 3.5m)
        Vec3d playerPos = client.player.getPos();
        detected.entrySet().removeIf(entry -> {
            BastionInfo info = entry.getValue();
            info.updateDistance(playerPos);

            if (info.getDistance() <= 3.5) {
                removedBastions.add(entry.getKey());
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
            if (state.isAir() || (!isBastionBlock(state))) {
                removedBastions.add(p);
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
                ChunkSection[] sections = chunk.getSectionArray();
                int startX = chunk.getPos().getStartX();
                int startZ = chunk.getPos().getStartZ();
                int worldBottomY = world.getBottomY();

                for (int i = 0; i < sections.length; i++) {
                    ChunkSection section = sections[i];
                    if (section == null || section.isEmpty()) continue;

                    // Fast pass: does section contain Gilded Blackstone or Bastion Bricks?
                    if (!section.hasAny(BastionTracker::isBastionBlock)) {
                        continue;
                    }

                    int sectionBottomY = worldBottomY + (i * 16);

                    for (int y = 0; y < 16; y++) {
                        int worldY = sectionBottomY + y;
                        if (worldY < 30 || worldY > 110) continue;

                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                mutablePos.set(startX + x, worldY, startZ + z);
                                if (removedBastions.contains(mutablePos)) continue;

                                BlockState state = section.getBlockState(x, y, z);
                                if (state.isOf(Blocks.GILDED_BLACKSTONE)) {
                                    BlockPos immutablePos = mutablePos.toImmutable();

                                    boolean isDuplicate = false;
                                    for (BlockPos existing : detected.keySet()) {
                                        if (Math.abs(existing.getX() - immutablePos.getX()) <= 8 &&
                                            Math.abs(existing.getZ() - immutablePos.getZ()) <= 8) {
                                            isDuplicate = true;
                                            break;
                                        }
                                    }

                                    if (!isDuplicate) {
                                        BastionInfo info = new BastionInfo(immutablePos, "Bastion Remnant");
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
    }

    private static boolean isBastionBlock(BlockState state) {
        return state.isOf(Blocks.GILDED_BLACKSTONE) ||
               state.isOf(Blocks.POLISHED_BLACKSTONE_BRICKS) ||
               state.isOf(Blocks.CHISELED_POLISHED_BLACKSTONE);
    }

    private static void mergeDuplicates() {
        List<BlockPos> keys = new ArrayList<>(detected.keySet());
        for (int i = 0; i < keys.size(); i++) {
            BlockPos p1 = keys.get(i);
            for (int j = i + 1; j < keys.size(); j++) {
                BlockPos p2 = keys.get(j);
                if (Math.abs(p1.getX() - p2.getX()) <= 8 && Math.abs(p1.getZ() - p2.getZ()) <= 8) {
                    detected.remove(p2);
                }
            }
        }
    }

    public static List<BastionInfo> getDetectedBastions() {
        List<BastionInfo> list = new ArrayList<>(detected.values());
        list.sort(Comparator.comparingDouble(BastionInfo::getDistance));
        return Collections.unmodifiableList(list);
    }

    public static void clear() {
        detected.clear();
        removedBastions.clear();
        tickCounter = 0;
    }
}
