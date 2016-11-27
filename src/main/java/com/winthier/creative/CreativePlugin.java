package com.winthier.creative;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class CreativePlugin extends JavaPlugin {
    private List<BuildWorld> buildWorlds;
    private Map<String, Warp> warps;
    private YamlConfiguration logoutLocations = null;
    final WorldCommand worldCommand = new WorldCommand(this);
    final Permission permission = new Permission(this);
    private final Set<UUID> ignores = new HashSet<>();
    @Getter static CreativePlugin instance = null;

    @Override
    public void onEnable() {
        instance = this;
        reloadConfig();
        saveDefaultConfig();
        saveResource("permissions.yml", false);
        getCommand("World").setExecutor(worldCommand);
        getCommand("wtp").setExecutor(new WTPCommand(this));
        getCommand("CreativeAdmin").setExecutor(new AdminCommand(this));
        getCommand("Warp").setExecutor(new WarpCommand(this));
        getServer().getPluginManager().registerEvents(new CreativeListener(this), this);
        for (Player player: getServer().getOnlinePlayers()) {
            permission.updatePermissions(player);
        }
    }

    @Override
    public void onDisable() {
        for (Player player: getServer().getOnlinePlayers()) {
            permission.resetPermissions(player);
            storeLogoutLocation(player);
        }
        saveLogoutLocations();
    }

    void reloadAllConfigs() {
        buildWorlds = null;
        warps = null;
        logoutLocations = null;
        permission.reload();
    }

    // Build Worlds

    public List<BuildWorld> getBuildWorlds() {
        if (buildWorlds == null) {
            buildWorlds = new ArrayList<>();
            File file = new File(getDataFolder(), "worlds.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (Map<?, ?> map: config.getMapList("worlds")) {
                MemoryConfiguration mem = new MemoryConfiguration();
                ConfigurationSection section = mem.createSection("tmp", map);
                BuildWorld buildWorld = BuildWorld.deserialize(section);
                buildWorlds.add(buildWorld);
            }
        }
        return buildWorlds;
    }

    public void saveBuildWorlds() {
        if (buildWorlds == null) return;
        List<Object> list = new ArrayList<>();
        for (BuildWorld buildWorld: getBuildWorlds()) {
            list.add(buildWorld.serialize());
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("worlds", list);
        File file = new File(getDataFolder(), "worlds.yml");
        try {
            config.save(file);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public BuildWorld getBuildWorldByPath(String path) {
        for (BuildWorld buildWorld: getBuildWorlds()) {
            if (path.equalsIgnoreCase(buildWorld.getPath())) {
                return buildWorld;
            }
        }
        return null;
    }

    public BuildWorld getBuildWorldByWorld(World world) {
        if (world == null) return null;
        return getBuildWorldByPath(world.getName());
    }

    public PlayerWorldList getPlayerWorldList(UUID uuid) {
        PlayerWorldList result = new PlayerWorldList();
        for (BuildWorld buildWorld: getBuildWorlds()) {
            Trust trust = buildWorld.getTrust(uuid);
            if (trust.isOwner()) {
                result.owner.add(buildWorld);
            } else if (trust.canBuild()) {
                result.build.add(buildWorld);
            } else if (trust.canVisit()) {
                result.visit.add(buildWorld);
            }
        }
        return result;
    }

    ConfigurationSection getLogoutLocations() {
        if (logoutLocations == null) {
            File file = new File(getDataFolder(), "logouts.yml");
            logoutLocations = YamlConfiguration.loadConfiguration(file);
        }
        return logoutLocations;
    }

    void saveLogoutLocations() {
        if (logoutLocations == null) return;
        File file = new File(getDataFolder(), "logouts.yml");
        try {
            logoutLocations.save(file);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    void storeLogoutLocation(Player player) {
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();
        ConfigurationSection config = getLogoutLocations().createSection(uuid.toString());
        config.set("world", loc.getWorld().getName());;
        config.set("x", loc.getX());
        config.set("y", loc.getY());
        config.set("z", loc.getZ());
        config.set("yaw", loc.getYaw());
        config.set("pitch", loc.getPitch());
        config.set("gamemode", player.getGameMode().name());
    }

    Location findSpawnLocation(Player player) {
        UUID uuid = player.getUniqueId();
        ConfigurationSection config = getLogoutLocations().getConfigurationSection(uuid.toString());
        if (config == null) return null;
        String worldName = config.getString("world");
        BuildWorld buildWorld = getBuildWorldByPath(worldName);
        if (buildWorld == null) return null;
        if (!doesIgnore(uuid) && !buildWorld.getTrust(uuid).canVisit()) return null;
        World world = buildWorld.loadWorld();
        if (world == null) return null;
        double x = config.getDouble("x");
        double y = config.getDouble("y");
        double z = config.getDouble("z");
        float yaw = (float)config.getDouble("yaw");
        float pitch = (float)config.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    public boolean toggleIgnore(Player player) {
        UUID uuid = player.getUniqueId();
        if (ignores.remove(uuid)) {
            return false;
        } else {
            ignores.add(uuid);
            return true;
        }
    }

    public boolean doesIgnore(Player player) {
        UUID uuid = player.getUniqueId();
        return ignores.contains(uuid);
    }

    public boolean doesIgnore(UUID uuid) {
        return ignores.contains(uuid);
    }

    public Map<String, Warp> getWarps() {
        if (warps == null) {
            warps = new HashMap<>();
            File file = new File(getDataFolder(), "warps.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (String key: config.getKeys(false)) {
                Warp warp = Warp.deserialize(key, config.getConfigurationSection(key));
                warps.put(warp.getName(), warp);
            }
        }
        return warps;
    }

    public void saveWarps() {
        if (warps == null) return;
        YamlConfiguration config = new YamlConfiguration();
        for (Warp warp: warps.values()) {
            ConfigurationSection section = config.createSection(warp.getName());
            warp.serialize(section);
        }
        File file = new File(getDataFolder(), "warps.yml");
        try {
            config.save(file);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
