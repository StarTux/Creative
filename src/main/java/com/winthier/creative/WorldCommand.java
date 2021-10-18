package com.winthier.creative;

import com.cavetale.money.Money;
import com.winthier.creative.util.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class WorldCommand implements TabExecutor {
    final CreativePlugin plugin;

    void load() {
    }

    static final class Wrong extends Exception {
        @Getter final String message;
        private boolean usage = false;

        Wrong() {
            this.message = null;
        }

        Wrong(final String msg) {
            this.message = msg;
        }

        static void noPerm() throws Wrong {
            throw new Wrong("You don't have permission.");
        }

        static void usage() throws Wrong {
            Wrong wrong = new Wrong();
            wrong.usage = true;
            throw wrong;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) return false;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            if (label.equalsIgnoreCase("worlds")) {
                listWorlds(player);
            } else {
                usage(player);
            }
            return true;
        }
        try {
            boolean res = onCommand(player, cmd, Arrays.copyOfRange(args, 1, args.length));
            if (!res) {
                usage(player, cmd);
                return true;
            }
        } catch (Wrong ce) {
            if (ce.usage) {
                usage(player, cmd);
            } else {
                player.sendMessage(Component.text(ce.getMessage(), NamedTextColor.RED));
            }
        }
        return true;
    }

    boolean onCommand(Player player, @NonNull String cmd, String[] args) throws Wrong {
        switch (cmd) {
        case "tp":
            if (args.length < 1) Wrong.usage();
            worldTeleport(player, String.join(" ", args));
            break;
        case "ls": case "list":
            if (args.length != 0) Wrong.usage();
            listWorlds(player);
            break;
        case "visit":
            if (args.length != 0) Wrong.usage();
            listVisits(player);
            break;
        case "info":
            if (args.length != 0) Wrong.usage();
            worldInfo(player);
            break;
        case "time":
            String arg = args.length >= 1 ? args[0] : null;
            worldTime(player, arg);
            break;
        case "difficulty":
            difficultyCommand(player, args);
            break;
        case "spawn":
            teleportToSpawn(player);
            break;
        case "setspawn":
            setWorldSpawn(player);
            break;
        case "trust":
            if (args.length != 1) Wrong.usage();
            trustCommand(player, args[0], Trust.BUILD);
            break;
        case "wetrust":
            if (args.length != 1) Wrong.usage();
            trustCommand(player, args[0], Trust.WORLD_EDIT);
            break;
        case "visittrust":
            if (args.length != 1) Wrong.usage();
            trustCommand(player, args[0], Trust.VISIT);
            break;
        case "ownertrust":
            if (args.length != 1) Wrong.usage();
            trustCommand(player, args[0], Trust.OWNER);
            break;
        case "untrust":
            if (args.length != 1) Wrong.usage();
            trustCommand(player, args[0], Trust.NONE);
            break;
        case "save": {
            if (args.length != 0) Wrong.usage();
            World world = player.getWorld();
            BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
            if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
                Wrong.noPerm();
            }
            world.save();
            player.sendMessage(Component.text("Saved your current world to disk.", NamedTextColor.GREEN));
            break;
        }
        case "rename":
            renameCommand(player, args);
            break;
        case "gamemode":
            gamemodeCommand(player, args);
            break;
        case "set": {
            if (args.length < 2) Wrong.usage();
            World world = player.getWorld();
            BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
            if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
                Wrong.noPerm();
            }
            changeWorldSetting(player, buildWorld, args[0].toLowerCase(),
                               List.of(Arrays.copyOfRange(args, 1, args.length)));
            break;
        }
        case "buy":
            buyCommand(player, args);
            break;
        case "unlock": return unlockCommand(player, args);
        case "grow": return growCommand(player, args);
        case "confirm":
            confirmCommand(player, args);
            break;
        case "cancel":
            cancelCommand(player, args);
            break;
        case "pvp":
            pvpCommand(player, args);
            break;
        default:
            Wrong.usage();
        }
        return true;
    }

    void confirm(Player player, String code, Runnable callback) {
        Meta meta = plugin.metaOf(player);
        meta.confirmCode = code;
        meta.confirmCallback = callback;
        player.sendMessage(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                    Component.text("[Confirm]", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/world confirm " + code))
                    .hoverEvent(HoverEvent.showText(Component.text("Confirm", NamedTextColor.GREEN))),
                    Component.text(" "),
                    Component.text("[Cancel]", NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/world cancel " + code))
                    .hoverEvent(HoverEvent.showText(Component.text("Cancel", NamedTextColor.RED))),
                }));
    }

    /**
     * Produce a String with 5 random readable characters.
     */
    String randomString() {
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyz"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "1234567890";
        int len = chars.length();
        for (int i = 0; i < 5; i += 1) {
            sb.append(chars.charAt(plugin.random.nextInt(len)));
        }
        return sb.toString();
    }

    /**
     * Callback for /world buy, then /world confirm.
     */
    void confirmBuy(Player player, double price, long size, BuyType type) {
        String base = player.getName().toLowerCase();
        String path;
        int suffix = 1;
        do {
            path = String.format("%s-%03d", base, suffix++);
        } while (plugin.getBuildWorldByPath(path) != null);
        plugin.getLogger().info("New world for " + base + ": " + path);
        if (!Money.take(player.getUniqueId(), price, plugin, "Buy creative world")) {
            player.sendMessage("You don't have enough money");
            return;
        }
        BuildWorld buildWorld = new BuildWorld(base, path, Builder.of(player));
        buildWorld.setSize(size);
        buildWorld.setCenterX(256);
        buildWorld.setCenterZ(256);
        plugin.getBuildWorlds().add(buildWorld);
        plugin.saveBuildWorlds();
        buildWorld.getWorldConfig().set("world.Seed", 0);
        buildWorld.getWorldConfig().set("world.WorldType", WorldType.FLAT.name());
        buildWorld.getWorldConfig().set("world.Environment", World.Environment.NORMAL.name());
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
        player.sendMessage(Component.text("Bought a world for ")
                           .append(Component.text(Money.format(price), NamedTextColor.GOLD))
                           .append(Component.text(". Please wait...")));
        World world = buildWorld.loadWorld();
        buildWorld.teleportToSpawn(player);
    }

    void confirmUnlock(Player player, BuildWorld buildWorld, BuildWorld.Flag flag) {
        if (flag.price == 0) return;
        if (buildWorld.isSet(flag)) return;
        if (!Money.take(player.getUniqueId(), flag.price, plugin, "Creative world unlock " + flag.key)) {
            player.sendMessage(Component.text("You don't have enough money", NamedTextColor.RED));
            return;
        }
        buildWorld.set(flag, true);
        plugin.saveBuildWorlds();
        plugin.getPermission().updatePermissions(buildWorld.getWorld());
        player.sendMessage(Component.text("Unlocked " + flag.key + " for ")
                           .append(Component.text(Money.format(flag.price), NamedTextColor.GOLD)));
    }

    List<String> filterContains(String term, List<String> in) {
        term = term.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String i: in) {
            if (i.toLowerCase().contains(term)) out.add(i);
        }
        return out;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) return null;
        if (args.length == 0) {
            return null;
        } else if (args.length == 1) {
            return filterContains(args[0], List.of("tp",
                                                   "ls", "list", "visit", "info",
                                                   "time", "spawn", "setspawn", "difficulty",
                                                   "trust", "wetrust", "visittrust", "ownertrust",
                                                   "untrust", "save", "rename", "gamemode", "set",
                                                   "buy", "unlock", "grow", "pvp"));
        } else if (args.length == 2 && args[0].equals("set")) {
            return filterContains(args[1], List.of("name", "description", "authors"));
        } else if (args.length == 2 && args[0].equals("difficulty")) {
            return filterContains(args[1], List.of("easy", "normal", "hard", "peaceful"));
        } else if (args.length == 2 && args[0].equals("buy")) {
            return filterContains(args[1], Stream.of(BuyType.values())
                                  .map(BuyType::name).map(String::toLowerCase).collect(Collectors.toList()));
        } else if (args.length == 2 && args[0].equals("tp")) {
            return plugin.completeWorldNames(player, args[1]);
        } else if (args.length == 2 && args[0].equals("gamemode")) {
            return filterContains(args[1], List.of("0", "1", "2", "3",
                                                   "survival", "creative", "adventure", "spectator"));
        }
        return null;
    }

    boolean worldTeleport(Player player, String worldName) throws Wrong {
        BuildWorld result = null;
        for (BuildWorld buildWorld : plugin.getBuildWorlds()) {
            if (!worldName.equals(buildWorld.getName())) continue;
            Trust trust = buildWorld.getTrust(player.getUniqueId());
            if (trust.isOwner()) {
                result = buildWorld;
                break;
            } else if (trust.canVisit()) {
                result = buildWorld;
            }
        }
        if (result == null) {
            throw new Wrong("World not found: " + worldName);
        }
        player.sendMessage(Component.text("Please wait.", NamedTextColor.GREEN));
        result.loadWorld();
        result.teleportToSpawn(player);
        player.sendMessage(Component.text("Teleported to " + result.getName(), NamedTextColor.GREEN));
        return true;
    }

    void listWorlds(Player player) {
        PlayerWorldList list = plugin.getPlayerWorldList(player.getUniqueId());
        player.sendMessage(Component.text("Your World List", NamedTextColor.GREEN));
        if (!list.owner.isEmpty()) {
            listWorlds(player, list.owner, "Worlds you own");
        }
        player.sendMessage(Component.text("Total " + list.owner.size() + " worlds", NamedTextColor.GREEN));
    }

    void listVisits(Player player) {
        PlayerWorldList list = plugin.getPlayerWorldList(player.getUniqueId());
        player.sendMessage(Component.text("World List", NamedTextColor.GREEN));
        if (!list.build.isEmpty()) {
            listWorlds(player, list.build, "Worlds you can build in");
        }
        if (!list.visit.isEmpty()) {
            listWorlds(player, list.visit, "Worlds you can visit");
        }
        int total = list.build.size() + list.visit.size();
        player.sendMessage(Component.text("Total " + total + " worlds", NamedTextColor.GREEN));
    }

    private void listWorlds(Player player, List<BuildWorld> list, String prefix) {
        List<Component> components = list.stream()
            .sorted(BuildWorld.NAME_SORT)
            .map(buildWorld -> Component.text("[" + buildWorld.getName() + "]", NamedTextColor.GREEN)
                 .hoverEvent(HoverEvent.showText(Component.text("Teleport to " + buildWorld.getName(),
                                                                NamedTextColor.GREEN)))
                 .clickEvent(ClickEvent.runCommand("/wtp " + buildWorld.getName())))
            .collect(Collectors.toList());
        player.sendMessage(Component.join(JoinConfiguration.builder()
                                          .prefix(Component.text(prefix + " "))
                                          .separator(Component.text(" "))
                                          .build(), components));
    }

    void worldTime(Player player, String arg) throws Wrong {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner()) Wrong.noPerm();
        if (arg == null) {
            long time = player.getWorld().getTime();
            String timeFormat = String.format("&a%02d&r:&a%02d&r (&2%d&r)", hours(time), minutes(time), time);
            player.sendMessage(Component.text("World time " + timeFormat, NamedTextColor.GREEN));
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
                player.sendMessage(Component.text("Time locked", NamedTextColor.GREEN));
                return;
            } else if ("unlock".equalsIgnoreCase(arg)) {
                player.getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
                player.sendMessage(Component.text("Time unlocked", NamedTextColor.GREEN));
                return;
            } else if (arg.contains(":")) {
                String[] arr = arg.split(":");
                if (arr.length != 2) throw new Wrong("Time expected: " + arg);
                long hours;
                long minutes;
                try {
                    hours = Long.parseLong(arr[0]);
                    minutes = Long.parseLong(arr[1]);
                } catch (NumberFormatException nfe) {
                    throw new Wrong("Time expected: " + arg);
                }
                time = raw(hours, minutes);
            } else {
                try {
                    time = Long.parseLong(arg);
                } catch (NumberFormatException nfe) {
                    throw new Wrong("Time expected: " + arg);
                }
            }
            player.getWorld().setTime(time);
            player.sendMessage(Component.text("Time set to " + time, NamedTextColor.GREEN));
        }
    }

    long hours(long raw) {
        long time = raw + 6000;
        long hours = time / 1000;
        if (hours >= 24) hours -= 24;
        return hours;
    }

    long minutes(long raw) {
        long time = raw + 6000;
        long minutes = ((time % 1000) * 60) / 1000;
        return minutes;
    }

    long raw(long hours, long minutes) {
        long time = (hours * 1000) + (minutes * 1000 / 60) - 6000;
        if (time < 0) time = 24000 + time;
        return time;
    }

    void teleportToSpawn(Player player) throws Wrong {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null) {
            Wrong.noPerm();
        } else if (!buildWorld.getTrust(uuid).canVisit()) {
            Wrong.noPerm();
        } else {
            buildWorld.teleportToSpawn(player);
        }
        player.sendMessage(Component.text("Teleported to the world spawn.", NamedTextColor.GREEN));
    }

    void setWorldSpawn(Player player) throws Wrong {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner()) Wrong.noPerm();
        buildWorld.setSpawnLocation(player.getLocation());
        buildWorld.saveWorldConfig();
        player.sendMessage(Component.text("World spawn was set to your current location.", NamedTextColor.GREEN));
    }

    void trustCommand(Player player, String target, Trust trust) throws Wrong {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
            Wrong.noPerm();
        }
        if (target.equals("*")) {
            buildWorld.setPublicTrust(trust);
            plugin.saveBuildWorlds();
            if (trust == Trust.NONE) {
                player.sendMessage(Component.text("Revoked public trust.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Changed public trust to " + trust.nice(), NamedTextColor.GREEN));
            }
        } else {
            Builder builder = Builder.find(target);
            if (builder == null) throw new Wrong("Player not found: " + target);
            if (!buildWorld.trustBuilder(builder, trust)) {
                throw new Wrong("Could not change trust level of " + builder.getName());
            }
            plugin.saveBuildWorlds();
            if (trust == Trust.NONE) {
                player.sendMessage(Component.text("Revoked trust of " + builder.getName(), NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Gave " + trust.nice() + " trust to " + builder.getName(), NamedTextColor.GREEN));
            }
        }
        plugin.getPermission().updatePermissions(player.getWorld());
    }

    private Component listTrusted(Player player, BuildWorld buildWorld, Trust trust) throws Wrong {
        List<String> names = new ArrayList<>();
        for (Builder builder: buildWorld.listTrusted(trust)) names.add(builder.getName());
        if (trust == Trust.OWNER && buildWorld.getOwner() != null) {
            names.add(buildWorld.getOwnerName());
        }
        Collections.sort(names);
        Component prefix;
        if (trust == Trust.OWNER) {
            prefix = Component.text("Owner ", NamedTextColor.GRAY);
        } else {
            prefix = Component.text(trust.nice() + " Trust ", NamedTextColor.GRAY);
        }
        if (names.isEmpty()) return prefix;
        Trust playerTrust = buildWorld.getTrust(player.getUniqueId());
        List<Component> components = new ArrayList<>();
        for (String name : names) {
            if (playerTrust.isOwner()) {
                components.add(Component.text(name, NamedTextColor.WHITE)
                               .clickEvent(ClickEvent.runCommand("/world untrust " + name))
                               .hoverEvent(HoverEvent.showText(Component.text("Untrust " + name,
                                                                              NamedTextColor.RED))));
            } else {
                components.add(Component.text(name, NamedTextColor.WHITE));
            }
        }
        return Component.join(JoinConfiguration.builder()
                              .prefix(prefix)
                              .separator(Component.space())
                              .build(),
                              components);
    }

    protected void worldInfo(Player player) throws Wrong {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null) Wrong.noPerm();
        Trust playerTrust = buildWorld.getTrust(player.getUniqueId());
        if (!playerTrust.canVisit()) Wrong.noPerm();
        List<ComponentLike> lines = new ArrayList<>();
        lines.add(Component.text("World Info " + buildWorld.getName(), NamedTextColor.GREEN, TextDecoration.BOLD));
        lines.add(listTrusted(player, buildWorld, Trust.OWNER));
        lines.add(listTrusted(player, buildWorld, Trust.WORLD_EDIT));
        lines.add(listTrusted(player, buildWorld, Trust.BUILD));
        lines.add(listTrusted(player, buildWorld, Trust.VISIT));
        if (!buildWorld.getBuildGroups().isEmpty()) {
            lines.add(Component.text().color(NamedTextColor.WHITE)
                      .append(Component.text(" Build Groups ", NamedTextColor.DARK_AQUA, TextDecoration.ITALIC))
                      .append(Component.text(String.join(" ", buildWorld.getBuildGroups()
                                                         .stream().map(Text::enumToCamelCase).collect(Collectors.toList())))));
        }
        if (buildWorld.getPublicTrust() != null && buildWorld.getPublicTrust() != Trust.NONE) {
            lines.add(Component.text(" Public Trust " + buildWorld.getPublicTrust().nice(), NamedTextColor.GREEN));
        }
        String description = buildWorld.getWorldConfig().getString("user.Description");
        if (description != null && !description.isEmpty()) {
            lines.add(Component.text(" Description " + description, NamedTextColor.GREEN));
        }
        List<String> authors = buildWorld.getWorldConfig().getStringList("user.Authors");
        if (!authors.isEmpty()) {
            lines.add(Component.text(" Authors " + String.join(", ", authors), NamedTextColor.GREEN));
        }
        player.sendMessage(Component.join(JoinConfiguration.separator(Component.newline()), lines));
    }

    void difficultyCommand(Player player, String[] args) throws Wrong {
        World world = player.getWorld();
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
        if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
            Wrong.noPerm();
        }
        if (args.length < 1) {
            Difficulty difficulty = player.getWorld().getDifficulty();
            player.sendMessage(Component.text("World difficulty is " + Text.enumToCamelCase(difficulty.name()), NamedTextColor.GREEN));
        } else if (args.length == 1) {
            Difficulty difficulty;
            try {
                difficulty = Difficulty.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new Wrong("Unknown difficulty: " + args[0]);
            }
            world.setDifficulty(difficulty);
            player.sendMessage(Component.text("World difficulty set to " + Text.enumToCamelCase(difficulty.name()), NamedTextColor.GREEN));
        } else {
            Wrong.usage();
        }
    }

    void renameCommand(Player player, String[] args) throws Wrong {
        if (args.length < 1) Wrong.usage();
        World world = player.getWorld();
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
        if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
            Wrong.noPerm();
        }
        StringBuilder sb = new StringBuilder(args[0]);
        for (int i = 1; i < args.length; ++i) {
            sb.append(" ").append(args[i]);
        }
        String name = sb.toString();
        buildWorld.setName(name);
        plugin.saveBuildWorlds();
        player.sendMessage(Component.text("Renamed your current world to " + name, NamedTextColor.GREEN));
    }

    void gamemodeCommand(Player player, String[] args) throws Wrong {
        if (args.length != 1) Wrong.usage();
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).canBuild()) {
            Wrong.noPerm();
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
            throw new Wrong("Invalid GameMode: " + args[0]);
        } else {
            player.setGameMode(newGM);
            player.sendMessage(Component.text("Set GameMode to " + newGM.name(), NamedTextColor.GREEN));
        }
    }

    enum BuyType {
        FLAT, VOID;
    }

    void buyCommand(Player player, String[] args) throws Wrong {
        if (!player.hasPermission("creative.world.buy")) {
            throw new Wrong("You don't have permission to buy a world!");
        }
        if (args.length > 1) Wrong.usage();
        BuyType type;
        if (args.length >= 1) {
            try {
                type = BuyType.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException iae) {
                throw new Wrong("Invalid type: " + args[0]);
            }
        } else {
            type = BuyType.VOID;
        }
        double price = 10000.0;
        long size = 256;
        String sizeFmt = "" + size;
        player.sendMessage(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                    Component.text("Buy a "),
                    Component.text(sizeFmt + "x" + sizeFmt + " " + Text.enumToCamelCase(type.name()),
                                   NamedTextColor.GREEN),
                    Component.text(" world for "),
                    Component.text(Money.format(price), NamedTextColor.GOLD),
                    Component.text("?"),
                }));
        String code = randomString();
        confirm(player, code, () -> confirmBuy(player, price, size, type));
    }

    boolean unlockCommand(Player player, String[] args) throws Wrong {
        if (!player.hasPermission("creative.world.buy")) {
            throw new Wrong("You don't have permission to buy a world!");
        }
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner()) {
            throw new Wrong("You cannot unlock features in this world.");
        }
        if (args.length == 0) {
            sendWorldFeatures(player, buildWorld);
            return true;
        }
        BuildWorld.Flag flag = BuildWorld.Flag.of(args[0]);
        if (flag == null || !flag.userCanEdit()) {
            throw new Wrong("Unknown flag: " + args[0]);
        }
        if (args.length == 1) {
            if (flag.price == 0) throw new Wrong("Cannot unlock");
            if (buildWorld.isSet(flag)) throw new Wrong("Already unlocked");
            player.sendMessage(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                        Component.text("Unlock "),
                        Component.text(flag.key, NamedTextColor.GREEN),
                        Component.text("  for "),
                        Component.text(Money.format(flag.price), NamedTextColor.GOLD),
                        Component.text("?"),
                    }));
            String code = randomString();
            confirm(player, code, () -> confirmUnlock(player, buildWorld, flag));
        } else if (args.length == 2) {
            boolean newValue = args[1].equals("on") ? true : false;
            if (buildWorld.isSet(flag) == newValue) {
                throw new Wrong("Already " + (newValue ? "enabled" : "disabled"));
            }
            buildWorld.set(flag, newValue);
            plugin.saveBuildWorlds();
            sendWorldFeatures(player, buildWorld);
            player.sendMessage(Component.text(flag.key + " " + (newValue ? "enabled" : "disabled"), NamedTextColor.GREEN));
            plugin.getPermission().updatePermissions(buildWorld.getWorld());
        } else {
            return false;
        }
        return true;
    }

    void sendWorldFeatures(Player player, BuildWorld buildWorld) {
        List<ComponentLike> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.text("World Features", NamedTextColor.GREEN));
        for (BuildWorld.Flag flag : BuildWorld.Flag.values()) {
            if (!flag.userCanEdit()) continue;
            boolean enabled = buildWorld.isSet(flag);
            if (flag.price == 0) {
                String cmd = "/world unlock " + flag.key + " on";
                String cmd2 = "/world unlock " + flag.key + " off";
                lines.add(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                            Component.text("[ON]", (enabled ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY))
                            .clickEvent(ClickEvent.runCommand(cmd))
                            .hoverEvent(HoverEvent.showText(Component.text(cmd, NamedTextColor.GREEN))),
                            Component.space(),
                            Component.text("[OFF]", (!enabled ? NamedTextColor.RED : NamedTextColor.DARK_GRAY))
                            .clickEvent(ClickEvent.runCommand(cmd2))
                            .hoverEvent(HoverEvent.showText(Component.text(cmd2, NamedTextColor.GREEN))),
                            Component.space(),
                            Component.text(flag.key, (enabled ? NamedTextColor.YELLOW : NamedTextColor.WHITE)),
                        }));
            } else {
                if (enabled) {
                    lines.add(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                                Component.text("[UNLOCKED]", NamedTextColor.DARK_GRAY),
                                Component.space(),
                                Component.text(flag.key, (enabled ? NamedTextColor.YELLOW : NamedTextColor.WHITE)),
                            }));
                } else {
                    String cmd = "/world unlock " + flag.key;
                    lines.add(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                                Component.text("[UNLOCK]", NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand(cmd))
                                .hoverEvent(HoverEvent.showText(Component.text(cmd, NamedTextColor.GREEN))),
                                Component.space(),
                                Component.text(flag.key, (enabled ? NamedTextColor.YELLOW : NamedTextColor.WHITE)),
                            }));
                }
            }
        }
        if (buildWorld.getSize() >= 0) {
            int growBy = 256;
            double price = 10000;
            Component tooltip = Component.join(JoinConfiguration.separator(Component.newline()), new Component[] {
                    Component.text("Grow world by " + growBy + " blocks"),
                    Component.text("in all directions"),
                    Component.text("Price: " + Money.format(price), NamedTextColor.GOLD),
                }).color(NamedTextColor.GREEN);
            lines.add(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                        Component.text("[GROW]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/world grow"))
                        .hoverEvent(HoverEvent.showText(tooltip)),
                        Component.text(" Size " + buildWorld.getSize()),
                    }));
        }
        lines.add(Component.empty());
        player.sendMessage(Component.join(JoinConfiguration.separator(Component.newline()), lines));
    }

    boolean growCommand(Player player, String[] args) throws Wrong {
        if (args.length != 0) return false;
        if (!player.hasPermission("creative.world.buy")) {
            throw new Wrong("You don't have permission to buy a world!");
        }
        int growBy = 256;
        double price = 10000;
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner() || buildWorld.getSize() < 0) {
            throw new Wrong("You cannot grow this world.");
        }
        player.sendMessage(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                    Component.text("Grow this world by "),
                    Component.text(growBy, NamedTextColor.GREEN),
                    Component.text(" blocks for "),
                    Component.text(Money.format(price), NamedTextColor.GOLD),
                    Component.text("?"),
                }));
        confirm(player, randomString(), () -> {
                if (!Money.take(player.getUniqueId(), price, plugin, "Creative world grow")) {
                    player.sendMessage(Component.text("You don't have enough money", NamedTextColor.RED));
                    return;
                }
                buildWorld.setSize(buildWorld.getSize() + growBy);
                plugin.saveBuildWorlds();
                World world = buildWorld.getWorld();
                if (world != null) {
                    world.getWorldBorder().setSize(buildWorld.getSize());
                }
                player.sendMessage(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                            Component.text("World border grown by "),
                            Component.text(growBy, NamedTextColor.GREEN),
                            Component.text(" blocks for "),
                            Component.text(Money.format(price), NamedTextColor.GOLD),
                        }));
            });
        return true;
    }

    void confirmCommand(Player player, String[] args) {
        if (args.length != 1) return;
        String code = args[0];
        Meta meta = plugin.metaOf(player);
        if (!code.equals(meta.confirmCode)) return;
        meta.confirmCode = null;
        Runnable run = meta.confirmCallback;
        meta.confirmCallback = null;
        run.run();
    }

    void cancelCommand(Player player, String[] args) {
        if (args.length != 1) return;
        String code = args[0];
        Meta meta = plugin.metaOf(player);
        if (!code.equals(meta.confirmCode)) return;
        meta.confirmCode = null;
        meta.confirmCallback = null;
        player.sendMessage(Component.text("Cancelled", NamedTextColor.RED));
    }

    protected void commandUsage(Player player, String sub, String args, String description, String suggestion) {
        Component message = Component.text("/world " + sub, NamedTextColor.YELLOW);
        if (args != null) {
            message = message.append(Component.text(" " + args, NamedTextColor.GOLD, TextDecoration.ITALIC));
        }
        message = message.append(Component.text(" " + description, NamedTextColor.GRAY));
        Component tooltip = Component.join(JoinConfiguration.separator(Component.newline()),
                                           Component.text(suggestion, NamedTextColor.GREEN),
                                           Component.text(description, NamedTextColor.GRAY));
        message = message
            .hoverEvent(HoverEvent.showText(tooltip))
            .clickEvent(ClickEvent.suggestCommand(suggestion));
        player.sendMessage(message);
    }

    protected void usage(Player player, String cmd) {
        switch (cmd) {
        case "list":
            commandUsage(player, "list", null, "List your worlds", "/world list");
            break;
        case "visit":
            commandUsage(player, "visit", null, "List worlds you can visit", "/world visit");
            break;
        case "info":
            commandUsage(player, "info", null, "Get world info", "/world info");
            break;
        case "buy":
            commandUsage(player, "buy", null, "Buy a world", "/world buy");
            break;
        case "unlock":
            commandUsage(player, "unlock", null, "Unlock world features", "/world unlock");
            break;
        case "rename": {
            BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
            if (buildWorld != null && !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
                buildWorld = null;
            }
            commandUsage(player, "rename", "<name>", "Change the name of your world",
                         "/world rename " + (buildWorld != null ? buildWorld.getName() : ""));
            break;
        }
        case "spawn":
            commandUsage(player, "spawn", null, "Warp to world spawn", "/world spawn");
            break;
        case "setspawn":
            commandUsage(player, "setspawn", null, "Set world spawn", "/world setspawn ");
            break;
        case "time":
            commandUsage(player, "time", "[time|lock|unlock]", "Get or set world time",
                         "/world time ");
            break;
        case "difficulty":
            commandUsage(player, "difficulty", "easy|normal|hard|peaceful",
                         "Get or set world difficulty",
                         "/world difficulty ");
            break;
        case "gamemode":
            commandUsage(player, "gamemode", "<mode>", "Change your GameMode",
                         "/world gamemode ");
            break;
        case "trust":
            commandUsage(player, "trust", "<player>", "Trust someone to build", "/world trust ");
            break;
        case "wetrust":
            commandUsage(player, "wetrust", "<player>", "Give someone WorldEdit trust",
                         "/world wetrust ");
            break;
        case "visittrust":
            commandUsage(player, "visittrust", "<player>", "Trust someone to visit",
                         "/world visittrust ");
            break;
        case "ownertrust":
            commandUsage(player, "ownertrust", "<player>", "Add a world owner",
                         "/world ownertrust ");
            break;
        case "untrust":
            commandUsage(player, "untrust", "<player>", "Revoke trust", "/world untrust ");
            break;
        case "save":
            commandUsage(player, "save", null, "Save your world to disk", "/world save");
            break;
        case "set":
            commandUsage(player, "set", "<name|description|authors> [...]", "World settings",
                         "/world set ");
            break;
        case "pvp":
            commandUsage(player, "pvp", null, "Toggle pvp",
                         "/world pvp ");
            break;
        default:
            player.sendMessage(Component.text("Unknown command: " + cmd, NamedTextColor.RED));
        }
    }

    void usage(Player player) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        boolean owner = buildWorld != null && buildWorld.getTrust(player.getUniqueId()).isOwner();
        player.sendMessage(Component.text("World Command Usage", NamedTextColor.GREEN));
        usage(player, "list");
        usage(player, "visit");
        usage(player, "info");
        usage(player, "buy");
        usage(player, "spawn");
        if (owner) {
            usage(player, "unlock");
            usage(player, "rename");
            usage(player, "setspawn");
            usage(player, "time");
            usage(player, "difficulty");
            usage(player, "gamemode");
            usage(player, "trust");
            if (buildWorld.isSet(BuildWorld.Flag.WORLD_EDIT)) {
                usage(player, "wetrust");
            }
            usage(player, "visittrust");
            usage(player, "ownertrust");
            usage(player, "untrust");
            usage(player, "save");
            usage(player, "set");
            usage(player, "pvp");
        }
    }

    void changeWorldSetting(Player player, BuildWorld buildWorld, String key, List<String> args) {
        if (key.equals("name")) {
            String name = String.join(" ", args);
            buildWorld.getWorldConfig().set("user.Name", name);
            buildWorld.setName(name);
            buildWorld.saveWorldConfig();
            plugin.saveBuildWorlds();
            player.sendMessage(Component.text("Set world name to " + String.join(" ", args), NamedTextColor.GREEN));
        } else if (key.equals("description")) {
            buildWorld.getWorldConfig().set("user.Description", String.join(" ", args));
            buildWorld.saveWorldConfig();
            player.sendMessage(Component.text("Set world description to " + String.join(" ", args), NamedTextColor.GREEN));
        } else if (key.equals("authors")) {
            buildWorld.getWorldConfig().set("user.Authors", args);
            buildWorld.saveWorldConfig();
            player.sendMessage(Component.text("Set world authors to " + String.join(", ", args), NamedTextColor.GREEN));
        }
    }

    void pvpCommand(Player player, String[] args) throws Wrong {
        World world = player.getWorld();
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
        UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner()) Wrong.noPerm();
        boolean pvp = !world.getPVP();
        world.setPVP(pvp);
        player.sendMessage(Component.text("Toggled PVP " + (pvp ? "on" : "off"), NamedTextColor.GREEN));
    }
}
