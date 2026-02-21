package com.winthier.creative.vote;

import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.core.util.Json;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Text;
import com.winthier.creative.BuildWorld;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import static com.winthier.creative.CreativePlugin.plugin;
import static com.winthier.creative.review.MapReviewMenu.starComponent;
import static com.winthier.creative.vote.MapVotes.mapVotes;
import static java.util.Comparator.comparingInt;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

/**
 * Store all available worlds and manage votes.
 *
 * Managed by MapVotes.
 *
 * The expected lifetime of this object is one vote.  New votes shall
 * create a new copy, see MapVotes.
 *
 * The regular callback is called after a map has been chosen and
 * loaded.
 * The voteHandler is the alternative callback which can be used if
 * the players should be split into smaller groups in the end.
 *
 * Set lobbyWorld and desiredGroupSize to enable groups.
 */
@Getter
public final class MapVote {
    private final MinigameMatchType minigame;
    // Settings
    @Setter private Component title = empty();
    @Setter private MapVoteScheme scheme = MapVoteScheme.CHANCE;
    @Setter private Consumer<MapVote> voteHandler = null;
    @Setter private Consumer<MapVoteResult> callback = null;
    @Setter private int maxTicks = 20 * 60;
    @Setter private int avoidRepetition = 3;
    @Setter private World lobbyWorld = null;
    @Setter private BiConsumer<Player, BuildWorld> voteCallback;
    @Setter private Consumer<MapTooltipBuilder> mapTooltipHandler;
    @Setter private int desiredGroupSize;
    // Runtime
    private Map<String, BuildWorld> maps = Map.of();
    private Set<String> blacklistedMaps = new HashSet<>();
    private Map<UUID, String> votes = new HashMap<>();
    private boolean voteActive;
    private int loadingWorlds = 0;
    private BukkitTask task;
    private int ticksLeft;
    private final Random random = new Random();
    private BossBar bossBar;

    public MapVote(final MinigameMatchType minigame) {
        this.minigame = minigame;
    }

    public void load() {
        Map<String, BuildWorld> newMaps = new HashMap<>();
        final boolean requireConfirmation = true;
        for (BuildWorld buildWorld : BuildWorld.findMinigameWorlds(minigame, requireConfirmation)) {
            newMaps.put(buildWorld.getPath(), buildWorld);
        }
        // Prune maps if configured to avoid repetition.
        if (avoidRepetition > 0) {
            MapVoteTag tag = loadTag();
            for (int i = 0; i < avoidRepetition; i += 1) {
                if (i >= tag.avoidRepetitionList.size()) break;
                blacklistedMaps.add(tag.avoidRepetitionList.get(i));
            }
            if (blacklistedMaps.size() >= newMaps.size()) {
                // No maps are left! Clear the list of repeated maps.
                blacklistedMaps.clear();
                tag.avoidRepetitionList.clear();
                saveTag(tag);
            }
        }
        this.maps = newMaps;
    }

    protected void startVote() {
        voteActive = true;
        votes.clear();
        ticksLeft = maxTicks;
        this.bossBar = BossBar.bossBar(textOfChildren(title, text(" /mapvote", YELLOW)), 0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        task = Bukkit.getScheduler().runTaskTimer(plugin(), this::tick, 0L, 1L);
        if (lobbyWorld != null) remindToVote(lobbyWorld);
    }

    protected void stopVote() {
        voteActive = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public File getTagFolder() {
        return new File(plugin().getDataFolder(), "votes");
    }

    public File getTagFile() {
        return new File(getTagFolder(), minigame.name().toLowerCase() + ".json");
    }

    public MapVoteTag loadTag() {
        return Json.load(getTagFile(), MapVoteTag.class, MapVoteTag::new);
    }

    public void saveTag(MapVoteTag tag) {
        getTagFolder().mkdirs();
        Json.save(getTagFile(), tag, true);
    }

    private void tick() {
        final int ticks = ticksLeft--;
        bossBar.progress(voteProgress());
        if (ticks <= 0) {
            finishVote();
        }
    }

    public float voteProgress() {
        return Math.max(0f, Math.min(1f, 1f - ((float) ticksLeft / (float) maxTicks)));
    }

    public boolean vote(Player player, BuildWorld map) {
        if (blacklistedMaps.contains(map.getPath())) return false;
        if (voteCallback != null) {
            voteCallback.accept(player, map);
        } else {
            if (!isVoteActive()) return false;
            votes.put(player.getUniqueId(), map.getPath());
            player.sendMessage(
                textOfChildren(
                    Mytems.CHECKED_CHECKBOX,
                    text(" Voted for map: ", GREEN),
                    text(map.getName()),
                    text(" by ", GRAY),
                    text(String.join(" ", map.getBuilderNames()))
                )
                .hoverEvent(showText(text("Change your vote?", GRAY)))
                .clickEvent(runCommand("/mapvote open " + minigame.name().toLowerCase()))
            );
        }
        return true;
    }

    public void finishVote() {
        stopVote();
        printVoteStats();
        if (voteHandler != null) {
            voteHandler.accept(this);
        } else if (lobbyWorld != null && desiredGroupSize > 0) {
            makeGroups();
        } else if (lobbyWorld != null) {
            findAndLoadWinnersFor(lobbyWorld.getPlayers(), this.callback);
        } else {
            findAndLoadWinner(List.copyOf(votes.keySet()), this.callback);
        }
    }

    public void findAndLoadWinnersFor(Collection<Player> players, Consumer<MapVoteResult> theCallback) {
        List<UUID> uuids = new ArrayList<>();
        for (var p : players) uuids.add(p.getUniqueId());
        findAndLoadWinner(uuids, theCallback);
    }

    public void findAndLoadWinner(Collection<UUID> playerIds, Consumer<MapVoteResult> theCallback) {
        BuildWorld buildWorldWinner = switch (scheme) {
        case CHANCE -> findWinnerByChance(playerIds);
        case HIGHEST -> findHighestWinner(playerIds);
        default -> throw new IllegalStateException("scheme=" + scheme);
        };
        if (avoidRepetition > 0) {
            MapVoteTag tag = loadTag();
            final List<String> list = new ArrayList<>();
            list.add(buildWorldWinner.getPath());
            list.addAll(tag.avoidRepetitionList);
            while (list.size() > avoidRepetition) {
                list.remove(list.size() - 1);
            }
            tag.avoidRepetitionList = list;
            saveTag(tag);
        }
        loadingWorlds += 1;
        buildWorldWinner.makeLocalCopyAsync(world -> {
                if (theCallback != null) {
                    final MapVoteResult result = new MapVoteResult(this, buildWorldWinner, world, List.copyOf(playerIds));
                    theCallback.accept(result);
                }
                loadingWorlds -= 1;
            });
    }

    public void makeGroups() {
        final List<Player> allPlayers = lobbyWorld.getPlayers();
        final List<Player> players = new ArrayList<>(allPlayers);
        Collections.shuffle(players);
        List<List<Player>> groups = new ArrayList<>();
        if (players.size() < desiredGroupSize) {
            groups.add(List.copyOf(players));
            players.clear();
        }
        while (players.size() >= desiredGroupSize) {
            List<Player> currentGroup = new ArrayList<>();
            groups.add(currentGroup);
            for (int i = 0; i < desiredGroupSize; i += 1) {
                currentGroup.add(players.remove(players.size() - 1));
            }
        }
        for (int i = 0; i < players.size(); i += 1) {
            groups.get(i % groups.size()).add(players.get(i));
        }
        for (List<Player> group : groups) {
            findAndLoadWinnersFor(group, this.callback);
        }
    }

    public BuildWorld findWinnerAtRandom() {
        final List<BuildWorld> all = List.copyOf(maps.values());
        return all.get(random.nextInt(all.size()));
    }

    public BuildWorld findWinnerByChance(Collection<UUID> playerIds) {
        final List<BuildWorld> randomMaps = new ArrayList<>();
        for (var uuid : playerIds) {
            String it = votes.get(uuid);
            if (it == null) continue;
            BuildWorld buildWorld = maps.get(it);
            if (buildWorld != null) randomMaps.add(buildWorld);
        }
        if (randomMaps.isEmpty()) return findWinnerAtRandom();
        return randomMaps.get(random.nextInt(randomMaps.size()));
    }

    public BuildWorld findHighestWinner(Collection<UUID> playerIds) {
        final Map<String, Integer> stats = new HashMap<>();
        for (var uuid : playerIds) {
            final String it = votes.get(uuid);
            if (it == null) continue;
            stats.compute(it, (s, i) -> i != null ? i + 1 : 1);
        }
        if (stats.isEmpty()) return findWinnerAtRandom();
        int highest = 0;
        for (int value : stats.values()) {
            if (value > highest) highest = value;
        }
        List<BuildWorld> candidates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            if (entry.getValue() != highest) continue;
            BuildWorld buildWorld = maps.get(entry.getKey());
            if (buildWorld == null) continue;
            candidates.add(buildWorld);
        }
        if (candidates.isEmpty()) return findWinnerAtRandom();
        return candidates.get(random.nextInt(candidates.size()));
    }

    public void printVoteStats() {
        plugin().getLogger().info("[MapVote] [" + minigame.displayName + "] " + votes);
        final Map<String, Integer> stats = new HashMap<>();
        for (String it : votes.values()) {
            stats.compute(it, (s, i) -> i != null ? i + 1 : 1);
        }
        plugin().getLogger().info("[MapVote] [" + minigame.displayName + "] " + stats);
    }

    public void remindToVote(World world) {
        for (Player player : world.getPlayers()) {
            remindToVote(player);
        }
    }

    public void remindToVote(Player player) {
        if (!voteActive) return;
        if (!player.hasPermission("creative.mapvote")) return;
        player.sendMessage(
            textOfChildren(
                newline(),
                Mytems.ARROW_RIGHT,
                (text(" Click here to vote on the next map", GREEN)
                 .hoverEvent(showText(text("Map Selection", GRAY)))
                 .clickEvent(runCommand("/mapvote open " + minigame.name().toLowerCase()))),
                newline()
            )
        );
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 0.5f);
    }

    public void openVoteDialog(Player player) {
        List<BuildWorld> buildWorldList = new ArrayList<>();
        buildWorldList.addAll(maps.values());
        Collections.sort(
            buildWorldList,
            comparingInt((BuildWorld bw) -> bw.getRow().getVoteScore()).reversed()
            .thenComparing(BuildWorld::getName, String.CASE_INSENSITIVE_ORDER)
        );
        final List<ActionButton> actions = new ArrayList<>(buildWorldList.size());
        for (BuildWorld buildWorld : buildWorldList) {
            final boolean isOnTimeout = blacklistedMaps.contains(buildWorld.getPath());
            final MapTooltipBuilder builder = new MapTooltipBuilder(buildWorld, isOnTimeout);
            builder.setTitle(List.of(text(buildWorld.getName(), BLUE)));
            if (isOnTimeout) {
                builder.setTimeout(List.of(text("On Timeout", DARK_GRAY, ITALIC)));
            }
            if (buildWorld.getRow().getVoteScore() > 0) {
                final int starCount = (int) Math.round((double) buildWorld.getRow().getVoteScore() / 100.0);
                builder.setRating(List.of(starComponent(starCount)));
            }
            String by = "By " + String.join(", ", buildWorld.getBuilderNames());
            builder.setCredits(Text.wrapLore(by, c -> c.color(GRAY)));
            if (buildWorld.getRow().getDescription() != null) {
                builder.setDescription(Text.wrapLore(buildWorld.getRow().getDescription(), c -> c.color(LIGHT_PURPLE).decorate(ITALIC)));
            }
            String raw = buildWorld.getName();
            if (raw.length() > 16) raw = raw.substring(0, 16);
            if (mapTooltipHandler != null) {
                mapTooltipHandler.accept(builder);
            }
            actions.add(
                ActionButton.builder(text(buildWorld.getName(), isOnTimeout ? DARK_GRAY : WHITE))
                .tooltip(builder.build())
                .action(
                    DialogAction.customClick(
                        (_a, _b) -> {
                            if (builder.isOnTimeout()) {
                                openVoteDialog(player);
                            } else {
                                vote(player, buildWorld);
                            }
                        },
                        ClickCallback.Options.builder()
                        .lifetime(Duration.ofSeconds(60))
                        .uses(1)
                        .build()
                    )
                )
                .build()
            );
        }
        player.showDialog(
            Dialog.create(
                factory -> {
                    DialogRegistryEntry.Builder builder = factory.empty();
                    builder.base(
                        DialogBase.builder(title)
                        .build()
                    );
                    builder.type(
                        DialogType.multiAction(actions)
                        .columns(1)
                        .build()
                    );
                }
            )
        );
    }

    public List<String> getWorldNames() {
        List<String> result = new ArrayList<>();
        result.addAll(maps.keySet());
        return result;
    }

    public BuildWorld getMap(String name) {
        return maps.get(name);
    }

    public static MapVote of(MinigameMatchType type) {
        return mapVotes().getMinigameVoteMap().get(type);
    }

    /**
     * Check if this vote is still active.
     * The requesting client would be interested if they should start
     * a new vote, so the this is true if we are still voting or
     * currently loading a world.
     */
    public static boolean isActive(MinigameMatchType type) {
        MapVote mapVote = of(type);
        return mapVote != null && (mapVote.voteActive || mapVote.loadingWorlds > 0);
    }

    public static MapVote start(MinigameMatchType type, Consumer<MapVote> theVote) {
        stop(type);
        MapVote result = new MapVote(type);
        theVote.accept(result);
        mapVotes().getMinigameVoteMap().put(type, result);
        result.load();
        result.startVote();
        return result;
    }

    public static MapVote stop(MinigameMatchType type) {
        MapVote result = mapVotes().getMinigameVoteMap().remove(type);
        if (result != null && result.voteActive) result.stopVote();
        return result;
    }
}
