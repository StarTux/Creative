package com.winthier.creative;

import com.winthier.perm.Perm;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

@Getter @Setter
final class BuildWorld {
    private String name;
    private String path; // Key
    private Builder owner;
    private final Map<UUID, Trusted> trusted = new HashMap<>();
    private Trust publicTrust = Trust.NONE;
    private YamlConfiguration worldConfig = null;
    Map<Flag, Boolean> flags = new EnumMap<>(Flag.class);
    private String buildGroup = null;
    // World Border
    private int centerX = 0;
    private int centerZ = 0;
    private long size = -1;
    private transient long mobCooldown = 0;

    public static final Comparator<BuildWorld> NAME_SORT = new Comparator<BuildWorld>() {
        @Override public int compare(BuildWorld a, BuildWorld b) {
            return a.name.compareTo(b.name);
        }
    };

    BuildWorld(final String name, final String path, final Builder owner) {
        this.name = name;
        this.path = path;
        this.owner = owner;
        for (Flag flag : Flag.values()) {
            flags.put(flag, flag.defaultValue);
        }
    }

    enum Flag {
        VOXEL_SNIPER("VoxelSniper", false, -1),
        WORLD_EDIT("WorldEdit", false, 1000),
        EXPLOSION("Explosion", false, 0),
        LEAF_DECAY("LeafDecay", false, 0),
        KEEP_IN_MEMORY("KeepInMemory", false, -1),
        COMMAND_BLOCKS("CommandBlocks", false, -1),
        PISTON("Piston", true, 0),
        REDSTONE("Redstone", true, 0),
        PROJECTILES("Projectiles", false, 0),
        MOBS("Mobs", false, 0),
        LIQUIDS("Liquids", false, 0),
        FALLING_BLOCKS("FallingBlocks", false, 0);

        public final String key;
        public final boolean defaultValue;
        public final double price;

        Flag(final String key, final boolean defaultValue, final double price) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.price = price;
        }

        boolean userCanEdit() {
            return price >= 0;
        }

        static Flag of(String in) {
            for (Flag flag : Flag.values()) {
                if (in.equalsIgnoreCase(flag.key)) return flag;
                if (in.equalsIgnoreCase(flag.name())) return flag;
            }
            return null;
        }
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
        if (buildGroup != null && Perm.isInGroup(uuid, buildGroup)) return Trust.WORLD_EDIT;
        Trusted t = trusted.get(uuid);
        if (t == null) return publicTrust;
        Trust result = t.getTrust();
        if (publicTrust.priority > result.priority) return publicTrust;
        return result;
    }

    boolean trustBuilder(Builder builder, Trust trust) {
        if (owner != null && owner.getUuid().equals(builder.getUuid())) return false;
        if (trust == Trust.NONE) {
            trusted.remove(builder.getUuid());
        } else {
            trusted.put(builder.getUuid(), new Trusted(builder, trust));
        }
        return true;
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
        } catch (IllegalArgumentException iae) { }
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
        } catch (IllegalArgumentException iae) { }
        creator.type(worldType);
        result = creator.createWorld();
        result.setSpawnFlags(true, true);
        result.setTicksPerAnimalSpawns(999999999);
        result.setTicksPerMonsterSpawns(999999999);
        result.setGameRuleValue("doMobLoot", "false");
        result.setGameRuleValue("doMobSpawning", "false");
        result.setGameRuleValue("doTileDrops", "false");
        result.setGameRuleValue("doWeatherCycle", "false");
        result.setGameRuleValue("mobGriefing", "false");
        result.setGameRuleValue("randomTickSpeed", "0");
        result.setGameRuleValue("showDeathMessages", "false");
        result.setGameRuleValue("spawnRadius", "0");
        result.setGameRuleValue("doFireTick", "0");
        WorldBorder border = result.getWorldBorder();
        if (size > 0) {
            border.setCenter(centerX, centerZ);
            border.setSize((double) size);
        }
        if (getWorldConfig().isConfigurationSection("world.SpawnLocation")) {
            int x = getWorldConfig().getInt("world.SpawnLocation.x");
            int y = getWorldConfig().getInt("world.SpawnLocation.y");
            int z = getWorldConfig().getInt("world.SpawnLocation.z");
            result.setSpawnLocation(x, y, z);
        }
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
        ConfigurationSection section = getWorldConfig()
            .getConfigurationSection("world.SpawnLocation");
        if (section == null) {
            return getWorld().getSpawnLocation();
        } else {
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            float yaw = (float) section.getDouble("yaw");
            float pitch = (float) section.getDouble("pitch");
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
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("path", path);
        if (owner != null) {
            result.put("owner", owner.serialize());
        }
        Map<String, Object> trustedMap = new LinkedHashMap<>();
        result.put("buildGroup", buildGroup);
        result.put("trusted", trustedMap);
        result.put("publicTrust", publicTrust.name());
        for (Map.Entry<UUID, Trusted> e: this.trusted.entrySet()) {
            trustedMap.put(e.getKey().toString(), e.getValue().serialize());
        }
        for (Flag flag : Flag.values()) {
            result.put(flag.key, flags.get(flag));
        }
        result.put("Size", size);
        result.put("CenterX", centerX);
        result.put("CenterZ", centerZ);
        return result;
    }

    public static BuildWorld deserialize(ConfigurationSection config) {
        String name = config.getString("name");
        String path = config.getString("path");
        Builder owner = Builder.deserialize(config.getConfigurationSection("owner"));
        BuildWorld result = new BuildWorld(name, path, owner);
        result.buildGroup = config.getString("buildGroup");
        ConfigurationSection trustedSection = config.getConfigurationSection("trusted");
        if (trustedSection != null) {
            for (String key: trustedSection.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                Trusted trusted = Trusted.deserialize(trustedSection.getConfigurationSection(key));
                result.trusted.put(uuid, trusted);
            }
        }
        result.publicTrust = Trust.of(config.getString("publicTrust", "NONE"));
        for (Flag flag : Flag.values()) {
            result.flags.put(flag, config.getBoolean(flag.key, flag.defaultValue));
        }
        result.size = config.getLong("Size", result.size);
        result.centerX = config.getInt("CenterX", result.centerX);
        result.centerZ = config.getInt("CenterZ", result.centerZ);
        return result;
    }

    public boolean isSet(Flag flag) {
        return flags.get(flag);
    }

    public void set(Flag flag, boolean value) {
        flags.put(flag, value);
    }

    /**
     * Used by AdminCommand::rankTrustCommand.
     */
    protected int trustedScore() {
        return publicTrust.canBuild()
            ? Integer.MAX_VALUE
            : trusted.size();
    }
}
