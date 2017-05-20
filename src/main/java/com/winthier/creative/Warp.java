package com.winthier.creative;

import java.util.Comparator;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

@Data
public class Warp {
    public final static Comparator<Warp> NAME_SORT = new Comparator<Warp>() {
        @Override public int compare(Warp a, Warp b) {
            return a.name.compareTo(b.name);
        }
    };

    final String name, world;
    final double x, y ,z;
    final float pitch, yaw;
    String permission = null;
    String displayName = null;

    static Warp of(String name, Location location) {
        String world = location.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();
        Warp result = new Warp(name, world, x, y, z, pitch, yaw);
        result.setDisplayName(name);
        return result;
    }

    static Warp deserialize(String name, ConfigurationSection config) {
        String world = config.getString("world");
        double x = config.getDouble("x");
        double y = config.getDouble("y");
        double z = config.getDouble("z");
        float yaw = (float)config.getDouble("yaw");
        float pitch = (float)config.getDouble("pitch");
        String permission = config.getString("permission", null);
        String displayName = config.getString("display_name", name);
        Warp warp = new Warp(name, world, x, y, z, pitch, yaw);
        warp.setPermission(permission);
        warp.setDisplayName(displayName);
        return warp;
    }

    void serialize(ConfigurationSection config) {
        config.set("world", world);
        config.set("x", x);
        config.set("y", y);
        config.set("z", z);
        config.set("yaw", yaw);
        config.set("pitch", pitch);
        config.set("display_name", displayName);
    }

    public Location getLocation() {
        BuildWorld buildWorld = CreativePlugin.getInstance().getBuildWorldByPath(world);
        World world;
        if (buildWorld == null) {
            world = Bukkit.getServer().getWorld(this.world);
        } else {
            world = buildWorld.loadWorld();
        }
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }
}
