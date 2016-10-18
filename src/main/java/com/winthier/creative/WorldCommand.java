package com.winthier.creative;

import com.winthier.creative.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class WorldCommand implements TabExecutor {
    final CreativePlugin plugin;

    static class CommandException extends RuntimeException {
        @Getter final String message;
        CommandException(String msg, Object... o) {
            if (o.length > 0) msg = String.format(msg, o);
            this.message = msg;
        }
        static void noPerm() {
            throw new CommandException("You don't have permission.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return false;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        try {
            if (cmd == null) {
                return false;
            } else if (cmd.equals("tp")) {
                if (args.length != 2) return false;
                String worldName = args[1];
                worldTeleport(player, worldName);
            } else if (cmd.equals("ls") || cmd.equals("list")) {
                if (args.length != 1) return false;
                listWorlds(player);
            } else if (cmd.equals("time")) {
                String arg = args.length >= 2 ? args[1] : null;
                worldTime(player, arg);
            } else if (cmd.equals("spawn")) {
                teleportToSpawn(player);
            } else if (cmd.equals("setspawn")) {
                setWorldSpawn(player);
            } else if (cmd.equals("trust")) {
                if (args.length < 2 || args.length > 3) return false;
                String target = args[1];
                Trust trust;
                if (args.length >= 3) {
                    trust = Trust.of(args[2]);
                } else {
                    trust = Trust.VISIT;
                }
                if (trust == Trust.NONE) return false;
                trust(player, target, trust);
            } else if (cmd.equals("untrust")) {
                if (args.length < 2) return false;
                String target = args[1];
                trust(player, target, Trust.NONE);
            } else {
                return false;
            }
        } catch (CommandException ce) {
            Msg.warn(player, "%s", ce.getMessage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return null;
        if (args.length == 0) {
            return null;
        } else if (args.length == 1) {
            return Arrays.asList("tp", "list", "time", "spawn", "setspawn", "trust", "untrust");
        } else {
            String cmd = args[0].toLowerCase();
            if (cmd.equals("trust")) {
                if (args.length == 3) {
                    List<String> result = new ArrayList<>();
                    for (Trust trust: Trust.values()) {
                        if (trust != Trust.NONE) {
                            result.add(trust.name().toLowerCase());
                        }
                    }
                    return result;
                }
            }
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
        for (BuildWorld buildWorld: list) {
            json.add(" ");
            json.add(Msg.button(ChatColor.GREEN,
                                "&f[&a" + buildWorld.getName() + "&r]",
                                "Teleport to " + buildWorld.getName(),
                                "/wtp " + buildWorld.getPath()));
        }
        Msg.raw(player, json);
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

    void trust(Player player, String target, Trust trust) {
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
            if (buildWorld.getTrust(builder.getUuid()) == Trust.NONE) {
                throw new CommandException("%s is not trusted in this world.", builder.getName());
            }
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
    }
}
