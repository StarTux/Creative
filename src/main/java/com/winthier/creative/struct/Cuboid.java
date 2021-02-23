package com.winthier.creative.struct;

import java.util.ArrayList;
import java.util.List;
import lombok.Value;
import org.bukkit.Location;
import org.bukkit.block.Block;

@Value
public final class Cuboid {
    public static final Cuboid ZERO = new Cuboid(Vec3i.ZERO, Vec3i.ZERO);
    public final Vec3i min;
    public final Vec3i max;

    public boolean contains(int x, int y, int z) {
        return x >= min.x && x <= max.x
            && y >= min.y && y <= max.y
            && z >= min.z && z <= max.z;
    }

    public boolean contains(Block block) {
        return contains(block.getX(), block.getY(), block.getZ());
    }

    public boolean contains(Location loc) {
        return contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public boolean contains(Vec3i v) {
        return contains(v.x, v.y, v.z);
    }

    @Override
    public String toString() {
        return "(" + min + ")-(" + max + ")";
    }

    public int getSizeX() {
        return max.x - min.x + 1;
    }

    public int getSizeY() {
        return max.y - min.y + 1;
    }

    public int getSizeZ() {
        return max.z - min.z + 1;
    }

    public int getVolume() {
        return getSizeX() * getSizeY() * getSizeZ();
    }

    public Vec3i getCenter() {
        return new Vec3i((min.x + max.x) / 2, (min.y + max.y) / 2, (min.z + max.z) / 2);
    }

    public List<Vec3i> enumerate() {
        List<Vec3i> result = new ArrayList<>();
        for (int y = min.y; y <= max.y; y += 1) {
            for (int z = min.z; z <= max.z; z += 1) {
                for (int x = min.x; x <= max.x; x += 1) {
                    result.add(new Vec3i(x, y, z));
                }
            }
        }
        return result;
    }

    public Vec3i clamp(Vec3i other) {
        int x = Math.max(other.x, min.x);
        int y = Math.max(other.y, min.y);
        int z = Math.max(other.z, min.z);
        x = Math.min(x, max.x);
        y = Math.min(y, max.y);
        z = Math.min(z, max.z);
        return new Vec3i(x, y, z);
    }
}
