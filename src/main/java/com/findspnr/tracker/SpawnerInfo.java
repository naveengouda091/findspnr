package com.findspnr.tracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Immutable value-object that describes one detected monster spawner.
 */
public class SpawnerInfo {

    private final BlockPos pos;
    private String entityType;
    private double distance;

    public SpawnerInfo(BlockPos pos, String entityType) {
        this.pos = pos;
        this.entityType = entityType;
        this.distance = 0.0;
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public BlockPos getPos() { return pos; }

    /** Centre of the spawner block as a Vec3d. */
    public Vec3d getCenterVec() {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public String getEntityType() { return entityType; }

    public void setEntityType(String entityType) { this.entityType = entityType; }

    public double getDistance() { return distance; }

    public void updateDistance(Vec3d playerPos) {
        this.distance = Math.sqrt(pos.getSquaredDistance(playerPos.x, playerPos.y, playerPos.z));
    }

    /**
     * Returns a human-readable name such as "Zombie Spawner" or "Monster Spawner"
     * when the entity type is unknown.
     */
    public String getFormattedName() {
        if (entityType == null || entityType.isBlank()) return "Monster Spawner";
        String name = entityType.replace("minecraft:", "").replace("_", " ").trim();
        if (!name.isEmpty()) name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return name + " Spawner";
    }

    // ── equals / hashCode (identity = block position) ─────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpawnerInfo si)) return false;
        return pos.equals(si.pos);
    }

    @Override
    public int hashCode() { return pos.hashCode(); }
}
