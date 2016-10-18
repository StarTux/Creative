package com.winthier.creative;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

@RequiredArgsConstructor
public class CreativeListener implements Listener {
    final CreativePlugin plugin;
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Location loc = event.getPlayer().getLocation();
        ConfigurationSection config = plugin.getLogoutLocations().createSection(uuid.toString());
        config.set("world", loc.getWorld().getName());;
        config.set("x", loc.getX());
        config.set("y", loc.getY());
        config.set("z", loc.getZ());
        config.set("yaw", loc.getYaw());
        config.set("pitch", loc.getPitch());
        config.set("gamemode", event.getPlayer().getGameMode().name());
        plugin.saveLogoutLocations();
    }

    @EventHandler
    public void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Location loc = findBuildWorldSpawnLocation(uuid);
        if (loc == null) {
            loc = plugin.getServer().getWorlds().get(0).getSpawnLocation();
        }
        event.setSpawnLocation(loc);
    }

    Location findBuildWorldSpawnLocation(UUID uuid) {
        ConfigurationSection config = plugin.getLogoutLocations().getConfigurationSection(uuid.toString());
        if (config == null) return null;
        String worldName = config.getString("world");
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldName);
        if (buildWorld == null) return null;
        if (!buildWorld.getTrust(uuid).canVisit()) return null;
        buildWorld.loadWorld();
        return buildWorld.getSpawnLocation();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        ConfigurationSection config = plugin.getLogoutLocations().getConfigurationSection(uuid.toString());
        GameMode gamemode;
        if (config == null) {
            gamemode = GameMode.CREATIVE;
        } else {
            try {
                gamemode = GameMode.valueOf(config.getString("gamemode", "CREATIVE"));
            } catch (IllegalArgumentException iae) {
                gamemode = GameMode.CREATIVE;
            }
        }
        event.getPlayer().setGameMode(gamemode);
    }
}
