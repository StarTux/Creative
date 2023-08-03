package com.winthier.creative.sql;

import com.winthier.creative.Trust;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import com.winthier.sql.SQLRow;
import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Name("trusted")
@Getter @Setter @NotNull
@UniqueKey(name = "world_player", value = {"world", "player"})
public final class SQLWorldTrust implements SQLRow {
    @Id private Integer id;
    private String world; // path
    private UUID player;
    private Trust trust;
    private Date created;

    public SQLWorldTrust() { }

    public SQLWorldTrust(final String world, final UUID player, final Trust trust) {
        this.world = world;
        this.player = player;
        this.trust = trust;
        this.created = new Date();
    }
}
