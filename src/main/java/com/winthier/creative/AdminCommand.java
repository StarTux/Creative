package com.winthier.creative;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
final class AdminCommand implements CommandExecutor {
    final CreativePlugin plugin;
    private BukkitRunnable updateTask;

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 0) return false;
        String cmd = args[0];
        String[] argl = Arrays.copyOfRange(args, 1, args.length);
        switch (cmd) {
        case "reload": return reloadCommand(sender, argl);
        case "info": return infoCommand(sender, argl);
        case "set": return setCommand(sender, argl);
        case "config": return configCommand(sender, argl);
        case "listunregistered": return listUnregisteredCommand(sender, argl);
        case "list": return listCommand(sender, argl);
        case "who": return whoCommand(sender, argl);
        case "listloaded": return listLoadedCommand(sender, argl);
        case "tp": return tpCommand(sender, argl);
        case "remove": return removeCommand(sender, argl);
        case "resetowner": return resetOwnerCommand(sender, argl);
        case "setowner": return setOwnerCommand(sender, argl);
        case "create": return createCommand(sender, argl);
        case "import": return importCommand(sender, argl);
        case "load": return loadCommand(sender, argl);
        case "unload": return unloadCommand(sender, argl);
        case "ignore": return ignoreCommand(sender, argl);
        case "warp": return warpCommand(sender, argl);
        case "setwarp": return setWarpCommand(sender, argl);
        default: return false;
        }
    }

    boolean reloadCommand(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.reloadAllConfigs();
        sender.sendMessage("Configs reloaded");
        return true;
    }

    boolean infoCommand(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        BuildWorld buildWorld;
        if (args.length == 0) {
            Player player = sender instanceof Player
                ? (Player) sender
                : null;
            if (player == null) return false;
            buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
            if (buildWorld == null) {
                player.sendMessage(ChatColor.RED
                                   + "This is not a build world!");
                return true;
            }
        } else {
            String name = args[0];
            buildWorld = plugin.getBuildWorldByPath(name);
            if (buildWorld == null) {
                sender.sendMessage("World not found: " + name);
                return true;
            }
        }
        sender.sendMessage("Name: " + buildWorld.getName());
        sender.sendMessage("Path: " + buildWorld.getPath());
        sender.sendMessage("Owner: " + buildWorld.getOwnerName());
        sender.sendMessage("Trusted: " + buildWorld.getTrusted().values().stream()
                           .map(trusted -> trusted.getBuilder().getName()
                                + "=" + trusted.getTrust().nice())
                           .collect(Collectors.joining(" ")));
        sender.sendMessage("Public Trust: " + buildWorld.getPublicTrust());
        sender.sendMessage("VoxelSniper: " + buildWorld.isVoxelSniper());
        sender.sendMessage("Explosion: " + buildWorld.isExplosion());
        sender.sendMessage("LeafDecay: " + buildWorld.isLeafDecay());
        sender.sendMessage("KeepInMemory: " + buildWorld.isKeepInMemory());
        sender.sendMessage("CommandBlocks: " + buildWorld.isCommandBlocks());
        return true;
    }

    boolean setCommand(CommandSender sender, String[] args) {
        Player player = sender instanceof Player
            ? (Player) sender
            : null;
        BuildWorld buildWorld;
        String key;
        String value;
        if (args.length == 3) {
            buildWorld = plugin.getBuildWorldByPath(args[0]);
            if (buildWorld == null) {
                sender.sendMessage("World not found: " + args[0]);
                return true;
            }
            key = args[1];
            value = args[2];
        } else if (args.length == 2) {
            if (player == null) return false;
            buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
            if (buildWorld == null) {
                player.sendMessage(ChatColor.RED
                                   + "This is not a build world!");
                return true;
            }
            key = args[0];
            value = args[1];
        } else {
            return false;
        }
        boolean newValue;
        switch (value.toLowerCase()) {
        case "false": case "off": case "no":
            newValue = false; break;
        case "true": case "on": case "yes":
            newValue = true; break;
        default:
            sender.sendMessage("Unknown value: " + value);
            return true;
        }
        switch (key.toLowerCase()) {
        case "voxelsniper":
            buildWorld.setVoxelSniper(newValue);
            sender.sendMessage("Set VoxelSniper=" + buildWorld.isVoxelSniper());
            break;
        case "explosion":
            buildWorld.setExplosion(newValue);
            sender.sendMessage("Set Explosion=" + buildWorld.isExplosion());
            break;
        case "leafdecay":
            buildWorld.setLeafDecay(newValue);
            sender.sendMessage("Set LeafDecay=" + buildWorld.isLeafDecay());
            break;
        case "keepinmemory":
            buildWorld.setKeepInMemory(newValue);
            sender.sendMessage("Set KeepInMemory=" + buildWorld.isKeepInMemory());
            break;
        case "commandblocks":
            buildWorld.setCommandBlocks(newValue);
            sender.sendMessage("Set CommandBlocks=" + buildWorld.isCommandBlocks());
            break;
        default:
            sender.sendMessage("Unknown settings: " + key);
            return true;
        }
        plugin.saveBuildWorlds();
        return true;
    }

    boolean configCommand(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String name = args[0];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(name);
        if (buildWorld == null) {
            sender.sendMessage("World not found: " + name);
            return true;
        }
        for (String key: buildWorld.getWorldConfig().getKeys(true)) {
            Object o = buildWorld.getWorldConfig().get(key);
            if (o instanceof ConfigurationSection) continue;
            sender.sendMessage(key + "='" + o + "'");
        }
        return true;
    }

    boolean listUnregisteredCommand(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage("Unregistered worlds:");
        int count = 0;
        for (String dir: plugin.getServer().getWorldContainer().list()) {
            if (plugin.getBuildWorldByPath(dir) == null) {
                sender.sendMessage(" " + dir);
                count += 1;
            }
        }
        sender.sendMessage("" + count + " worlds listed.");
        return true;
    }

    boolean listCommand(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (args.length == 0) {
            int count = 0;
            for (BuildWorld buildWorld: plugin.getBuildWorlds()) {
                sender.sendMessage(buildWorld.getName() + " /"
                                   + buildWorld.getPath() + " " + buildWorld.getOwnerName());
                count += 1;
            }
            sender.sendMessage("" + count + " build worlds listed");
            return true;
        }
        String name = args[0];
        Builder builder = Builder.find(name);
        if (builder == null) {
            sender.sendMessage("Builder not found: " + name);
            return true;
        }
        PlayerWorldList list = plugin.getPlayerWorldList(builder.getUuid());
        Msg.send(sender, "&e%s World List", builder.getName());
        if (!list.owner.isEmpty()) {
            Msg.send(sender, "&7Owner (&r%d&7)", list.owner.size());
            for (BuildWorld bw: list.owner) {
                Msg.send(sender, "&a%s &8/%s", bw.getPath(), bw.getName());
            }
        }
        if (!list.build.isEmpty()) {
            Msg.send(sender, "&7Build (&r%d&7)", list.build.size());
            for (BuildWorld bw: list.build) {
                Msg.send(sender, "&a%s &8/%s", bw.getPath(), bw.getName());
            }
        }
        if (!list.visit.isEmpty()) {
            Msg.send(sender, "&7Visit (&r%d&7)", list.visit.size());
            for (BuildWorld bw: list.visit) {
                Msg.send(sender, "&a%s &8/%s", bw.getPath(), bw.getName());
            }
        }
        Msg.send(sender, "&7Total (&r%d&7)", list.count());
        return true;
    }

    boolean whoCommand(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        Msg.send(sender, "&eWorld Player List");
        for (World world: plugin.getServer().getWorlds()) {
            List<Player> players = world.getPlayers();
            if (players.isEmpty()) continue;
            String s = Msg.format("&7%s &8(&r%d&8)&r",
                                  world.getName(), players.size());
            StringBuilder sb = new StringBuilder(s);
            for (Player p: players) {
                sb.append(" ").append(p.getName());
            }
            sender.sendMessage(sb.toString());
        }
        return true;
    }

    boolean listLoadedCommand(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        int count = 0;
        for (World world: plugin.getServer().getWorlds()) {
            if (plugin.getBuildWorldByWorld(world) == null) {
                sender.sendMessage(ChatColor.RED + world.getName()
                                   + ChatColor.RESET + " (unregistered");
            } else {
                sender.sendMessage(ChatColor.GREEN + world.getName()
                                   + ChatColor.RESET + " (registered)");
            }
            count += 1;
        }
        sender.sendMessage("" + count + " worlds are currently loaded.");
        return true;
    }

    boolean tpCommand(CommandSender sender, String[] args) {
        Player player = sender instanceof Player
            ? (Player) sender
            : null;
        String worldName;
        Player target;
        if (args.length == 1) {
            if (player == null) return false;
            target = player;
            worldName = args[0];
        } else if (args.length == 2) {
            String targetName = args[0];
            target = plugin.getServer().getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage("Player not found: " + targetName);
                return true;
            }
            worldName = args[1];
        } else {
            return false;
        }
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldName);
        if (buildWorld == null) {
            sender.sendMessage("World not found: " + worldName);
            return true;
        }
        buildWorld.loadWorld();
        buildWorld.teleportToSpawn(target);
        Msg.send(sender, "&eTeleported %s to world %s.", target.getName(), buildWorld.getName());
        return true;
    }

    boolean removeCommand(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String worldKey = args[0];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldKey);
        if (buildWorld == null) {
            sender.sendMessage("World not found: " + worldKey);
            return true;
        }
        plugin.getBuildWorlds().remove(buildWorld);
        plugin.saveBuildWorlds();
        sender.sendMessage("World removed: " + buildWorld.getPath());
        return true;
    }

    boolean resetOwnerCommand(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String worldKey = args[0];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldKey);
        if (buildWorld == null) {
            sender.sendMessage("World not found: " + worldKey);
            return true;
        }
        buildWorld.setOwner(null);
        plugin.saveBuildWorlds();
        sender.sendMessage("Removed owner of world " + buildWorld.getPath());
        return true;
    }

    boolean setOwnerCommand(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        String worldKey = args[0];
        String ownerName = args[1];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldKey);
        if (buildWorld == null) {
            sender.sendMessage("World not found: " + worldKey);
            return true;
        }
        Builder owner = Builder.find(ownerName);
        if (owner == null) {
            sender.sendMessage("Builder not found: " + ownerName);
            return true;
        }
        buildWorld.setOwner(owner);
        plugin.saveBuildWorlds();
        sender.sendMessage("Made " + owner.getName()
                           + " the owner of world " + buildWorld.getPath());
        return true;
    }

    boolean createCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/ca create"
                               + " n:name"
                               + " p:path"
                               + " o:owner"
                               + " g:generator"
                               + " G:generatorSettings"
                               + " e:environment"
                               + " t:worldType"
                               + " s:seed"
                               + " S:generateStructures");
            return true;
        }
        Builder owner = null;
        String name = null;
        String generator = null;
        String generatorSettings = null;
        World.Environment environment = World.Environment.NORMAL;
        WorldType worldType = WorldType.NORMAL;
        boolean generateStructures = true;
        Long seed = null;
        String path = null;
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            String[] tok = arg.split(":", 2);
            if (tok.length != 2 || tok[0].length() != 1) {
                sender.sendMessage("Bad arg: '" + arg + "'");
                return true;
            }
            char param = tok[0].charAt(0);
            String value = tok[1];
            switch (param) {
            case 'o':
                owner = Builder.find(value);
                if (owner == null) {
                    sender.sendMessage("Builder not found: " + value);
                    return true;
                }
                break;
            case 'g':
                generator = value;
                break;
            case 'S':
                if (value.equals("true")) {
                    generateStructures = true;
                } else if (value.equals("false")) {
                    generateStructures = false;
                } else {
                    sender.sendMessage("Bad value for structures: " + value);
                    return true;
                }
                break;
            case 'G':
                generatorSettings = value;
                break;
            case 's':
                try {
                    seed = Long.parseLong(value);
                } catch (NumberFormatException nfe) {
                    seed = (long) value.hashCode();
                }
                break;
            case 'n':
                name = value;
                break;
            case 'p':
                path = value;
                break;
            case 't':
                try {
                    worldType = WorldType.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException iae) {
                    sender.sendMessage("Unknown world type: '" + value + "'");
                    return true;
                }
                break;
            case 'e':
                try {
                    environment = World.Environment.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException iae) {
                    sender.sendMessage("Unknown environment: '" + value + "'");
                    return true;
                }
            default: break;
            }
        }
        if (path == null) path = name;
        if (name == null) name = path;
        if (path == null) {
            sender.sendMessage("Path missing!");
            return true;
        }
        if (plugin.getBuildWorldByPath(path) != null) {
            sender.sendMessage("World already exists: '" + path + "'");
            return true;
        }
        BuildWorld buildWorld = new BuildWorld(name, path, owner);
        plugin.getBuildWorlds().add(buildWorld);
        plugin.saveBuildWorlds();
        // getWorldConfig() calls mkdirs()
        buildWorld.getWorldConfig().set("world.Generator", generator);
        buildWorld.getWorldConfig().set("world.GenerateStructures", generateStructures);
        buildWorld.getWorldConfig().set("world.GeneratorSettings", generatorSettings);
        buildWorld.getWorldConfig().set("world.Seed", seed);
        buildWorld.getWorldConfig().set("world.WorldType", worldType.name());
        buildWorld.getWorldConfig().set("world.Environment", environment.name());
        buildWorld.saveWorldConfig();
        sender.sendMessage("World '" + path + "' created.");
        return true;
    }

    boolean importCommand(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        String name = args[0];
        String generator = args[1];
        World world = plugin.getServer().getWorld(name);
        if (world == null) {
            sender.sendMessage("World not found: " + name);
            return true;
        }
        name = world.getName();
        if (plugin.getBuildWorldByPath(name) != null) {
            sender.sendMessage("Build world already exists: " + name);
            return true;
        }
        WorldCreator creator = WorldCreator.name(name);
        creator.copy(world);
        BuildWorld buildWorld = new BuildWorld(name, name, null);
        plugin.getBuildWorlds().add(buildWorld);
        plugin.saveBuildWorlds();
        buildWorld.getWorldConfig().set("world.Generator", generator);
        buildWorld.getWorldConfig().set("world.Seed", creator.seed());
        buildWorld.getWorldConfig().set("world.WorldType", creator.type().name());
        buildWorld.getWorldConfig().set("world.Environment", creator.environment().name());
        buildWorld.saveWorldConfig();
        sender.sendMessage("World '" + name + "' imported.");
        return true;
    }

    boolean loadCommand(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String name = args[0];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(name);
        if (buildWorld == null) {
            sender.sendMessage("World not found: " + name);
            return true;
        }
        buildWorld.reloadWorldConfig();
        World world = buildWorld.loadWorld();
        if (world == null) {
            sender.sendMessage("Could not load world: " + buildWorld.getPath());
        } else {
            sender.sendMessage("World loaded: " + world.getName());
        }
        return true;
    }

    boolean unloadCommand(CommandSender sender, String[] args) {
        if (args.length > 2) return false;
        String name = args[0];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(name);
        if (buildWorld == null) {
            sender.sendMessage("World not found: " + name);
            return true;
        }
        World world = buildWorld.getWorld();
        if (world == null) {
            sender.sendMessage("Could not unload world: " + buildWorld.getPath());
            return true;
        }
        boolean shouldSave = true;
        if (args.length >= 2) {
            String shouldSaveArg = args[1].toLowerCase();
            if (shouldSaveArg.equals("true")) {
                shouldSave = true;
            } else if (shouldSaveArg.equals("false")) {
                shouldSave = false;
            } else {
                return false;
            }
        }
        if (plugin.getServer().unloadWorld(world, shouldSave)) {
            sender.sendMessage("World unloaded: " + buildWorld.getPath());
        } else {
            sender.sendMessage("Could not unload world: " + buildWorld.getPath());
        }
        return true;
    }

    boolean ignoreCommand(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        Player player = sender instanceof Player
            ? (Player) sender
            : null;
        if (player == null) return false;
        if (plugin.toggleIgnore(player)) {
            Msg.info(player, "Ignoring world perms");
        } else {
            Msg.info(player, "No longer ignoring world perms");
        }
        plugin.getPermission().updatePermissions(player);
        return true;
    }

    boolean warpCommand(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        Player player = sender instanceof Player
            ? (Player) sender
            : null;
        if (player == null) return false;
        String name = Stream.of(args).collect(Collectors.joining(" "));
        Warp warp = plugin.getWarps().get(name);
        if (warp == null) {
            Msg.warn(player, "Warp not found: %s", name);
            return true;
        }
        Location loc = warp.getLocation();
        if (loc == null) {
            Msg.warn(player, "Warp not found: %s", warp.getName());
            return true;
        }
        player.teleport(loc);
        Msg.info(player, "Warped to %s", warp.getName());
        return true;
    }

    boolean setWarpCommand(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        Player player = sender instanceof Player
            ? (Player) sender
            : null;
        if (player == null) return false;
        String name = Stream.of(args).collect(Collectors.joining(" "));
        Location loc = player.getLocation();
        Warp warp = Warp.of(name, loc);
        plugin.getWarps().put(name, warp);
        plugin.saveWarps();
        Msg.info(player, "Created warp '%s'", name);
        return true;
    }
}
