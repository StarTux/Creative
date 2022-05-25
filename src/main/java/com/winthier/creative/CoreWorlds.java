package com.winthier.creative;

import com.cavetale.core.worlds.Worlds;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;

@RequiredArgsConstructor
public final class CoreWorlds implements Worlds {
    private final CreativePlugin plugin;

    @Override
    public CreativePlugin getPlugin() {
        return plugin;
    }

    @Override
    public void loadWorld(String name, Consumer<World> callback) {
        callback.accept(loadWorld(name));
    }

    private World loadWorld(String name) {
        BuildWorld buildWorld = plugin.getBuildWorldByPath(name);
        return buildWorld != null
            ? buildWorld.loadWorld()
            : null;
    }
}
