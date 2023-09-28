package com.winthier.creative.sql;

import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import com.winthier.sql.SQLRow;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data @Name("reviews") @NotNull @UniqueKey({"path", "player"})
public final class SQLReview implements SQLRow {
    @Id private Integer id;
    @Keyed private String path; // unique key
    private UUID player; // unique key
    @Default("0") private int stars;
    @Nullable @Default("NULL") @VarChar(255) private String comment;
    @Default("NOW()") private Date updated;

    public SQLReview() { }

    public SQLReview(final String path, final UUID player) {
        this.path = path;
        this.player = player;
    }
}
