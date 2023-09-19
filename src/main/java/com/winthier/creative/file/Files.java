package com.winthier.creative.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import static com.winthier.creative.CreativePlugin.plugin;

public final class Files {
    private static void copyFileStructure(File source, File target, int depth) {
        plugin().getLogger().info("Files#copyFileStructure " + source + " => " + target);
        if (source.isDirectory()) {
            if (!target.exists()) {
                if (!target.mkdirs()) {
                    throw new IllegalStateException("Couldn't create world directory: " + target);
                }
            }
            String[] files = source.list();
            for (String file : files) {
                File srcFile = new File(source, file);
                File destFile = new File(target, file);
                copyFileStructure(srcFile, destFile, depth + 1);
            }
        } else {
            if (depth == 1 && List.of("uid.dat", "session.lock").contains(source.getName())) return;
            try {
                InputStream in = new FileInputStream(source);
                OutputStream out = new FileOutputStream(target);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                in.close();
                out.close();
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }
    }

    public static void copyFileStructure(File source, File target) {
        copyFileStructure(source, target, 0);
    }

    public static void deleteFileStructure(File file) {
        plugin().getLogger().info("Files#deleteFileStructure " + file);
        if (!file.exists()) return;
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteFileStructure(child);
            }
        }
        file.delete();
    }

    private Files() { }
}
