package com.winthier.creative.vote;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.winthier.creative.BuildWorld;
import com.winthier.creative.CreativePlugin;
import com.winthier.creative.review.MapReview;
import org.bukkit.entity.Player;
import static com.winthier.creative.vote.MapVotes.mapVotes;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class MapVoteCommand extends AbstractCommand<CreativePlugin> {
    public static final String PERMISSION = "creative.mapvote";

    public MapVoteCommand(final CreativePlugin plugin) {
        super(plugin, "mapvote");
    }

    @Override
    protected void onEnable() {
        rootNode.description("Vote on a map");
        rootNode.playerCaller(this::mapVote);
        rootNode.addChild("open").arguments("<minigame>")
            .description("Open vote book")
            .hidden(true)
            .playerCaller(this::open);
        rootNode.addChild("vote").arguments("<minigame> <path>")
            .description("Vote on a map")
            .hidden(true)
            .playerCaller(this::vote);
        rootNode.addChild("review").arguments("<path>")
            .description("Review a map")
            .hidden(true)
            .playerCaller(this::review);
    }

    private void mapVote(Player player) {
        if (mapVotes().getMinigameVoteMap().isEmpty()) {
            throw new CommandWarn("There is no active vote");
        }
        for (MapVote mapVote : mapVotes().getMinigameVoteMap().values()) {
            if (!mapVote.isVoteActive()) continue;
            mapVote.openVoteBook(player);
            return;
        }
        throw new CommandWarn("There is no active vote");
    }

    private boolean open(Player player, String[] args) {
        if (args.length != 1) return false;
        MinigameMatchType minigame = parseMinigame(args[0]);
        if (minigame == null) {
            throw new CommandWarn("There is no active vote");
        }
        MapVote mapVote = MapVote.of(minigame);
        if (mapVote == null || !mapVote.isVoteActive()) {
            throw new CommandWarn("There is no active vote");
        }
        mapVote.openVoteBook(player);
        return true;
    }

    private boolean vote(Player player, String[] args) {
        if (args.length != 2) return false;
        MinigameMatchType minigame = parseMinigame(args[0]);
        if (minigame == null) {
            throw new CommandWarn("There is no active vote");
        }
        MapVote mapVote = MapVote.of(minigame);
        if (mapVote == null || !mapVote.isVoteActive()) {
            throw new CommandWarn("There is no active vote");
        }
        final String mapName = args[1];
        BuildWorld map = mapVote.getMap(mapName);
        if (map == null) {
            throw new CommandWarn("Map not available");
        }
        mapVote.vote(player.getUniqueId(), map);
        player.sendMessage(text("Voted for map: " + map.getName() + " by " + String.join(" ", map.getBuilderNames()), GREEN)
                           .hoverEvent(showText(text("Change your vote", GRAY)))
                           .clickEvent(runCommand("/mapvote open " + minigame.name().toLowerCase())));
        return true;
    }

    private static MinigameMatchType parseMinigame(String arg) {
        try {
            return MinigameMatchType.valueOf(arg.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    private boolean review(Player player, String[] args) {
        if (args.length != 1) return false;
        final String worldName = args[0];
        final MapReview mapReview = MapReview.of(worldName);
        if (mapReview == null || !mapReview.isActive()) {
            throw new CommandWarn("The review is over");
        }
        mapReview.open(player);
        return true;
    }
}
