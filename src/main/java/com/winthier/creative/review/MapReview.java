package com.winthier.creative.review;

import com.cavetale.mytems.Mytems;
import com.winthier.creative.BuildWorld;
import com.winthier.creative.sql.SQLReview;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import static com.winthier.creative.review.MapReviews.mapReviews;
import static com.winthier.creative.sql.Database.sql;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * A MapReview allows players of a minigame to rate (review) the map
 * on which they just played.
 *
 * A review starts when the start() method is called, and deactivates
 * when stop() is called or when the world is unloaded.  Reminding
 * players to vote is left to the plugin via the remind() and
 * remindAll() methods, plus its "once" alternatives which can be
 * spammed because it only sends the message once per player for each
 * MapReview instance.
 *
 * MapReviews are tied to the world in which the minigame match took
 * place in and which the players are in, which is expected to be a
 * local world copy of a BuildWorld.
 *
 * Instances are lightweight and don't require much upkeep.  They are
 * managed by the MapReviews instance but all required methods to
 * start, stop, and remind, are in here and static.
 *
 * Example code which is able to run every tick:
 * <code>
 *   if (!MapReview.isActive(world)) {
 *       MapReview.start(world, buildWorld);
 *   }
 *   MapReview.in(world).remindAllOnce();
 * </code>
 * A reference to the World and BuildWorld are required.
 */
@Getter @RequiredArgsConstructor
public final class MapReview {
    private final World localWorldCopy;
    private final BuildWorld buildWorld;
    private boolean active = false;
    private Set<UUID> alreadyReminded = new HashSet<>();

    public void enable() {
        active = true;
    }

    public void disable() {
        active = false;
    }

    public void open(Player player) {
        sql().find(SQLReview.class)
            .eq("path", buildWorld.getPath())
            .eq("player", player.getUniqueId())
            .findUniqueAsync(row -> {
                    if (row != null) {
                        open(player, row);
                    } else {
                        final SQLReview newRow = new SQLReview(buildWorld.getPath(), player.getUniqueId());
                        sql().saveAsync(newRow, Set.of("player", "path"), r -> {
                                open(player, newRow);
                            });
                    }
                });
    }

    private void open(Player player, SQLReview row) {
        new MapReviewMenu(player, this, row).enable().open();
    }

    public static MapReview of(final String worldName) {
        return mapReviews().pathReviewMap.get(worldName);
    }

    public static MapReview in(final World inWorld) {
        return mapReviews().pathReviewMap.get(inWorld.getName());
    }

    public static void stop(final World localWorldCopy) {
        MapReview old = mapReviews().pathReviewMap.remove(localWorldCopy.getName());
        if (old != null) old.disable();
    }

    public static MapReview start(final World theLocalWorldCopy, BuildWorld theBuildWorld) {
        stop(theLocalWorldCopy);
        MapReview mapReview = new MapReview(theLocalWorldCopy, theBuildWorld);
        mapReview.enable();
        mapReviews().pathReviewMap.put(theLocalWorldCopy.getName(), mapReview);
        return mapReview;
    }

    public static boolean isActive(final World localWorldCopy) {
        MapReview theReview = in(localWorldCopy);
        return theReview != null && theReview.active;
    }

    public void remindAll() {
        for (Player player : localWorldCopy.getPlayers()) {
            remind(player);
        }
    }

    public void remindAllOnce() {
        for (Player player : localWorldCopy.getPlayers()) {
            remindOnce(player);
        }
    }

    public void remind(Player player) {
        player.sendMessage(join(separator(newline()),
                                List.of(empty(),
                                        textOfChildren(Mytems.STAR,
                                                       text(" Click here to rate ", GOLD),
                                                       text(buildWorld.getName(), WHITE)),
                                        empty()))
                           .hoverEvent(showText(text(buildWorld.getName() + " Review", GRAY)))
                           .clickEvent(runCommand("/mapvote review " + localWorldCopy.getName())));
    }

    public void remindOnce(Player player) {
        if (!alreadyReminded.add(player.getUniqueId())) return;
        remind(player);
    }
}
