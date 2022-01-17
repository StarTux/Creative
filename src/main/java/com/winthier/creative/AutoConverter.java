package com.winthier.creative;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

@RequiredArgsConstructor
public final class AutoConverter {
    private final CreativePlugin plugin;
    private final ArrayList<String> worldNames = new ArrayList<>();
    private BuildWorld currentBuildWorld;
    private World currentWorld;
    private int chunksToLoad;

    private void log(String msg) {
        plugin.getLogger().info("[AutoConverter] " + msg);
    }

    private void warn(String msg) {
        plugin.getLogger().warning("[AutoConverter] " + msg);
    }

    protected void start() {
        File folder = new File(plugin.getDataFolder(), "autoconvert");
        if (!folder.exists()) {
            warn("Folder doesn't exist: " + folder);
            return;
        }
        HashSet<String> worldNameSet = new HashSet<>();
        for (File file : folder.listFiles()) {
            if (!file.isFile()) continue;
            if (!file.getName().endsWith(".yml")) continue;
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (config.isList("worlds")) {
                worldNameSet.addAll(config.getStringList("worlds"));
            }
            if (config.isList("maps")) {
                worldNameSet.addAll(config.getStringList("maps"));
            }
        }
        if (worldNameSet.isEmpty()) {
            warn("No world names found in " + folder + "!");
            return;
        }
        worldNameSet.removeIf(worldName -> {
                if (plugin.getBuildWorldByPath(worldName) != null) return false;
                warn("Build world not found: " + worldName);
                return true;
            });
        worldNames.addAll(worldNameSet);
        worldNames.sort(String.CASE_INSENSITIVE_ORDER);
        log("Starting auto conversion of " + worldNames.size() + " worlds...");
        popWorld();
    }

    private void popWorld() {
        if (worldNames.isEmpty()) {
            log("Done!");
            return;
        }
        String worldName = worldNames.remove(worldNames.size() - 1);
        log("Next: " + worldName);
        currentBuildWorld = plugin.getBuildWorldByPath(worldName);
        currentWorld = currentBuildWorld.loadWorld();
        currentBuildWorld.setKeepInMemory(true);
        chunksToLoad = 1;
        Chunk spawnChunk = currentWorld.getSpawnLocation().getChunk();
        final int cx = spawnChunk.getX();
        final int cz = spawnChunk.getZ();
        final int spawnRadius = 10;
        for (int dz = -spawnRadius; dz <= spawnRadius; dz += 1) {
            for (int dx = -spawnRadius; dx <= spawnRadius; dx += 1) {
                chunksToLoad += 1;
                final int x = cx + dx;
                final int z = cz + dz;
                currentWorld.getChunkAtAsync(x, z, (Consumer<Chunk>) chunk -> {
                        for (int y = currentWorld.getMinHeight(); y < currentWorld.getMaxHeight(); y += 16) {
                            currentWorld.getBlockAt(x << 4, y, z << 4);
                        }
                        chunkLoadCallback();
                    });
            }
        }
        chunkLoadCallback();
    }

    private void chunkLoadCallback() {
        chunksToLoad -= 1;
        if (chunksToLoad > 0) return;
        currentWorld.save();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                currentBuildWorld.setKeepInMemory(false);
                plugin.unloadEmptyWorld(currentWorld);
                log("World finished: " + currentBuildWorld.getPath());
                currentWorld = null;
                currentBuildWorld = null;
                popWorld();
            }, 200L);
    }
}
