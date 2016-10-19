package com.winthier.creative;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;

@RequiredArgsConstructor
public class Permission {
    final CreativePlugin plugin;

    void updatePermissions(Player player) {
        resetPermissions(player);
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null);
        Trust trust = buildWorld.getTrust(player.getUniqueId());
        if (trust.canUseWorldEdit() && player.hasPermission("creative.worldedit")) {
            givePermission(player, "worldedit.*");
        }
    }

    void updatePermissions(World world) {
        for (Player player: world.getPlayers()) {
            updatePermissions(player);
        }
    }

    void givePermission(Player player, String perm) {
        player.addAttachment(plugin, perm, true);
    }

    void resetPermissions(Player player) {
        List<PermissionAttachment> list = new ArrayList<>();
        for (PermissionAttachmentInfo info: player.getEffectivePermissions()) {
            PermissionAttachment attach = info.getAttachment();
            if (attach != null && attach.getPlugin() == plugin) {
                list.add(info.getAttachment());
            }
        }
        for (PermissionAttachment attach: list) {
            player.removeAttachment(attach);
        }
    }
}
