package com.winthier.creative;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

@RequiredArgsConstructor
public class CreativeListener implements Listener {
    final CreativePlugin plugin;
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Store Logout Location
        plugin.storeLogoutLocation(player);
        plugin.saveLogoutLocations();
        // Reset Permissions
        plugin.permission.resetPermissions(player);
        // Unload Empty World
        unloadEmptyWorld(player.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        // Store Logout Location
        plugin.storeLogoutLocation(player);
        plugin.saveLogoutLocations();
        // Reset Permissions
        plugin.permission.resetPermissions(player);
        // Unload Empty World
        unloadEmptyWorld(player.getWorld());
    }

    @EventHandler
    public void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        Location loc = plugin.findSpawnLocation(event.getPlayer());
        if (loc == null) {
            loc = plugin.getServer().getWorlds().get(0).getSpawnLocation();
        }
        event.setSpawnLocation(loc);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
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
        player.setGameMode(gamemode);
        // Update Permission
        plugin.permission.updatePermissions(player);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.permission.updatePermissions(event.getPlayer());
        unloadEmptyWorld(event.getFrom());
    }

    void unloadEmptyWorld(World world) {
        // if (!plugin.isEnabled()) return;
        // final String name = world.getName();
        // new BukkitRunnable() {
        //     @Override public void run() {
        //         World world = plugin.getServer().getWorld(name);
        //         if (world == null) return;
        //         if (!world.getPlayers().isEmpty()) return;
        //         if (!plugin.getServer().unloadWorld(world, true)) return;
        //         plugin.getLogger().info("Unloaded world " + name);
        //     }
        // }.runTaskLater(plugin, 1L);
    }

    // Build Permission Check

    void checkBuildEvent(Player player, Cancellable event) {
        if (plugin.doesIgnore(player)) return;
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null) {
            event.setCancelled(true);
            return;
        }
        if (!buildWorld.getTrust(player.getUniqueId()).canBuild()) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        checkBuildEvent(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        checkBuildEvent(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        checkBuildEvent(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        checkBuildEvent(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
        checkBuildEvent(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player) {
            checkBuildEvent((Player)event.getEntity(), event);
        }
    }
}
