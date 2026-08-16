package com.findspnr.tracker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BastionInfo {
    private final BlockPos pos;
    private final String type; // "Bastion Remnant"
    private double distance;

    public BastionInfo(BlockPos pos, String type) {
        this.pos = pos;
        this.type = type;
    }

    public void updateDistance(Vec3d playerPos) {
        double dx = pos.getX() + 0.5 - playerPos.x;
        double dy = pos.getY() + 0.5 - playerPos.y;
        double dz = pos.getZ() + 0.5 - playerPos.z;
        this.distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public BlockPos getPos() { return pos; }
    public String getType() { return type; }
    public double getDistance() { return distance; }
}
