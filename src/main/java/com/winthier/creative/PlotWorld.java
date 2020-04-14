package com.winthier.creative;

import com.winthier.generic_events.GenericEvents;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Value;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class PlotWorld {
    public final String name;
    int roadSize;
    int plotSize;
    int gridSize;
    List<Plot> plots = new ArrayList<>();

    @Value
    public static final class Plot {
        public final PlotWorld plotWorld;
        public final int x;
        public final int z;
        public final UUID owner;

        boolean isOwner(Player player) {
            return player.getUniqueId().equals(owner);
        }

        void getCornerBlock(World world, Consumer<Block> callback) {
            int bx = (x - 1) * plotWorld.gridSize + plotWorld.roadSize / 2;
            int bz = (z - 1) * plotWorld.gridSize + plotWorld.roadSize / 2;
            world.getChunkAtAsync(bx, bz, c -> {
                    Block block = world.getHighestBlockAt(bx, bz);
                    callback.accept(block);
                });
        }
    }

    void load(ConfigurationSection config) {
        roadSize = config.getInt("roads");
        plotSize = config.getInt("plots");
        gridSize = roadSize + plotSize;
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;
            int x = section.getInt("x");
            int z = section.getInt("z");
            UUID owner = UUID.fromString(section.getString("owner"));
            plots.add(new Plot(this, x, z, owner));
        }
    }

    Plot getPlot(final int x, final int z) {
        for (Plot plot : plots) {
            if (x == plot.x && z == plot.z) return plot;
        }
        return null;
    }

    int worldToGrid(int num) {
        num -= roadSize / 2;
        return num < 0
            ? (num + 1) / gridSize
            : num / gridSize + 1;
    }

    int worldToInner(int num) {
        num -= roadSize / 2;
        int result = num % gridSize;
        return result < 0
            ? result + gridSize
            : result;
    }

    Plot plotAt(Block block) {
        final int bx = block.getX();
        final int bz = block.getZ();
        // Plot 1,1 starts at origin,origin
        final int origin = roadSize / 2;
        int gridX = worldToGrid(bx);
        int gridZ = worldToGrid(bz);
        int innerX = worldToInner(bx);
        int innerZ = worldToInner(bz);
        if (innerX >= plotSize) return null;
        if (innerZ >= plotSize) return null;
        return getPlot(gridX, gridZ);
    }

    boolean canBuild(Player player, Block block) {
        Plot plot = plotAt(block);
        return plot != null && plot.isOwner(player);
    }

    String debug(Block block) {
        final int bx = block.getX();
        final int bz = block.getZ();
        // Plot 1,1 starts at origin,origin
        final int origin = roadSize / 2;
        int gridX = worldToGrid(bx);
        int gridZ = worldToGrid(bz);
        int innerX = worldToInner(bx);
        int innerZ = worldToInner(bz);
        String coordStr = "grid=" + gridX + "," + gridZ
            + " inner=" + innerX + "," + innerZ;
        if (innerX >= plotSize) return "road " + coordStr;
        if (innerZ >= plotSize) return "road " + coordStr;
        Plot plot = getPlot(gridX, gridZ);
        if (plot == null) return "empty plot " + coordStr;
        return "plot " + GenericEvents.cachedPlayerName(plot.owner)
            + " " + coordStr;
    }
}
