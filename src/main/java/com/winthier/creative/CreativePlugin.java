package com.winthier.creative;

import com.cavetale.core.connect.NetworkServer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class CreativePlugin extends JavaPlugin {
    private final List<BuildWorld> buildWorlds = new ArrayList<>();
    private final Map<String, PlotWorld> plotWorlds = new LinkedHashMap<>();
    private final Permission permission = new Permission(this);
    private final Set<UUID> ignores = new HashSet<>();
    private static CreativePlugin instance = null;
    private WorldEditListener worldEditListener = new WorldEditListener(this);
    private final Random random = ThreadLocalRandom.current();
    private final CoreWorlds coreWorlds = new CoreWorlds(this);
    // Commands
    private final AdminCommand adminCommand = new AdminCommand(this);
    private final CreativeCommand creativeCommand = new CreativeCommand(this);
    private final CTPCommand ctpCommand = new CTPCommand(this);
    private final KitCommand kitCommand = new KitCommand(this);
    private boolean isCreativeServer;

    public CreativePlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.isCreativeServer = NetworkServer.CREATIVE.isThisServer();
        adminCommand.enable();
        creativeCommand.enable();
        ctpCommand.enable();
        kitCommand.enable();
        if (isCreativeServer()) {
            coreWorlds.register();
            saveResource("permissions.yml", false);
            for (Player player: getServer().getOnlinePlayers()) {
                permission.updatePermissions(player);
            }
            getServer().getPluginManager().registerEvents(new CreativeListener(this), this);
            if (Bukkit.getPluginManager().isPluginEnabled("Shutdown")) {
                Bukkit.getPluginManager().registerEvents(new ShutdownListener(this), this);
            }
            loadPlotWorlds();
            loadBuildWorlds();
            worldEditListener.enable();
        }
        getLogger().info(isCreativeServer
                         ? "This is the Creative server"
                         : "This is NOT the Creative server");
    }

    @Override
    public void onDisable() {
        if (isCreativeServer()) {
            worldEditListener.disable();
            for (Player player: getServer().getOnlinePlayers()) {
                permission.resetPermissions(player);
            }
            // Unload all empty worlds!
            for (BuildWorld buildWorld : buildWorlds) {
                World world = buildWorld.getWorld();
                if (world != null) {
                    unloadEmptyWorld(world);
                }
            }
            coreWorlds.unregister();
        }
    }

    protected boolean unloadEmptyWorld(final World world) {
        if (world == null) return false;
        if (!world.getPlayers().isEmpty()) return false;
        world.save();
        if (!Bukkit.unloadWorld(world, true)) return false;
        getLogger().info("Unloaded world " + world.getName());
        return true;
    }

    public void reloadAllConfigs() {
        reloadConfig();
        loadBuildWorlds();
        permission.reload();
        loadPlotWorlds();
    }

    public void loadBuildWorlds() {
        buildWorlds.clear();
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

    public void saveBuildWorlds() {
        if (buildWorlds == null) return;
        List<Object> list = new ArrayList<>();
        for (BuildWorld buildWorld : buildWorlds) {
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
        for (BuildWorld buildWorld : buildWorlds) {
            if (path.equals(buildWorld.getPath())) {
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
        for (BuildWorld buildWorld : buildWorlds) {
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

    public void loadPlotWorlds() {
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

    public boolean isPlotWorld(World world) {
        return plotWorlds.containsKey(world.getName());
    }

    public boolean canBuildInPlot(Player player, Block block) {
        PlotWorld plotWorld = plotWorlds.get(block.getWorld().getName());
        if (plotWorld == null) return false;
        return plotWorld.canBuild(player, block);
    }

    public PlotWorld getPlotWorld(World world) {
        return plotWorlds.get(world.getName());
    }

    public List<String> completeWorldNames(Player player, String arg) {
        List<String> result = new ArrayList<>();
        String argl = arg.toLowerCase();
        UUID uuid = player.getUniqueId();
        for (BuildWorld buildWorld : buildWorlds) {
            String name = buildWorld.getName();
            if (name == null) name = buildWorld.getPath();
            if (buildWorld.getTrust(uuid).canVisit() && name.toLowerCase().contains(argl)) {
                result.add(buildWorld.getName());
            }
        }
        return result;
    }

    public static CreativePlugin plugin() {
        return instance;
    }

    public boolean isCreativeServer() {
        return isCreativeServer;
    }
}
