package com.winthier.creative;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

@Getter @Setter
public class BuildWorld {
    String name;
    String path; // Key
    Builder owner;
    final Map<UUID, Trusted> trusted = new HashMap<>();
    Trust publicTrust = Trust.NONE;
    YamlConfiguration worldConfig = null;

    public final static Comparator<BuildWorld> NAME_SORT = new Comparator<BuildWorld>() {
        @Override public int compare(BuildWorld a, BuildWorld b) {
            return a.name.compareTo(b.name);
        }
    };

    BuildWorld(String name, String path, Builder owner) {
        this.name = name;
        this.path = path;
        this.owner = owner;
    }

    CreativePlugin getPlugin() {
        return CreativePlugin.getInstance();
    }

    List<Builder> listTrusted(Trust trust) {
        List<Builder> result = new ArrayList<>();
        for (Map.Entry<UUID, Trusted> e: this.trusted.entrySet()) {
            if (e.getValue().getTrust() == trust) {
                result.add(e.getValue().getBuilder());
            }
        }
        return result;
    }

    Trust getTrust(UUID uuid) {
        if (getPlugin().doesIgnore(uuid)) return Trust.OWNER;
        if (owner != null && owner.getUuid().equals(uuid)) return Trust.OWNER;
        Trusted t = trusted.get(uuid);
        if (t == null) return publicTrust;
        Trust result = t.getTrust();
        if (publicTrust.priority > result.priority) return publicTrust;
        return result;
    }

    boolean trustBuilder(Builder builder, Trust trust) {
        if (trust == Trust.NONE) {
            return trusted.remove(builder.getUuid()) != null;
        } else {
            if (owner != null && owner.getUuid().equals(builder.getUuid())) return false;
            trusted.put(builder.getUuid(), new Trusted(builder, trust));
            return true;
        }
    }

    String getOwnerName() {
        if (owner == null) {
            return "N/A";
        } else {
            return owner.getName();
        }
    }

    World getWorld() {
        return Bukkit.getServer().getWorld(path);
    }

    File getWorldFolder() {
        return new File(getPlugin().getServer().getWorldContainer(), path);
    }

    World loadWorld() {
        World result = getWorld();
        if (result != null) return result;
        File dir = getWorldFolder();
        if (!dir.isDirectory()) {
            getPlugin().getLogger().warning("World folder does not exist: " + path);
            return null;
        }
        WorldCreator creator = WorldCreator.name(path);
        World.Environment environment = World.Environment.NORMAL;
        try {
            String tmp = getWorldConfig().getString("world.Environment");
            if (tmp != null) environment = World.Environment.valueOf(tmp);
        } catch (IllegalArgumentException iae) {}
        creator.environment(environment);
        creator.generateStructures(getWorldConfig().getBoolean("world.GenerateStructures", true));
        creator.generator(getWorldConfig().getString("world.Generator"));
        String generatorSettings = getWorldConfig().getString("world.GeneratorSettings");
        if (generatorSettings != null) creator.generatorSettings(generatorSettings);
        creator.seed(getWorldConfig().getLong("world.Seed", 0));
        WorldType worldType = WorldType.NORMAL;
        try {
            String tmp = getWorldConfig().getString("world.WorldType");
            if (tmp != null) worldType = WorldType.valueOf(tmp);
        } catch (IllegalArgumentException iae) {}
        creator.type(worldType);
        result = creator.createWorld();
        result.setDifficulty(Difficulty.PEACEFUL); // TODO
        result.setSpawnFlags(false, false); // TODO
        result.setGameRuleValue("doMobLoot", "false");
        result.setGameRuleValue("doMobSpawning", "false");
        result.setGameRuleValue("doTileDrops", "false");
        result.setGameRuleValue("doWeatherCycle", "false");
        result.setGameRuleValue("mobGriefing", "false");
        result.setGameRuleValue("randomTickSpeed", "0");
        result.setGameRuleValue("showDeathMessages", "false");
        result.setGameRuleValue("spawnRadius", "0");
        return result;
    }

    ConfigurationSection getWorldConfig() {
        if (worldConfig == null) {
            File dir = getWorldFolder();
            dir.mkdirs();
            File file = new File(dir, "config.yml");
            worldConfig = YamlConfiguration.loadConfiguration(file);
        }
        return worldConfig;
    }

    void reloadWorldConfig() {
        worldConfig = null;
    }

    void saveWorldConfig() {
        if (worldConfig == null) return;
        try {
            worldConfig.save(new File(getWorldFolder(), "config.yml"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    void teleportToSpawn(Player player) {
        Location loc = getSpawnLocation();
        if (loc == null) return;
        player.teleport(loc);
    }

    Location getSpawnLocation() {
        if (getWorld() == null) return null;
        ConfigurationSection section = getWorldConfig().getConfigurationSection("world.SpawnLocation");
        if (section == null) {
            return getWorld().getSpawnLocation();
        } else {
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            float yaw = (float)section.getDouble("yaw");
            float pitch = (float)section.getDouble("pitch");
            return new Location(getWorld(), x, y, z, yaw, pitch);
        }
    }

    void setSpawnLocation(Location loc) {
        Block block = loc.getBlock();
        if (getWorld() != null) {
            getWorld().setSpawnLocation(block.getX(), block.getY(), block.getZ());
        }
        getWorldConfig().set("world.SpawnLocation.x", loc.getX());
        getWorldConfig().set("world.SpawnLocation.y", loc.getY());
        getWorldConfig().set("world.SpawnLocation.z", loc.getZ());
        getWorldConfig().set("world.SpawnLocation.pitch", loc.getPitch());
        getWorldConfig().set("world.SpawnLocation.yaw", loc.getYaw());
    }

    // Serialization

    Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("path", path);
        if (owner != null) {
            result.put("owner", owner.serialize());
        }
        Map<String, Object> trustedMap = new HashMap<>();
        result.put("trusted", trustedMap);
        result.put("publicTrust", publicTrust.name());
        for (Map.Entry<UUID, Trusted> e: this.trusted.entrySet()) {
            trustedMap.put(e.getKey().toString(), e.getValue().serialize());
        }
        return result;
    }

    public static BuildWorld deserialize(ConfigurationSection config) {
        String name = config.getString("name");
        String path = config.getString("path");
        Builder owner = Builder.deserialize(config.getConfigurationSection("owner"));
        BuildWorld result = new BuildWorld(name, path, owner);
        ConfigurationSection trustedSection = config.getConfigurationSection("trusted");
        if (trustedSection != null) {
            for (String key: trustedSection.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                Trusted trusted = Trusted.deserialize(trustedSection.getConfigurationSection(key));
                result.trusted.put(uuid, trusted);
            }
        }
        result.publicTrust = Trust.of(config.getString("publicTrust", "NONE"));
        return result;
    }
}
