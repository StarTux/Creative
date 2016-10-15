package com.winthier.creative;

import com.winthier.creative.util.Msg;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;
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
        } else if (cmd.equals("importall")) {
            int count = 0;
            for (World world: plugin.getServer().getWorlds()) {
                String name = world.getName();
                if (plugin.buildWorldByPath(name) != null) continue;
                BuildWorld buildWorld = new BuildWorld(name, name, null);
                plugin.getBuildWorlds().add(buildWorld);
                count += 1;
            }
            plugin.saveBuildWorlds();
            sender.sendMessage("" + count + " worlds imported.");
        } else if (cmd.equals("create")) {
            if (args.length != 2) return false;
            String name = args[1];
            BuildWorld existing = plugin.buildWorldByPath(name);
            if (existing != null) {
                sender.sendMessage("World already exists: " + name);
                return true;
            }
            BuildWorld buildWorld = new BuildWorld(name, name, null);
            plugin.getBuildWorlds().add(buildWorld);
            sender.sendMessage("World created: " + name);
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
            BuildWorld bw = plugin.buildWorldByPath(name);
            if (bw == null) {
                sender.sendMessage("World not found: " + name);
                return true;
            }
            bw.teleportToSpawn(player);
            sender.sendMessage("Teleported to world " + bw.getName());
        } else if (cmd.equals("remove")) {
            if (args.length != 2) return false;
            String worldKey = args[1];
            BuildWorld buildWorld = plugin.buildWorldByPath(worldKey);
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
            BuildWorld buildWorld = plugin.buildWorldByPath(worldKey);
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
            BuildWorld buildWorld = plugin.buildWorldByPath(worldKey);
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
        } else if (cmd.equals("count")) {
            int count = 0;
            int unowned = 0;
            for (BuildWorld buildWorld: plugin.getBuildWorlds()) {
                count += 1;
                if (buildWorld.getOwner() == null) unowned += 1;
            }
            sender.sendMessage("" + unowned + "/" + count + " worlds are unowned");
        }
        return true;
    }

    // void nextUnassignedWorld(Player player) {
    //     for (BuildWorld buildWorld: plugin.getBuildWorlds()) {
    //         if (buildWorld.getOwner() == null) {
    //             Msg.raw(player,
    //                     Msg.button("Build World \"" + buildWorld.getPath() + "\"",
    //                                "Assign owner for " + buildWorld.getName(),
    //                                "/creativeadmin setowner " + buildWorld.getPath() + " "));
    //             return;
    //         }
    //     }
    //     player.sendMessage("finis");
    // }
}
