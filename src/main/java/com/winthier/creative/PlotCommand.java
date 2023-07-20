package com.winthier.creative;

import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import static com.winthier.creative.CreativePlugin.plugin;

@RequiredArgsConstructor
public final class PlotCommand {
    private final CommandNode rootNode;

    protected void onEnable() {
        rootNode.description("Plot commands");
        rootNode.addChild("list")
            .description("List your plots")
            .playerCaller(this::list);
    }

    private void list(Player player) {
        List<PlotWorld.Plot> list = listPlots(player);
        if (list.isEmpty()) {
            throw new CommandWarn("You don't have any plots");
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
    }

    private boolean warp(Player player, String[] args) {
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
        BuildWorld buildWorld = plugin().getBuildWorldByPath(plot.plotWorld.name);
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

    private List<PlotWorld.Plot> listPlots(Player player) {
        List<PlotWorld.Plot> list = new ArrayList<>();
        for (PlotWorld plotWorld : plugin().getPlotWorlds().values()) {
            for (PlotWorld.Plot plot : plotWorld.plots) {
                if (plot.isOwner(player)) list.add(plot);
            }
        }
        return list;
    }
}
