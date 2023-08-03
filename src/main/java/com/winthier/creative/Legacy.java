package com.winthier.creative;

import com.winthier.creative.sql.SQLWorld;
import com.winthier.creative.sql.SQLWorldTrust;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import static com.winthier.creative.CreativePlugin.plugin;
import static com.winthier.creative.sql.Database.sql;

public final class Legacy {
    public static void transferAllBuildWorlds() {
        final List<BuildWorld> list = deserializeAllBuildWorlds();
        List<SQLWorld> worlds = new ArrayList<>();
        List<SQLWorldTrust> trusted = new ArrayList<>();
        for (BuildWorld buildWorld : list) {
            buildWorld.getRow().pack();
            worlds.add(buildWorld.getRow());
            trusted.addAll(buildWorld.getTrusted().values());
        }
        plugin().getLogger().info("Inserting " + worlds.size() + " worlds");
        int result;
        result = sql().insert(worlds);
        plugin().getLogger().info("Result = " + result);
        plugin().getLogger().info("Inserting " + trusted.size() + " trusted");
        result = sql().insert(trusted);
        plugin().getLogger().info("Result = " + result);
        plugin().getLogger().info("Done");
    }

    public static List<BuildWorld> deserializeAllBuildWorlds() {
        final List<BuildWorld> list = new ArrayList<>();
        File file = new File(plugin().getDataFolder(), "worlds.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (Map<?, ?> map: config.getMapList("worlds")) {
            MemoryConfiguration mem = new MemoryConfiguration();
            ConfigurationSection section = mem.createSection("tmp", map);
            BuildWorld buildWorld = deserializeBuildWorld(section);
            list.add(buildWorld);
        }
        return list;
    }

    public static BuildWorld deserializeBuildWorld(ConfigurationSection config) {
        String name = config.getString("name");
        String path = config.getString("path");
        String ownerString = config.getString("owner.uuid");
        UUID owner = ownerString != null
            ? UUID.fromString(ownerString)
            : null;
        BuildWorld result = new BuildWorld(name, path, owner);
        result.getRow().getCachedTag().getBuildGroups().addAll(config.getStringList("buildGroups"));
        ConfigurationSection trustedSection = config.getConfigurationSection("trusted");
        if (trustedSection != null) {
            for (String key: trustedSection.getKeys(false)) {
                final UUID uuid = UUID.fromString(key);
                final String trustType = trustedSection.getString(key + ".trust");
                final Trust trust = Trust.of(trustType);
                if (trust == null) {
                    throw new IllegalStateException("[" + path + "] Illegal trust type: " + trustType);
                }
                result.getTrusted().put(uuid, new SQLWorldTrust(path, uuid, trust));
            }
        }
        result.getRow().setPublicTrust(Trust.of(config.getString("publicTrust", "NONE")));
        for (BuildWorld.Flag flag : BuildWorld.Flag.values()) {
            final boolean value = config.getBoolean(flag.key, flag.defaultValue);
            if (value == flag.defaultValue) continue;
            result.getRow().getCachedTag().getFlags().put(flag, value);
        }
        result.getRow().setBorderSize(config.getInt("Size", 0));
        result.getRow().setBorderCenterX(config.getInt("CenterX", 0));
        result.getRow().setBorderCenterZ(config.getInt("CenterZ", 0));
        // WorldConfig file
        File folder = new File(Bukkit.getWorldContainer(), path);
        File file = new File(folder, "config.yml");
        if (!file.isFile()) {
            plugin().getLogger().severe("[" + path + "] No world config: " + file);
        } else {
            ConfigurationSection worldConfig = YamlConfiguration.loadConfiguration(file);
            result.getRow().setDescription(worldConfig.getString("user.Description"));
            // Generator
            result.getRow().setGenerator(worldConfig.getString("world.Generator"));
            result.getRow().setSeed(worldConfig.getLong("world.Seed"));
            result.getRow().setWorldType(worldConfig.getString("world.WorldType"));
            result.getRow().setEnvironment(worldConfig.getString("world.Environment"));
            result.getRow().setGenerateStructures(worldConfig.getBoolean("world.GenerateStructures"));
            result.getRow().setGeneratorSettings(worldConfig.getString("world.GeneratorSettings"));
            // Spawn
            result.getRow().setSpawnX(worldConfig.getDouble("world.SpawnLocation.x"));
            result.getRow().setSpawnY(worldConfig.getDouble("world.SpawnLocation.y"));
            result.getRow().setSpawnZ(worldConfig.getDouble("world.SpawnLocation.z"));
            result.getRow().setSpawnYaw(worldConfig.getDouble("world.SpawnLocation.yaw"));
            result.getRow().setSpawnPitch(worldConfig.getDouble("world.SpawnLocation.pitch"));
        }
        // Fini
        return result;
    }

    private Legacy() { }
}
