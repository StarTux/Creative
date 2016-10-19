package com.winthier.creative;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

@RequiredArgsConstructor
public class CreativeListener implements Listener {
    final CreativePlugin plugin;
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Store Logout Location
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();
        ConfigurationSection config = plugin.getLogoutLocations().createSection(uuid.toString());
        config.set("world", loc.getWorld().getName());;
        config.set("x", loc.getX());
        config.set("y", loc.getY());
        config.set("z", loc.getZ());
        config.set("yaw", loc.getYaw());
        config.set("pitch", loc.getPitch());
        config.set("gamemode", player.getGameMode().name());
        plugin.saveLogoutLocations();
        // Reset Permissions
        plugin.permission.resetPermissions(player);
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
        World world = buildWorld.loadWorld();
        if (world == null) return null;
        double x = config.getDouble("x");
        double y = config.getDouble("y");
        double z = config.getDouble("z");
        float yaw = (float)config.getDouble("yaw");
        float pitch = (float)config.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
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

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.permission.updatePermissions(event.getPlayer());
        World from = event.getFrom();
        if (from.getPlayers().isEmpty()) {
            if (plugin.getServer().unloadWorld(from, true)) {
                plugin.getLogger().info("Unloaded world " + from.getName());
            }
        }
    }
}
