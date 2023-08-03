package com.winthier.creative.sql;

import com.cavetale.core.util.Json;
import com.winthier.creative.BuildWorld.Flag;
import com.winthier.creative.Trust;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.WorldType;
import static com.winthier.creative.CreativePlugin.plugin;

@Name("worlds")
@Getter @Setter @NotNull
public final class SQLWorld implements SQLRow {
    @Id private Integer id;
    @Unique private String path; // key
    private String name;
    @Nullable private String description;
    @Nullable private UUID owner = null;
    @Nullable @VarChar(15) private Trust publicTrust;
    @Text private String tag = "{}";
    private transient Tag cachedTag;

    @Data
    public static final class Tag {
        private List<String> buildGroups = new ArrayList<>();
        private Map<Flag, Boolean> flags = new EnumMap<>(Flag.class);
    }

    public void unpack() {
        cachedTag = Json.deserialize(tag, Tag.class, Tag::new);
    }

    public void pack() {
        tag = Json.serialize(cachedTag);
    }

    public Tag getCachedTag() {
        if (cachedTag == null) unpack();
        return cachedTag;
    }

    // World Border
    private int borderCenterX = 0;
    private int borderCenterZ = 0;
    private int borderSize = -1;

    // Spawn
    private double spawnX;
    private double spawnY;
    private double spawnZ;
    private double spawnYaw;
    private double spawnPitch;

    // Generator
    @Nullable private String generator = null;
    private long seed = 0L;
    @Nullable private String worldType = null;
    @Nullable private String environment;
    private boolean generateStructures = false;
    @Nullable @Text private String generatorSettings = null;

    public boolean isSpawnSet() {
        return spawnX != 0.0
            || spawnY != 0.0
            || spawnZ != 0.0
            || spawnYaw != 0.0
            || spawnPitch != 0.0;
    }

    public WorldType getWorldTypeValue() {
        if (worldType == null) return WorldType.NORMAL;
        try {
            return WorldType.valueOf(worldType.toUpperCase());
        } catch (IllegalArgumentException iae) {
            plugin().getLogger().severe("[SQLWorld] " + id + ": Invalid worldType: " + worldType);
            return WorldType.NORMAL;
        }
    }

    public World.Environment getEnvironmentValue() {
        if (environment == null) return World.Environment.NORMAL;
        try {
            return World.Environment.valueOf(environment.toUpperCase());
        } catch (IllegalArgumentException iae) {
            plugin().getLogger().severe("[SQLWorld] " + id + ": Invalid environment: " + environment);
            return World.Environment.NORMAL;
        }
    }
}
