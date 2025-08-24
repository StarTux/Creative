package com.winthier.creative.vote;

import com.winthier.creative.BuildWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import static com.winthier.creative.CreativePlugin.plugin;
import static com.winthier.creative.file.Files.deleteWorld;

@Value
public final class MapVoteResult {
    private final MapVote mapVote;
    private final BuildWorld buildWorldWinner;
    private final World localWorldCopy;
    private final List<UUID> playerIds;

    public boolean delete() {
        final boolean result = deleteWorld(localWorldCopy);
        if (!result) {
            plugin().getLogger().severe("[" + buildWorldWinner.getPath() + "] Failed to delete local world copy: " + localWorldCopy.getName());
        }
        return result;
    }

    public List<Player> getPlayers() {
        final List<Player> result = new ArrayList<>();
        for (UUID uuid : playerIds) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            result.add(player);
        }
        return result;
    }
}
