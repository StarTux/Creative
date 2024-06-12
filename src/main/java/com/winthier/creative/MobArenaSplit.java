/*
package com.winthier.creative;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.core.struct.Vec3i;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.WorldType;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.structure.Structure;
import static com.cavetale.core.util.CamelCase.splitCamelCase;
import static com.winthier.creative.CreativePlugin.plugin;

public final class MobArenaSplit {
    private BuildWorld originBuildWorld;
    private final List<String> arenaNames = new ArrayList<>();
    private final Map<String, List<Area>> areasMap = new HashMap<>();
    private final Random random = new Random(0L);
    private int doneCount = 0;

    public void start() {
        originBuildWorld = BuildWorld.findWithPath("halloween_arenas");
        originBuildWorld.setKeepInMemory(true);
        originBuildWorld.loadWorld();
        final AreasFile areasFile = AreasFile.load(originBuildWorld.getWorld(), "MobArena");
        areasMap.putAll(areasFile.getAreas());
        arenaNames.addAll(areasMap.keySet());
        iter();
    }

    private void log(String msg) {
        plugin().getLogger().info("[MobArenaSplit] " + msg);
    }

    private void iter() {
        if (arenaNames.isEmpty()) {
            log("DONE splitting " + doneCount + " worlds");
            return;
        }
        final String name = arenaNames.remove(0);
        log("[" + name + "] starting");
        final List<Area> areas = areasMap.get(name);
        final Area mainArea = areas.get(0);
        // We want to place the new center at 255, 255
        final Vec3i center = mainArea.getCenter();
        final int offsetX = 255 - center.x;
        final int offsetZ = 255 - center.z;
        final String path = "mob_arena_" + String.join("_", splitCamelCase(name)).toLowerCase();
        final BuildWorld newBuildWorld = new BuildWorld(name, path, null);
        newBuildWorld.getRow().setGenerator("VoidGenerator");
        newBuildWorld.getRow().setSeed(0L);
        newBuildWorld.getRow().setWorldType(WorldType.FLAT.name().toLowerCase());
        newBuildWorld.getRow().setEnvironment(originBuildWorld.loadWorld().getEnvironment().name().toLowerCase());
        newBuildWorld.getRow().setGenerateStructures(false);
        newBuildWorld.getRow().setGeneratorSettings("");
        newBuildWorld.getRow().setSpawnX(255.5);
        newBuildWorld.getRow().setSpawnY(65.0);
        newBuildWorld.getRow().setSpawnZ(255.5);
        newBuildWorld.getWorldFolder().mkdirs();
        newBuildWorld.loadWorld();
        newBuildWorld.getWorld().setTime(originBuildWorld.getWorld().getTime());
        newBuildWorld.getRow().setPurpose("mob_arena");
        newBuildWorld.getRow().setPurposeConfirmed(true);
        newBuildWorld.getRow().setPurposeConfirmedWhen(new Date());
        for (GameRule gameRule : GameRule.values()) {
            newBuildWorld.getWorld().setGameRule(gameRule, originBuildWorld.getWorld().getGameRuleValue(gameRule));
        }
        for (Area area : areas) {
            if (!"spawn".equals(area.getName())) continue;
            newBuildWorld.getRow().setSpawnY((double) area.getMin().getY());
            break;
        }
        int pasteCount = 0;
        for (int y = mainArea.getMin().getY(); y <= mainArea.getMax().getY(); y += 48) {
            for (int z = mainArea.getMin().getZ(); z <= mainArea.getMax().getZ(); z += 48) {
                for (int x = mainArea.getMin().getX(); x <= mainArea.getMax().getX(); x += 48) {
                    final int x2 = Math.min(mainArea.getMax().getX() + 1, x + 48);
                    final int y2 = Math.min(mainArea.getMax().getY() + 1, y + 48);
                    final int z2 = Math.min(mainArea.getMax().getZ() + 1, z + 48);
                    final Structure structure = Bukkit.getStructureManager().createStructure();
                    final Location a = originBuildWorld.getWorld().getBlockAt(x, y, z).getLocation();
                    final Location b = originBuildWorld.getWorld().getBlockAt(x2, y2, z2).getLocation();
                    structure.fill(a, b, true);
                    final int x3 = x + offsetX;
                    final int y3 = y;
                    final int z3 = z + offsetZ;
                    final Location c = newBuildWorld.getWorld().getBlockAt(x3, y3, z3).getLocation();
                    structure.place(c, true, StructureRotation.NONE, Mirror.NONE, 0, 1f, random);
                    pasteCount += 1;
                }
            }
        }
        newBuildWorld.getWorld().save();
        AreasFile newAreasFile = new AreasFile();
        final List<Area> newAreas = new ArrayList<>();
        for (Area area : areas) {
            newAreas.add(area.shift(offsetX, 0, offsetZ));
        }
        newAreasFile.getAreas().put(name, newAreas);
        newAreasFile.save(newBuildWorld.getWorld(), "MobArena");
        log("[" + path + "] done, pasted " + pasteCount);
        newBuildWorld.insertAsync(() -> {
                plugin().getBuildWorlds().add(newBuildWorld);
                plugin().unloadEmptyWorld(newBuildWorld.getWorld());
                doneCount += 1;
                Bukkit.getScheduler().runTask(plugin(), this::iter);
            });
    }
}
*/
