package com.winthier.creative;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandContext;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.core.perm.Perm;
import com.cavetale.core.playercache.PlayerCache;
import com.winthier.creative.file.Files;
import com.winthier.creative.sql.SQLWorld;
import com.winthier.creative.sql.SQLWorldTrust;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import static com.cavetale.core.command.CommandArgCompleter.enumLowerList;
import static com.cavetale.core.command.CommandArgCompleter.list;
import static com.cavetale.core.command.CommandArgCompleter.requireEnum;
import static com.cavetale.core.command.CommandArgCompleter.supplyList;
import static com.winthier.creative.CreativePlugin.plugin;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.suggestCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class AdminCommand extends AbstractCommand<CreativePlugin> {
    protected AutoConverter autoConverter;

    AdminCommand(final CreativePlugin plugin) {
        super(plugin, "creativeadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload configs")
            .senderCaller(this::reloadCommand);
        rootNode.addChild("info").arguments("[path]")
            .description("Get world info")
            .completers(supplyList(AdminCommand::supplyWorldPaths))
            .senderCaller(this::info);
        rootNode.addChild("set").arguments("[world] <flag> <value>")
            .description("Change world settings")
            .completers(supplyList(AdminCommand::supplyWorldPaths),
                        enumLowerList(BuildWorld.Flag.class),
                        CommandArgCompleter.BOOLEAN)
            .senderCaller(this::setCommand);
        rootNode.addChild("config").arguments("<world>")
            .description("Print world config")
            .completers(supplyList(AdminCommand::supplyWorldPaths))
            .senderCaller(this::configCommand);
        rootNode.addChild("listunregistered").denyTabCompletion()
            .remoteServer(NetworkServer.CREATIVE)
            .description("List unregistered worlds")
            .senderCaller(this::listUnregisteredCommand);
        rootNode.addChild("list").arguments("[player]")
            .description("List (player) worlds")
            .completers(CommandArgCompleter.PLAYER_CACHE)
            .senderCaller(this::listCommand);
        rootNode.addChild("who").denyTabCompletion()
            .remoteServer(NetworkServer.CREATIVE)
            .description("List players in worlds")
            .senderCaller(this::whoCommand);
        rootNode.addChild("listloaded").denyTabCompletion()
            .remoteServer(NetworkServer.CREATIVE)
            .description("List loaded worlds")
            .senderCaller(this::listLoadedCommand);
        rootNode.addChild("tp").arguments("[player] <world>")
            .remoteServer(NetworkServer.CREATIVE)
            .description("Teleport to world")
            .completers(supplyList(AdminCommand::supplyWorldPaths))
            .senderCaller(this::tpCommand);
        rootNode.addChild("remove").arguments("<world>")
            .description("Remove a world")
            .completers(supplyList(AdminCommand::supplyWorldPaths))
            .senderCaller(this::removeCommand);
        rootNode.addChild("trust").arguments("<world> <player> <trust>")
            .description("Edit player world trust")
            .completers(supplyList(AdminCommand::supplyWorldPaths),
                        CommandArgCompleter.PLAYER_CACHE,
                        enumLowerList(Trust.class))
            .senderCaller(this::trustCommand);
        rootNode.addChild("publictrust").arguments("<world> <trust>")
            .description("Set public trust")
            .completers(supplyList(AdminCommand::supplyWorldPaths),
                        enumLowerList(Trust.class))
            .senderCaller(this::publicTrustCommand);
        rootNode.addChild("cleartrust").arguments("<world>")
            .description("Clear trusted players")
            .completers(supplyList(AdminCommand::supplyWorldPaths))
            .senderCaller(this::clearTrustCommand);
        rootNode.addChild("ranktrust").arguments("<world>")
            .description("Rank worlds by trusted players")
            .completers(supplyList(AdminCommand::supplyWorldPaths))
            .senderCaller(this::rankTrustCommand);
        rootNode.addChild("resetowner").arguments("<world>")
            .description("Reset world ownership")
            .completers(supplyList(AdminCommand::supplyWorldPaths))
            .senderCaller(this::resetOwnerCommand);
        rootNode.addChild("setowner").arguments("<world> <player>")
            .description("Set world ownership")
            .completers(supplyList(AdminCommand::supplyWorldPaths),
                        CommandArgCompleter.PLAYER_CACHE)
            .senderCaller(this::setOwnerCommand);
        rootNode.addChild("create").arguments("n:name p:path o:owner g:generator G:generatorSettings e:environment t:worldType s:seed S:generateStructures")
            .remoteServer(NetworkServer.CREATIVE)
            .completers(AdminCommand::completeCreateArgs, CommandArgCompleter.REPEAT)
            .description("Create a world")
            .senderCaller(this::createCommand);
        rootNode.addChild("createvoid").arguments("<name> [environment]")
            .remoteServer(NetworkServer.CREATIVE)
            .description("Create empty world")
            .completers(supplyList(AdminCommand::supplyWorldPaths),
                        enumLowerList(World.Environment.class))
            .senderCaller(this::createVoidCommand);
        rootNode.addChild("import").arguments("<world> [generator]")
            .remoteServer(NetworkServer.CREATIVE)
            .description("Import loaded world")
            .completers(supplyList(AdminCommand::loadedWorldNames),
                        list(List.of("VoidGenerator")))
            .senderCaller(this::importCommand);
        rootNode.addChild("load").arguments("<world>")
            .remoteServer(NetworkServer.CREATIVE)
            .description("Load build world")
            .completers(supplyList(AdminCommand::supplyWorldPaths))
            .senderCaller(this::loadCommand);
        rootNode.addChild("unload").arguments("<world>")
            .remoteServer(NetworkServer.CREATIVE)
            .description("Unload build world")
            .completers(supplyList(AdminCommand::supplyWorldPaths))
            .senderCaller(this::unloadCommand);
        rootNode.addChild("ignore").denyTabCompletion()
            .description("Ignore build restrictions")
            .playerCaller(this::ignoreCommand);
        rootNode.addChild("debugplot").denyTabCompletion()
            .description("Plot world debug")
            .playerCaller(this::debugPlotCommand);
        rootNode.addChild("buildgroups").arguments("<world> [group...]")
            .description("Set build groups")
            .completers(supplyList(() -> Perm.get().getGroupNames()))
            .senderCaller(this::buildGroupsCommand);
        rootNode.addChild("copy").arguments("<from> <to>")
            .description("Create a world copy")
            .completers(supplyList(AdminCommand::supplyWorldPaths),
                        supplyList(AdminCommand::supplyWorldPaths))
            .senderCaller(this::copy);
        rootNode.addChild("autoconvert").denyTabCompletion()
            .description("Load and save all worlds")
            .senderCaller(this::autoConvertCommand);
        rootNode.addChild("transferall").arguments("<from> <to>")
            .description("Account transfer")
            .completers(CommandArgCompleter.PLAYER_CACHE,
                        CommandArgCompleter.PLAYER_CACHE)
            .senderCaller(this::transferAllCommand);
        // rootNode.addChild("legacyworldconvert").denyTabCompletion()
        //     .description("Transfer legacy worlds")
        //     .senderCaller(this::legacyWorldConvert);
        // Minigame
        CommandNode minigameNode = rootNode.addChild("minigame")
            .description("Minigame subcommands");
        minigameNode.addChild("list").arguments("<minigame>")
            .description("List all games for minigame")
            .completers(enumLowerList(MinigameMatchType.class))
            .senderCaller(this::minigameList);
        minigameNode.addChild("reset").arguments("<world>")
            .description("Reset minigame purpose")
            .completers(supplyList(AdminCommand::supplyWorldPaths))
            .senderCaller(this::minigameReset);
        minigameNode.addChild("set").arguments("<world> <minigame>")
            .description("Set minigame purpose")
            .completers(supplyList(AdminCommand::supplyWorldPaths),
                        enumLowerList(MinigameMatchType.class))
            .senderCaller(this::minigameSet);
        minigameNode.addChild("confirm").arguments("<world> true|false")
            .description("Confirm minigame validity")
            .completers(supplyList(AdminCommand::supplyWorldPaths),
                        CommandArgCompleter.BOOLEAN)
            .senderCaller(this::minigameConfirm);
        // Purpose
        CommandNode purposeNode = rootNode.addChild("purpose")
            .description("Purpose subcommands");
        purposeNode.addChild("list").arguments("<type>")
            .description("List worlds with purpose")
            .completers(enumLowerList(BuildWorldPurpose.class))
            .senderCaller(this::purposeList);
        purposeNode.addChild("reset").arguments("<world>")
            .description("Reset world purpose")
            .completers(supplyList(AdminCommand::supplyWorldPaths))
            .senderCaller(this::purposeReset);
        purposeNode.addChild("set").arguments("<world> <purpose>")
            .description("Set world purpose")
            .completers(supplyList(AdminCommand::supplyWorldPaths),
                        enumLowerList(BuildWorldPurpose.class))
            .senderCaller(this::purposeSet);
        purposeNode.addChild("index").arguments("<world> <index>")
            .description("Set purpose index")
            .completers(supplyList(AdminCommand::supplyWorldPaths),
                        CommandArgCompleter.integer(i -> true))
            .senderCaller(this::purposeIndex);
        purposeNode.addChild("score").arguments("<world> <score>")
            .description("Set purpose score")
            .completers(supplyList(AdminCommand::supplyWorldPaths),
                        CommandArgCompleter.integer(i -> true))
            .senderCaller(this::purposeScore);
        purposeNode.addChild("import").denyTabCompletion()
            .description("Import purposes from file")
            .senderCaller(this::purposeImport);
    }

    private static List<String> supplyWorldPaths() {
        List<String> result = new ArrayList<>();
        for (BuildWorld buildWorld : plugin().getBuildWorlds()) {
            result.add(buildWorld.getPath());
        }
        return result;
    }

    private static List<String> loadedWorldNames() {
        List<String> result = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            result.add(world.getName());
        }
        return result;
    }

    private static List<String> completeCreateArgs(CommandContext context, CommandNode node, String arg) {
        List<String> result = new ArrayList<>();
        if (arg.length() < 2) {
            for (String c : List.of("n", "p", "o", "g", "G", "e", "t", "s", "S")) {
                String prefix = c + ":";
                if (prefix.startsWith(arg)) {
                    result.add(prefix);
                }
            }
        } else {
            String prefix = arg.substring(0, 2);
            String param = arg.substring(2);
            String lower = param.toLowerCase();
            if (!prefix.endsWith(":")) return result;
            switch (prefix.charAt(0)) {
            case 'n': // name
                for (BuildWorld buildWorld : plugin().getBuildWorlds()) {
                    String name = buildWorld.getName().replace(" ", "");
                    if (name.toLowerCase().contains(lower)) {
                        result.add(prefix + name);
                    }
                }
                break;
            case 'p': // path
                for (String path : supplyWorldPaths()) {
                    if (path.toLowerCase().contains(lower)) {
                        result.add(prefix + path);
                    }
                }
                break;
            case 'o': // owner
                for (String name : PlayerCache.completeNames(param)) {
                    result.add(prefix + name);
                }
                break;
            case 'g': // generator
                for (String gen : List.of("VoidGenerator")) {
                    if (gen.toLowerCase().contains(lower)) {
                        result.add(prefix + gen);
                    }
                }
                break;
            case 'G': // generator settings
                break;
            case 'e':
                for (World.Environment env : World.Environment.values()) {
                    if (env.name().toLowerCase().contains(lower)) {
                        result.add(prefix + env.name().toLowerCase());
                    }
                }
                break;
            case 't': // world type
                for (WorldType type : WorldType.values()) {
                    if (type.name().toLowerCase().contains(lower)) {
                        result.add(prefix + type.name().toLowerCase());
                    }
                }
                break;
            case 's': // seed
                break;
            case 'S':
                if ("false".contains(lower)) {
                    result.add(prefix + "false");
                }
                break;
            default: return result;
            }
        }
        return result;
    }

    private BuildWorld requireBuildWorld(String arg) {
        final BuildWorld buildWorld = plugin.getBuildWorldByPath(arg);
        if (buildWorld == null) throw new CommandWarn("World path not found: " + arg);
        return buildWorld;
    }

    private void requireCreativeServer() {
        if (!plugin.isCreativeServer()) {
            throw new CommandWarn("Must be on creative");
        }
    }

    private void reloadCommand(CommandSender sender) {
        requireCreativeServer();
        plugin.reloadAllConfigs();
        sender.sendMessage(text("Configs reloaded", AQUA));
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        final BuildWorld buildWorld;
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
        final SQLWorld row = buildWorld.getRow();
        sender.sendMessage(textOfChildren(text("Id ", GRAY), text(row.getId(), YELLOW)).insertion("" + row.getId()));
        sender.sendMessage(textOfChildren(text("Path ", GRAY), text(buildWorld.getPath(), YELLOW)).insertion(buildWorld.getPath()));
        sender.sendMessage(textOfChildren(text("Name ", GRAY), text(buildWorld.getName(), YELLOW)));
        final String desc = row.getDescription() != null ? row.getDescription() : "";
        sender.sendMessage(textOfChildren(text("Description ", GRAY), text(desc, YELLOW)));
        sender.sendMessage(textOfChildren(text("Owner ", GRAY), text(buildWorld.getOwnerName(), YELLOW)));
        if (!row.isSpawnSet()) {
            sender.sendMessage(textOfChildren(text("Spawn ", GRAY), text("N/A", DARK_GRAY, ITALIC)));
        } else {
            sender.sendMessage(textOfChildren(text("Spawn ", GRAY),
                                              text((int) Math.round(row.getSpawnX()), YELLOW),
                                              text(",", GRAY),
                                              text((int) Math.round(row.getSpawnY()), YELLOW),
                                              text(",", GRAY),
                                              text((int) Math.round(row.getSpawnZ()), YELLOW)));
        }
        String groups = "[" + String.join(" ", buildWorld.getBuildGroups()) + "]";
        sender.sendMessage(textOfChildren(text("BuildGroups ", GRAY), text(groups, YELLOW)));
        List<Component> trusted = new ArrayList<>();
        for (SQLWorldTrust trustRow : buildWorld.getTrusted().values()) {
            final String name = PlayerCache.nameForUuid(trustRow.getPlayer());
            trusted.add(textOfChildren(text(name, YELLOW),
                                       text(":", GRAY),
                                       text(trustRow.getTrustValue().nice(), YELLOW))
                        .insertion("/cra trust " + buildWorld.getPath() + " " + name + " " + trustRow.getTrustValue().name().toLowerCase()));
        }
        sender.sendMessage(textOfChildren(text("Trusted ", GRAY), join(separator(space()), trusted)));
        sender.sendMessage(textOfChildren(text("Public Trust ", GRAY), text(buildWorld.getPublicTrust().nice(), YELLOW)));
        List<Component> flags = new ArrayList<>();
        for (BuildWorld.Flag flag : BuildWorld.Flag.values()) {
            final boolean value = buildWorld.isSet(flag);
            flags.add(textOfChildren(text(flag.key + ":", GRAY), text(value, value ? GREEN : RED))
                      .insertion("/cra set " + buildWorld.getPath() + " " + flag.key + " " + value));
        }
        sender.sendMessage(textOfChildren(text("Flags ", GRAY), join(separator(space()), flags)));
        sender.sendMessage(textOfChildren(text("Border ", GRAY),
                                          text("center:", GRAY),
                                          text(row.getBorderCenterX() + ","
                                               + row.getBorderCenterZ(), YELLOW),
                                          text(" size:", GRAY),
                                          text(row.getBorderSize(), YELLOW)));
        sender.sendMessage(textOfChildren(text("Generator ", GRAY), text("" + row.getGenerator(), YELLOW))
                           .insertion("" + row.getGenerator()));
        sender.sendMessage(textOfChildren(text("Seed ", GRAY), text(row.getSeed(), YELLOW))
                           .insertion("" + row.getSeed()));
        sender.sendMessage(textOfChildren(text("Environment ", GRAY), text("" + row.getEnvironment(), YELLOW)));
        sender.sendMessage(textOfChildren(text("WorldType ", GRAY), text("" + row.getWorldType(), YELLOW)));
        sender.sendMessage(textOfChildren(text("GeneratorSettings ", GRAY),
                                          text(row.isGenerateStructures(), YELLOW),
                                          text(", ", GRAY),
                                          text("" + row.getGeneratorSettings(), YELLOW)));
        final BuildWorldPurpose purpose = row.parsePurpose();
        if (purpose != null) {
            sender.sendMessage(textOfChildren(text("Purpose ", GRAY),
                                              text(purpose.displayName, YELLOW),
                                              space(),
                                              (row.isPurposeConfirmed() ? text("Confirmed", GREEN) : text("Unconfirmed", RED))));
        }
        final MinigameMatchType minigame = row.parseMinigame();
        if (minigame != null) {
            sender.sendMessage(textOfChildren(text("Minigame ", GRAY),
                                              text(minigame.displayName, YELLOW)));
        }
        sender.sendMessage(textOfChildren(text("Vote score ", GRAY), text(buildWorld.getRow().getVoteScore(), YELLOW)));
        return true;
    }

    private boolean setCommand(CommandSender sender, String[] args) {
        requireCreativeServer();
        Player player = sender instanceof Player
            ? (Player) sender
            : null;
        BuildWorld buildWorld;
        String key;
        String value;
        if (args.length == 3) {
            buildWorld = plugin.getBuildWorldByPath(args[0]);
            if (buildWorld == null) {
                throw new CommandWarn("World not found: " + args[0]);
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
            throw new CommandWarn("Unknown value: " + value);
        }
        BuildWorld.Flag flag = BuildWorld.Flag.of(key);
        if (flag == null) {
            throw new CommandWarn("Invalid flag: " + key);
        }
        buildWorld.set(flag, newValue);
        buildWorld.saveAsync("tag", () -> sender.sendMessage(text(flag.key + " set to " + newValue, YELLOW)));
        World bukkitWorld = buildWorld.getWorld();
        if (bukkitWorld != null) {
            plugin.getPermission().updatePermissions(bukkitWorld);
        }
        return true;
    }

    private boolean configCommand(CommandSender sender, String[] args) {
        requireCreativeServer();
        if (args.length != 1) return false;
        String name = args[0];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(name);
        if (buildWorld == null) {
            throw new CommandWarn("World not found: " + name);
        }
        for (String key : buildWorld.getWorldConfig().getKeys(true)) {
            Object o = buildWorld.getWorldConfig().get(key);
            if (o instanceof ConfigurationSection) continue;
            sender.sendMessage(textOfChildren(text(key + "='", GRAY),
                                              text(o.toString(), YELLOW),
                                              text("'", GRAY)));
        }
        return true;
    }

    private void listUnregisteredCommand(CommandSender sender) {
        requireCreativeServer();
        sender.sendMessage(text("Unregistered worlds:", AQUA));
        int count = 0;
        for (String dir: plugin.getServer().getWorldContainer().list()) {
            if (plugin.getBuildWorldByPath(dir) == null) {
                sender.sendMessage(text(" " + dir, YELLOW));
                count += 1;
            }
        }
        sender.sendMessage(text("" + count + " worlds listed", AQUA));
    }

    private boolean listCommand(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (args.length == 0) {
            int count = 0;
            for (BuildWorld buildWorld : plugin.getBuildWorlds()) {
                sender.sendMessage(text(buildWorld.getName() + " /"
                                        + buildWorld.getPath() + " " + buildWorld.getOwnerName(), AQUA));
                count += 1;
            }
            sender.sendMessage(text("" + count + " build worlds listed", AQUA));
            return true;
        }
        final String name = args[0];
        final PlayerCache builder = PlayerCache.require(name);
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

    private void whoCommand(CommandSender sender) {
        requireCreativeServer();
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
                      .append(text(sb.toString(), GREEN))
                      .clickEvent(suggestCommand("/cra tp " + world.getName()))
                      .hoverEvent(showText(text("/cra tp " + world.getName(), YELLOW)))
                      .insertion(world.getName()));
        }
        sender.sendMessage(join(separator(newline()), lines));
    }

    private void listLoadedCommand(CommandSender sender) {
        requireCreativeServer();
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
        sender.sendMessage(text("" + count + " worlds are currently loaded.", AQUA));
    }

    private boolean tpCommand(CommandSender sender, String[] args) {
        requireCreativeServer();
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
                throw new CommandWarn("Player not found: " + targetName);
            }
            worldName = args[1];
        } else {
            return false;
        }
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldName);
        if (buildWorld == null) {
            throw new CommandWarn("World not found: " + worldName);
        }
        buildWorld.loadWorld();
        buildWorld.teleportToSpawn(target);
        sender.sendMessage(text("Teleported " + target.getName()
                                + " to world " + buildWorld.getName(),
                                YELLOW));
        return true;
    }

    private boolean removeCommand(CommandSender sender, String[] args) {
        requireCreativeServer();
        if (args.length != 1) return false;
        String worldKey = args[0];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldKey);
        if (buildWorld == null) {
            throw new CommandWarn("World not found: " + worldKey);
        }
        if (buildWorld.getWorld() != null) {
            throw new CommandWarn("World still loaded: " + buildWorld.getName());
        }
        buildWorld.deleteAsync(() -> {
                plugin.getBuildWorlds().remove(buildWorld);
                Files.deleteFileStructure(buildWorld.getWorldFolder());
                sender.sendMessage(text("World removed: " + buildWorld.getPath(),
                                        YELLOW));
            });
        return true;
    }

    private boolean resetOwnerCommand(CommandSender sender, String[] args) {
        requireCreativeServer();
        if (args.length > 1) return false;
        BuildWorld buildWorld;
        if (args.length >= 1) {
            String worldKey = args[0];
            buildWorld = plugin.getBuildWorldByPath(worldKey);
            if (buildWorld == null) {
                throw new CommandWarn("World not found: " + worldKey);
            }
        } else if (sender instanceof Player) {
            requireCreativeServer();
            Player player = (Player) sender;
            buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
            if (buildWorld == null) {
                throw new CommandWarn("This is not a build world!");
            }
        } else {
            throw new CommandWarn("Player expected");
        }
        buildWorld.getRow().setOwner(null);
        buildWorld.saveAsync("owner", () -> {
                sender.sendMessage(text("Removed owner of world " + buildWorld.getPath(), YELLOW));
            });
        return true;
    }

    private boolean setOwnerCommand(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        String worldKey = args[0];
        String ownerName = args[1];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldKey);
        if (buildWorld == null) {
            throw new CommandWarn("World not found: " + worldKey);
        }
        PlayerCache owner = PlayerCache.require(ownerName);
        buildWorld.getRow().setOwner(owner.getUuid());
        buildWorld.saveAsync("owner", () -> {
                sender.sendMessage(text("Made " + owner.getName() + " the owner of world " + buildWorld.getPath(), YELLOW));
            });
        return true;
    }

    private boolean createCommand(CommandSender sender, String[] args) {
        requireCreativeServer();
        if (args.length == 0) return false;
        PlayerCache owner = null;
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
                throw new CommandWarn("Bad arg: '" + arg + "'");
            }
            char param = tok[0].charAt(0);
            String value = tok[1];
            switch (param) {
            case 'o':
                owner = PlayerCache.require(value);
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
                    throw new CommandWarn("Bad value for structures: " + value);
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
                    throw new CommandWarn("Unknown world type: '" + value + "'");
                }
                break;
            case 'e':
                try {
                    environment = World.Environment.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException iae) {
                    throw new CommandWarn("Unknown environment: '" + value + "'");
                }
            default: break;
            }
        }
        if (path == null && name != null) path = name.toLowerCase();
        if (name == null) name = path;
        if (path == null) {
            throw new CommandWarn("Path missing!");
        }
        if (!path.matches("[a-z0-9_-]+")) {
            throw new CommandWarn("Invalid path name (must be lowercase): " + path);
        }
        if (plugin.getBuildWorldByPathIgnoreCase(path) != null) {
            throw new CommandWarn("World already exists: '" + path + "'");
        }
        final BuildWorld buildWorld = new BuildWorld(name, path, (owner != null ? owner.uuid : null));
        buildWorld.getRow().setGenerator(generator);
        buildWorld.getRow().setGenerateStructures(generateStructures);
        buildWorld.getRow().setGeneratorSettings(generatorSettings);
        buildWorld.getRow().setSeed(seed);
        buildWorld.getRow().setWorldType(worldType.name().toLowerCase());
        buildWorld.getRow().setEnvironment(environment.name().toLowerCase());
        buildWorld.getRow().setSpawnX(255.5);
        buildWorld.getRow().setSpawnY(65.0);
        buildWorld.getRow().setSpawnZ(255.5);
        final String finalPath = path;
        final String finalGenerator = generator;
        final boolean finalGenerateStructures = generateStructures;
        final String finalGeneratorSettings = generatorSettings;
        final Long finalSeed = seed;
        final WorldType finalWorldType = worldType;
        final World.Environment finalEnvironment = environment;
        buildWorld.insertAsync(() -> {
                plugin.getBuildWorlds().add(buildWorld);
                // getWorldConfig() calls mkdirs()
                buildWorld.getWorldConfig().set("world.Generator", finalGenerator);
                buildWorld.getWorldConfig().set("world.GenerateStructures", finalGenerateStructures);
                buildWorld.getWorldConfig().set("world.GeneratorSettings", finalGeneratorSettings);
                if (finalSeed != null) buildWorld.getWorldConfig().set("world.Seed", finalSeed);
                buildWorld.getWorldConfig().set("world.WorldType", finalWorldType.name().toLowerCase());
                buildWorld.getWorldConfig().set("world.Environment", finalEnvironment.name().toLowerCase());
                buildWorld.saveWorldConfig();
                sender.sendMessage(text("World '" + finalPath + "' created", YELLOW));
            });
        return true;
    }

    private boolean createVoidCommand(CommandSender sender, String[] args) {
        requireCreativeServer();
        if (!(sender instanceof Player)) {
            throw new CommandWarn("Player expected");
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
                throw new CommandWarn("Unknown environment: '" + value + "'");
            }
        }
        String path = name.toLowerCase();
        if (plugin.getBuildWorldByPathIgnoreCase(path) != null) {
            throw new CommandWarn("World already exists: '" + path + "'");
        }
        BuildWorld buildWorld = new BuildWorld(name, path, player.getUniqueId());
        buildWorld.getRow().setGenerator("VoidGenerator");
        buildWorld.getRow().setSeed(0L);
        buildWorld.getRow().setWorldType(WorldType.FLAT.name().toLowerCase());
        buildWorld.getRow().setEnvironment(environment.name().toLowerCase());
        buildWorld.getRow().setGenerateStructures(false);
        buildWorld.getRow().setGeneratorSettings("");
        buildWorld.getRow().setSpawnX(255.5);
        buildWorld.getRow().setSpawnY(65.0);
        buildWorld.getRow().setSpawnZ(255.5);
        final World.Environment finalEnvironment = environment;
        buildWorld.insertAsync(() -> {
                plugin.getBuildWorlds().add(buildWorld);
                // getWorldConfig() calls mkdirs()
                buildWorld.getWorldConfig().set("world.Generator", "VoidGenerator");
                buildWorld.getWorldConfig().set("world.GenerateStructures", "false");
                buildWorld.getWorldConfig().set("world.GeneratorSettings", "");
                buildWorld.getWorldConfig().set("world.Seed", 0L);
                buildWorld.getWorldConfig().set("world.WorldType", WorldType.FLAT.name());
                buildWorld.getWorldConfig().set("world.Environment", finalEnvironment.name());
                buildWorld.saveWorldConfig();
                buildWorld.loadWorld();
                buildWorld.teleportToSpawn(player);
                sender.sendMessage(text("World '" + path + "' created", YELLOW));
            });
        return true;
    }

    private boolean importCommand(CommandSender sender, String[] args) {
        requireCreativeServer();
        if (args.length != 2) return false;
        String name = args[0];
        String generator = args[1];
        final World world = plugin.getServer().getWorld(name);
        if (world == null) {
            throw new CommandWarn("World not found: " + name);
        }
        name = world.getName();
        if (plugin.getBuildWorldByPathIgnoreCase(name) != null) {
            throw new CommandWarn("Build world already exists: " + name);
        }
        WorldCreator creator = WorldCreator.name(name);
        creator.copy(world);
        BuildWorld buildWorld = new BuildWorld(name, name, null);
        buildWorld.insertAsync(() -> {
                plugin.getBuildWorlds().add(buildWorld);
                buildWorld.getWorldConfig().set("world.Generator", generator);
                buildWorld.getWorldConfig().set("world.Seed", creator.seed());
                buildWorld.getWorldConfig().set("world.WorldType", creator.type().name());
                buildWorld.getWorldConfig().set("world.Environment", creator.environment().name());
                buildWorld.saveWorldConfig();
                sender.sendMessage(text("World '" + world.getName() + "' imported", YELLOW));
            });
        return true;
    }

    private boolean loadCommand(CommandSender sender, String[] args) {
        requireCreativeServer();
        if (args.length != 1) return false;
        String name = args[0];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(name);
        if (buildWorld == null) {
            throw new CommandWarn("World not found: " + name);
        }
        buildWorld.reloadWorldConfig();
        World world = buildWorld.loadWorld();
        if (world == null) {
            throw new CommandWarn("Could not load world: " + buildWorld.getPath());
        }
        sender.sendMessage(text("World loaded: " + world.getName(), YELLOW));
        return true;
    }

    private boolean unloadCommand(CommandSender sender, String[] args) {
        requireCreativeServer();
        if (args.length > 2) return false;
        String name = args[0];
        BuildWorld buildWorld = plugin.getBuildWorldByPath(name);
        if (buildWorld == null) {
            throw new CommandWarn("World not found: " + name);
        }
        World world = buildWorld.getWorld();
        if (world == null) {
            throw new CommandWarn("Could not unload world: " + buildWorld.getPath());
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
        if (!plugin.getServer().unloadWorld(world, shouldSave)) {
            throw new CommandWarn("Could not unload world: " + buildWorld.getPath());
        }
        sender.sendMessage(text("World unloaded: " + buildWorld.getPath(), YELLOW));
        return true;
    }

    private void ignoreCommand(Player player) {
        requireCreativeServer();
        if (plugin.toggleIgnore(player)) {
            player.sendMessage(text("Ignoring world perms", YELLOW));
        } else {
            player.sendMessage(text("No longer ignoring world perms", YELLOW));
        }
        plugin.getPermission().updatePermissions(player);
    }

    private void debugPlotCommand(Player player) {
        requireCreativeServer();
        Block block = player.getLocation().getBlock();
        PlotWorld plotWorld = plugin.getPlotWorld(block.getWorld());
        if (plotWorld == null) {
            throw new CommandWarn("No plot world!");
        }
        player.sendMessage(text(block.getX() + "," + block.getY() + "," + block.getZ()
                                + ": " + plotWorld.debug(block), YELLOW));
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
        PlayerCache target = PlayerCache.require(playerName);
        final Trust trust = requireEnum(Trust.class, trustName);
        buildWorld.setTrust(target.getUuid(), trust, () -> {
                Player targetPlayer = Bukkit.getPlayer(target.uuid);
                if (targetPlayer != null) {
                    plugin.getPermission().updatePermissions(targetPlayer);
                }
                sender.sendMessage(text(playerName + " now has " + trust.nice() + " trust in world " + buildWorld.getName(), AQUA));
            });
        return true;
    }

    private boolean publicTrustCommand(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        final String worldName = args[0];
        final String trustName = args[1];
        final BuildWorld buildWorld = plugin.getBuildWorldByPath(worldName);
        if (buildWorld == null) {
            throw new CommandWarn("World not found: " + worldName);
        }
        final Trust trust = requireEnum(Trust.class, trustName);
        if (trust == buildWorld.getPublicTrust()) {
            throw new CommandWarn("Public trust already is " + trust.nice() + " in " + buildWorld.getPath());
        }
        buildWorld.getRow().setPublicTrust(trust.name().toLowerCase());
        buildWorld.saveAsync("publicTrust", () -> {
                sender.sendMessage(text("Set public trust " + buildWorld.getPath() + " to " + trust.nice(), YELLOW));
            });
        return true;
    }

    private boolean clearTrustCommand(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        BuildWorld buildWorld = requireBuildWorld(args[0]);
        final int count = buildWorld.getTrusted().size();
        buildWorld.clearTrustAsync(() -> {
                sender.sendMessage(text("Removed " + count + " players who were trusted in " + buildWorld.getPath(), YELLOW));
            });
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
        final BuildWorld buildWorld = requireBuildWorld(args[0]);
        List<String> buildGroups = List.of(Arrays.copyOfRange(args, 1, args.length));
        buildWorld.getRow().getCachedTag().setBuildGroups(buildGroups);
        buildWorld.saveAsync("tag", () -> {
                if (!buildGroups.isEmpty()) {
                    sender.sendMessage(text("Build groups of " + buildWorld.getName() + " set to " + buildGroups,
                                            YELLOW));
                } else {
                    sender.sendMessage(text("Build groups of " + buildWorld.getName() + " reset",
                                            YELLOW));
                }
            });
        return true;
    }

    private boolean copy(CommandSender sender, String[] args) {
        requireCreativeServer();
        if (args.length != 2) return false;
        final String a = args[0];
        final String b = args[1];
        final BuildWorld oldWorld = requireBuildWorld(a);
        final BuildWorld no = plugin.getBuildWorldByPathIgnoreCase(b);
        if (no != null) throw new CommandWarn("World already exists: " + no.getPath());
        SQLWorld row = oldWorld.getRow().clone();
        row.setId(null);
        row.setPath(b);
        BuildWorld buildWorld = new BuildWorld(row, List.of());
        buildWorld.insertAsync(() -> {
                plugin.getBuildWorlds().add(buildWorld);
                Files.copyFileStructure(oldWorld.getWorldFolder(), buildWorld.getWorldFolder());
                sender.sendMessage(text("World copied: " + oldWorld.getPath() + " => " + buildWorld.getPath(), YELLOW));
            });
        return true;
    }

    private void autoConvertCommand(CommandSender sender) {
        requireCreativeServer();
        if (!(sender instanceof ConsoleCommandSender)) {
            throw new CommandWarn("Console expected");
        }
        if (autoConverter != null) {
            throw new CommandWarn("An auto convesion task is already running");
        }
        sender.sendMessage(text("Starting auto conversion. See console...", YELLOW));
        autoConverter = new AutoConverter(plugin);
        autoConverter.start();
    }

    private boolean transferAllCommand(CommandSender sender, String[] args) {
        requireCreativeServer();
        if (!(sender instanceof ConsoleCommandSender)) {
            throw new CommandWarn("Console expected");
        }
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
        for (BuildWorld buildWorld : plugin.getBuildWorlds()) {
            if (buildWorld.getOwner() != null && from.uuid.equals(buildWorld.getOwner())) {
                buildWorld.getRow().setOwner(to.getUuid());
                buildWorld.saveAsync("owner", () -> { });
                total += 1;
                worldCount += 1;
            }
            Trust trust = buildWorld.getTrust(from.uuid);
            if (trust != null) {
                buildWorld.setTrust(from.uuid, Trust.NONE, () -> { });
                buildWorld.setTrust(to.uuid, trust, () -> { });
                total += 1;
                trustCount += 1;
            }
        }
        if (total == 0) {
            sender.sendMessage(text(from.name + " does not have any creative worlds", RED));
            return true;
        }
        sender.sendMessage(text("Transferred worlds from " + from.name + " to " + to.name + ":"
                                + " worlds=" + worldCount
                                + " trust=" + trustCount,
                                YELLOW));
        return true;
    }

    private void legacyWorldConvert(CommandSender sender) {
        requireCreativeServer();
        sender.sendMessage(text("Starting conversion. See console", YELLOW));
        Legacy.transferAllBuildWorlds();
    }

    private boolean minigameList(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final MinigameMatchType type = CommandArgCompleter.requireEnum(MinigameMatchType.class, args[0]);
        int total = 0;
        sender.sendMessage(text("Build worlds for minigame " + type.displayName, GOLD, BOLD));
        for (BuildWorld buildWorld : BuildWorld.findMinigameWorlds(type, false)) {
            if (buildWorld.getRow().parsePurpose() != BuildWorldPurpose.MINIGAME) continue;
            if (buildWorld.getRow().parseMinigame() != type) continue;
            sender.sendMessage(textOfChildren(text("- ", GRAY),
                                              text(buildWorld.getName(), YELLOW),
                                              text(" by " + buildWorld.getOwnerName(), GRAY),
                                              space(),
                                              (buildWorld.getRow().isPurposeConfirmed()
                                               ? text("Confirmed", GREEN)
                                               : text("Unconfirmed", RED)))
                               .hoverEvent(showText(buildWorld.adminTooltip()))
                               .clickEvent(suggestCommand("/cra tp " + buildWorld.getPath()))
                               .insertion(buildWorld.getPath()));
            total += 1;
        }
        sender.sendMessage(text("Total " + total, GRAY));
        return true;
    }

    private boolean minigameReset(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final BuildWorld buildWorld = requireBuildWorld(args[0]);
        buildWorld.getRow().resetPurpose();
        buildWorld.savePurposeAsync(() -> {
                sender.sendMessage(text("Purpose reset: " + buildWorld.getPath(), YELLOW));
            });
        return true;
    }

    private boolean minigameSet(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        final BuildWorld buildWorld = requireBuildWorld(args[0]);
        final MinigameMatchType minigame = CommandArgCompleter.requireEnum(MinigameMatchType.class, args[1]);
        buildWorld.getRow().setMinigame(minigame);
        buildWorld.savePurposeAsync(() -> {
                sender.sendMessage(text("Minigame set: " + buildWorld.getPath() + ", " + minigame, YELLOW));
            });
        return true;
    }

    private boolean minigameConfirm(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        final BuildWorld buildWorld = requireBuildWorld(args[0]);
        final boolean value = CommandArgCompleter.requireBoolean(args[1]);
        final MinigameMatchType minigame = buildWorld.getRow().parseMinigame();
        if (minigame == null) {
            throw new CommandWarn("Not a minigame world: " + buildWorld.getPath());
        }
        buildWorld.getRow().setPurposeConfirmed(value);
        buildWorld.getRow().setPurposeConfirmedWhen(new Date());
        buildWorld.savePurposeAsync(() -> {
                sender.sendMessage(text("Minigame confirmed: " + buildWorld.getPath() + ", " + minigame + ", " + value, YELLOW));
            });
        return true;
    }

    private boolean purposeList(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final BuildWorldPurpose type = CommandArgCompleter.requireEnum(BuildWorldPurpose.class, args[0]);
        int total = 0;
        sender.sendMessage(text("Build worlds with purpose " + type.displayName, GOLD, BOLD));
        for (BuildWorld buildWorld : plugin.getBuildWorlds()) {
            if (buildWorld.getRow().parsePurpose() != type) continue;
            List<Component> tooltip = new ArrayList<>();
            sender.sendMessage(textOfChildren(text("- ", GRAY),
                                              text(buildWorld.getName(), YELLOW),
                                              text(" by ", GRAY),
                                              text(buildWorld.getOwnerName(), YELLOW))
                               .hoverEvent(showText(buildWorld.adminTooltip()))
                               .clickEvent(suggestCommand("/cra tp " + buildWorld.getPath()))
                               .insertion(buildWorld.getPath()));
            total += 1;
        }
        sender.sendMessage(text("Total " + total, GRAY));
        return true;
    }

    private boolean purposeSet(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        final BuildWorld buildWorld = requireBuildWorld(args[0]);
        final BuildWorldPurpose purpose = CommandArgCompleter.requireEnum(BuildWorldPurpose.class, args[1]);
        buildWorld.getRow().setPurpose(purpose.name().toLowerCase());
        buildWorld.savePurposeAsync(() -> {
                sender.sendMessage(text("Purpose set: " + buildWorld.getPath() + ", " + purpose, YELLOW));
            });
        return true;
    }

    private boolean purposeReset(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final BuildWorld buildWorld = requireBuildWorld(args[0]);
        buildWorld.getRow().resetPurpose();
        buildWorld.savePurposeAsync(() -> {
                sender.sendMessage(text("Purpose reset: " + buildWorld.getPath(), YELLOW));
            });
        return true;
    }

    private boolean purposeIndex(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        final BuildWorld buildWorld = requireBuildWorld(args[0]);
        final int value = CommandArgCompleter.requireInt(args[1]);
        final BuildWorldPurpose purpose = buildWorld.getRow().parsePurpose();
        if (purpose == null) {
            throw new CommandWarn("No purpose: " + buildWorld.getPath());
        }
        buildWorld.getRow().setPurposeIndex(value);
        buildWorld.savePurposeAsync(() -> {
                sender.sendMessage(text("Purpose index updated: " + buildWorld.getPath() + ", " + purpose + ", " + value, YELLOW));
            });
        return true;
    }

    private boolean purposeScore(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        final BuildWorld buildWorld = requireBuildWorld(args[0]);
        final int value = CommandArgCompleter.requireInt(args[1]);
        final BuildWorldPurpose purpose = buildWorld.getRow().parsePurpose();
        if (purpose == null) {
            throw new CommandWarn("No purpose: " + buildWorld.getPath());
        }
        buildWorld.getRow().setVoteScore(value);
        buildWorld.savePurposeAsync(() -> {
                sender.sendMessage(text("Vote score updated: " + buildWorld.getPath() + ", " + purpose + ", " + value, YELLOW));
            });
        return true;
    }

    private void purposeImport(CommandSender sender) {
        File folder = new File(plugin.getDataFolder(), "purposeimport");
        if (!folder.isDirectory()) {
            throw new CommandWarn("Folder not found: " + folder);
        }
        for (File file : folder.listFiles()) {
            String name = file.getName();
            if (!name.endsWith(".yml")) continue;
            name = name.substring(0, name.length() - 4);
            final MinigameMatchType type;
            try {
                type = MinigameMatchType.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException iae) {
                sender.sendMessage(text("Unknown minigame type: " + name, RED));
                continue;
            }
            final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            int count = 0;
            List<String> lines = config.getStringList("Worlds");
            if (lines.isEmpty()) lines = config.getStringList("worlds");
            if (lines.isEmpty()) lines = config.getStringList("Maps");
            if (lines.isEmpty()) lines = config.getStringList("maps");
            if (lines.isEmpty()) {
                sender.sendMessage(text(file + ": World list not found!", RED));
                continue;
            }
            for (String line : lines) {
                BuildWorld buildWorld = plugin.getBuildWorldByPath(line);
                if (buildWorld == null) {
                    sender.sendMessage(text(type + ": World not found: " + line, RED));
                    continue;
                }
                buildWorld.getRow().setPurpose(BuildWorldPurpose.MINIGAME.name().toLowerCase());
                buildWorld.getRow().setPurposeType(type.name().toLowerCase());
                buildWorld.getRow().setPurposeConfirmed(true);
                buildWorld.getRow().setPurposeConfirmedWhen(new Date());
                buildWorld.savePurposeAsync(() -> { });
                count += 1;
            }
            sender.sendMessage(text(type + ": " + count + " worlds imported", YELLOW));
        }
    }
}
