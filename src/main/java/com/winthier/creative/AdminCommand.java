package com.winthier.creative;

import com.winthier.creative.util.Msg;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
        } else if (cmd.equals("listworlds")) {
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
                Msg.send(sender, "Owner (%d)", list.owner.size());
                for (BuildWorld bw: list.owner) Msg.send(sender, "%s (%s)", bw.getName(), bw.getPath());
                Msg.send(sender, "Build (%d)", list.build.size());
                for (BuildWorld bw: list.build) Msg.send(sender, "%s (%s)", bw.getName(), bw.getPath());
                Msg.send(sender, "Visit (%d)", list.visit.size());
                for (BuildWorld bw: list.visit) Msg.send(sender, "%s (%s)", bw.getName(), bw.getPath());
                Msg.send(sender, "total (%d)", list.count());
            }
        } else if (cmd.equals("tp")) {
            if (args.length != 2) return false;
            if (player == null) return false;
            String name = args[1];
            BuildWorld bw = plugin.getBuildWorldByPath(name);
            if (bw == null) {
                sender.sendMessage("World not found: " + name);
                return true;
            }
            bw.teleportToSpawn(player);
            sender.sendMessage("Teleported to world " + bw.getName());
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
            buildWorld.trusted.put(builder.getUuid(), new Trusted(builder, trust));
            plugin.saveBuildWorlds();
            sender.sendMessage("Given " + trust.name() + " to " + builder.getName() + " in " + buildWorld.getPath());
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
