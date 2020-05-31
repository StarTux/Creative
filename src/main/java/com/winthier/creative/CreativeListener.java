package com.winthier.creative;

import com.winthier.generic_events.PlayerCanBuildEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FireworkExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

@RequiredArgsConstructor
public final class CreativeListener implements Listener {
    final CreativePlugin plugin;

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Store Logout Location
        plugin.storeLogoutLocation(player);
        plugin.saveLogoutLocations();
        // Reset Permissions
        plugin.getPermission().resetPermissions(player);
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
        ConfigurationSection config = plugin.getLogoutLocations()
            .getConfigurationSection(uuid.toString());
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
        updatePermissions(player);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        updatePermissions(event.getPlayer());
        unloadEmptyWorld(event.getFrom());
    }

    void updatePermissions(Player player) {
        new BukkitRunnable() {
            @Override public void run() {
                plugin.getPermission().updatePermissions(player);
            }
        }.runTask(plugin);
    }

    void unloadEmptyWorld(World world) {
        if (!plugin.isEnabled()) return;
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
        if (buildWorld == null) return;
        if (buildWorld.isSet(BuildWorld.Flag.KEEP_IN_MEMORY)) return;
        final String name = world.getName();
        new BukkitRunnable() {
            @Override public void run() {
                World world = plugin.getServer().getWorld(name);
                if (world == null) return;
                if (!world.getPlayers().isEmpty()) return;
                if (!plugin.getServer().unloadWorld(world, true)) return;
                plugin.getLogger().info("Unloaded world " + name);
            }
        }.runTaskLater(plugin, 200L);
    }

    // Build Permission Check

    void checkBuildEvent(Player player, Block block, Cancellable event) {
        if (plugin.doesIgnore(player)) return;
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null) {
            event.setCancelled(true);
            return;
        }
        if (block != null && plugin.isPlotWorld(block.getWorld())) {
            if (!plugin.canBuildInPlot(player, block)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!buildWorld.getTrust(player.getUniqueId()).canBuild()) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        checkBuildEvent(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        checkBuildEvent(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && event.hasBlock()) {
            // Turtle eggs, farmland, maybe more
            Material mat = event.getClickedBlock().getType();
            if (!mat.name().contains("PRESSURE_PLATE")) {
                event.setCancelled(true);
                return;
            }
        }
        checkBuildEvent(event.getPlayer(), event.getClickedBlock(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Block block = event.getRightClicked().getLocation().getBlock();
        checkBuildEvent(event.getPlayer(), block, event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
        Block block = event.getRightClicked().getLocation().getBlock();
        checkBuildEvent(event.getPlayer(), block, event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player) {
            checkBuildEvent((Player) event.getEntity(), event.getBlock(), event);
        }
    }

    // Explosion

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityExplode(EntityExplodeEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getEntity().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.EXPLOSION)) {
            event.setCancelled(true);
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockExplode(BlockExplodeEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.EXPLOSION)) {
            event.setCancelled(true);
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLeavesDecay(LeavesDecayEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.LEAF_DECAY)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void playerCanBuild(PlayerCanBuildEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        Block block;
        if (event.getSender() instanceof BlockCommandSender) {
            BlockCommandSender sender = (BlockCommandSender) event.getSender();
            block = sender.getBlock();
        } else if (event.getSender() instanceof CommandMinecart) {
            CommandMinecart cart = (CommandMinecart) event.getSender();
            block = cart.getLocation().getBlock();
        } else {
            return;
        }
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(block.getWorld());
        if (buildWorld == null) return;
        String msg = "CommandBlock at " + block.getWorld().getName()
            + " " + block.getX()
            + " " + block.getY()
            + " " + block.getZ()
            + " permitted=" + buildWorld.isSet(BuildWorld.Flag.COMMAND_BLOCKS)
            + " command=" + event.getCommand();
        plugin.getLogger().info(msg);
        if (!buildWorld.isSet(BuildWorld.Flag.COMMAND_BLOCKS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityPlace(EntityPlaceEvent event) {
        if (event.getPlayer().isOp()) return;
        if (event.getEntity() instanceof CommandMinecart) {
            plugin.getLogger().info(event.getPlayer().getName()
                                    + " tried placing Command Minecart");
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        BuildWorld buildWorld = plugin
            .getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.PISTON)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        BuildWorld buildWorld = plugin
            .getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.PISTON)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent event) {
        BuildWorld buildWorld = plugin
            .getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.REDSTONE)) {
            event.setNewCurrent(event.getOldCurrent());
            return;
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        switch (event.getEntity().getType()) {
        case FIREWORK:
            event.setCancelled(true);
        default:
            break;
        }
    }

    @EventHandler
    public void onFireworkExplode(FireworkExplodeEvent event) {
        event.setCancelled(true);
    }
}
