package com.winthier.creative;

import com.cavetale.core.event.block.PlayerCanBuildEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FireworkExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
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
        unloadEmptyWorldLater(player.getWorld());
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
        unloadEmptyWorldLater(event.getFrom());
    }

    void updatePermissions(Player player) {
        new BukkitRunnable() {
            @Override public void run() {
                plugin.getPermission().updatePermissions(player);
            }
        }.runTask(plugin);
    }

    private void unloadEmptyWorldLater(final World theWorld) {
        if (!plugin.isEnabled()) return;
        if (!theWorld.getPlayers().isEmpty()) return;
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(theWorld);
        if (buildWorld == null) return;
        if (buildWorld.isSet(BuildWorld.Flag.KEEP_IN_MEMORY)) return;
        final String name = theWorld.getName();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                World world = Bukkit.getWorld(name);
                if (world != null) {
                    plugin.unloadEmptyWorld(world);
                }
            }, 600L);
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
            if (!Tag.PRESSURE_PLATES.isTagged(mat)) {
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
        } else if (event.getEntity() instanceof FallingBlock) {
            if (event.getTo().isAir()) {
                BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
                if (buildWorld == null) return;
                if (!buildWorld.isSet(BuildWorld.Flag.FALLING_BLOCKS)) {
                    event.setCancelled(true);
                }
            }
        } else {
            BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
            if (buildWorld == null) return;
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        if (event.getEntity() instanceof Player) {
            checkBuildEvent((Player) event.getEntity(), event.getBlock(), event);
        } else {
            BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
            if (buildWorld == null) return;
            event.setCancelled(true);
            return;
        }
    }

    /**
     * Called when a block is formed or spreads based on world
     * conditions.
     *
     * Use BlockSpreadEvent to catch blocks that actually spread and
     * don't just "randomly" form.
     *
     * Examples:
     * - Snow forming due to a snow storm.
     * - Ice forming in a snowy Biome like Taiga or Tundra.
     * - Obsidian / Cobblestone forming due to contact with water.
     * - Concrete forming due to mixing of concrete powder and water.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    void onBlockForm(BlockFormEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        event.setCancelled(true);
    }

    // Explosion

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityExplode(EntityExplodeEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getEntity().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.EXPLOSION)) {
            event.setCancelled(true);
        }
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockExplode(BlockExplodeEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.EXPLOSION)) {
            event.setCancelled(true);
        }
        event.blockList().clear();
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
        checkBuildEvent(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getEntity().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.PROJECTILES)) {
            event.setCancelled(true);
        }
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
            plugin.getLogger().info(event.getPlayer().getName() + " tried placing Command Minecart");
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
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.REDSTONE)) {
            event.setNewCurrent(event.getOldCurrent());
            return;
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        World world = event.getEntity().getWorld();
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
        if (buildWorld == null) return;
        switch (event.getEntity().getType()) {
        case FIREWORK:
            if (!buildWorld.isSet(BuildWorld.Flag.PROJECTILES)) {
                event.setCancelled(true);
            }
            break;
        case FALLING_BLOCK:
            if (!buildWorld.isSet(BuildWorld.Flag.FALLING_BLOCKS)) {
                event.setCancelled(true);
            } else {
                final int count = world.getEntitiesByClass(FallingBlock.class).size();
                final int max = plugin.getMaxFallingBlocks();
                if (count < max) {
                    event.getEntity().setPersistent(false);
                } else {
                    event.setCancelled(true);
                }
            }
            break;
        default:
            break;
        }
    }

    @EventHandler
    public void onFireworkExplode(FireworkExplodeEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getEntity().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.PROJECTILES)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onPlayerPortal(PlayerPortalEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    void onEntityPortal(EntityPortalEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        checkBuildEvent((Player) event.getDamager(), event.getEntity().getLocation().getBlock(), event);
    }

    @EventHandler
    void onCreatureSpawn(CreatureSpawnEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getLocation().getWorld());
        if (buildWorld == null) return;
        switch (event.getSpawnReason()) {
        case CUSTOM:
        case SHOULDER_ENTITY:
            return;
        case SPAWNER_EGG: {
            if (!buildWorld.isSet(BuildWorld.Flag.MOBS)) {
                event.setCancelled(true);
                return;
            }
            long now = Instant.now().getEpochSecond();
            if (now <= buildWorld.getMobCooldown()) {
                event.setCancelled(true);
                return;
            }
            String name = event.getEntity().getCustomName();
            if (name != null && !name.isEmpty()) {
                event.setCancelled(true);
                return;
            }
            buildWorld.setMobCooldown(now + 2);
            return;
        }
        case DEFAULT:
            if (event.getEntityType() == EntityType.ARMOR_STAND) return;
        default:
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    void onBlockFromTo(BlockFromToEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (event.getBlock().isLiquid()) {
            if (!buildWorld.isSet(BuildWorld.Flag.LIQUIDS)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    void onHangingBreak(HangingBreakEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getEntity().getWorld());
        if (buildWorld == null) return;
        switch (event.getCause()) {
        case OBSTRUCTION:
        case PHYSICS:
            event.setCancelled(true);
            break;
        default:
            break;
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onInventoryCreative(InventoryCreativeEvent event) {
        ItemStack item = event.getCursor();
        if (item == null || item.getType() == Material.AIR) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (player.isOp()) return;
        if (item.isSimilar(new ItemStack(item.getType()))) return;
        if (item.serializeAsBytes().length < 1024) return;
        event.setCancelled(true);
    }
}
