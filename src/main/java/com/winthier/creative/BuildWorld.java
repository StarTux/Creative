package com.winthier.creative;

import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.core.perm.Perm;
import com.cavetale.core.playercache.PlayerCache;
import com.winthier.creative.file.Files;
import com.winthier.creative.sql.SQLWorld;
import com.winthier.creative.sql.SQLWorldTrust;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpawnCategory;
import static com.winthier.creative.ConnectListener.broadcastWorldUpdate;
import static com.winthier.creative.CreativePlugin.plugin;
import static com.winthier.creative.sql.Database.sql;

@Getter @Setter
public final class BuildWorld {
    private SQLWorld row;
    private final Map<UUID, SQLWorldTrust> trusted = new HashMap<>();
    // World Config
    private YamlConfiguration worldConfig = null;
    // World Border
    private transient long mobCooldown = 0;
    private transient boolean keepInMemory;

    public static final Comparator<BuildWorld> NAME_SORT = new Comparator<BuildWorld>() {
            @Override public int compare(BuildWorld a, BuildWorld b) {
                return String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName());
            }
        };

    public BuildWorld(final SQLWorld row, final List<SQLWorldTrust> trustedList) {
        this.row = row;
        for (SQLWorldTrust it : trustedList) {
            trusted.put(it.getPlayer(), it);
        }
    }

    public BuildWorld(final String name, final String path, final UUID owner) {
        this.row = new SQLWorld();
        row.setName(name);
        row.setPath(path);
        row.setOwner(owner);
        row.setCachedTag(new SQLWorld.Tag());
        row.setCreated(new Date());
    }

    private void info(String msg) {
        plugin().getLogger().info("[" + getPath() + "] " + msg);
    }

    private void severe(String msg) {
        plugin().getLogger().severe("[" + getPath() + "] " + msg);
    }

    public enum Flag {
        VOXEL_SNIPER("VoxelSniper", false, -1),
        WORLD_EDIT("WorldEdit", false, 1000),
        EXPLOSION("Explosion", false, 0),
        LEAF_DECAY("LeafDecay", false, 0),
        KEEP_IN_MEMORY("KeepInMemory", false, -1),
        COMMAND_BLOCKS("CommandBlocks", false, -1),
        PISTON("Piston", true, 0),
        REDSTONE("Redstone", true, 0),
        PROJECTILES("Projectiles", false, 0),
        MOBS("Mobs", false, 0),
        LIQUIDS("Liquids", false, 0),
        FALLING_BLOCKS("FallingBlocks", false, 0),
        LOCKED("Locked", false, -1);

        public final String key;
        public final boolean defaultValue;
        public final double price;

        Flag(final String key, final boolean defaultValue, final double price) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.price = price;
        }

        public boolean userCanEdit() {
            return price >= 0;
        }

        public static Flag of(String in) {
            for (Flag flag : Flag.values()) {
                if (in.equalsIgnoreCase(flag.key)) return flag;
                if (in.equalsIgnoreCase(flag.name())) return flag;
            }
            return null;
        }
    }

    public List<UUID> listTrusted(Trust trust) {
        List<UUID> result = new ArrayList<>();
        for (SQLWorldTrust it : trusted.values()) {
            if (it.getTrustValue() == trust) {
                result.add(it.getPlayer());
            }
        }
        return result;
    }

    public Trust getTrust(UUID uuid) {
        if (plugin().doesIgnore(uuid)) return Trust.OWNER;
        final UUID owner = row.getOwner();
        if (owner != null && owner.equals(uuid)) return Trust.OWNER;
        SQLWorldTrust worldTrust = trusted.get(uuid);
        if (worldTrust != null && worldTrust.getTrustValue().isOwner()) {
            return worldTrust.getTrustValue();
        }
        final List<String> buildGroups = row.getCachedTag().getBuildGroups();
        if (!buildGroups.isEmpty()) {
            for (String buildGroup : buildGroups) {
                if (Perm.get().isInGroup(uuid, buildGroup)) {
                    return Trust.WORLD_EDIT;
                }
            }
        }
        final Trust publicTrust = getPublicTrust();
        if (worldTrust == null) return publicTrust;
        Trust personalTrust = worldTrust.getTrustValue();
        return publicTrust.priority > personalTrust.priority
            ? publicTrust
            : personalTrust;
    }

    /**
     * Create or change a player's trust level.
     * @param uuid the palyer uuid
     * @param trust the trust type
     * @param callback the callback on success
     * @return true if something changed, false otherwise
     */
    public boolean setTrust(UUID uuid, Trust trust, Runnable callback) {
        final UUID owner = getOwner();
        if (owner != null && owner.equals(uuid)) return false;
        if (trust == Trust.NONE) {
            SQLWorldTrust worldTrust = trusted.remove(uuid);
            if (worldTrust == null) return false;
            sql().deleteAsync(worldTrust, returnValue -> {
                    if (returnValue == 0) {
                        severe("Could not delete trust: " + returnValue + ", " + worldTrust);
                    } else {
                        broadcastWorldUpdate(this);
                        callback.run();
                    }
                });
            return true;
        } else {
            SQLWorldTrust worldTrust = trusted.get(uuid);
            if (worldTrust == null) {
                final SQLWorldTrust newWorldTrust = new SQLWorldTrust(getPath(), uuid, trust);
                sql().insertAsync(newWorldTrust, returnValue -> {
                        if (returnValue == 0) {
                            severe("Could not insert trust: " + returnValue + ", " + newWorldTrust);
                        } else {
                            trusted.put(uuid, newWorldTrust);
                            broadcastWorldUpdate(this);
                            callback.run();
                        }
                    });
                return true;
            } else if (worldTrust.getTrustValue() == trust) {
                return false;
            } else {
                worldTrust.setTrust(trust.name().toLowerCase());
                sql().updateAsync(worldTrust, Set.of("trust"), returnValue -> {
                        if (returnValue == 0) {
                            severe("Could not update trust: " + returnValue + ", " + worldTrust);
                        } else {
                            broadcastWorldUpdate(this);
                            callback.run();
                        }
                    });
                return true;
            }
        }
    }

    public String getOwnerName() {
        if (row.getOwner() == null) {
            return "N/A";
        } else {
            return PlayerCache.nameForUuid(row.getOwner());
        }
    }

    public File getWorldFolder() {
        if (!plugin().isCreativeServer()) return null;
        return new File(plugin().getServer().getWorldContainer(), getPath());
    }

    public World getWorld() {
        if (!plugin().isCreativeServer()) return null;
        return Bukkit.getServer().getWorld(getPath());
    }

    public World loadWorld() {
        if (!plugin().isCreativeServer()) return null;
        final World alreadyLoaded = getWorld();
        if (alreadyLoaded != null) return alreadyLoaded;
        final File dir = getWorldFolder();
        if (!dir.isDirectory()) {
            severe("World folder does not exist: " + dir);
            return null;
        }
        return createWorld();
    }

    private World createWorld() {
        return createWorld(getPath());
    }

    private World createWorld(final String path) {
        WorldCreator creator = WorldCreator.name(path);
        creator.generator(row.getGenerator());
        creator.environment(row.getEnvironmentValue());
        creator.generateStructures(row.isGenerateStructures());
        if (row.getGeneratorSettings() != null) {
            creator.generatorSettings(row.getGeneratorSettings());
        }
        if (row.getSeed() != null) creator.seed(row.getSeed());
        creator.type(row.getWorldTypeValue());
        creator.keepSpawnLoaded(TriState.FALSE);
        final World result = creator.createWorld();
        result.setSpawnFlags(true, true);
        for (SpawnCategory spawnCategory : SpawnCategory.values()) {
            if (spawnCategory == SpawnCategory.MISC) continue;
            result.setSpawnLimit(spawnCategory, 0);
            result.setTicksPerSpawns(spawnCategory, 999999999);
        }
        result.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        result.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, true);
        result.setGameRule(GameRule.DISABLE_ELYTRA_MOVEMENT_CHECK, true);
        result.setGameRule(GameRule.DISABLE_RAIDS, true);
        //result.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        result.setGameRule(GameRule.DO_ENTITY_DROPS, false);
        result.setGameRule(GameRule.DO_FIRE_TICK, false);
        result.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        result.setGameRule(GameRule.DO_INSOMNIA, false);
        result.setGameRule(GameRule.DO_LIMITED_CRAFTING, false);
        result.setGameRule(GameRule.DO_MOB_LOOT, false);
        result.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        result.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        result.setGameRule(GameRule.DO_TILE_DROPS, false);
        result.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        result.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        result.setGameRule(GameRule.DROWNING_DAMAGE, true);
        result.setGameRule(GameRule.FALL_DAMAGE, true);
        result.setGameRule(GameRule.FIRE_DAMAGE, true);
        result.setGameRule(GameRule.FORGIVE_DEAD_PLAYERS, true);
        result.setGameRule(GameRule.FREEZE_DAMAGE, true);
        result.setGameRule(GameRule.KEEP_INVENTORY, true);
        result.setGameRule(GameRule.LOG_ADMIN_COMMANDS, true);
        result.setGameRule(GameRule.MAX_COMMAND_CHAIN_LENGTH, 1);
        result.setGameRule(GameRule.MAX_ENTITY_CRAMMING, 0);
        result.setGameRule(GameRule.MOB_GRIEFING, false);
        result.setGameRule(GameRule.NATURAL_REGENERATION, true);
        result.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 101);
        result.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        result.setGameRule(GameRule.REDUCED_DEBUG_INFO, false);
        result.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, true);
        result.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        result.setGameRule(GameRule.SPAWN_RADIUS, 0);
        result.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        result.setGameRule(GameRule.UNIVERSAL_ANGER, false);
        WorldBorder border = result.getWorldBorder();
        if (row.getBorderSize() > 0) {
            border.setCenter(row.getBorderCenterX(), row.getBorderCenterZ());
            border.setSize((double) row.getBorderSize());
        }
        if (row.isSpawnSet()) {
            result.setSpawnLocation(getSpawnLocation());
        }
        return result;
    }

    public ConfigurationSection getWorldConfig() {
        if (worldConfig == null) {
            File dir = getWorldFolder();
            dir.mkdirs();
            File file = new File(dir, "config.yml");
            worldConfig = YamlConfiguration.loadConfiguration(file);
        }
        return worldConfig;
    }

    public void reloadWorldConfig() {
        worldConfig = null;
    }

    public void saveWorldConfig() {
        if (worldConfig == null) return;
        try {
            worldConfig.save(new File(getWorldFolder(), "config.yml"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void teleportToSpawn(Player player) {
        Location loc = getSpawnLocation();
        if (loc == null) return;
        player.teleportAsync(loc);
    }

    public Location getSpawnLocation() {
        World w = getWorld();
        if (w == null) return null;
        return new Location(w, row.getSpawnX(), row.getSpawnY(), row.getSpawnZ(),
                            (float) row.getSpawnYaw(), (float) row.getSpawnPitch());
    }

    public void setSpawnLocation(Location loc) {
        Block block = loc.getBlock();
        if (getWorld() != null) {
            getWorld().setSpawnLocation(loc);
        }
        row.setSpawnX(loc.getX());
        row.setSpawnY(loc.getY());
        row.setSpawnZ(loc.getZ());
        row.setSpawnYaw(loc.getYaw());
        row.setSpawnPitch(loc.getPitch());
    }

    public boolean isSet(Flag flag) {
        return row.getCachedTag().getFlags().getOrDefault(flag, flag.defaultValue);
    }

    public void set(Flag flag, boolean value) {
        row.getCachedTag().getFlags().put(flag, value);
    }

    /**
     * Used by AdminCommand::rankTrustCommand.
     */
    protected int trustedScore() {
        return row.getPublicTrustValue().canBuild()
            ? Integer.MAX_VALUE
            : trusted.size();
    }

    public String getPath() {
        return row.getPath();
    }

    public String getName() {
        return row.getName();
    }

    public UUID getOwner() {
        return row.getOwner();
    }

    public List<String> getBuildGroups() {
        return row.getCachedTag().getBuildGroups();
    }

    public Trust getPublicTrust() {
        return row.getPublicTrustValue();
    }

    public void saveAsync(String rowName, Runnable callback) {
        saveAsync(Set.of(rowName), callback);
    }

    public void saveSpawnAsync(Runnable callback) {
        saveAsync(Set.of("spawnX", "spawnY", "spawnZ", "spawnYaw", "spawnPitch"), callback);
    }

    public void savePurposeAsync(Runnable callback) {
        saveAsync(Set.of("purpose", "purposeType", "purposeConfirmed"), callback);
    }

    public void saveAsync(Set<String> rowNames, Runnable callback) {
        if (rowNames.contains("tag")) {
            row.pack();
        }
        sql().updateAsync(row, rowNames, returnValue -> {
                if (returnValue == 0) {
                    severe("Could not update `" + rowNames + "`: " + returnValue + ", " + row);
                } else {
                    broadcastWorldUpdate(this);
                    callback.run();
                }
            });
    }

    public void insertAsync(Runnable callback) {
        row.pack();
        sql().insertAsync(row, returnValue -> {
                if (returnValue == 0) {
                    severe("Could not insert: " + returnValue + ", " + row);
                } else {
                    broadcastWorldUpdate(this);
                    callback.run();
                }
            });
    }

    public void deleteAsync(Runnable callback) {
        sql().deleteAsync(row, returnValue -> {
                if (returnValue == 0) {
                    severe("Could not delete: " + returnValue + ", " + row);
                } else {
                    broadcastWorldUpdate(this);
                    callback.run();
                }
            });
    }

    public int clearTrustAsync(Runnable callback) {
        List<SQLWorldTrust> list = List.copyOf(trusted.values());
        sql().deleteAsync(list, returnValue -> {
                if (returnValue == 0) {
                    severe("Could not clear trust: " + returnValue + ", " + list);
                } else {
                    broadcastWorldUpdate(this);
                    trusted.clear();
                    callback.run();
                }
            });
        return list.size();
    }

    public void makeCopyAsync(Consumer<World> callback) {
        final File src = new File("/home/cavetale/creative/worlds/" + getPath());
        if (!src.exists()) {
            throw new IllegalStateException("Source folder not found: " + src);
        }
        String tmpPath = null;
        File tmpDest = null;
        for (int suffix = 0; suffix <= 999; suffix += 1) {
            tmpPath = String.format("tmp_%03d_%s", suffix, getPath());
            tmpDest = new File(Bukkit.getWorldContainer(), tmpPath);
            if (tmpDest.exists()) continue;
            break;
        }
        final File finalDest = tmpDest;
        if (finalDest.exists()) {
            throw new IllegalStateException("Cannot find folder: " + getPath());
        }
        info("BuildWorld#makeCopyAsync using dest: " + finalDest);
        finalDest.mkdirs();
        final String finalPath = tmpPath;
        Bukkit.getScheduler().runTaskAsynchronously(plugin(), () -> {
                Files.copyFileStructure(src, finalDest);
                Bukkit.getScheduler().runTask(plugin(), () -> {
                        final World world = createWorld(finalPath);
                        callback.accept(world);
                    });
            });
    }

    public static List<BuildWorld> findMinigameWorlds(MinigameMatchType type, boolean requireConfirmation) {
        List<BuildWorld> result = new ArrayList<>();
        for (BuildWorld it : plugin().getBuildWorlds()) {
            if (it.getRow().parsePurpose() != BuildWorldPurpose.MINIGAME) continue;
            if (it.getRow().parseMinigame() != type) continue;
            if (requireConfirmation && !it.getRow().isPurposeConfirmed()) continue;
            result.add(it);
        }
        return result;
    }
}
