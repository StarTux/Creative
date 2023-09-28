package com.winthier.creative.review;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import static com.winthier.creative.CreativePlugin.plugin;

/**
 * The manager for all MapReviews, stored in the plugin.
 *
 * It is worth noting that while reviews are mapped by their
 * localWorldCopy name, they are going to review the path of the
 * original BuildWorld.
 */
public final class MapReviews implements Listener {
    protected final Map<String, MapReview> pathReviewMap = new HashMap<>();

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin());
    }

    public void disable() {
        for (MapReview it : pathReviewMap.values()) {
            it.disable();
        }
        pathReviewMap.clear();
    }

    public static MapReviews mapReviews() {
        return plugin().getMapReviews();
    }

    @EventHandler
    private void onWorldUnload(WorldUnloadEvent event) {
        MapReview.stop(event.getWorld());
    }
}
