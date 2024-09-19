package com.winthier.creative.sql;

import static com.winthier.sql.SQLDatabase.testTableCreation;

public final class SQLTest {
    public void test() {
        for (var it : Database.getAllTableClasses()) {
            System.out.println(testTableCreation(it));
        }
    }
}
