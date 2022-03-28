package com.winthier.creative.util;

import com.winthier.creative.CreativePlugin;
import java.io.File;

public final class Files {
    private Files() { }

    public static void deleteRecursively(File file) {
        if (!file.exists()) return;
        CreativePlugin.getInstance().getLogger().info("deleteRecursively " + file);
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
