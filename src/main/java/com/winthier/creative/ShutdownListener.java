package com.winthier.creative;

import com.winthier.shutdown.event.ShutdownTriggerEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public final class ShutdownListener implements Listener {
    private final CreativePlugin plugin;

    @EventHandler(ignoreCancelled = true)
    private void onShutdownTrigger(ShutdownTriggerEvent event) {
        if (plugin.adminCommand.autoConverter != null) {
            event.setCancelled(true);
        }
    }
}
