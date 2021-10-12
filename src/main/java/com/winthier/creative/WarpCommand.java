package com.winthier.creative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class WarpCommand implements TabExecutor {
    final CreativePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) return false;
        if (args.length == 0) {
            listWarps(player);
        } else {
            StringBuilder sb = new StringBuilder(args[0]);
            for (int i = 1; i < args.length; ++i) {
                sb.append(" ").append(args[i]);
            }
            String name = sb.toString();
            Warp warp = plugin.getWarps().get(name);
            if (warp == null
                || (warp.getPermission() != null
                    && !warp.getPermission().isEmpty()
                    && !player.hasPermission(warp.getPermission()))) {
                player.sendMessage(Component.text("Warp not found: " + name, NamedTextColor.RED));
                return true;
            }
            player.teleport(warp.getLocation());
            player.sendMessage(Component.text("Warped to " + warp.getName(), NamedTextColor.GREEN));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return null;
        String arg = args[args.length - 1].toLowerCase();
        return plugin.getWarps().keySet().stream()
            .filter(k -> k.toLowerCase().contains(arg))
            .collect(Collectors.toList());
    }

    private void listWarps(Player player) {
        Component prefix = Component.text("Warps ", NamedTextColor.GRAY);
        List<Warp> warps = new ArrayList<>(plugin.getWarps().values());
        Collections.sort(warps, Warp.NAME_SORT);
        List<Component> components = new ArrayList<>(warps.size());
        for (Warp warp: warps) {
            String cmd = "/warp " + warp.getName();
            components.add(Component.text("[" + warp.getDisplayName() + "]", NamedTextColor.GREEN)
                           .clickEvent(ClickEvent.runCommand(cmd))
                           .hoverEvent(HoverEvent.showText(Component.text(cmd, NamedTextColor.GREEN))));
        }
        player.sendMessage(Component.join(JoinConfiguration.builder()
                                          .prefix(prefix)
                                          .separator(Component.space())
                                          .build(),
                                          components));
    }
}
