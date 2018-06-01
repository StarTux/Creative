package com.winthier.creative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class WorldCommand implements TabExecutor {
    final CreativePlugin plugin;

    static final class CommandException extends RuntimeException {
        @Getter final String message;
        private boolean usage = false;
        CommandException(String msg, Object... o) {
            if (o.length > 0) msg = String.format(msg, o);
            this.message = msg;
        }
        CommandException(boolean usage) {
            this.message = null;
            this.usage = usage;
        }
        static void noPerm() {
            throw new CommandException("You don't have permission.");
        }
        static void usage() {
            throw new CommandException(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return false;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        try {
            if (cmd == null) {
                if (label.equalsIgnoreCase("worlds")) {
                    listWorlds(player);
                } else {
                    usage(player);
                }
            } else if (cmd.equals("tp")) {
                if (args.length != 2) CommandException.usage();
                String worldName = args[1];
                worldTeleport(player, worldName);
            } else if (cmd.equals("ls") || cmd.equals("list")) {
                if (args.length != 1) CommandException.usage();
                listWorlds(player);
            } else if (cmd.equals("info")) {
                if (args.length != 1) CommandException.usage();
                worldInfo(player);
            } else if (cmd.equals("time")) {
                String arg = args.length >= 2 ? args[1] : null;
                worldTime(player, arg);
            } else if (cmd.equals("difficulty")) {
                World world = player.getWorld();
                BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
                if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
                    CommandException.noPerm();
                }
                if (args.length < 2) {
                    Difficulty difficulty = player.getWorld().getDifficulty();
                    Msg.info(player, "World difficulty is %s.", Msg.camelCase(difficulty.name()));
                } else if (args.length == 2) {
                    Difficulty difficulty;
                    try {
                        difficulty = Difficulty.valueOf(args[1]);
                    } catch (IllegalArgumentException iae) {
                        throw new CommandException("Unknown difficulty: %s", args[1]);
                    }
                    world.setDifficulty(difficulty);
                    Msg.info(player, "World difficulty set to %s.", Msg.camelCase(difficulty.name()));
                } else {
                    CommandException.usage();
                }
            } else if (cmd.equals("spawn")) {
                teleportToSpawn(player);
            } else if (cmd.equals("setspawn")) {
                setWorldSpawn(player);
            } else if (cmd.equals("trust")) {
                if (args.length != 2) CommandException.usage();
                trustCommand(player, args[1], Trust.BUILD);
            } else if (cmd.equals("wetrust")) {
                if (args.length != 2) CommandException.usage();
                trustCommand(player, args[1], Trust.WORLD_EDIT);
            } else if (cmd.equals("visittrust")) {
                if (args.length != 2) CommandException.usage();
                trustCommand(player, args[1], Trust.VISIT);
            } else if (cmd.equals("ownertrust")) {
                if (args.length != 2) CommandException.usage();
                trustCommand(player, args[1], Trust.OWNER);
            } else if (cmd.equals("untrust")) {
                if (args.length != 2) CommandException.usage();
                trustCommand(player, args[1], Trust.NONE);
            } else if (cmd.equals("save")) {
                World world = player.getWorld();
                BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
                if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
                    CommandException.noPerm();
                }
                world.save();
                Msg.info(player, "Saved your current world to disk.");
            } else if (cmd.equals("rename")) {
                if (args.length < 2) CommandException.usage();
                World world = player.getWorld();
                BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
                if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
                    CommandException.noPerm();
                }
                StringBuilder sb = new StringBuilder(args[1]);
                for (int i = 2; i < args.length; ++i) {
                    sb.append(" ").append(args[i]);
                }
                String name = sb.toString();
                buildWorld.setName(name);
                plugin.saveBuildWorlds();
                Msg.info(player, "Renamed your current world to '%s'.", name);
            } else if ((cmd.equals("gamemode") || cmd.equals("gm")) && args.length == 2) {
                GameMode newGM;
                switch (args[1].toLowerCase()) {
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
                    Msg.warn(player, "Invalid GameMode: %s", args[1]);
                } else {
                    player.setGameMode(newGM);
                    Msg.info(player, "Set GameMode to %s", newGM.name());
                }
            } else if (cmd.equals("set")) {
                if (args.length < 3) CommandException.usage();
                World world = player.getWorld();
                BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
                if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
                    CommandException.noPerm();
                }
                changeWorldSetting(player, buildWorld, args[1].toLowerCase(), Arrays.asList(Arrays.copyOfRange(args, 2, args.length)));
            } else {
                CommandException.usage();
            }
        } catch (CommandException ce) {
            if (ce.usage) {
                usage(player);
            } else {
                Msg.warn(player, "%s", ce.getMessage());
            }
        }
        return true;
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
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return null;
        if (args.length == 0) {
            return null;
        } else if (args.length == 1) {
            return filterStartsWith(args[0], Arrays.asList("list", "info", "time", "spawn", "setspawn", "difficulty", "trust", "untrust"));
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
        buildWorld.loadWorld();
        buildWorld.teleportToSpawn(player);
        Msg.info(player, "Teleported to %s.", buildWorld.getName());
        return true;
    }

    PlayerWorldList listWorlds(Player player) {
        PlayerWorldList list = plugin.getPlayerWorldList(player.getUniqueId());
        Msg.info(player, "World List");
        if (!list.owner.isEmpty()) {
            listWorlds(player, list.owner, "Worlds you own");
        }
        if (!list.build.isEmpty()) {
            listWorlds(player, list.build, "Worlds you can build in");
        }
        if (!list.visit.isEmpty()) {
            listWorlds(player, list.visit, "Worlds you can visit");
        }
        Msg.send(player, "Total %d worlds", list.count());
        return list;
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

    void worldTime(Player player, String arg) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner()) CommandException.noPerm();
        if (arg == null) {
            long time = player.getWorld().getTime();
            Msg.send(player, "World time &a%02d&r:&a%02d&r (&2%d&r)", hours(time), minutes(time), time);
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
                if (arr.length != 2) throw new CommandException("Time expected: %s", arg);
                long hours, minutes;
                try {
                    hours = Long.parseLong(arr[0]);
                    minutes = Long.parseLong(arr[1]);
                } catch (NumberFormatException nfe) {
                    throw new CommandException("Time expected: %s", arg);
                }
                time = raw(hours, minutes);
            } else {
                try {
                    time = Long.parseLong(arg);
                } catch (NumberFormatException nfe) {
                    throw new CommandException("Time expected: %s", arg);
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

    void teleportToSpawn(Player player) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null) {
            CommandException.noPerm();
        } else if (!buildWorld.getTrust(uuid).canVisit()) {
            CommandException.noPerm();
        } else {
            buildWorld.teleportToSpawn(player);
        }
        Msg.info(player, "Teleported to the world spawn.");
    }

    void setWorldSpawn(Player player) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        UUID uuid = player.getUniqueId();
        if (buildWorld == null || !buildWorld.getTrust(uuid).isOwner()) CommandException.noPerm();
        buildWorld.setSpawnLocation(player.getLocation());
        buildWorld.saveWorldConfig();
        Msg.info(player, "World spawn was set to your current location.");
    }

    void trustCommand(Player player, String target, Trust trust) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null || !buildWorld.getTrust(player.getUniqueId()).isOwner()) {
            CommandException.noPerm();
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
            if (builder == null) throw new CommandException("Player not found: %s.", target);
            if (!buildWorld.trustBuilder(builder, trust)) {
                throw new CommandException("Could not change trust level of %s.", builder.getName());
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

    void listTrusted(Player player, BuildWorld buildWorld, Trust trust) {
        List<String> names = new ArrayList<>();
        for (Builder builder: buildWorld.listTrusted(trust)) names.add(builder.getName());
        if (trust == Trust.OWNER && buildWorld.getOwner() != null) names.add(buildWorld.getOwnerName());
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

    void worldInfo(Player player) {
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null) CommandException.noPerm();
        Trust playerTrust = buildWorld.getTrust(player.getUniqueId());
        if (!playerTrust.canVisit()) CommandException.noPerm();
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

    void commandUsage(Player player, String sub, String args, String description, String suggestion) {
        String cmd;
        if (args == null) {
            cmd = "/World " + sub;
        } else {
            cmd = "/World " + sub + " &o" + args;
        }
        String tooltip = "&a" + cmd + "\n" + description;
        Msg.raw(player,
                Msg.button(ChatColor.WHITE,
                           cmd,
                           tooltip,
                           suggestion),
                Msg.format("&8 - &7"),
                Msg.button(ChatColor.GRAY,
                           description,
                           tooltip,
                           suggestion));
    }

    void usage(Player player) {
        Msg.info(player, "&lWorld&3 Command Usage");
        commandUsage(player, "List", null, "List your worlds", "/world list");
        commandUsage(player, "Info", null, "Get world info", "/world info");
        commandUsage(player, "Spawn", null, "Warp to world spawn", "/world spawn");
        commandUsage(player, "SetSpawn", null, "Set world spawn", "/world setspawn ");
        commandUsage(player, "Time", "[Time|Lock|Unlock]", "Get or set world time", "/world time");
        commandUsage(player, "Difficulty", "Easy|Normal|Hard|Peaceful", "Get or set world time", "/world difficulty ");
        commandUsage(player, "GameMode|GM", "<Mode>", "Change your GameMode", "/world gamemode ");
        commandUsage(player, "Trust", "<Player>", "Trust someone to build", "/world trust ");
        commandUsage(player, "WETrust", "<Player>", "Give someone WorldEdit trust", "/world wetrust ");
        commandUsage(player, "VisitTrust", "<Player>", "Trust someone to visit", "/world visittrust ");
        commandUsage(player, "OwnerTrust", "<Player>", "Add a world owner", "/world ownertrust ");
        commandUsage(player, "Untrust", "<Player>", "Revoke trust", "/world untrust ");
        commandUsage(player, "Save", null, "Save your world to disk", "/world save");
        commandUsage(player, "Set", "<Name|Description|Authors> [...]", "World settings", "/world set ");
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
