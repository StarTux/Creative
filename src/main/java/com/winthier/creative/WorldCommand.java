package com.winthier.creative;

import com.winthier.creative.util.Msg;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class WorldCommand implements CommandExecutor {
    final CreativePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return false;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            return false;
        } else if (cmd.equals("tp")) {
            if (args.length != 2) return false;
            String worldName = args[1];
            worldTeleport(player, worldName);
        } else if (cmd.equals("ls") || cmd.equals("list")) {
            if (args.length != 1) return false;
            listWorlds(player);
        } else {
            return false;
        }
        return true;
    }

    boolean worldTeleport(Player player, String worldName) {
        BuildWorld buildWorld = plugin.buildWorldByPath(worldName);
        if (buildWorld == null) {
            Msg.warn(player, "World not found: %s", worldName);
            return false;
        }
        Trust trust = buildWorld.getTrust(player.getUniqueId());
        if (trust == null || trust == Trust.NONE) {
            Msg.warn(player, "World not found: %s", worldName);
            return false;
        }
        buildWorld.teleportToSpawn(player);
        Msg.info(player, "Teleported to %s.", buildWorld.getName());
        return true;
    }

    PlayerWorldList listWorlds(Player player) {
        PlayerWorldList list = plugin.getPlayerWorldList(player.getUniqueId());
        Msg.info(player, "World List");
        if (!list.owner.isEmpty()) {
            Msg.send(player, "World you own");
            listWorlds(player, list.owner);
        }
        if (!list.build.isEmpty()) {
            Msg.send(player, "World you can build in");
            listWorlds(player, list.build);
        }
        if (!list.visit.isEmpty()) {
            Msg.send(player, "World you can visit");
            listWorlds(player, list.visit);
        }
        Msg.send(player, "Total %d worlds", list.count());
        return list;
    }

    private void listWorlds(Player player, List<BuildWorld> list) {
        for (BuildWorld buildWorld: list) {
            Msg.raw(player,
                    Msg.button(ChatColor.BLUE,
                               " " + buildWorld.getName(),
                               "Teleport to " + buildWorld.getName(),
                               "/wtp " + buildWorld.getPath()));
        }
    }
}
