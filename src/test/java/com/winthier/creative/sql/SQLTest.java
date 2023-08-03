package com.winthier.creative.sql;

import org.junit.Test;
import static com.winthier.sql.SQLDatabase.testTableCreation;

public final class SQLTest {
    @Test
        public void test() {
        for (var it : Database.getAllTableClasses()) {
            System.out.println(testTableCreation(it));
        }
    }
}
