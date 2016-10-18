package com.winthier.creative;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

@RequiredArgsConstructor
public class CreativeListener implements Listener {
    final CreativePlugin plugin;
    
    // @EventHandler
    // public void onPlayerJoin(PlayerJoinEvent event) {
    //     System.out.println("SPAWN " + event.getPlayer().getLocation().getWorld().getName());
    // }

    // @EventHandler
    // public void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
    //     System.out.println("SPAWN LOCATION " + event.getSpawnLocation().getWorld().getName());
    // }
}
