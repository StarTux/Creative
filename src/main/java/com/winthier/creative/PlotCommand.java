package com.winthier.creative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class PlotCommand implements TabExecutor {
    final CreativePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 0) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player required");
            return true;
        }
        Player player = (Player) sender;
        return onCommand(player, args[0], Arrays.copyOfRange(args, 1, args.length));
    }

    boolean onCommand(Player player, String cmd, String[] args) {
        switch (cmd) {
        case "list": {
            if (args.length != 0) return false;
            List<PlotWorld.Plot> list = listPlots(player);
            if (list.isEmpty()) {
                player.sendMessage(ChatColor.RED + "You don't have any plots");
                return true;
            }
            int i = 0;
            int size = list.size();
            player.sendMessage(ChatColor.AQUA + "You have " + size
                               + (size == 1 ? " plot:" : " plots:"));
            for (PlotWorld.Plot plot : list) {
                int index = i++;
                ComponentBuilder cb = new ComponentBuilder();
                cb.append("" + index + ") ").color(ChatColor.AQUA);
                String scmd = "/plot warp " + index;
                cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, scmd));
                cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Msg.lore(scmd)));
                cb.append(plot.plotWorld.name).color(ChatColor.WHITE);
                cb.append(" " + plot.x + "," + plot.z).color(ChatColor.GRAY);
                player.spigot().sendMessage(cb.create());
            }
            return true;
        }
        case "warp": {
            if (args.length != 1) return false;
            int index;
            try {
                index = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                player.sendMessage(ChatColor.RED + "Bad index: " + args[0]);
                return true;
            }
            List<PlotWorld.Plot> list = listPlots(player);
            if (index < 0 || index >= list.size()) {
                player.sendMessage(ChatColor.RED + "Bad index: " + index);
                return true;
            }
            PlotWorld.Plot plot = list.get(index);
            BuildWorld buildWorld = plugin.getBuildWorldByPath(plot.plotWorld.name);
            if (buildWorld == null) {
                player.sendMessage(ChatColor.RED
                                   + "An error occured. Please contact an administrator.");
                return true;
            }
            Msg.info(player, "Please wait");
            World world = buildWorld.loadWorld();
            plot.getCornerBlock(world, b -> {
                    player.teleport(b.getLocation().add(0.5, 1.5, 0.5));
                    player.sendMessage(ChatColor.AQUA + "Warped to plot.");
                });
            return true;
        }
        default:
            return false;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (args.length == 0) return null;
        if (args.length == 1) {
            return Stream.of("list", "warp").filter(s -> s.contains(args[0]))
                .collect(Collectors.toList());
        }
        return null;
    }

    List<PlotWorld.Plot> listPlots(Player player) {
        List<PlotWorld.Plot> list = new ArrayList<>();
        for (PlotWorld plotWorld : plugin.plotWorlds.values()) {
            for (PlotWorld.Plot plot : plotWorld.plots) {
                if (plot.isOwner(player)) list.add(plot);
            }
        }
        return list;
    }
}
