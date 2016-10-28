package com.winthier.creative;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private YamlConfiguration logoutLocations = null;
    final WorldCommand worldCommand = new WorldCommand(this);
    final Permission permission = new Permission(this);
    @Getter static CreativePlugin instance = null;

    @Override
    public void onEnable() {
        instance = this;
        reloadConfig();
        saveDefaultConfig();
        getCommand("World").setExecutor(worldCommand);
        getCommand("wtp").setExecutor(new WTPCommand(this));
        getCommand("CreativeAdmin").setExecutor(new AdminCommand(this));
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
        logoutLocations = null;
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
        if (!player.isOp() && !buildWorld.getTrust(uuid).canVisit()) return null;
        World world = buildWorld.loadWorld();
        if (world == null) return null;
        double x = config.getDouble("x");
        double y = config.getDouble("y");
        double z = config.getDouble("z");
        float yaw = (float)config.getDouble("yaw");
        float pitch = (float)config.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }
}
