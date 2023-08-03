package com.winthier.creative;

import com.cavetale.core.connect.Connect;
import com.cavetale.core.event.connect.ConnectMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import static com.winthier.creative.CreativePlugin.plugin;

public final class ConnectListener implements Listener {
    public static final String CHANNEL_WORLD_UPDATE = "creative.world_update";

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin());
    }

    @EventHandler
    private void onConnectMessage(ConnectMessageEvent event) {
        if (event.getChannel().equals(CHANNEL_WORLD_UPDATE)) {
            final String path = event.getPayload();
            plugin().getLogger().info("World update received: " + path);
            plugin().reloadBuildWorldAsync(path, bw -> { });
        }
    }

    public static void broadcastWorldUpdate(BuildWorld bw) {
        Connect.get().broadcastMessage(CHANNEL_WORLD_UPDATE, bw.getPath());
    }
}
