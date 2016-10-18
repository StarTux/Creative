package com.winthier.creative;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class CreativePlugin extends JavaPlugin {
    private List<BuildWorld> buildWorlds;
    final WorldCommand worldCommand = new WorldCommand(this);
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
    }

    @Override
    public void onDisable() {
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
}
