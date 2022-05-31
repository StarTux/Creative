package com.winthier.creative;

import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.core.event.block.PlayerBreakBlockEvent;
import com.cavetale.core.event.player.PlayerTPAEvent;
import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
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
import org.bukkit.entity.Projectile;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
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
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class CreativeListener implements Listener {
    private final CreativePlugin plugin;

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Store Logout Location
        plugin.storeLogoutLocation(player);
        plugin.saveLogoutLocations();
        // Reset Permissions
        plugin.getPermission().resetPermissions(player);
        // Unload Empty World
        unloadEmptyWorldLater(player.getWorld());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        Location loc = plugin.findSpawnLocation(event.getPlayer());
        if (loc == null) {
            loc = plugin.getServer().getWorlds().get(0).getSpawnLocation();
        }
        event.setSpawnLocation(loc);
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
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
    private void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        updatePermissions(event.getPlayer());
        unloadEmptyWorldLater(event.getFrom());
    }

    private void updatePermissions(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getPermission().updatePermissions(player));
    }

    private void unloadEmptyWorldLater(final World theWorld) {
        if (!plugin.isEnabled()) return;
        if (theWorld.getPlayers().size() > 1) return;
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(theWorld);
        if (buildWorld == null) return;
        if (buildWorld.isSet(BuildWorld.Flag.KEEP_IN_MEMORY)) return;
        if (buildWorld.isKeepInMemory()) return;
        final String name = theWorld.getName();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                World world = Bukkit.getWorld(name);
                if (world != null) {
                    plugin.unloadEmptyWorld(world);
                }
            }, 600L);
    }

    /**
     * Generic build permission check.
     */
    private boolean checkBuildEvent(Player player, Block block, Cancellable event) {
        if (plugin.doesIgnore(player)) return true;
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null) {
            event.setCancelled(true);
            return false;
        }
        if (block != null && plugin.isPlotWorld(block.getWorld())) {
            if (!plugin.canBuildInPlot(player, block)) {
                event.setCancelled(true);
                return false;
            }
            return true;
        }
        if (!buildWorld.getTrust(player.getUniqueId()).canBuild()) {
            event.setCancelled(true);
            return false;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onBlockBreak(BlockBreakEvent event) {
        checkBuildEvent(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onBlockPlace(BlockPlaceEvent event) {
        checkBuildEvent(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOW)
    private void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        checkBuildEvent(player, event.getClickedBlock(), event);
        if (event.useInteractedBlock() == Event.Result.DENY) return;
        switch (event.getAction()) {
        case PHYSICAL: {
            if (!event.hasBlock()) return;
            // Turtle eggs, farmland, maybe more
            Material mat = event.getClickedBlock().getType();
            if (Tag.PRESSURE_PLATES.isTagged(mat)) return;
            event.setCancelled(true);
            return;
        }
        case RIGHT_CLICK_BLOCK: {
            if (!event.hasBlock()) return;
            if (player.isSneaking()) return;
            Material blockType = event.getClickedBlock().getType();
            if (blockType == Material.CAKE) {
                event.setCancelled(true);
                return;
            }
        }
        default: break;
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Block block = event.getRightClicked().getLocation().getBlock();
        checkBuildEvent(event.getPlayer(), block, event);
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
        Block block = event.getRightClicked().getLocation().getBlock();
        checkBuildEvent(event.getPlayer(), block, event);
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onEntityChangeBlock(EntityChangeBlockEvent event) {
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
    private void onEntityBlockForm(EntityBlockFormEvent event) {
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
    private void onBlockForm(BlockFormEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        event.setCancelled(true);
    }

    // Explosion

    @EventHandler(priority = EventPriority.LOW)
    private void onEntityExplode(EntityExplodeEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getEntity().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.EXPLOSION)) {
            event.setCancelled(true);
        }
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onBlockExplode(BlockExplodeEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.EXPLOSION)) {
            event.setCancelled(true);
        }
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onLeavesDecay(LeavesDecayEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.LEAF_DECAY)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerBlockAbility(PlayerBlockAbilityQuery query) {
        checkBuildEvent(query.getPlayer(), query.getBlock(), query);
    }

    @EventHandler
    private void onPlayerBreakBlock(PlayerBreakBlockEvent event) {
        checkBuildEvent(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler
    private void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    private void onProjectileLaunch(ProjectileLaunchEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getEntity().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.PROJECTILES)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onServerCommand(ServerCommandEvent event) {
        Block block;
        CommandMinecart cart = null;
        if (event.getSender() instanceof BlockCommandSender) {
            BlockCommandSender sender = (BlockCommandSender) event.getSender();
            block = sender.getBlock();
        } else if (event.getSender() instanceof CommandMinecart) {
            cart = (CommandMinecart) event.getSender();
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
            if (cart != null) {
                cart.remove();
            } else {
                block.setType(Material.AIR, false);
            }
        }
    }

    @EventHandler
    private void onEntityPlace(EntityPlaceEvent event) {
        if (event.getPlayer().isOp()) return;
        if (event.getEntity() instanceof CommandMinecart) {
            plugin.getLogger().info(event.getPlayer().getName() + " tried placing Command Minecart");
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    private void onBlockPistonExtend(BlockPistonExtendEvent event) {
        BuildWorld buildWorld = plugin
            .getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.PISTON)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    private void onBlockPistonRetract(BlockPistonRetractEvent event) {
        BuildWorld buildWorld = plugin
            .getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.PISTON)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    private void onBlockRedstone(BlockRedstoneEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.REDSTONE)) {
            event.setNewCurrent(event.getOldCurrent());
            return;
        }
    }

    @EventHandler
    private void onEntitySpawn(EntitySpawnEvent event) {
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
    private void onFireworkExplode(FireworkExplodeEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getEntity().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.isSet(BuildWorld.Flag.PROJECTILES)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerPortal(PlayerPortalEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    private void onEntityPortal(EntityPortalEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        checkBuildEvent((Player) event.getDamager(), event.getEntity().getLocation().getBlock(), event);
    }

    @EventHandler
    private void onCreatureSpawn(CreatureSpawnEvent event) {
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
            Component name = event.getEntity().customName();
            if (name != null && !Component.empty().equals(name)) {
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
    private void onBlockFromTo(BlockFromToEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        if (event.getBlock().isLiquid()) {
            if (!buildWorld.isSet(BuildWorld.Flag.LIQUIDS)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onHangingBreak(HangingBreakEvent event) {
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
    private void onInventoryCreative(InventoryCreativeEvent event) {
        ItemStack item = event.getCursor();
        if (item == null || item.getType() == Material.AIR) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (player.isOp()) return;
        if (item.isSimilar(new ItemStack(item.getType()))) return;
        if (item.serializeAsBytes().length < 1024) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    private void onTNTPrime(TNTPrimeEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getBlock().getWorld());
        if (buildWorld == null) return;
        event.setCancelled(true);
    }

    /**
     * Intercept teleports into locked worlds.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onPlayerTeleportWorlds(PlayerTeleportEvent event) {
        if (Objects.equals(event.getFrom().getWorld(), event.getTo().getWorld())) return;
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getTo().getWorld());
        if (buildWorld == null) return;
        Player player = event.getPlayer();
        if (event.getCause() == TeleportCause.SPECTATE) {
            if (!buildWorld.getTrust(player.getUniqueId()).canVisit()) {
                event.setCancelled(true);
                player.sendMessage(text("You cannot enter this world", RED));
                return;
            }
        }
        if (buildWorld.isSet(BuildWorld.Flag.LOCKED)) {
            if (!buildWorld.getTrust(player.getUniqueId()).canVisit()) {
                event.setCancelled(true);
                player.sendMessage(text("This world is locked", RED));
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onPlayerTPA(PlayerTPAEvent event) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(event.getTarget().getWorld());
        if (buildWorld == null) return;
        if (!buildWorld.getTrust(event.getRequester()).canVisit()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            checkBuildEvent(player, event.getEntity().getLocation().getBlock(), event);
        } else if (event.getRemover() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                checkBuildEvent(player, event.getEntity().getLocation().getBlock(), event);
            } else {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }
}
