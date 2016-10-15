package com.winthier.creative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

@Getter @Setter
public class BuildWorld {
    String name;
    String path; // Key
    Builder owner;
    final Map<UUID, Trusted> trusted = new HashMap<>();

    BuildWorld(String name, String path, Builder owner) {
        this.name = name;
        this.path = path;
        this.owner = owner;
    }

    List<Builder> listTrusted(Trust trust) {
        List<Builder> result = new ArrayList<>();
        for (Map.Entry<UUID, Trusted> e: this.trusted.entrySet()) {
            if (e.getValue().getTrust() == trust) {
                result.add(e.getValue().getBuilder());
            }
        }
        return result;
    }

    Trust getTrust(UUID uuid) {
        if (owner != null && owner.getUuid().equals(uuid)) return Trust.OWNER;
        Trusted t = trusted.get(uuid);
        if (t == null) return Trust.NONE;
        return t.getTrust();
    }

    String getOwnerName() {
        if (owner == null) {
            return "N/A";
        } else {
            return owner.getName();
        }
    }

    World getWorld() {
        return Bukkit.getServer().getWorld(path);
    }

    void teleportToSpawn(Player player) {
        World world = getWorld();
        player.teleport(world.getSpawnLocation());
    }

    // Serialization

    Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("path", path);
        if (owner != null) {
            result.put("owner", owner.serialize());
        }
        Map<String, Object> trustedMap = new HashMap<>();
        result.put("trusted", trustedMap);
        for (Map.Entry<UUID, Trusted> e: this.trusted.entrySet()) {
            trustedMap.put(e.getKey().toString(), e.getValue().serialize());
        }
        return result;
    }

    public static BuildWorld deserialize(ConfigurationSection config) {
        String name = config.getString("name");
        String path = config.getString("path");
        Builder owner = Builder.deserialize(config.getConfigurationSection("owner"));
        BuildWorld result = new BuildWorld(name, path, owner);
        ConfigurationSection trustedSection = config.getConfigurationSection("trusted");
        if (trustedSection != null) {
            for (String key: trustedSection.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                Trusted trusted = Trusted.deserialize(trustedSection.getConfigurationSection(key));
                result.trusted.put(uuid, trusted);
            }
        }
        return result;
    }
}
