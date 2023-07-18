package com.winthier.creative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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

    private boolean onCommand(Player player, String cmd, String[] args) {
        switch (cmd) {
        case "list": {
            if (args.length != 0) return false;
            List<PlotWorld.Plot> list = listPlots(player);
            if (list.isEmpty()) {
                player.sendMessage(Component.text("You don't have any plots", NamedTextColor.RED));
                return true;
            }
            int i = 0;
            int size = list.size();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.text("You have " + (size == 1 ? " one plot" : size + " plots") + ":", NamedTextColor.GRAY));
            for (PlotWorld.Plot plot : list) {
                int index = i++;
                String scmd = "/plot warp " + index;
                lines.add(Component.text("" + index + ") ", NamedTextColor.AQUA)
                          .append(Component.text(plot.plotWorld.name, NamedTextColor.WHITE))
                          .append(Component.text(" " + plot.x + "," + plot.z, NamedTextColor.GRAY))
                          .clickEvent(ClickEvent.runCommand(scmd))
                          .hoverEvent(HoverEvent.showText(Component.text(scmd, NamedTextColor.GREEN))));
            }
            player.sendMessage(Component.join(JoinConfiguration.separator(Component.newline()), lines));
            return true;
        }
        case "warp": {
            if (args.length != 1) return false;
            int index;
            try {
                index = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                player.sendMessage(Component.text("Bad index: " + args[0], NamedTextColor.RED));
                return true;
            }
            List<PlotWorld.Plot> list = listPlots(player);
            if (index < 0 || index >= list.size()) {
                player.sendMessage(Component.text("Bad index: " + index, NamedTextColor.RED));
                return true;
            }
            PlotWorld.Plot plot = list.get(index);
            BuildWorld buildWorld = plugin.getBuildWorldByPath(plot.plotWorld.name);
            if (buildWorld == null) {
                player.sendMessage(Component.text("An error occured. Please contact an administrator",
                                                  NamedTextColor.RED));
                return true;
            }
            player.sendMessage(Component.text("Please wait", NamedTextColor.GREEN));
            World world = buildWorld.loadWorld();
            plot.getCornerBlock(world, b -> {
                    player.teleport(b.getLocation().add(0.5, 1.5, 0.5));
                    player.sendMessage(Component.text("Warped to plot", NamedTextColor.GREEN));
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

    private List<PlotWorld.Plot> listPlots(Player player) {
        List<PlotWorld.Plot> list = new ArrayList<>();
        for (PlotWorld plotWorld : plugin.getPlotWorlds().values()) {
            for (PlotWorld.Plot plot : plotWorld.plots) {
                if (plot.isOwner(player)) list.add(plot);
            }
        }
        return list;
    }
}
