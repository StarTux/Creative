package com.winthier.creative.vote;

import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.core.util.Json;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Text;
import com.winthier.creative.BuildWorld;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.scheduler.BukkitTask;
import static com.winthier.creative.CreativePlugin.plugin;
import static com.winthier.creative.review.MapReviewMenu.starComponent;
import static com.winthier.creative.vote.MapVotes.mapVotes;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.separator;
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
    // Runtime
    private Map<String, BuildWorld> maps = Map.of();
    private Map<UUID, String> votes = new HashMap<>();
    private boolean voteActive;
    private boolean loadingWorld;
    private BukkitTask task;
    private int ticksLeft;
    private final Random random = new Random();
    private BossBar bossBar;

    protected MapVote(final MinigameMatchType minigame) {
        this.minigame = minigame;
    }

    protected void load() {
        Map<String, BuildWorld> newMaps = new HashMap<>();
        final boolean requireConfirmation = true;
        for (BuildWorld buildWorld : BuildWorld.findMinigameWorlds(minigame, requireConfirmation)) {
            newMaps.put(buildWorld.getPath(), buildWorld);
        }
        // Prune maps if configured to avoid repetition.
        if (avoidRepetition > 0) {
            Map<String, BuildWorld> prunedMaps = new HashMap<>(newMaps);
            MapVoteTag tag = loadTag();
            for (int i = 0; i < avoidRepetition; i += 1) {
                if (i >= tag.avoidRepetitionList.size()) break;
                final String key = tag.avoidRepetitionList.get(i);
                prunedMaps.remove(key);
            }
            if (prunedMaps.isEmpty()) {
                // No maps are left! Clear the list of repeated maps.
                tag.avoidRepetitionList.clear();
                saveTag(tag);
            } else {
                newMaps = prunedMaps;
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

    public void vote(UUID uuid, BuildWorld map) {
        votes.put(uuid, map.getPath());
    }

    public void finishVote() {
        stopVote();
        printVoteStats();
        if (voteHandler != null) {
            voteHandler.accept(this);
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
        loadingWorld = true;
        buildWorldWinner.makeLocalCopyAsync(world -> {
                if (theCallback != null) {
                    MapVoteResult result = new MapVoteResult(this, buildWorldWinner, world);
                    theCallback.accept(result);
                }
                loadingWorld = false;
            });
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
        player.sendMessage(textOfChildren(newline(),
                                          Mytems.ARROW_RIGHT,
                                          (text(" Click here to vote on the next map", GREEN)
                                           .hoverEvent(showText(text("Map Selection", GRAY)))
                                           .clickEvent(runCommand("/mapvote open " + minigame.name().toLowerCase()))),
                                          newline()));
    }

    public void openVoteBook(Player player) {
        List<BuildWorld> mapList = new ArrayList<>();
        mapList.addAll(maps.values());
        Collections.sort(mapList, comparing(BuildWorld::getName, String.CASE_INSENSITIVE_ORDER));
        Collections.sort(mapList, comparingInt((BuildWorld bw) -> bw.getRow().getVoteScore()).reversed());
        List<Component> lines = new ArrayList<>();
        for (BuildWorld buildWorld : mapList) {
            List<Component> tooltip = new ArrayList<>();
            String raw = buildWorld.getName();
            if (raw.length() > 16) raw = raw.substring(0, 16);
            Component displayName = text(raw, BLUE);
            tooltip.add(displayName);
            if (buildWorld.getRow().getVoteScore() > 0) {
                final int starCount = (int) Math.round((double) buildWorld.getRow().getVoteScore() / 100.0);
                tooltip.add(starComponent(starCount));
            }
            String by = "By " + String.join(", ", buildWorld.getBuilderNames());
            tooltip.addAll(Text.wrapLore(by, c -> c.color(GRAY)));
            if (buildWorld.getRow().getDescription() != null) {
                tooltip.addAll(Text.wrapLore(buildWorld.getRow().getDescription(), c -> c.color(LIGHT_PURPLE).decorate(ITALIC)));
            }
            final String command = "/mapvote vote " + minigame.name().toLowerCase() + " " + buildWorld.getPath();
            lines.add(displayName
                      .hoverEvent(showText(join(separator(newline()), tooltip)))
                      .clickEvent(runCommand(command)));
        }
        bookLines(player, lines);
    }

    private void bookLines(Player player, List<Component> lines) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                if (m instanceof BookMeta meta) {
                    meta.author(text("Cavetale"));
                    meta.title(text("Title"));
                    meta.pages(toPages(lines));
                }
            });
        player.closeInventory();
        player.openBook(book);
    }

    private List<Component> toPages(List<Component> lines) {
        final int lineCount = lines.size();
        final int linesPerPage = 10;
        List<Component> pages = new ArrayList<>((lineCount - 1) / linesPerPage + 1);
        for (int i = 0; i < lineCount; i += linesPerPage) {
            List<Component> page = new ArrayList<>(14);
            page.add(title);
            page.add(empty());
            page.addAll(lines.subList(i, Math.min(lines.size(), i + linesPerPage)));
            pages.add(join(separator(newline()), page));
        }
        return pages;
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
        return mapVote != null && (mapVote.voteActive || mapVote.loadingWorld);
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
