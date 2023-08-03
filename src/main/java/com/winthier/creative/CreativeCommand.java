package com.winthier.creative;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandContext;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.command.RemotePlayer;
import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.money.Money;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.mytems.item.coin.Coin;
import com.winthier.creative.util.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import static com.cavetale.core.command.CommandArgCompleter.enumLowerList;
import static com.cavetale.core.command.CommandArgCompleter.list;
import static com.cavetale.core.command.CommandArgCompleter.supplyList;
import static com.winthier.creative.CreativePlugin.plugin;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

final class CreativeCommand extends AbstractCommand<CreativePlugin> {
    public static final String BUY_PERMISSION = "creative.buy";

    protected CreativeCommand(final CreativePlugin plugin) {
        super(plugin, "creative");
    }

    private final Map<UUID, Meta> metaMap = new HashMap<>();

    @Override
    protected void onEnable() {
        rootNode.addChild("tp").arguments("<world>")
            .remoteServer(NetworkServer.CREATIVE)
            .description("Teleport to world")
            .completers(this::completeWorldNames)
            .remotePlayerCaller(this::worldTeleport);
        rootNode.addChild("list").denyTabCompletion()
            .alias("ls")
            .description("List your worlds")
            .playerCaller(this::listWorlds);
        rootNode.addChild("visit").denyTabCompletion()
            .description("List worlds you can visit")
            .playerCaller(this::listVisits);
        rootNode.addChild("buy").arguments("<type> <dimension>")
            .remoteServer(NetworkServer.CREATIVE)
            .description("Buy a new world")
            .completers(enumLowerList(BuyType.class),
                        enumLowerList(DimensionType.class))
            .remotePlayerCaller(this::buyCommand);
        rootNode.addChild("confirm").denyTabCompletion()
            .remoteServer(NetworkServer.CREATIVE)
            .hidden(true)
            .remotePlayerCaller(this::confirmCommand);
        rootNode.addChild("cancel").denyTabCompletion()
            .remoteServer(NetworkServer.CREATIVE)
            .hidden(true)
            .remotePlayerCaller(this::cancelCommand);
        if (plugin().isCreativeServer()) {
            rootNode.addChild("info").denyTabCompletion()
                .description("Get world info")
                .playerCaller(this::worldInfo);
            rootNode.addChild("time").arguments("[time]")
                .description("Set world time")
                .completers(supplyList(this::worldTimeCompletions))
                .playerCaller(this::worldTime);
            rootNode.addChild("difficulty").arguments("[difficulty]")
                .description("Set world difficulty")
                .completers(enumLowerList(Difficulty.class))
                .playerCaller(this::difficultyCommand);
            rootNode.addChild("spawn").denyTabCompletion()
                .description("Teleport to world spawn")
                .playerCaller(this::teleportToSpawn);
            rootNode.addChild("setspawn").denyTabCompletion()
                .description("Set world spawn")
                .playerCaller(this::setWorldSpawn);
            rootNode.addChild("trust").arguments("<player>")
                .description("Give build trust")
                .completers(PlayerCache.NAME_COMPLETER)
                .playerCaller(this::trust);
            rootNode.addChild("wetrust").arguments("<player>")
                .description("Give WorldEdit trust")
                .completers(PlayerCache.NAME_COMPLETER)
                .playerCaller(this::weTrust);
            rootNode.addChild("visittrust").arguments("<player>")
                .description("Give visit trust")
                .completers(PlayerCache.NAME_COMPLETER)
                .playerCaller(this::visitTrust);
            rootNode.addChild("ownertrust").arguments("<player>")
                .description("Give owner trust")
                .completers(PlayerCache.NAME_COMPLETER)
                .playerCaller(this::ownerTrust);
            rootNode.addChild("untrust").arguments("<player>")
                .description("Remove player trust")
                .completers(PlayerCache.NAME_COMPLETER)
                .playerCaller(this::untrust);
            rootNode.addChild("save").denyTabCompletion()
                .description("Save world")
                .playerCaller(this::save);
            rootNode.addChild("rename").arguments("<name>")
                .description("Rename world")
                .completers()
                .playerCaller(this::renameCommand);
            rootNode.addChild("gamemode").arguments("<gamemode>")
                .description("Change your game mode")
                .completers(enumLowerList(GameMode.class))
                .playerCaller(this::gamemodeCommand);
            rootNode.addChild("set").arguments("<setting> <value...>")
                .description("Change world setting")
                .completers(list("name", "description", "authors"),
                            CommandArgCompleter.EMPTY)
                .playerCaller(this::set);
            rootNode.addChild("unlock").arguments("<flag> [value]")
                .description("Unlock world features")
                .completers(enumLowerList(BuildWorld.Flag.class),
                            CommandArgCompleter.BOOLEAN)
                .playerCaller(this::unlockCommand);
            rootNode.addChild("grow").denyTabCompletion()
                .description("Grow your world borders")
                .playerCaller(this::growCommand);
            rootNode.addChild("pvp").denyTabCompletion()
                .description("Toggle world PvP")
                .playerCaller(this::pvpCommand);
            new PlotCommand(rootNode.addChild("plot")).onEnable();
        }
    }

    public List<String> completeWorldNames(CommandContext context, CommandNode node, String arg) {
        if (!context.isPlayer()) return List.of();
        return plugin.completeWorldNames(context.player, arg);
    }

    public boolean worldTeleport(RemotePlayer remote, String[] args) {
        if (args.length == 0) return false;
        final String worldName = String.join(" ", args);
        BuildWorld result = null;
        for (BuildWorld buildWorld : plugin.getBuildWorlds()) {
            if (!worldName.equals(buildWorld.getName())) continue;
            Trust trust = buildWorld.getTrust(remote.getUniqueId());
            if (trust.isOwner()) {
                result = buildWorld;
                break;
            } else if (trust.canVisit()) {
                result = buildWorld;
            }
        }
        if (result == null) {
            throw new CommandWarn("World not found: " + worldName);
        }
        final BuildWorld buildWorld = result;
        remote.sendMessage(text("Please wait.", GREEN));
        buildWorld.loadWorld();
        final Location loc = buildWorld.getSpawnLocation();
        remote.bring(plugin, loc, player -> {
                player.sendMessage(text("Teleported to " + buildWorld.getName(), GREEN));
            });
        return true;
    }

    private void listWorlds(Player player) {
        PlayerWorldList list = plugin.getPlayerWorldList(player.getUniqueId());
        player.sendMessage(text("Your World List", GREEN));
        if (!list.owner.isEmpty()) {
            listWorlds(player, list.owner, "Worlds you own");
        }
        player.sendMessage(text("Total " + list.owner.size() + " worlds", GREEN));
    }

    private void listVisits(Player player) {
        PlayerWorldList list = plugin.getPlayerWorldList(player.getUniqueId());
        player.sendMessage(text("World List", GREEN));
        if (!list.build.isEmpty()) {
            listWorlds(player, list.build, "Worlds you can build in");
        }
        if (!list.visit.isEmpty()) {
            listWorlds(player, list.visit, "Worlds you can visit");
        }
        int total = list.build.size() + list.visit.size();
        player.sendMessage(text("Total " + total + " worlds", GREEN));
    }

    private void listWorlds(Player player, List<BuildWorld> list, String prefix) {
        List<Component> components = list.stream()
            .sorted(BuildWorld.NAME_SORT)
            .map(buildWorld -> text("[" + buildWorld.getName() + "]", GREEN)
                 .hoverEvent(HoverEvent.showText(text("Teleport to " + buildWorld.getName(),
                                                      GREEN)))
                 .clickEvent(ClickEvent.runCommand("/ctp " + buildWorld.getName())))
            .collect(Collectors.toList());
        player.sendMessage(join(JoinConfiguration.builder()
                                .prefix(text(prefix + " "))
                                .separator(text(" "))
                                .build(), components));
    }

    private boolean buyCommand(RemotePlayer player, String[] args) {
        if (!player.hasPermission(BUY_PERMISSION)) {
            throw new CommandWarn("You don't have permission to buy a world!");
        }
        if (args.length > 2) return false;
        BuyType type;
        if (args.length >= 1) {
            try {
                type = BuyType.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new CommandWarn("Invalid type: " + args[0]);
            }
        } else {
            type = BuyType.VOID;
        }
        DimensionType dimension;
        if (args.length >= 2) {
            String arg = args[1];
            try {
                dimension = DimensionType.valueOf(arg.toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new CommandWarn("Invalid dimension: " + arg);
            }
        } else {
            dimension = DimensionType.OVERWORLD;
        }
        double price = 10000.0;
        int size = 256;
        String sizeFmt = "" + size;
        player.sendMessage(textOfChildren(text("Buy a "),
                                          text(sizeFmt + "x" + sizeFmt + " " + Text.enumToCamelCase(type.name()),
                                               GREEN),
                                          text(" " + dimension.displayName + " for "),
                                          Coin.format(price),
                                          text("?")));
        String code = randomString();
        confirm(player, code, player2 -> confirmBuy(player2, price, size, type, dimension));
        return true;
    }

    /**
     * Other commands call this for players to confirm stuff in the
     * future.
     */
    private void confirm(RemotePlayer player, String code, Consumer<RemotePlayer> callback) {
        Meta meta = new Meta();
        metaMap.put(player.getUniqueId(), meta);
        meta.confirmCode = code;
        meta.confirmCallback = callback;
        player.sendMessage(textOfChildren(text("[Confirm]", GREEN)
                                          .clickEvent(ClickEvent.runCommand("/creative confirm " + code))
                                          .hoverEvent(HoverEvent.showText(text("Confirm", GREEN))),
                                          text(" "),
                                          text("[Cancel]", RED)
                                          .clickEvent(ClickEvent.runCommand("/creative cancel " + code))
                                          .hoverEvent(HoverEvent.showText(text("Cancel", RED)))));
    }

    /**
     * Produce a String with 5 random readable characters.
     */
    private String randomString() {
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyz"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "1234567890";
        int len = chars.length();
        for (int i = 0; i < 5; i += 1) {
            sb.append(chars.charAt(plugin.getRandom().nextInt(len)));
        }
        return sb.toString();
    }

    /**
     * Callback for /creative buy, then /creative confirm.
     */
    private void confirmBuy(RemotePlayer player, double price, int size, BuyType type, DimensionType dimension) {
        final String base = player.getName().toLowerCase();
        String path;
        int suffix = 1;
        do {
            path = String.format("%s-%03d", base, suffix++);
        } while (plugin.getBuildWorldByPath(path) != null);
        plugin.getLogger().info("New world for " + base + ": " + path);
        if (!Money.get().take(player.getUniqueId(), price, plugin, "Buy creative world")) {
            player.sendMessage("You don't have enough money");
            return;
        }
        final BuildWorld buildWorld = new BuildWorld(base, path, player.getUniqueId());
        buildWorld.getRow().setBorderSize(size);
        buildWorld.getRow().setBorderCenterX(256);
        buildWorld.getRow().setBorderCenterZ(256);
        buildWorld.insertAsync(() -> {
                buildWorld.getWorldConfig().set("world.Seed", 0);
                buildWorld.getWorldConfig().set("world.WorldType", WorldType.FLAT.name());
                buildWorld.getWorldConfig().set("world.Environment", dimension.environment.name());
                buildWorld.getWorldConfig().set("world.GenerateStructures", false);
                buildWorld.getWorldConfig().set("world.GeneratorSettings", "");
                buildWorld.getWorldConfig().set("world.SpawnLocation.x", 256);
                buildWorld.getWorldConfig().set("world.SpawnLocation.y", 64);
                buildWorld.getWorldConfig().set("world.SpawnLocation.z", 256);
                buildWorld.getWorldConfig().set("world.SpawnLocation.pitch", 0);
                buildWorld.getWorldConfig().set("world.SpawnLocation.yaw", 0);
                switch (type) {
                case FLAT:
                    buildWorld.getWorldConfig().set("world.Generator", "FlatGenerator");
                    break;
                case VOID:
                default:
                    buildWorld.getWorldConfig().set("world.Generator", "VoidGenerator");
                    break;
                }
                buildWorld.saveWorldConfig();
                plugin.getBuildWorlds().add(buildWorld);
                player.sendMessage(text("Bought a world for ")
                                   .append(Coin.format(price))
                                   .append(text(". Please wait...")));
                buildWorld.loadWorld();
                final Location loc = buildWorld.getSpawnLocation();
                player.bring(plugin, loc, player2 -> {
                        player2.sendMessage(text("Teleported to " + buildWorld.getName(), GREEN));
                    });
            });
    }

    private void confirmUnlock(RemotePlayer player, BuildWorld buildWorld, BuildWorld.Flag flag) {
        if (flag.price == 0) return;
        if (buildWorld.isSet(flag)) return;
        if (!Money.get().take(player.getUniqueId(), flag.price, plugin, "Creative world unlock " + flag.key)) {
            player.sendMessage(text("You don't have enough money", RED));
            return;
        }
        buildWorld.set(flag, true);
        buildWorld.saveAsync("tag", () -> {
                plugin.getPermission().updatePermissions(buildWorld.getWorld());
                player.sendMessage(text("Unlocked " + flag.key + " for ")
                                   .append(Coin.format(flag.price)));
            });
    }

    private boolean worldTime(Player player, String[] args) {
        String arg = args.length >= 1 ? args[0] : null;
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner()) {
            throw new CommandWarn("You don't have permission");
        }
        if (arg == null) {
            long time = player.getWorld().getTime();
            String timeFormat = String.format("%02d:%02d (%d)", hours(time), minutes(time), time);
            player.sendMessage(text("World time " + timeFormat, GREEN));
        } else {
            long time;
            if ("day".equalsIgnoreCase(arg)) {
                time = 1000;
            } else if ("night".equalsIgnoreCase(arg)) {
                time = 13000;
            } else if ("noon".equalsIgnoreCase(arg)) {
                time = 6000;
            } else if ("midnight".equalsIgnoreCase(arg)) {
                time = 18000;
            } else if ("lock".equalsIgnoreCase(arg)) {
                player.getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                player.sendMessage(text("Time locked", GREEN));
                return true;
            } else if ("unlock".equalsIgnoreCase(arg)) {
                player.getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
                player.sendMessage(text("Time unlocked", GREEN));
                return true;
            } else if (arg.contains(":")) {
                String[] arr = arg.split(":");
                if (arr.length != 2) {
                    throw new CommandWarn("Time expected: " + arg);
                }
                long hours;
                long minutes;
                try {
                    hours = Long.parseLong(arr[0]);
                    minutes = Long.parseLong(arr[1]);
                } catch (NumberFormatException nfe) {
                    throw new CommandWarn("Time expected: " + arg);
                }
                time = raw(hours, minutes);
            } else {
                try {
                    time = Long.parseLong(arg);
                } catch (NumberFormatException nfe) {
                    throw new CommandWarn("Time expected: " + arg);
                }
            }
            player.getWorld().setTime(time);
            player.sendMessage(text("Time set to " + time, GREEN));
        }
        return true;
    }

    private List<String> worldTimeCompletions() {
        List<String> result = new ArrayList<>();
        result.add("day");
        result.add("night");
        result.add("noon");
        result.add("midnight");
        result.add("lock");
        result.add("unlock");
        for (int hour = 0; hour < 24; hour += 1) {
            for (int minute = 0; minute < 60; minute += 15) {
                result.add(String.format("%02d:%02d", hour, minute));
            }
        }
        for (int raw = 0; raw < 24000; raw += 1000) {
            result.add("" + raw);
        }
        return result;
    }

    private long hours(long raw) {
        long time = raw + 6000;
        long hours = time / 1000;
        if (hours >= 24) hours -= 24;
        return hours;
    }

    private long minutes(long raw) {
        long time = raw + 6000;
        long minutes = ((time % 1000) * 60) / 1000;
        return minutes;
    }

    private long raw(long hours, long minutes) {
        long time = (hours * 1000) + (minutes * 1000 / 60) - 6000;
        if (time < 0) time = 24000 + time;
        return time;
    }

    private void teleportToSpawn(Player player) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null) {
            throw new CommandWarn("You don't have permission");
        } else if (!buildWorld.getTrust(uuid).canVisit()) {
            throw new CommandWarn("You don't have permission");
        } else {
            buildWorld.teleportToSpawn(player);
        }
        player.sendMessage(text("Teleported to the world spawn.", GREEN));
    }

    private void setWorldSpawn(Player player) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner()) {
            throw new CommandWarn("You don't have permission");
        }
        buildWorld.setSpawnLocation(player.getLocation());
        buildWorld.saveSpawnAsync(() -> {
                player.sendMessage(text("World spawn was set to your current location.", GREEN));
            });
    }

    private boolean trust(Player player, String[] args) {
        if (args.length != 1) return false;
        return trustCommand(player, args[0], Trust.BUILD);
    }

    private boolean weTrust(Player player, String[] args) {
        if (args.length != 1) return false;
        return trustCommand(player, args[0], Trust.WORLD_EDIT);
    }

    private boolean visitTrust(Player player, String[] args) {
        if (args.length != 1) return false;
        return trustCommand(player, args[0], Trust.VISIT);
    }

    private boolean ownerTrust(Player player, String[] args) {
        if (args.length != 1) return false;
        return trustCommand(player, args[0], Trust.OWNER);
    }

    private boolean untrust(Player player, String[] args) {
        if (args.length != 1) return false;
        return trustCommand(player, args[0], Trust.NONE);
    }

    private boolean trustCommand(Player player, String target, Trust trust) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
            throw new CommandWarn("You don't have permission");
        }
        if (target.equals("*")) {
            buildWorld.getRow().setPublicTrust(trust);
            buildWorld.saveAsync("publicTrust", () -> {
                    if (trust == Trust.NONE) {
                        player.sendMessage(text("Revoked public trust.", GREEN));
                    } else {
                        player.sendMessage(text("Changed public trust to " + trust.nice(), GREEN));
                    }
                });
        } else {
            PlayerCache builder = PlayerCache.require(target);
            final boolean returnValue = buildWorld.setTrust(builder.uuid, trust, () -> {
                    if (trust == Trust.NONE) {
                        player.sendMessage(text("Revoked trust of " + builder.getName(), GREEN));
                    } else {
                        player.sendMessage(text("Gave " + trust.nice() + " trust to " + builder.getName(), GREEN));
                    }
                    plugin.getPermission().updatePermissions(player.getWorld());
                });
            if (!returnValue) {
                throw new CommandWarn("Could not change trust level of " + builder.getName());
            }
        }
        return true;
    }

    private void save(Player player) {
        World world = player.getWorld();
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
        if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
            throw new CommandWarn("You don't have permission");
        }
        world.save();
        player.sendMessage(text("Saved your current world to disk.", GREEN));
    }

    private Component listTrusted(Player player, BuildWorld buildWorld, Trust trust) {
        List<String> names = new ArrayList<>();
        for (UUID uuid : buildWorld.listTrusted(trust)) {
            names.add(PlayerCache.nameForUuid(uuid));
        }
        if (trust == Trust.OWNER && buildWorld.getOwner() != null) {
            names.add(buildWorld.getOwnerName());
        }
        Collections.sort(names);
        Component prefix;
        if (trust == Trust.OWNER) {
            prefix = text("Owner ", GRAY);
        } else {
            prefix = text(trust.nice() + " Trust ", GRAY);
        }
        if (names.isEmpty()) return prefix;
        Trust playerTrust = buildWorld.getTrust(player.getUniqueId());
        List<Component> components = new ArrayList<>();
        for (String name : names) {
            if (playerTrust.isOwner()) {
                components.add(text(name, WHITE)
                               .clickEvent(ClickEvent.runCommand("/creative untrust " + name))
                               .hoverEvent(HoverEvent.showText(text("Untrust " + name,
                                                                    RED))));
            } else {
                components.add(text(name, WHITE));
            }
        }
        return join(JoinConfiguration.builder()
                    .prefix(prefix)
                    .separator(space())
                    .build(),
                    components);
    }

    private void worldInfo(Player player) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null) {
            throw new CommandWarn("You don't have permission");
        }
        Trust playerTrust = buildWorld.getTrust(player.getUniqueId());
        if (!playerTrust.canVisit()) {
            throw new CommandWarn("You don't have permission");
        }
        List<ComponentLike> lines = new ArrayList<>();
        lines.add(text("World Info " + buildWorld.getName(), GREEN, BOLD));
        lines.add(listTrusted(player, buildWorld, Trust.OWNER));
        lines.add(listTrusted(player, buildWorld, Trust.WORLD_EDIT));
        lines.add(listTrusted(player, buildWorld, Trust.BUILD));
        lines.add(listTrusted(player, buildWorld, Trust.VISIT));
        if (!buildWorld.getBuildGroups().isEmpty()) {
            lines.add(text().color(WHITE)
                      .append(text(" Build Groups ", DARK_AQUA, ITALIC))
                      .append(text(String.join(" ", buildWorld.getBuildGroups()
                                               .stream().map(Text::enumToCamelCase).collect(Collectors.toList())))));
        }
        if (buildWorld.getPublicTrust() != null && buildWorld.getPublicTrust() != Trust.NONE) {
            lines.add(text(" Public Trust " + buildWorld.getPublicTrust().nice(), GREEN));
        }
        String description = buildWorld.getWorldConfig().getString("user.Description");
        if (description != null && !description.isEmpty()) {
            lines.add(text(" Description " + description, GREEN));
        }
        List<String> authors = buildWorld.getWorldConfig().getStringList("user.Authors");
        if (!authors.isEmpty()) {
            lines.add(text(" Authors " + String.join(", ", authors), GREEN));
        }
        player.sendMessage(join(separator(newline()), lines));
    }

    private boolean difficultyCommand(Player player, String[] args) {
        World world = player.getWorld();
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
        if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
            throw new CommandWarn("You don't have permission");
        }
        if (args.length < 1) {
            Difficulty difficulty = player.getWorld().getDifficulty();
            player.sendMessage(text("World difficulty is " + Text.enumToCamelCase(difficulty.name()), GREEN));
        } else if (args.length == 1) {
            Difficulty difficulty;
            try {
                difficulty = Difficulty.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new CommandWarn("Unknown difficulty: " + args[0]);
            }
            world.setDifficulty(difficulty);
            player.sendMessage(text("World difficulty set to " + Text.enumToCamelCase(difficulty.name()), GREEN));
        } else {
            return false;
        }
        return true;
    }

    private boolean renameCommand(Player player, String[] args) {
        if (args.length < 1) return false;
        World world = player.getWorld();
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
        if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
            throw new CommandWarn("You don't have permission");
        }
        StringBuilder sb = new StringBuilder(args[0]);
        for (int i = 1; i < args.length; ++i) {
            sb.append(" ").append(args[i]);
        }
        String name = sb.toString();
        buildWorld.getRow().setName(name);
        buildWorld.saveAsync("name", () -> {
                player.sendMessage(text("Renamed your current world to " + name, GREEN));
            });
        return true;
    }

    private boolean gamemodeCommand(Player player, String[] args) {
        if (args.length != 1) return false;
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).canBuild()) {
            throw new CommandWarn("You don't have permission");
        }
        GameMode newGM;
        switch (args[0].toLowerCase()) {
        case "0": newGM = GameMode.SURVIVAL; break;
        case "1": newGM = GameMode.CREATIVE; break;
        case "2": newGM = GameMode.ADVENTURE; break;
        case "3": newGM = GameMode.SPECTATOR; break;
        case "survival": newGM = GameMode.SURVIVAL; break;
        case "creative": newGM = GameMode.CREATIVE; break;
        case "adventure": newGM = GameMode.ADVENTURE; break;
        case "spectator": newGM = GameMode.SPECTATOR; break;
        default:
            newGM = null;
        }
        if (newGM == null) {
            throw new CommandWarn("Invalid GameMode: " + args[0]);
        } else {
            player.setGameMode(newGM);
            player.sendMessage(text("Set GameMode to " + newGM.name(), GREEN));
        }
        return true;
    }

    enum BuyType {
        FLAT, VOID;
    }

    private boolean unlockCommand(Player player, String[] args) {
        if (!player.hasPermission(BUY_PERMISSION)) {
            throw new CommandWarn("You don't have permission to buy a world!");
        }
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner()) {
            throw new CommandWarn("You cannot unlock features in this world.");
        }
        if (args.length == 0) {
            sendWorldFeatures(player, buildWorld);
            return true;
        }
        BuildWorld.Flag flag = BuildWorld.Flag.of(args[0]);
        if (flag == null || !flag.userCanEdit()) {
            throw new CommandWarn("Unknown flag: " + args[0]);
        }
        if (args.length == 1) {
            if (flag.price == 0) {
                throw new CommandWarn("Cannot unlock");
            }
            if (buildWorld.isSet(flag)) {
                throw new CommandWarn("Already unlocked");
            }
            player.sendMessage(textOfChildren(text("Unlock "),
                                              text(flag.key, GREEN),
                                              text("  for "),
                                              Coin.format(flag.price),
                                              text("?")));
            String code = randomString();
            confirm(RemotePlayer.wrap(player), code, remote -> confirmUnlock(remote, buildWorld, flag));
        } else if (args.length == 2) {
            boolean newValue = args[1].equals("on") ? true : false;
            if (buildWorld.isSet(flag) == newValue) {
                throw new CommandWarn("Already " + (newValue ? "enabled" : "disabled"));
            }
            buildWorld.set(flag, newValue);
            buildWorld.saveAsync("tag", () -> {
                    sendWorldFeatures(player, buildWorld);
                    player.sendMessage(text(flag.key + " " + (newValue ? "enabled" : "disabled"), GREEN));
                    plugin.getPermission().updatePermissions(buildWorld.getWorld());
                });
        } else {
            return false;
        }
        return true;
    }

    private void sendWorldFeatures(Player player, BuildWorld buildWorld) {
        List<ComponentLike> lines = new ArrayList<>();
        lines.add(empty());
        lines.add(text("World Features", GREEN));
        for (BuildWorld.Flag flag : BuildWorld.Flag.values()) {
            if (!flag.userCanEdit()) continue;
            boolean enabled = buildWorld.isSet(flag);
            if (flag.price == 0) {
                String cmd = "/creative unlock " + flag.key + " on";
                String cmd2 = "/creative unlock " + flag.key + " off";
                lines.add(textOfChildren(text("[ON]", (enabled ? GREEN : DARK_GRAY))
                                         .clickEvent(ClickEvent.runCommand(cmd))
                                         .hoverEvent(HoverEvent.showText(text(cmd, GREEN))),
                                         space(),
                                         text("[OFF]", (!enabled ? RED : DARK_GRAY))
                                         .clickEvent(ClickEvent.runCommand(cmd2))
                                         .hoverEvent(HoverEvent.showText(text(cmd2, GREEN))),
                                         space(),
                                         text(flag.key, (enabled ? YELLOW : WHITE))));
            } else {
                if (enabled) {
                    lines.add(textOfChildren(text("[UNLOCKED]", DARK_GRAY),
                                             space(),
                                             text(flag.key, (enabled ? YELLOW : WHITE))));
                } else {
                    String cmd = "/creative unlock " + flag.key;
                    lines.add(textOfChildren(text("[UNLOCK]", GREEN)
                                             .clickEvent(ClickEvent.runCommand(cmd))
                                             .hoverEvent(HoverEvent.showText(text(cmd, GREEN))),
                                             space(),
                                             text(flag.key, (enabled ? YELLOW : WHITE))));
                }
            }
        }
        if (buildWorld.getRow().getBorderSize() >= 0) {
            final int growBy = 256;
            final double price = 10000;
            final Component tooltip = join(separator(newline()), new Component[] {
                    text("Grow world by " + growBy + " blocks"),
                    text("in all directions"),
                    textOfChildren(text("Price: ", GRAY), Coin.format(price))
                }).color(GREEN);
            lines.add(textOfChildren(text("[GROW]", GREEN)
                                     .clickEvent(ClickEvent.runCommand("/creative grow"))
                                     .hoverEvent(HoverEvent.showText(tooltip)),
                                     text(" Size " + buildWorld.getRow().getBorderSize())));
        }
        lines.add(empty());
        player.sendMessage(join(separator(newline()), lines));
    }

    private boolean growCommand(Player player, String[] args) {
        if (args.length != 0) return false;
        if (!player.hasPermission(BUY_PERMISSION)) {
            throw new CommandWarn("You don't have permission to buy a world!");
        }
        final int growBy = 256;
        final double price = 10000;
        final BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        final UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner() || buildWorld.getRow().getBorderSize() < 0) {
            throw new CommandWarn("You cannot grow this world.");
        }
        player.sendMessage(textOfChildren(text("Grow this world by "),
                                          text(growBy, GREEN),
                                          text(" blocks for "),
                                          Coin.format(price),
                                          text("?")));
        confirm(RemotePlayer.wrap(player), randomString(), remote -> {
                if (!Money.get().take(remote.getUniqueId(), price, plugin, "Creative world grow")) {
                    remote.sendMessage(text("You don't have enough money", RED));
                    return;
                }
                buildWorld.getRow().setBorderSize(buildWorld.getRow().getBorderSize() + growBy);
                buildWorld.saveAsync("size", () -> {
                        World world = buildWorld.getWorld();
                        if (world != null) {
                            world.getWorldBorder().setSize(buildWorld.getRow().getBorderSize());
                        }
                        remote.sendMessage(textOfChildren(text("World border grown by "),
                                                          text(growBy, GREEN),
                                                          text(" blocks for "),
                                                          Coin.format(price)));
                    });
            });
        return true;
    }

    private boolean confirmCommand(RemotePlayer player, String[] args) {
        if (args.length != 1) return true;
        String code = args[0];
        Meta meta = metaMap.remove(player.getUniqueId());
        if (meta == null || !code.equals(meta.confirmCode)) return true;
        meta.confirmCallback.accept(player);
        return true;
    }

    private boolean cancelCommand(RemotePlayer player, String[] args) {
        if (args.length != 1) return true;
        String code = args[0];
        Meta meta = metaMap.remove(player.getUniqueId());
        if (meta == null || !code.equals(meta.confirmCode)) return true;
        player.sendMessage(text("Cancelled", RED));
        return true;
    }

    private boolean set(Player player, String[] args) {
        if (args.length < 2) return false;
        World world = player.getWorld();
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
        if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
            throw new CommandWarn("You don't have permission");
        }
        changeWorldSetting(player, buildWorld, args[0].toLowerCase(),
                           List.of(Arrays.copyOfRange(args, 1, args.length)));
        return true;
    }

    private void changeWorldSetting(Player player, BuildWorld buildWorld, String key, List<String> args) {
        if (key.equals("name")) {
            final String name = String.join(" ", args);
            // File
            buildWorld.getWorldConfig().set("user.Name", name);
            buildWorld.saveWorldConfig();
            // SQL
            buildWorld.getRow().setName(name);
            buildWorld.saveAsync("name", () -> {
                    player.sendMessage(text("Set world name to " + String.join(" ", args), GREEN));
                });
        } else if (key.equals("description")) {
            final String desc = String.join(" ", args);
            // File
            buildWorld.getWorldConfig().set("user.Description", desc);
            buildWorld.saveWorldConfig();
            // SQL
            buildWorld.getRow().setDescription(desc);
            buildWorld.saveAsync("description", () -> {
                    player.sendMessage(textOfChildren(text("Set world description to ", GREEN),
                                                      text(desc, GRAY)));
                });
        }
    }

    private void pvpCommand(Player player) {
        World world = player.getWorld();
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
        UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner()) {
            throw new CommandWarn("You don't have permission");
        }
        boolean pvp = !world.getPVP();
        world.setPVP(pvp);
        player.sendMessage(text("Toggled PVP " + (pvp ? "on" : "off"), GREEN));
    }

    @RequiredArgsConstructor
    public enum DimensionType {
        OVERWORLD(World.Environment.NORMAL, "Overworld"),
        NETHER(World.Environment.NETHER, "Nether"),
        END(World.Environment.THE_END, "End");

        public final World.Environment environment;
        public final String displayName;
    }
}
