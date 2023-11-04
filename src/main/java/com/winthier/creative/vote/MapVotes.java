package com.winthier.creative.vote;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.winthier.creative.file.Files;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import static com.winthier.creative.CreativePlugin.plugin;

/**
 * Manager class for Map Voting.
 * Initialized in CreativePlugin.
 */
@Getter
public final class MapVotes implements Listener {
    private final Map<MinigameMatchType, MapVote> minigameVoteMap = new EnumMap<>(MinigameMatchType.class);
    private MapVoteCommand mapVoteCommand;

    public void enable() {
        mapVoteCommand = new MapVoteCommand(plugin());
        mapVoteCommand.enable();
        for (File file : Bukkit.getWorldContainer().listFiles()) {
            if (file.getName().startsWith("tmp_")) {
                Files.deleteFileStructure(file);
            }
        }
        Bukkit.getPluginManager().registerEvents(this, plugin());
    }

    public void disable() {
        for (MapVote it : minigameVoteMap.values()) {
            it.stopVote();
        }
        minigameVoteMap.clear();
    }

    public static MapVotes mapVotes() {
        return plugin().getMapVotes();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin(), () -> {
                for (MapVote mapVote : minigameVoteMap.values()) {
                    if (!mapVote.isVoteActive()) continue;
                    if (!player.getWorld().equals(mapVote.getLobbyWorld())) continue;
                    mapVote.remindToVote(player);
                }
            }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerHud(PlayerHudEvent event) {
        final Player player = event.getPlayer();
        for (MapVote mapVote : minigameVoteMap.values()) {
            if (!mapVote.isVoteActive()) continue;
            if (!player.getWorld().equals(mapVote.getLobbyWorld())) continue;
            event.bossbar(PlayerHudPriority.HIGH, mapVote.getBossBar());
        }
    }
}
