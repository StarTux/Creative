package com.winthier.creative.sql;

import com.winthier.sql.SQLDatabase;
import com.winthier.sql.SQLRow;
import java.util.List;
import static com.winthier.creative.CreativePlugin.plugin;

public final class Database {
    private static Database instance;
    private SQLDatabase sqlDatabase;

    public Database() {
        instance = this;
    }

    public static List<Class<? extends SQLRow>> getAllTableClasses() {
        return List.of(SQLWorld.class,
                       SQLWorldTrust.class,
                       SQLReview.class);
    }

    public void enable() {
        sqlDatabase = new SQLDatabase(plugin());
        sqlDatabase.registerTables(getAllTableClasses());
        sqlDatabase.createAllTables();
    }

    public void disable() {
        sqlDatabase.waitForAsyncTask();
        sqlDatabase.close();
    }

    public static Database database() {
        return instance;
    }

    public static SQLDatabase sql() {
        return instance.sqlDatabase;
    }
}
