package com.winthier.creative;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class WorldEditListener {
    private final CreativePlugin plugin;

    void enable() {
        WorldEdit.getInstance().getEventBus().register(this);
    }

    void disable() {
        WorldEdit.getInstance().getEventBus().unregister(this);
    }

    @Subscribe
    public void onEditSession(final EditSessionEvent event) {
        final Actor actor = event.getActor();
        if (actor == null || !actor.isPlayer()) return;
        final Player player = Bukkit.getPlayerExact(actor.getName());
        final World world = Bukkit.getWorld(event.getWorld().getName());
        // Do nothing outside of plot worlds.
        final BuildWorld buildWorld = plugin.getBuildWorldByWorld(world);
        if (buildWorld == null) {
            return;
        }
        event.setExtent(new MyExtent(event.getExtent(), player, buildWorld));
    }

    class MyExtent extends AbstractDelegateExtent {
        private final Extent extent;
        private final BuildWorld buildWorld;
        private final Player player;
        private final UUID uuid;

        MyExtent(final Extent extent, final Player player, final BuildWorld buildWorld) {
            super(extent);
            this.extent = extent;
            this.buildWorld = buildWorld;
            this.player = player;
            this.uuid = player.getUniqueId();
        }

        @Override
        public boolean setBlock(final BlockVector3 loc, final BlockStateHolder bl)
            throws WorldEditException {
            if (player.isOp()) {
                return extent.setBlock(loc, bl);
            }
            if (plugin.doesIgnore(uuid)) {
                return extent.setBlock(loc, bl);
            }
            if (!buildWorld.getTrust(uuid).canUseWorldEdit()) {
                return false;
            }
            BlockType type = bl.getBlockType();
            switch (bl.getBlockType().getId()) {
            case "minecraft:chain_command_block":
            case "minecraft:command_block":
            case "minecraft:repeating_command_block":
                return false;
            default: break;
            }
            return extent.setBlock(loc, bl);
        }
    }
}
