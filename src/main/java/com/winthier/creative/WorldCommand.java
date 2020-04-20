package com.winthier.creative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
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
        // Here we could load some data if we wanted...
    }

    static final class Wrong extends Exception {
        @Getter final String message;
        private boolean usage = false;

        Wrong() {
            this.message = null;
        }

        Wrong(final String msg, final Object... o) {
            this.message = Msg.format(msg, o);
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
            onCommand(player, cmd, Arrays.copyOfRange(args, 1, args.length));
        } catch (Wrong ce) {
            if (ce.usage) {
                usage(player, cmd);
            } else {
                Msg.warn(player, "%s", ce.getMessage());
            }
        }
        return true;
    }

    void onCommand(Player player, @NonNull String cmd, String[] args) throws Wrong {
        switch (cmd) {
        case "tp":
            if (args.length != 1) Wrong.usage();
            String worldName = args[0];
            worldTeleport(player, worldName);
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
            Msg.info(player, "Saved your current world to disk.");
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
                               Arrays.asList(Arrays.copyOfRange(args, 1, args.length)));
            break;
        }
        case "buy":
            buyCommand(player, args);
            break;
        case "confirm":
            confirmCommand(player, args);
            break;
        case "cancel":
            cancelCommand(player, args);
            break;
        default:
            Wrong.usage();
        }
    }

    void confirm(Player player, String code, Runnable callback) {
        Meta meta = plugin.metaOf(player);
        meta.confirmCode = code;
        meta.confirmCallback = callback;
        ComponentBuilder cb = Msg.componentBuilder();
        cb.append("[Confirm]").color(ChatColor.GREEN);
        cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/world confirm " + code));
        cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Msg.lore(ChatColor.GREEN + "Confirm")));
        cb.append(" ").reset();
        cb.append("[Cancel]").color(ChatColor.RED);
        cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/world cancel " + code));
        cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Msg.lore(ChatColor.RED + "Cancel")));
        player.spigot().sendMessage(cb.create());
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
    void confirmBuy(Player player, double price, long size) {
        String base = player.getName();
        String path;
        int suffix = 1;
        do {
            path = String.format("%s-%03d", base, suffix++);
        } while (plugin.getBuildWorldByPath(path) != null);
        plugin.getLogger().info("New world for " + base + ": " + path);
        if (!plugin.vault.take(player, price)) {
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
        buildWorld.getWorldConfig().set("world.SpawnLocation.x", 256);
        buildWorld.getWorldConfig().set("world.SpawnLocation.y", 5);
        buildWorld.getWorldConfig().set("world.SpawnLocation.z", 256);
        buildWorld.getWorldConfig().set("world.SpawnLocation.pitch", 0);
        buildWorld.getWorldConfig().set("world.SpawnLocation.yaw", 0);
        buildWorld.saveWorldConfig();
        player.sendMessage("Bought a world for "
                           + ChatColor.GREEN + plugin.vault.format(price)
                           + ChatColor.WHITE + ". Please wait...");
        World world = buildWorld.loadWorld();
        buildWorld.teleportToSpawn(player);
    }

    List<String> filterStartsWith(String term, List<String> in) {
        term = term.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String i: in) {
            if (i.toLowerCase().startsWith(term)) out.add(i);
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
            return filterStartsWith(args[0], Arrays.asList("list", "visit", "info", "time", "spawn",
                                                           "setspawn", "difficulty", "trust",
                                                           "untrust"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return filterStartsWith(args[1], Arrays.asList("name", "description", "authors"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("difficulty")) {
            return filterStartsWith(args[1], Arrays.asList("easy", "normal", "hard", "peaceful"));
        }
        return null;
    }

    boolean worldTeleport(Player player, String worldName) {
        BuildWorld buildWorld = plugin.getBuildWorldByPath(worldName);
        if (buildWorld == null) {
            Msg.warn(player, "World not found: %s", worldName);
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (!buildWorld.getTrust(uuid).canVisit()) {
            Msg.warn(player, "World not found: %s", worldName);
            return false;
        }
        Msg.info(player, "Please wait.");
        buildWorld.loadWorld();
        buildWorld.teleportToSpawn(player);
        Msg.info(player, "Teleported to %s.", buildWorld.getName());
        return true;
    }

    void listWorlds(Player player) {
        PlayerWorldList list = plugin.getPlayerWorldList(player.getUniqueId());
        Msg.info(player, "Your World List");
        if (!list.owner.isEmpty()) {
            listWorlds(player, list.owner, "Worlds you own");
        }
        Msg.send(player, "&7Total %d worlds", list.owner.size());
    }

    void listVisits(Player player) {
        PlayerWorldList list = plugin.getPlayerWorldList(player.getUniqueId());
        Msg.info(player, "World List");
        if (!list.build.isEmpty()) {
            listWorlds(player, list.build, "Worlds you can build in");
        }
        if (!list.visit.isEmpty()) {
            listWorlds(player, list.visit, "Worlds you can visit");
        }
        int total = list.build.size() + list.visit.size();
        Msg.send(player, "&7Total %d worlds", total);
    }

    private void listWorlds(Player player, List<BuildWorld> list, String prefix) {
        Collections.sort(list, BuildWorld.NAME_SORT);
        List<Object> json = new ArrayList<>();
        json.add(Msg.button(ChatColor.WHITE, prefix, null, null));
        int count = 1;
        for (BuildWorld buildWorld: list) {
            json.add(" ");
            json.add(Msg.button(ChatColor.GREEN,
                                "&f[&a" + buildWorld.getName() + "&r]",
                                "Teleport to " + buildWorld.getName(),
                                "/wtp " + buildWorld.getPath()));
            count += 1;
            if (count >= 3 && !json.isEmpty()) {
                count = 0;
                Msg.raw(player, json);
                json.clear();
            }
        }
        if (!json.isEmpty()) {
            Msg.raw(player, json);
        }
    }

    void worldTime(Player player, String arg) throws Wrong {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner()) Wrong.noPerm();
        if (arg == null) {
            long time = player.getWorld().getTime();
            Msg.send(player, "World time &a%02d&r:&a%02d&r (&2%d&r)",
                     hours(time), minutes(time), time);
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
                player.getWorld().setGameRuleValue("doDaylightCycle", "false");
                Msg.info(player, "Time locked");
                return;
            } else if ("unlock".equalsIgnoreCase(arg)) {
                player.getWorld().setGameRuleValue("doDaylightCycle", "true");
                Msg.info(player, "Time unlocked");
                return;
            } else if (arg.contains(":")) {
                String[] arr = arg.split(":");
                if (arr.length != 2) throw new Wrong("Time expected: %s", arg);
                long hours;
                long minutes;
                try {
                    hours = Long.parseLong(arr[0]);
                    minutes = Long.parseLong(arr[1]);
                } catch (NumberFormatException nfe) {
                    throw new Wrong("Time expected: %s", arg);
                }
                time = raw(hours, minutes);
            } else {
                try {
                    time = Long.parseLong(arg);
                } catch (NumberFormatException nfe) {
                    throw new Wrong("Time expected: %s", arg);
                }
            }
            player.getWorld().setTime(time);
            Msg.info(player, "Time set to %d.", time);
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
        Msg.info(player, "Teleported to the world spawn.");
    }

    void setWorldSpawn(Player player) throws Wrong {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner()) Wrong.noPerm();
        buildWorld.setSpawnLocation(player.getLocation());
        buildWorld.saveWorldConfig();
        Msg.info(player, "World spawn was set to your current location.");
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
                Msg.info(player, "Revoked public trust.");
            } else {
                Msg.info(player, "Changed public trust to %s", trust.nice());
            }
        } else {
            Builder builder = Builder.find(target);
            if (builder == null) throw new Wrong("Player not found: %s.", target);
            if (!buildWorld.trustBuilder(builder, trust)) {
                throw new Wrong("Could not change trust level of %s.", builder.getName());
            }
            plugin.saveBuildWorlds();
            if (trust == Trust.NONE) {
                Msg.info(player, "Revoked trust of %s.", builder.getName());
            } else {
                Msg.info(player, "Gave %s trust to %s.", trust.nice(), builder.getName());
            }
        }
        plugin.getPermission().updatePermissions(player.getWorld());
    }

    void listTrusted(Player player, BuildWorld buildWorld, Trust trust) throws Wrong {
        List<String> names = new ArrayList<>();
        for (Builder builder: buildWorld.listTrusted(trust)) names.add(builder.getName());
        if (trust == Trust.OWNER && buildWorld.getOwner() != null) {
            names.add(buildWorld.getOwnerName());
        }
        if (names.isEmpty()) return;
        Collections.sort(names);
        List<Object> json = new ArrayList<>();
        if (trust == Trust.OWNER) {
            json.add(Msg.format(" &3&o%s", trust.nice()));
        } else {
            json.add(Msg.format(" &3&o%s Trust", trust.nice()));
        }
        Trust playerTrust = buildWorld.getTrust(player.getUniqueId());
        for (String name: names) {
            json.add(" ");
            if (playerTrust.isOwner()) {
                json.add(Msg.button(
                             ChatColor.WHITE,
                             name, "Untrust " + name,
                             "/world untrust " + name));
            } else {
                json.add(Msg.button(
                             ChatColor.WHITE,
                             name,
                             null,
                             null));
            }
        }
        Msg.raw(player, json);
    }

    void worldInfo(Player player) throws Wrong {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null) Wrong.noPerm();
        Trust playerTrust = buildWorld.getTrust(player.getUniqueId());
        if (!playerTrust.canVisit()) Wrong.noPerm();
        Msg.info(player, "&l%s &3World Info", buildWorld.getName());
        listTrusted(player, buildWorld, Trust.OWNER);
        listTrusted(player, buildWorld, Trust.WORLD_EDIT);
        listTrusted(player, buildWorld, Trust.BUILD);
        listTrusted(player, buildWorld, Trust.VISIT);
        if (buildWorld.getPublicTrust() != null && buildWorld.getPublicTrust() != Trust.NONE) {
            Msg.send(player, " &3&oPublic Trust &r%s", buildWorld.getPublicTrust().nice());
        }
        String description = buildWorld.getWorldConfig().getString("user.Description");
        if (description != null && !description.isEmpty()) {
            Msg.send(player, " &3&oDescription &r%s", description);
        }
        List<String> authors = buildWorld.getWorldConfig().getStringList("user.Authors");
        if (!authors.isEmpty()) {
            Msg.send(player, " &3&oAuthors &r%s", Msg.fold(authors, ", "));
        }
    }

    void difficultyCommand(Player player, String[] args) throws Wrong {
        World world = player.getWorld();
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
        if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
            Wrong.noPerm();
        }
        if (args.length < 1) {
            Difficulty difficulty = player.getWorld().getDifficulty();
            Msg.info(player, "World difficulty is %s.", Msg.camelCase(difficulty.name()));
        } else if (args.length == 1) {
            Difficulty difficulty;
            try {
                difficulty = Difficulty.valueOf(args[0]);
            } catch (IllegalArgumentException iae) {
                throw new Wrong("Unknown difficulty: %s", args[0]);
            }
            world.setDifficulty(difficulty);
            Msg.info(player, "World difficulty set to %s.",
                     Msg.camelCase(difficulty.name()));
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
        Msg.info(player, "Renamed your current world to '%s'.", name);
    }

    void gamemodeCommand(Player player, String[] args) throws Wrong {
        if (args.length != 1) Wrong.usage();
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
            Wrong.noPerm();
        }
        GameMode newGM;
        switch (args[0].toLowerCase()) {
        case "0": newGM = GameMode.SURVIVAL; break;
        case "1": newGM = GameMode.CREATIVE; break;
        case "2": newGM = GameMode.ADVENTURE; break;
        case "survival": newGM = GameMode.SURVIVAL; break;
        case "creative": newGM = GameMode.CREATIVE; break;
        case "adventure": newGM = GameMode.ADVENTURE; break;
        default:
            newGM = null;
        }
        if (newGM == null) {
            Msg.warn(player, "Invalid GameMode: %s", args[0]);
        } else {
            player.setGameMode(newGM);
            Msg.info(player, "Set GameMode to %s", newGM.name());
        }
    }

    void buyCommand(Player player, String[] args) throws Wrong {
        if (args.length != 0) Wrong.usage();
        double price = 10000.0;
        long size = 256;
        String sizeFmt = "" + size;
        ComponentBuilder cb = Msg.componentBuilder();
        cb.append("Buy a ").color(ChatColor.WHITE);
        cb.append(sizeFmt).color(ChatColor.GREEN);
        cb.append("x").color(ChatColor.GRAY);
        cb.append(sizeFmt).color(ChatColor.GREEN);
        cb.append(" flat world for ").color(ChatColor.WHITE);
        cb.append(plugin.vault.format(price)).color(ChatColor.GREEN);
        cb.append("?").color(ChatColor.WHITE);
        player.spigot().sendMessage(cb.create());
        String code = randomString();
        confirm(player, code, () -> confirmBuy(player, price, size));
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
        player.sendMessage(ChatColor.RED + "Cancelled.");
    }

    void commandUsage(Player player, String sub, String args,
                      String description, String suggestion) {
        String cmd;
        if (args == null) {
            cmd = "/world " + sub;
        } else {
            cmd = "/world " + sub + " &o" + args;
        }
        String tooltip = "&a" + cmd + "\n" + description;
        Msg.raw(player,
                Msg.button(ChatColor.YELLOW,
                           cmd,
                           tooltip,
                           suggestion),
                Msg.format("&8 - &7"),
                Msg.button(ChatColor.GRAY,
                           description,
                           tooltip,
                           suggestion));
    }

    void usage(Player player, String cmd) {
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
            commandUsage(player, "buy", null, "Buy a world.", "/world buy");
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
        default:
            player.sendMessage(ChatColor.RED + "Unknown command: " + cmd);
        }
    }

    void usage(Player player) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        boolean owner = buildWorld != null && buildWorld.getTrust(player.getUniqueId()).isOwner();
        Msg.info(player, "&lWorld&3 Command Usage");
        usage(player, "list");
        usage(player, "visit");
        usage(player, "info");
        usage(player, "buy");
        usage(player, "spawn");
        if (owner) {
            usage(player, "rename");
            usage(player, "setspawn");
            usage(player, "time");
            usage(player, "difficulty");
            usage(player, "gamemode");
            usage(player, "trust");
            if (buildWorld.isWorldEdit()) {
                usage(player, "wetrust");
            }
            usage(player, "visittrust");
            usage(player, "ownertrust");
            usage(player, "untrust");
            usage(player, "save");
            usage(player, "set");
        }
    }

    void changeWorldSetting(Player player, BuildWorld buildWorld, String key, List<String> args) {
        if (key.equals("name")) {
            String name = Msg.fold(args, " ");
            buildWorld.getWorldConfig().set("user.Name", name);
            buildWorld.setName(name);
            buildWorld.saveWorldConfig();
            plugin.saveBuildWorlds();
            Msg.info(player, "Set world name to '%s'.", Msg.fold(args, " "));
        } else if (key.equals("description")) {
            buildWorld.getWorldConfig().set("user.Description", Msg.fold(args, " "));
            buildWorld.saveWorldConfig();
            Msg.info(player, "Set world description to '%s'.", Msg.fold(args, " "));
        } else if (key.equals("authors")) {
            buildWorld.getWorldConfig().set("user.Authors", args);
            buildWorld.saveWorldConfig();
            Msg.info(player, "Set world authors to %s.", Msg.fold(args, ", "));
        }
    }
}
