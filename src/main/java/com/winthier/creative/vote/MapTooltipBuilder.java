package com.winthier.creative.vote;

import com.winthier.creative.BuildWorld;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@Data
public final class MapTooltipBuilder {
    private final BuildWorld buildWorld;
    private final boolean onTimeout;
    private List<Component> title = List.of();
    private List<Component> timeout = List.of();
    private List<Component> rating = List.of();
    private List<Component> credits = List.of();
    private List<Component> description = List.of();

    public Component build() {
        final List<Component> tooltip = new ArrayList<>();
        tooltip.addAll(title);
        tooltip.addAll(timeout);
        tooltip.addAll(rating);
        tooltip.addAll(credits);
        tooltip.addAll(description);
        return join(separator(newline()), tooltip);
    }
}
