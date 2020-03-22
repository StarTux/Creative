package com.winthier.creative;

import com.winthier.playercache.PlayerCache;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Value;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

@Value
final class Builder {
    private UUID uuid;

    String getName() {
        return PlayerCache.nameForUuid(uuid);
    }

    private Builder(final UUID uuid, final String name) {
        this.uuid = uuid;
    }

    public static Builder of(UUID uuid, String name) {
        return new Builder(uuid, name);
    }

    public static Builder of(Player player) {
        return of(player.getUniqueId(), player.getName());
    }

    public static Builder find(String name) {
        UUID uuid = PlayerCache.uuidForName(name);
        if (uuid == null) return null;
        return of(uuid, name);
    }

    Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("uuid", uuid.toString());
        result.put("name", getName());
        return result;
    }

    public static Builder deserialize(ConfigurationSection config) {
        if (config == null) return null;
        UUID uuid = UUID.fromString(config.getString("uuid"));
        String name = config.getString("name");
        return of(uuid, name);
    }
}
