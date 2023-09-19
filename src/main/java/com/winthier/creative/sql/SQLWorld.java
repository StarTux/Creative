package com.winthier.creative.sql;

import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.core.util.Json;
import com.winthier.creative.BuildWorld.Flag;
import com.winthier.creative.BuildWorldPurpose;
import com.winthier.creative.Trust;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.bukkit.World;
import org.bukkit.WorldType;
import static com.winthier.creative.CreativePlugin.plugin;

@Name("worlds")
@Data @NotNull
public final class SQLWorld implements SQLRow {
    @Id private Integer id;
    @Unique private String path; // key
    private String name;
    @Nullable private String description;
    @Nullable private UUID owner = null;
    @Nullable @VarChar(15) private String publicTrust;
    private Date created;
    @Text private String tag = "{}";
    private transient Tag cachedTag;

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
    @Nullable private Long seed = 0L;
    @Nullable private String worldType = null;
    @Nullable private String environment;
    private boolean generateStructures = false;
    @Nullable @Text private String generatorSettings = null;

    // Purpose
    @Nullable private String purpose;
    @Nullable private String purposeType;
    @Nullable @LongText private String purposeTag;
    @Default("0") private boolean purposeConfirmed;
    @Nullable private Date purposeConfirmedWhen;
    @Default("0") private int purposeIndex;
    @Default("0") private int voteScore;

    public SQLWorld() { }

    public SQLWorld(final SQLWorld o) {
        this.path = o.path;
        this.name = o.name;
        this.description = o.description;
        this.owner = o.owner;
        this.publicTrust = o.publicTrust;
        this.created = o.created;
        this.tag = o.tag;
        this.borderCenterX = o.borderCenterX;
        this.borderCenterZ = o.borderCenterZ;
        this.borderSize = o.borderSize;
        this.spawnX = o.spawnX;
        this.spawnY = o.spawnY;
        this.spawnZ = o.spawnZ;
        this.spawnYaw = o.spawnYaw;
        this.spawnPitch = o.spawnPitch;
        this.generator = o.generator;
        this.seed = o.seed;
        this.worldType = o.worldType;
        this.environment = o.environment;
        this.generateStructures = o.generateStructures;
        this.generatorSettings = o.generatorSettings;
        this.purpose = o.purpose;
        this.purposeType = o.purposeType;
        this.purposeTag = o.purposeTag;
        this.purposeConfirmed = o.purposeConfirmed;
        this.purposeConfirmedWhen = o.purposeConfirmedWhen;
        this.purposeIndex = o.purposeIndex;
        this.voteScore = o.voteScore;
    }

    public SQLWorld clone() {
        return new SQLWorld(this);
    }

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

    public boolean isSpawnSet() {
        return spawnX != 0.0
            || spawnY != 0.0
            || spawnZ != 0.0
            || spawnYaw != 0.0
            || spawnPitch != 0.0;
    }

    public Trust getPublicTrustValue() {
        if (publicTrust == null) return Trust.NONE;
        try {
            return Trust.valueOf(publicTrust.toUpperCase());
        } catch (IllegalArgumentException iae) {
            plugin().getLogger().severe("[SQLWorld] " + id + ": Invalid public trust: " + publicTrust);
            return Trust.NONE;
        }
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

    public BuildWorldPurpose parsePurpose() {
        if (purpose == null) return BuildWorldPurpose.UNKNOWN;
        try {
            return BuildWorldPurpose.valueOf(purpose.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return BuildWorldPurpose.UNKNOWN;
        }
    }

    public MinigameMatchType parseMinigame() {
        if (parsePurpose() != BuildWorldPurpose.MINIGAME) return null;
        if (purposeType == null) return null;
        try {
            return MinigameMatchType.valueOf(purposeType.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    public void resetPurpose() {
        this.purpose = null;
        this.purposeType = null;
        this.purposeConfirmed = false;
    }

    public void setMinigame(MinigameMatchType type) {
        this.purpose = BuildWorldPurpose.MINIGAME.name().toLowerCase();
        this.purposeType = type.name().toLowerCase();
        this.purposeConfirmed = false;
    }
}
