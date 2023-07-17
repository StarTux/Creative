package com.winthier.creative;

import com.cavetale.core.command.CommandWarn;
import com.winthier.creative.util.Files;
import com.winthier.playercache.PlayerCache;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
final class AdminCommand implements TabExecutor {
    final CreativePlugin plugin;
    protected AutoConverter autoConverter;

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
        case "ranktrust": return rankTrustCommand(sender, argl);
        case "resetowner": return resetOwnerCommand(sender, argl);
        case "setowner": return setOwnerCommand(sender, argl);
        case "create": return createCommand(sender, argl);
        case "createvoid": return createVoidCommand(sender, argl);
        case "import": return importCommand(sender, argl);
        case "load": return loadCommand(sender, argl);
        case "unload": return unloadCommand(sender, argl);
        case "ignore": return ignoreCommand(sender, argl);
        case "debugplot": return debugPlotCommand(sender, argl);
        case "buildgroups": return buildGroupsCommand(sender, argl);
        case "autoconvert": return autoConvertCommand(sender, argl);
        case "transferall": return transferAllCommand(sender, argl);
        default: return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return Collections.emptyList();
        String arg = args[args.length - 1];
        if (args.length == 1) {
            return Stream.of("info", "remove", "trust", "cleartrust",
                             "ranktrust", "resetowner", "setowner",
                             "import", "load", "unload", "tp",
                             "config", "set", "debugplot",
                             "ignore", "createvoid", "create",
                             "listloaded", "who", "list",
                             "listunregistered", "reload",
                             "buildgroups", "autoconvert",
                             "transferall")
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
        case "buildgroups":
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
        case "transferall":
            if (args.length == 1 || args.length == 2) {
                return List.of("TODO");
            } else {
                return List.of();
            }
        default: return null;
        }
    }

    private List<String> tabCompleteWorldPaths(String arg) {
        String lower = arg.toLowerCase();
        return plugin.getBuildWorlds().stream()
            .map(BuildWorld::getPath)
            .filter(path -> path.toLowerCase().contains(lower))
            .collect(Collectors.toList());
    }

    private boolean reloadCommand(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.reloadAllConfigs();
        sender.sendMessage(text("Configs reloaded", AQUA));
        return true;
    }

    private boolean infoCommand(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        BuildWorld buildWorld;
        if (args.length == 0) {
            Player player = sender instanceof Player
                ? (Player) sender
                : null;
            if (player == null) return false;
            buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
            if (buildWorld == null) {
                throw new CommandWarn("This is not a build world!");
            }
        } else {
            String name = args[0];
            buildWorld = plugin.getBuildWorldByPath(name);
            if (buildWorld == null) {
                throw new CommandWarn("World not found: " + name);
            }
        }
        sender.sendMessage(textOfChildren(text("Name ", GRAY), text(buildWorld.getName(), YELLOW)));
        sender.sendMessage(textOfChildren(text("Path ", GRAY), text(buildWorld.getPath(), YELLOW)));
        sender.sendMessage(textOfChildren(text("Owner ", GRAY), text(buildWorld.getOwnerName(), YELLOW)));
        String groups = "[" + String.join(" ", buildWorld.getBuildGroups()) + "]";
        sender.sendMessage(textOfChildren(text("BuildGroups ", GRAY), text(groups, YELLOW)));
        String names = buildWorld.getTrusted().values().stream()
            .map(trusted -> trusted.getBuilder().getName() + "=" + trusted.getTrust().nice())
            .collect(Collectors.joining(" "));
        sender.sendMessage(textOfChildren(text("Trusted ", GRAY), text(names, YELLOW)));
        sender.sendMessage(textOfChildren(text("Public Trust ", GRAY), text(buildWorld.getPublicTrust().nice(), YELLOW)));
        for (BuildWorld.Flag flag : BuildWorld.Flag.values()) {
            boolean value = buildWorld.isSet(flag);
            sender.sendMessage(textOfChildren(text(flag.key + " ", GRAY), text(value, value ? GREEN : RED)));
        }
        sender.sendMessage(textOfChildren(text("Size ", GRAY), text(buildWorld.getSize(), YELLOW)));
        sender.sendMessage(textOfChildren(text("Center ", GRAY), text(buildWorld.getCenterX() + "," + buildWorld.getCenterZ(), YELLOW)));
        return true;
    }

    private boolean setCommand(CommandSender sender, String[] args) {
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
                throw new CommandWarn("This is not a build world!");
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
            throw new CommandWarn("Invalid flag: " + key);
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

    private boolean configCommand(CommandSender sender, String[] args) {
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

    private boolean listUnregisteredCommand(CommandSender sender, String[] args) {
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

    private boolean listCommand(CommandSender sender, String[] args) {
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
        List<Component> lines = new ArrayList<>();
        lines.add(text(builder.getName() + " World List", YELLOW));
        if (!list.owner.isEmpty()) {
            String names = list.owner.stream()
                .map(BuildWorld::getPath)
                .collect(Collectors.joining(" "));
            lines.add(textOfChildren(text("Owner (" + list.owner.size() + ") "),
                                     text(names, GREEN)));
        }
        if (!list.build.isEmpty()) {
            String names = list.build.stream()
                .map(BuildWorld::getPath)
                .collect(Collectors.joining(" "));
            lines.add(textOfChildren(text("Build (" + list.build.size() + ") "),
                                     text(names, GREEN)));
        }
        if (!list.visit.isEmpty()) {
            String names = list.visit.stream()
                .map(BuildWorld::getPath)
                .collect(Collectors.joining(" "));
            lines.add(textOfChildren(text("Visit (" + list.visit.size() + ") "),
                                     text(names, GREEN)));
        }
        lines.add(text("Total (" + list.count() + ")", GRAY));
        sender.sendMessage(join(separator(newline()), lines));
        return true;
    }

    private boolean whoCommand(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        List<Component> lines = new ArrayList<>();
        lines.add(text("World Player List", YELLOW));
        for (World world: plugin.getServer().getWorlds()) {
            List<Player> players = world.getPlayers();
            if (players.isEmpty()) continue;
            StringBuilder sb = new StringBuilder();
            for (Player p: players) {
                sb.append(" ").append(p.getName());
            }
            lines.add(text(world.getName() + "(" + players.size() + ")", GRAY)
                      .append(text(sb.toString(), GREEN)));
        }
        sender.sendMessage(join(separator(newline()), lines));
        return true;
    }

    private boolean listLoadedCommand(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        int count = 0;
        for (World world: plugin.getServer().getWorlds()) {
            if (plugin.getBuildWorldByWorld(world) == null) {
                sender.sendMessage(textOfChildren(text(world.getName(), RED),
                                                  text(" (unregistered)", GRAY, ITALIC)));
            } else {
                sender.sendMessage(textOfChildren(text(world.getName(), GREEN),
                                                  text(" (registered)", GRAY, ITALIC)));
            }
            count += 1;
        }
        sender.sendMessage("" + count + " worlds are currently loaded.");
        return true;
    }

    private boolean tpCommand(CommandSender sender, String[] args) {
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
        sender.sendMessage(text("Teleported " + target.getName()
                                + " to world " + buildWorld.getName(),
                                YELLOW));
        return true;
    }

    private boolean removeCommand(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String worldKey = args[0];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldKey);
        if (buildWorld == null) {
            throw new CommandWarn("World not found: " + worldKey);
        }
        if (buildWorld.getWorld() != null) {
            throw new CommandWarn("World still loaded: " + buildWorld.getName());
        }
        plugin.getBuildWorlds().remove(buildWorld);
        plugin.saveBuildWorlds();
        Files.deleteRecursively(buildWorld.getWorldFolder());
        sender.sendMessage(text("World removed: " + buildWorld.getPath(),
                                YELLOW));
        return true;
    }

    private boolean resetOwnerCommand(CommandSender sender, String[] args) {
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
                throw new CommandWarn("This is not a build world!");
            }
        } else {
            throw new CommandWarn("Player expected");
        }
        buildWorld.setOwner(null);
        plugin.saveBuildWorlds();
        sender.sendMessage("Removed owner of world " + buildWorld.getPath());
        return true;
    }

    private boolean setOwnerCommand(CommandSender sender, String[] args) {
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

    private boolean createCommand(CommandSender sender, String[] args) {
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

    private boolean createVoidCommand(CommandSender sender, String[] args) {
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

    private boolean importCommand(CommandSender sender, String[] args) {
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

    private boolean loadCommand(CommandSender sender, String[] args) {
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

    private boolean unloadCommand(CommandSender sender, String[] args) {
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

    private boolean ignoreCommand(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        Player player = sender instanceof Player
            ? (Player) sender
            : null;
        if (player == null) return false;
        if (plugin.toggleIgnore(player)) {
            player.sendMessage(text("Ignoring world perms", YELLOW));
        } else {
            player.sendMessage(text("No longer ignoring world perms", YELLOW));
        }
        plugin.getPermission().updatePermissions(player);
        return true;
    }

    private boolean debugPlotCommand(CommandSender sender, String[] args) {
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
        player.sendMessage(text(block.getX() + "," + block.getY() + "," + block.getZ()
                                + ": " + plotWorld.debug(block), YELLOW));
        return true;
    }

    private boolean trustCommand(CommandSender sender, String[] args) {
        if (args.length != 3) return false;
        String worldName = args[0];
        String playerName = args[1];
        String trustName = args[2];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldName);
        if (buildWorld == null) {
            throw new CommandWarn("World not found: " + worldName);
        }
        Builder builder = Builder.find(playerName);
        if (builder == null) {
            throw new CommandWarn("Builder not found: " + playerName);
        }
        Trust trust;
        if (trustName.equalsIgnoreCase("none")) {
            trust = Trust.NONE;
        } else {
            trust = Trust.of(trustName);
            if (trust == null || trust == Trust.NONE) {
                throw new CommandWarn("Unknown trust type: " + trustName);
            }
        }
        buildWorld.trustBuilder(builder, trust);
        plugin.saveBuildWorlds();
        Player target = builder.toPlayer();
        if (target != null) {
            plugin.getPermission().updatePermissions(target);
        }
        sender.sendMessage(text(playerName + " now has " + trust.nice() + " trust in world " + buildWorld.getName(), AQUA));
        return true;
    }

    private boolean clearTrustCommand(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String worldName = args[0];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldName);
        if (buildWorld == null) {
            throw new CommandWarn("World not found: " + worldName);
        }
        boolean publicTrust = false;
        if (buildWorld.getPublicTrust().canBuild()) {
            buildWorld.setPublicTrust(Trust.VISIT);
            publicTrust = true;
        }
        int count = 0;
        for (Iterator<Map.Entry<UUID, Trusted>> iter = buildWorld.getTrusted().entrySet().iterator(); iter.hasNext();) {
            if (!iter.next().getValue().getTrust().isOwner()) {
                iter.remove();
                count += 1;
            }
        }
        if (!publicTrust && count == 0) {
            throw new CommandWarn("Nobody is trusted in " + buildWorld.getPath() + "!");
        }
        plugin.saveBuildWorlds();
        if (publicTrust) {
            sender.sendMessage(text("Reset public trust to visit in " + buildWorld.getPath(), YELLOW));
        }
        sender.sendMessage(text("Removed " + count + " players who were trusted in " + buildWorld.getPath(), YELLOW));
        return true;
    }

    private boolean rankTrustCommand(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        int page = 0;
        if (args.length >= 1) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                throw new CommandWarn("Invalid page: " + args[0]);
            }
        }
        List<BuildWorld> worlds = new ArrayList<>(plugin.getBuildWorlds());
        final int pageSize = 20;
        final int maxPageIndex = worlds.size() / pageSize;
        if (page < 0 || page > maxPageIndex) {
            throw new CommandWarn("Page index out of bounds: " + page);
        }
        Collections.sort(worlds, (b, a) -> (Integer.compare(a.trustedScore(), b.trustedScore())));
        List<Component> lines = new ArrayList<>();
        lines.add(text("World trust list (page " + page + ")", YELLOW));
        final int startIndex = page * pageSize;
        final int endIndex = startIndex + pageSize;
        for (int i = startIndex; i < endIndex; i += 1) {
            if (i >= worlds.size()) break;
            BuildWorld world = worlds.get(i);
            lines.add(text("#" + i, GRAY)
                      .append(text(" " + Math.min(999, world.trustedScore()), YELLOW))
                      .append(text(" " + world.getPath(), WHITE)));
        }
        sender.sendMessage(join(separator(newline()), lines));
        return true;
    }

    private boolean buildGroupsCommand(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        BuildWorld buildWorld = plugin.getBuildWorldByPath(args[0]);
        if (buildWorld == null) {
            sender.sendMessage("World not found: " + args[0]);
            return true;
        }
        List<String> buildGroups = List.of(Arrays.copyOfRange(args, 1, args.length));
        buildWorld.setBuildGroups(buildGroups);
        plugin.saveBuildWorlds();
        if (!buildGroups.isEmpty()) {
            sender.sendMessage(text("Build groups of " + buildWorld.getName() + " set to " + buildGroups,
                                    YELLOW));
        } else {
            sender.sendMessage(text("Build groups of " + buildWorld.getName() + " reset",
                                    YELLOW));
        }
        return true;
    }

    private boolean autoConvertCommand(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        if (autoConverter != null) {
            sender.sendMessage(text("An auto convesion task is already running", RED));
            return true;
        }
        sender.sendMessage("Starging auto conversion. See console...");
        autoConverter = new AutoConverter(plugin);
        autoConverter.start();
        return true;
    }

    private boolean transferAllCommand(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        PlayerCache from = PlayerCache.forArg(args[0]);
        if (from == null) {
            sender.sendMessage(text("Player not found: " + args[0], RED));
            return true;
        }
        PlayerCache to = PlayerCache.forArg(args[1]);
        if (to == null) {
            sender.sendMessage(text("Player not found: " + args[1], RED));
            return true;
        }
        if (from.equals(to)) {
            sender.sendMessage(text("Players are identical: " + from.getName(), RED));
            return true;
        }
        int total = 0;
        int worldCount = 0;
        int trustCount = 0;
        Builder toBuilder = Builder.of(to.uuid);
        for (BuildWorld buildWorld : plugin.getBuildWorlds()) {
            if (buildWorld.getOwner() != null && from.uuid.equals(buildWorld.getOwner().getUuid())) {
                buildWorld.setOwner(toBuilder);
                total += 1;
                worldCount += 1;
            }
            Trusted trusted = buildWorld.getTrusted().remove(from.uuid);
            if (trusted != null) {
                buildWorld.getTrusted().put(to.uuid, new Trusted(toBuilder, trusted.getTrust()));
                total += 1;
                trustCount += 1;
            }
        }
        if (total == 0) {
            sender.sendMessage(text(from.name + " does not have any creative worlds", RED));
            return true;
        }
        plugin.saveBuildWorlds();
        sender.sendMessage(text("Transferred worlds from " + from.name + " to " + to.name + ":"
                                + " worlds=" + worldCount
                                + " trust=" + trustCount,
                                YELLOW));
        return true;
    }
}
