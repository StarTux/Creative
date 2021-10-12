package com.winthier.creative;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class WTPCommand implements TabExecutor {
    final CreativePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) return false;
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) return false;
        try {
            plugin.getWorldCommand().worldTeleport(player, String.join(" ", args));
        } catch (WorldCommand.Wrong wrong) {
            sender.sendMessage(Component.text(wrong.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        if (args.length == 1) {
            return plugin.completeWorldNames((Player) sender, args[0]);
        }
        return List.of();
    }
}
