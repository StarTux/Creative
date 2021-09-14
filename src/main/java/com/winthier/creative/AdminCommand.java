package com.winthier.creative;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
final class AdminCommand implements TabExecutor {
    final CreativePlugin plugin;
    private BukkitRunnable updateTask;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
        case "trust": return trustCommand(sender, argl);
        case "cleartrust": return clearTrustCommand(sender, argl);
        case "resetowner": return resetOwnerCommand(sender, argl);
        case "setowner": return setOwnerCommand(sender, argl);
        case "create": return createCommand(sender, argl);
        case "createvoid": return createVoidCommand(sender, argl);
        case "import": return importCommand(sender, argl);
        case "load": return loadCommand(sender, argl);
        case "unload": return unloadCommand(sender, argl);
        case "ignore": return ignoreCommand(sender, argl);
        case "warp": return warpCommand(sender, argl);
        case "setwarp": return setWarpCommand(sender, argl);
        case "deletewarp": return deleteWarpCommand(sender, argl);
        case "debugplot": return debugPlotCommand(sender, argl);
        default: return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return Collections.emptyList();
        String arg = args[args.length - 1];
        if (args.length == 1) {
            return Stream.of("info", "remove", "trust", "cleartrust",
                             "resetowner", "setowner", "import",
                             "load", "unload", "tp", "config", "set",
                             "debugplot", "deletewarp", "setwarp",
                             "warp", "ignore", "createvoid", "create",
                             "listloaded", "who", "list",
                             "listunregistered", "reload")
                .filter(s -> s.contains(arg))
                .collect(Collectors.toList());
        }
        switch (args[0]) {
        case "info":
        case "remove":
        case "trust":
        case "cleartrust":
        case "resetowner":
        case "setowner":
        case "import":
        case "load":
        case "unload":
        case "tp":
        case "config":
            if (args.length == 2) {
                return tabCompleteWorldPaths(arg);
            }
            return null;
        case "set":
            if (args.length == 2) {
                String argl = arg.toLowerCase();
                return Stream.of(BuildWorld.Flag.values())
                    .map(e -> e.key)
                    .filter(s -> s.toLowerCase().contains(argl))
                    .collect(Collectors.toList());
            } else if (args.length == 3) {
                return Stream.of("true", "false")
                    .filter(s -> s.contains(arg))
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        default: return null;
        }
    }

    List<String> tabCompleteWorldPaths(String arg) {
        String lower = arg.toLowerCase();
        return plugin.getBuildWorlds().stream()
            .map(BuildWorld::getPath)
            .filter(path -> path.toLowerCase().contains(lower))
            .collect(Collectors.toList());
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
        for (BuildWorld.Flag flag : BuildWorld.Flag.values()) {
            sender.sendMessage(flag.key + ": " + buildWorld.isSet(flag));
        }
        sender.sendMessage("Size: " + buildWorld.getSize());
        sender.sendMessage("Center: " + buildWorld.getCenterX() + "," + buildWorld.getCenterZ());
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
        BuildWorld.Flag flag = BuildWorld.Flag.of(key);
        if (flag == null) {
            player.sendMessage(ChatColor.RED + "Invalid flag: " + key);
            return true;
        }
        buildWorld.set(flag, newValue);
        sender.sendMessage(flag.key + " set to " + newValue);
        plugin.saveBuildWorlds();
        World bukkitWorld = buildWorld.getWorld();
        if (bukkitWorld != null) {
            plugin.getPermission().updatePermissions(bukkitWorld);
        }
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
            for (BuildWorld buildWorld : plugin.getBuildWorlds()) {
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
        final String delim = ChatColor.GRAY + " " + ChatColor.GREEN;
        if (!list.owner.isEmpty()) {
            Msg.send(sender, "&7Owner (&r%d&7)&a %s", list.owner.size(),
                     list.owner.stream()
                     .map(BuildWorld::getPath)
                     .collect(Collectors.joining(delim)));
        }
        if (!list.build.isEmpty()) {
            Msg.send(sender, "&7Build (&r%d&7)&a %s", list.build.size(),
                     list.build.stream()
                     .map(BuildWorld::getPath)
                     .collect(Collectors.joining(delim)));
        }
        if (!list.visit.isEmpty()) {
            Msg.send(sender, "&7Visit (&r%d&7)&a %s", list.visit.size(),
                     list.visit.stream()
                     .map(BuildWorld::getPath)
                     .collect(Collectors.joining(delim)));
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
        if (args.length > 1) return false;
        BuildWorld buildWorld;
        if (args.length >= 1) {
            String worldKey = args[0];
            buildWorld = plugin.getBuildWorldByPath(worldKey);
            if (buildWorld == null) {
                sender.sendMessage("World not found: " + worldKey);
                return true;
            }
        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
            if (buildWorld == null) {
                player.sendMessage(ChatColor.RED + "This is not a build world!");
                return true;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Player expected");
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
        if (path == null && name != null) path = name.toLowerCase();
        if (name == null) name = path;
        if (path == null) {
            sender.sendMessage("Path missing!");
            return true;
        }
        if (!path.matches("[a-z0-9_-]+")) {
            sender.sendMessage("Invalid path name (must be lowercase): " + path);
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

    boolean createVoidCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player expected");
            return true;
        }
        Player player = (Player) sender;
        if (args.length != 1 && args.length != 2) return false;
        String name = args[0];
        World.Environment environment = World.Environment.NORMAL;
        if (args.length >= 2) {
            String value = args[1];
            try {
                environment = World.Environment.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException iae) {
                sender.sendMessage("Unknown environment: '" + value + "'");
                return true;
            }
        }
        String path = name.toLowerCase();
        if (plugin.getBuildWorldByPath(path) != null) {
            sender.sendMessage("World already exists: '" + path + "'");
            return true;
        }
        BuildWorld buildWorld = new BuildWorld(name, path, Builder.of(player));
        plugin.getBuildWorlds().add(buildWorld);
        plugin.saveBuildWorlds();
        // getWorldConfig() calls mkdirs()
        buildWorld.getWorldConfig().set("world.Generator", "VoidGenerator");
        buildWorld.getWorldConfig().set("world.GenerateStructures", "false");
        buildWorld.getWorldConfig().set("world.GeneratorSettings", "");
        buildWorld.getWorldConfig().set("world.Seed", 0L);
        buildWorld.getWorldConfig().set("world.WorldType", WorldType.FLAT.name());
        buildWorld.getWorldConfig().set("world.Environment", environment.name());
        buildWorld.saveWorldConfig();
        buildWorld.loadWorld();
        buildWorld.teleportToSpawn(player);
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

    boolean deleteWarpCommand(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        String name = Stream.of(args).collect(Collectors.joining(" "));
        if (plugin.getWarps().remove(name) != null) {
            plugin.saveWarps();
            sender.sendMessage("Deleted warp: " + name);
        } else {
            sender.sendMessage("Warp not found: " + name);
        }
        return true;
    }

    boolean debugPlotCommand(CommandSender sender, String[] args) {
        Player player = sender instanceof Player
            ? (Player) sender
            : null;
        if (player == null) return false;
        Block block = player.getLocation().getBlock();
        PlotWorld plotWorld = plugin.getPlotWorld(block.getWorld());
        if (plotWorld == null) {
            player.sendMessage("No plot world!");
            return true;
        }
        player.sendMessage(Msg.toString(block) + ": " + plotWorld.debug(block));
        return true;
    }

    boolean trustCommand(CommandSender sender, String[] args) {
        if (args.length != 3) return false;
        String worldName = args[0];
        String playerName = args[1];
        String trustName = args[2];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldName);
        if (buildWorld == null) {
            sender.sendMessage(ChatColor.RED + "World not found: " + worldName);
            return true;
        }
        Builder builder = Builder.find(playerName);
        if (builder == null) {
            sender.sendMessage("Builder not found: " + playerName);
            return true;
        }
        Trust trust;
        if (trustName.equalsIgnoreCase("none")) {
            trust = Trust.NONE;
        } else {
            trust = Trust.of(trustName);
            if (trust == null || trust == Trust.NONE) {
                sender.sendMessage(ChatColor.RED + "Unknown trust type: " + trustName);
                return true;
            }
        }
        buildWorld.trustBuilder(builder, trust);
        plugin.saveBuildWorlds();
        Player target = builder.toPlayer();
        if (target != null) {
            plugin.getPermission().updatePermissions(target);
        }
        sender.sendMessage(ChatColor.GREEN + playerName + " now has " + trust.nice()
                           + " trust in world " + buildWorld.getName());
        return true;
    }

    boolean clearTrustCommand(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String worldName = args[0];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldName);
        if (buildWorld == null) {
            sender.sendMessage(ChatColor.RED + "World not found: " + worldName);
            return true;
        }
        int count = 0;
        for (Iterator<Map.Entry<UUID, Trusted>> iter = buildWorld.getTrusted().entrySet().iterator(); iter.hasNext();) {
            if (!iter.next().getValue().getTrust().isOwner()) {
                iter.remove();
                count += 1;
            }
        }
        if (count == 0) {
            sender.sendMessage(ChatColor.RED + "Nobody is trusted in " + buildWorld.getPath() + "!");
            return true;
        }
        plugin.saveBuildWorlds();
        sender.sendMessage(ChatColor.YELLOW + "Removed " + count + " players who were trusted in " + buildWorld.getPath());
        return true;
    }
}
