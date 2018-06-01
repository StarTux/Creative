package com.winthier.creative;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class WTPCommand implements CommandExecutor {
    final CreativePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) return false;
        Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return false;
        String cmd = args.length > 0 ? args[0] : null;
        try {
            plugin.getWorldCommand().worldTeleport(player, cmd);
        } catch (WorldCommand.CommandException ce) {
            Msg.warn(player, "%s", ce.getMessage());
        }
        return true;
    }
}
