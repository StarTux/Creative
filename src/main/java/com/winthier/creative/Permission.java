package com.winthier.creative;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;

@RequiredArgsConstructor
final class Permission {
    private final CreativePlugin plugin;
    private YamlConfiguration permissionsFile = null;

    void updatePermissions(Player player) {
        resetPermissions(player);
        BuildWorld buildWorld = plugin.getBuildWorldByWorld(player.getWorld());
        if (buildWorld == null) return;
        Trust trust = buildWorld.getTrust(player.getUniqueId());
        if (trust.canBuild()) {
            for (String perm: getPermissionsFile().getStringList("Build")) {
                givePermission(player, perm);
            }
        }
        if (buildWorld.isSet(BuildWorld.Flag.WORLD_EDIT)
            && trust.canUseWorldEdit()
            && player.hasPermission("creative.worldedit")) {
            for (String perm: getPermissionsFile().getStringList("WorldEdit")) {
                givePermission(player, perm);
            }
            if (buildWorld.isSet(BuildWorld.Flag.VOXEL_SNIPER)) {
                for (String perm: getPermissionsFile().getStringList("VoxelSniper")) {
                    givePermission(player, perm);
                }
            }
        }
        if (player.hasPermission("creative.dangerous")) {
            for (String perm: getPermissionsFile().getStringList("Dangerous")) {
                givePermission(player, perm);
            }
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
        boolean found;
        do {
            found = false;
            List<PermissionAttachment> list = new ArrayList<>();
            for (PermissionAttachmentInfo info: player.getEffectivePermissions()) {
                PermissionAttachment attach = info.getAttachment();
                if (attach != null && attach.getPlugin() == plugin) {
                    list.add(info.getAttachment());
                    found = true;
                }
            }
            for (PermissionAttachment attach: list) {
                player.removeAttachment(attach);
            }
        } while (found);
    }

    YamlConfiguration getPermissionsFile() {
        if (permissionsFile == null) {
            File file = new File(plugin.getDataFolder(), "permissions.yml");
            permissionsFile = YamlConfiguration.loadConfiguration(file);
        }
        return permissionsFile;
    }

    void reload() {
        permissionsFile = null;
        for (Player player: plugin.getServer().getOnlinePlayers()) {
            updatePermissions(player);
        }
    }
}
