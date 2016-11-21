package com.winthier.creative;

import com.winthier.creative.util.Msg;
import java.util.List;
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

@RequiredArgsConstructor
public class AdminCommand implements CommandExecutor {
    final CreativePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Player player = sender instanceof Player ? (Player)sender : null;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            return false;
        } else if (cmd.equals("reload")) {
            plugin.reloadAllConfigs();
            sender.sendMessage("Configs reloaded");
        } else if (cmd.equals("info")) {
            if (args.length != 2) return false;
            String name = args[1];
            BuildWorld buildWorld = plugin.getBuildWorldByPath(name);
            if (buildWorld == null) {
                sender.sendMessage("World not found: " + name);
                return true;
            }
            sender.sendMessage("World Name: " + buildWorld.getName());
            sender.sendMessage("World Path: " + buildWorld.getPath());
            sender.sendMessage("Owner: " + buildWorld.getOwnerName());
            for (Trust trust: Trust.values()) {
                StringBuilder sb = new StringBuilder(trust.name());
                List<Builder> trusted = buildWorld.listTrusted(trust);
                if (trusted.isEmpty()) continue;
                sb.append("(").append(trusted.size()).append(")");
                for (Builder builder: trusted) {
                    sb.append(" ").append(builder.getName());
                }
                sender.sendMessage(sb.toString());
            }
            sender.sendMessage("Public Trust: " + buildWorld.getPublicTrust());
        } else if (cmd.equals("config")) {
            if (args.length != 2) return false;
            String name = args[1];
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
        } else if (cmd.equals("listunregistered")) {
            sender.sendMessage("Unregistered worlds:");
            int count = 0;
            for (String dir: plugin.getServer().getWorldContainer().list()) {
                if (plugin.getBuildWorldByPath(dir) == null) {
                    sender.sendMessage(" " + dir);
                    count += 1;
                }
            }
            sender.sendMessage("" + count + " worlds listed.");
        } else if (cmd.equals("list")) {
            if (args.length == 1) {
                int count = 0;
                for (BuildWorld buildWorld: plugin.getBuildWorlds()) {
                    sender.sendMessage(buildWorld.getName() + " /" + buildWorld.getPath() + " " + buildWorld.getOwnerName());
                    count += 1;
                }
                sender.sendMessage("" + count + " build worlds listed");
            } else if (args.length == 2) {
                String name = args[1];
                Builder builder = Builder.find(name);
                if (builder == null) {
                    sender.sendMessage("Builder not found: " + name);
                    return true;
                }
                PlayerWorldList list = plugin.getPlayerWorldList(builder.getUuid());
                Msg.send(sender, "&e%s World List", builder.getName());
                if (!list.owner.isEmpty()) {
                    Msg.send(sender, "&7Owner (&r%d&7)", list.owner.size());
                    for (BuildWorld bw: list.owner) Msg.send(sender, "&a%s &8/%s", bw.getPath(), bw.getName());
                }
                if (!list.build.isEmpty()) {
                    Msg.send(sender, "&7Build (&r%d&7)", list.build.size());
                    for (BuildWorld bw: list.build) Msg.send(sender, "&a%s &8/%s", bw.getPath(), bw.getName());
                }
                if (!list.visit.isEmpty()) {
                    Msg.send(sender, "&7Visit (&r%d&7)", list.visit.size());
                    for (BuildWorld bw: list.visit) Msg.send(sender, "&a%s &8/%s", bw.getPath(), bw.getName());
                }
                Msg.send(sender, "&7Total (&r%d&7)", list.count());
            }
        } else if (cmd.equals("who")) {
            Msg.send(sender, "&eWorld Player List");
            for (World world: plugin.getServer().getWorlds()) {
                List<Player> players = world.getPlayers();
                if (players.isEmpty()) continue;
                StringBuilder sb = new StringBuilder(Msg.format("&7%s &8(&r%d&8)&r", world.getName(), players.size()));
                for (Player p: players) {
                    sb.append(" ").append(p.getName());
                }
                sender.sendMessage(sb.toString());
            }
        } else if (cmd.equals("listloaded")) {
            int count = 0;
            for (World world: plugin.getServer().getWorlds()) {
                if (plugin.getBuildWorldByWorld(world) == null) {
                    sender.sendMessage(ChatColor.RED + world.getName() + ChatColor.RESET + " (unregistered");
                } else {
                    sender.sendMessage(ChatColor.GREEN + world.getName() + ChatColor.RESET + " (registered)");
                }
                count += 1;
            }
            sender.sendMessage("" + count + " worlds are currently loaded.");
        } else if (cmd.equals("tp")) {
            String worldName;
            Player target;
            if (args.length == 2) {
                if (player == null) return false;
                target = player;
                worldName = args[1];
            } else if (args.length == 3) {
                String targetName = args[1];
                target = plugin.getServer().getPlayerExact(targetName);
                if (target == null) {
                    sender.sendMessage("Player not found: " + targetName);
                    return true;
                }
                worldName = args[2];
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
        } else if (cmd.equals("nosave")) {
            if (player == null) return false;
            World world = player.getWorld();
            boolean old = world.isAutoSave();
            world.setAutoSave(!old);
            player.sendMessage("New value: " + !old);
        } else if (cmd.equals("debug")) {
            if (player == null) return false;
            World world = player.getWorld();
            sender.sendMessage("World " + world.getName());
            sender.sendMessage("PvP " + world.getPVP());
            sender.sendMessage("Animals " + world.getAllowAnimals() + " " + world.getTicksPerAnimalSpawns());
            sender.sendMessage("Monsters " + world.getAllowMonsters() + " " + world.getTicksPerMonsterSpawns());
            sender.sendMessage("Difficulty " + world.getDifficulty());
            sender.sendMessage("AutoSave " + world.isAutoSave());
        } else if (cmd.equals("remove")) {
            if (args.length != 2) return false;
            String worldKey = args[1];
            BuildWorld buildWorld = plugin.getBuildWorldByPath(worldKey);
            if (buildWorld == null) {
                sender.sendMessage("World not found: " + worldKey);
                return true;
            }
            plugin.getBuildWorlds().remove(buildWorld);
            plugin.saveBuildWorlds();
            sender.sendMessage("World removed: " + buildWorld.getPath());
        } else if (cmd.equals("trust")) {
            if (args.length != 4) return false;
            String worldKey = args[1];
            String builderName = args[2];
            String trustArg = args[3];
            BuildWorld buildWorld = plugin.getBuildWorldByPath(worldKey);
            if (buildWorld == null) {
                sender.sendMessage("World not found: " + worldKey);
                return true;
            }
            Builder builder = Builder.find(builderName);
            if (builder == null) {
                sender.sendMessage("Builder not found: " + builderName);
                return true;
            }
            Trust trust = Trust.of(trustArg);
            if (trust == null) {
                sender.sendMessage("Bad trust arg: " + trustArg);
                return true;
            }
            if (trust == Trust.NONE) {
                buildWorld.trusted.remove(builder.getUuid());
            } else {
                buildWorld.trusted.put(builder.getUuid(), new Trusted(builder, trust));
            }
            plugin.saveBuildWorlds();
            sender.sendMessage("Given " + trust.name() + " to " + builder.getName() + " in " + buildWorld.getPath());
            World world = buildWorld.getWorld();
            if (world != null) plugin.permission.updatePermissions(world);
        } else if (cmd.equals("resetowner")) {
            if (args.length != 2) return false;
            String worldKey = args[1];
            BuildWorld buildWorld = plugin.getBuildWorldByPath(worldKey);
            if (buildWorld == null) {
                sender.sendMessage("World not found: " + worldKey);
                return true;
            }
            buildWorld.setOwner(null);
            plugin.saveBuildWorlds();
            sender.sendMessage("Removed owner of world " + buildWorld.getPath());
        } else if (cmd.equals("setowner")) {
            if (args.length != 3) return false;
            String worldKey = args[1];
            String ownerName = args[2];
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
            sender.sendMessage("Made " + owner.getName() + " the owner of world " + buildWorld.getPath());
        } else if (cmd.equals("create")) {
            createWorld(sender, args);
        } else if (cmd.equals("import")) {
            if (args.length != 3) return false;
            String name = args[1];
            String generator = args[2];
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
        } else if (cmd.equals("load")) {
            if (args.length != 2) return false;
            String name = args[1];
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
        } else if (cmd.equals("unload")) {
            if (args.length >= 4) return false;
            String name = args[1];
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
            if (args.length >= 3) {
                String shouldSaveArg = args[2].toLowerCase();
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
        } else if (cmd.equals("ignore")) {
            if (player == null) return false;
            if (plugin.toggleIgnore(player)) {
                Msg.info(player, "Ignoring world perms");
            } else {
                Msg.info(player, "No longer ignoring world perms");
            }
            plugin.getPermission().updatePermissions(player);
        } else if (cmd.equals("warp") && args.length > 1) {
            if (player == null) return false;
            StringBuilder sb = new StringBuilder(args[1]);
            for (int i = 2; i < args.length; ++i) {
                sb.append(" ").append(args[i]);
            }
            String name = sb.toString();
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
        } else if (cmd.equals("setwarp") && args.length > 1) {
            if (player == null) return false;
            StringBuilder sb = new StringBuilder(args[1]);
            for (int i = 2; i < args.length; ++i) {
                sb.append(" ").append(args[i]);
            }
            String name = sb.toString();
            Location loc = player.getLocation();
            Warp warp = Warp.of(name, loc);
            plugin.getWarps().put(name, warp);
            plugin.saveWarps();
            Msg.info(player, "Created warp '%s'", name);
        }
        return true;
    }

    void createWorld(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            sender.sendMessage("/CreativeAdmin create n:name p:path o:owner g:generator G:generatorSettings e:environment t:worldType s:seed S:generateStructures");
            return;
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
        for (int i = 1; i < args.length; ++i) {
            String arg = args[i];
            String[] tok = arg.split(":", 2);
            if (tok.length != 2 || tok[0].length() != 1) {
                sender.sendMessage("Bad arg: '" + arg + "'");
                return;
            }
            char param = tok[0].charAt(0);
            String value = tok[1];
            switch (param) {
            case 'o':
                owner = Builder.find(value);
                if (owner == null) {
                    sender.sendMessage("Builder not found: " + value);
                    return;
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
                    return;
                }
                break;
            case 'G':
                generatorSettings = value;
                break;
            case 's':
                try {
                    seed = Long.parseLong(value);
                } catch (NumberFormatException nfe) {
                    seed = (long)value.hashCode();
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
                    return;
                }
                break;
            case 'e':
                try {
                    environment = World.Environment.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException iae) {
                    sender.sendMessage("Unknown environment: '" + value + "'");
                    return;
                }
            }
        }
        if (path == null) path = name;
        if (name == null) name = path;
        if (path == null) {
            sender.sendMessage("Path missing!");
            return;
        }
        if (plugin.getBuildWorldByPath(path) != null) {
            sender.sendMessage("World already exists: '" + path + "'");
            return;
        }
        BuildWorld buildWorld = new BuildWorld(name, path, owner);
        plugin.getBuildWorlds().add(buildWorld);
        plugin.saveBuildWorlds();
        buildWorld.getWorldConfig().set("world.Generator", generator);
        buildWorld.getWorldConfig().set("world.GenerateStructures", generateStructures);
        buildWorld.getWorldConfig().set("world.GeneratorSettings", generatorSettings);
        buildWorld.getWorldConfig().set("world.Seed", seed);
        buildWorld.getWorldConfig().set("world.WorldType", worldType.name());
        buildWorld.getWorldConfig().set("world.Environment", environment.name());
        buildWorld.saveWorldConfig();
        sender.sendMessage("World '" + path + "' created.");
    }
}
