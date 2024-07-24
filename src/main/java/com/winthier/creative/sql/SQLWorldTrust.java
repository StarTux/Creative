package com.winthier.creative.sql;

import com.winthier.creative.Trust;
import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import java.util.Date;
import java.util.UUID;
import lombok.Data;
import static com.winthier.creative.CreativePlugin.plugin;

@Data
@Name("trusted")
@NotNull
@UniqueKey(name = "world_player", value = {"world", "player"})
public final class SQLWorldTrust implements SQLRow {
    @Id private Integer id;
    private String world; // path
    private UUID player;
    @VarChar(15) private String trust;
    private Date created;

    public SQLWorldTrust() { }

    public SQLWorldTrust(final String world, final UUID player, final Trust trust) {
        this.world = world;
        this.player = player;
        this.trust = trust.name().toLowerCase();
        this.created = new Date();
    }

    public Trust getTrustValue() {
        if (trust == null) return Trust.NONE;
        try {
            return Trust.valueOf(trust.toUpperCase());
        } catch (IllegalArgumentException iae) {
            plugin().getLogger().severe("[SQLWorldTrust] " + id + ": Invalid trust: " + trust);
            return Trust.NONE;
        }
    }
}
