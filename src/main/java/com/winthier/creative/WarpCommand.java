package com.winthier.creative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
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
                Msg.warn(player, "Warp not found: %s", name);
                return true;
            }
            player.teleport(warp.getLocation());
            Msg.info(player, "Warped to %s", warp.getName());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return null;
        String arg = args[args.length - 1].toLowerCase();
        return plugin.getWarps().keySet().stream()
            .filter(k -> k.toLowerCase().startsWith(arg))
            .collect(Collectors.toList());
    }

    private void listWarps(Player player) {
        List<Object> json = new ArrayList<>();
        json.add(Msg.button(ChatColor.WHITE, "Warps", null, null));
        int count = 1;
        List<Warp> warps = new ArrayList<>(plugin.getWarps().values());
        Collections.sort(warps, Warp.NAME_SORT);
        for (Warp warp: warps) {
            json.add(" ");
            json.add(Msg.button(ChatColor.GREEN,
                                "&f[&a" + warp.getDisplayName() + "&f]",
                                "Warp to " + warp.getDisplayName(),
                                "/warp " + warp.getName()));
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
}
