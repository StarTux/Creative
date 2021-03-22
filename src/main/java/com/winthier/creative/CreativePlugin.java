package com.winthier.creative;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class CreativePlugin extends JavaPlugin {
    private List<BuildWorld> buildWorlds;
    Map<String, PlotWorld> plotWorlds = new LinkedHashMap<>();
    private Map<String, Warp> warps;
    private YamlConfiguration logoutLocations = null;
    private final WorldCommand worldCommand = new WorldCommand(this);
    private final Permission permission = new Permission(this);
    private final Set<UUID> ignores = new HashSet<>();
    @Getter private static CreativePlugin instance = null;
    WorldEditListener worldEditListener = new WorldEditListener(this);
    final Vault vault = new Vault(this);
    final Metadata metadata = new Metadata(this);
    final Random random = ThreadLocalRandom.current();

    @Override
    public void onEnable() {
        instance = this;
        reloadConfig();
        saveDefaultConfig();
        try {
            saveResource("permissions.yml", false);
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        }
        getCommand("world").setExecutor(worldCommand);
        worldCommand.load();
        getCommand("wtp").setExecutor(new WTPCommand(this));
        getCommand("creativeadmin").setExecutor(new AdminCommand(this));
        getCommand("warp").setExecutor(new WarpCommand(this));
        getCommand("plot").setExecutor(new PlotCommand(this));
        getCommand("area").setExecutor(new AreaCommand(this).enable());
        getServer().getPluginManager().registerEvents(new CreativeListener(this), this);
        for (Player player: getServer().getOnlinePlayers()) {
            permission.updatePermissions(player);
        }
        getBuildWorlds();
        worldEditListener.enable();
        vault.setup();
        loadPlotWorlds();
    }

    @Override
    public void onDisable() {
        worldEditListener.disable();
        for (Player player: getServer().getOnlinePlayers()) {
            permission.resetPermissions(player);
            storeLogoutLocation(player);
        }
        saveLogoutLocations();
    }

    void reloadAllConfigs() {
        reloadConfig();
        buildWorlds = null;
        warps = null;
        logoutLocations = null;
        permission.reload();
        worldCommand.load();
        loadPlotWorlds();
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
                if (buildWorld.isSet(BuildWorld.Flag.KEEP_IN_MEMORY)) buildWorld.loadWorld();
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
            if (path.equals(buildWorld.getPath())) {
                return buildWorld;
            }
        }
        return null;
    }

    public BuildWorld getBuildWorldByName(String name) {
        for (BuildWorld buildWorld: getBuildWorlds()) {
            if (name.equals(buildWorld.getName()) || name.equals(buildWorld.getPath())) {
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
        config.set("world", loc.getWorld().getName());
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
        float yaw = (float) config.getDouble("yaw");
        float pitch = (float) config.getDouble("pitch");
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

    Meta metaOf(Player player) {
        return metadata.get(player, "creative:meta", Meta.class, Meta::new);
    }

    void loadPlotWorlds() {
        plotWorlds.clear();
        File dir = new File(getDataFolder(), "plots");
        if (!dir.exists()) return;
        for (File file : dir.listFiles()) {
            String name = file.getName();
            if (!name.endsWith(".yml")) continue;
            name = name.substring(0, name.length() - 4);
            ConfigurationSection cnf = YamlConfiguration.loadConfiguration(file);
            PlotWorld plotWorld = new PlotWorld(name);
            plotWorld.load(cnf);
            plotWorlds.put(name, plotWorld);
        }
    }

    boolean isPlotWorld(World world) {
        return plotWorlds.containsKey(world.getName());
    }

    boolean canBuildInPlot(Player player, Block block) {
        PlotWorld plotWorld = plotWorlds.get(block.getWorld().getName());
        if (plotWorld == null) return false;
        return plotWorld.canBuild(player, block);
    }

    PlotWorld getPlotWorld(World world) {
        return plotWorlds.get(world.getName());
    }

    public List<String> completeWorldNames(Player player, String arg) {
        List<String> result = new ArrayList<>();
        String argl = arg.toLowerCase();
        UUID uuid = player.getUniqueId();
        for (BuildWorld buildWorld: getBuildWorlds()) {
            String name = buildWorld.getName();
            if (name == null) name = buildWorld.getPath();
            if (buildWorld.getTrust(uuid).canVisit() && name.toLowerCase().contains(argl)) {
                result.add(buildWorld.getName());
            }
        }
        return result;
    }
}
