package com.winthier.creative.review;

import com.cavetale.core.font.GuiOverlay;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Gui;
import com.winthier.creative.sql.SQLReview;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.core.font.Unicode.tiny;
import static com.winthier.creative.CreativePlugin.plugin;
import static com.winthier.creative.sql.Database.sql;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
public final class MapReviewMenu {
    private final Player player;
    private final MapReview mapReview;
    private final SQLReview row;
    private int currentStars = 0;

    public static Component starComponent(final int starCount) {
        List<Component> stars = new ArrayList<>();
        for (int i = 0; i < starCount; i += 1) {
            stars.add(Mytems.STAR.component);
        }
        return join(noSeparators(), stars);
    }

    public static String slogan(final int starCount) {
        return switch (starCount) {
        case 5 -> "Love it!";
        case 4 -> "Great map!";
        case 3 -> "Good map";
        case 2 -> "Needs improvement";
        default -> "Stinks";
        };
    }

    public MapReviewMenu enable() {
        currentStars = row.getStars();
        return this;
    }

    public void open() {
        final int size = 3 * 9;
        final Gui gui = new Gui(plugin()).size(size).title(text(mapReview.getBuildWorld().getName(), WHITE));
        GuiOverlay.Builder overlay = GuiOverlay.BLANK.builder(size, DARK_GRAY)
            .title(textOfChildren(text(mapReview.getBuildWorld().getName(), GOLD), text(tiny(" review"), WHITE)));
        for (int i = 0; i < 5; i += 1) {
            final int starCount = i + 1;
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(textOfChildren(starComponent(starCount), text(" " + starCount + "/5 Stars", GOLD)));
            tooltip.add(text(slogan(starCount), GRAY));
            tooltip.add(text("All reviews are secret and anonymous", DARK_GRAY, ITALIC));
            ItemStack icon = currentStars >= starCount
                ? Mytems.STAR.createIcon(tooltip)
                : Mytems.CHECKBOX.createIcon(tooltip);
            gui.setItem(11 + i, icon, click -> {
                    if (!click.isLeftClick()) return;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    currentStars = starCount;
                    open();
                });
        }
        gui.setItem(18, Mytems.OK.createIcon(List.of(text("Confirm", GREEN))), click -> {
                if (!click.isLeftClick()) return;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                player.closeInventory();
                confirm();
            });
        gui.title(overlay.build());
        gui.open(player);
    }

    public void confirm() {
        if (!mapReview.isActive()) return;
        if (row.getStars() == currentStars) {
            confirmed();
            return;
        }
        row.setStars(currentStars);
        row.setUpdated(new Date());
        sql().saveAsync(row, Set.of("stars", "updated"), r -> {
                if (r == 0) return;
                confirmed();
            });
    }

    private void confirmed() {
        mapReview.getBuildWorld().updateVoteScore();
        player.sendMessage(textOfChildren(text("Thank you for rating ", GOLD),
                                          text(mapReview.getBuildWorld().getName() + " ", WHITE),
                                          starComponent(currentStars),
                                          text(" (" + slogan(currentStars) + ")", GRAY)));
    }
}
