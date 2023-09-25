package com.winthier.creative.vote;

import com.winthier.creative.BuildWorld;
import lombok.Value;
import org.bukkit.World;
import static com.winthier.creative.CreativePlugin.plugin;
import static com.winthier.creative.file.Files.deleteWorld;

@Value
public final class MapVoteResult {
    private final MapVote mapVote;
    private final BuildWorld buildWorldWinner;
    private final World localWorldCopy;

    public boolean delete() {
        final boolean result = deleteWorld(localWorldCopy);
        if (!result) {
            plugin().getLogger().severe("[" + buildWorldWinner.getPath() + "] Failed to delete local world copy: " + localWorldCopy.getName());
        }
        return result;
    }
}
