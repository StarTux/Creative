package com.winthier.creative;

import com.cavetale.core.command.CommandNode;
import com.cavetale.core.util.Json;
import com.winthier.creative.file.Files;
import java.io.File;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
public final class AdminWorldCommands {
    private final CreativePlugin plugin;
    private final CommandNode parentNode;

    public void enable() {
        parentNode.addChild("stillthere").denyTabCompletion()
            .description("Check if all worlds still have a folder")
            .senderCaller(this::stillThere);
        parentNode.addChild("migrate26").denyTabCompletion()
            .description("World migration 2026")
            .senderCaller(this::migrate26);
    }

    private void stillThere(CommandSender sender) {
        int notFoundCount = 0;
        for (BuildWorld buildWorld : plugin.getBuildWorlds()) {
            if (buildWorld.getWorldFolder().exists()) continue;
            notFoundCount += 1;
            sender.sendMessage(
                textOfChildren(
                    text("Not found: ", DARK_RED),
                    text(buildWorld.getPath(), RED),
                    space(),
                    text(buildWorld.getName(), DARK_RED)
                )
            );
        }
        if (notFoundCount > 0) {
            sender.sendMessage(text("Total " + notFoundCount + " not found", RED));
        } else {
            sender.sendMessage(text("All worlds are still there!", GREEN));
        }
    }

    private void migrate26(CommandSender sender) {
        int fixLevelDatCount = 0;
        int fixEnvCount = 0;
        int totalEnvCount = 0;
        for (BuildWorld buildWorld : plugin.getBuildWorlds()) {
            final File folder = buildWorld.getWorldFolder();
            if (!folder.exists()) continue;
            final File levelDat = new File(folder, "level.dat.json");
            if (!levelDat.exists()) {
                sender.sendMessage(text("Missing file: " + levelDat, RED));
            } else {
                final boolean result = fixLevelDat(sender, buildWorld, levelDat);
                if (result) fixLevelDatCount += 1;
            }
            final World.Environment environment = buildWorld.getRow().getEnvironmentValue();
            if (environment == World.Environment.NETHER) {
                totalEnvCount += 1;
                final boolean result = fixEnv(sender, buildWorld, environment, "DIM-1");
                if (result) fixEnvCount += 1;
            } else if (environment == World.Environment.THE_END) {
                totalEnvCount += 1;
                final boolean result = fixEnv(sender, buildWorld, environment, "DIM1");
                if (result) fixEnvCount += 1;
            }
        }
        sender.sendMessage(text("Fixed " + fixLevelDatCount + "/" + plugin.getBuildWorlds().size() + " level.dat(s)", YELLOW));
        sender.sendMessage(text("Fixed " + fixEnvCount + "/" + totalEnvCount + " environment(s)", YELLOW));
    }

    private boolean fixLevelDat(CommandSender sender, BuildWorld buildWorld, File levelDat) {
        final Map<?, ?> map = Json.load(levelDat, Map.class);
        final ConfigurationSection config = new MemoryConfiguration().createSection("tmp", map);
        Integer advanceTimeValue = config.getInt("game_rules.minecraft:advance_time");
        boolean doSaveTag = false;
        if (advanceTimeValue == null) {
            advanceTimeValue = config.getInt("GameRules.doDaylightCycle");
        }
        if (advanceTimeValue == null) {
            sender.sendMessage(text("No advance time value: " + levelDat, RED));
        } else {
            buildWorld.set(BuildWorld.Flag.LOCK_TIME, advanceTimeValue == 0);
            doSaveTag = true;
        }
        final Long timeValue = config.getLong("Data.Time");
        if (timeValue == null) {
            sender.sendMessage(text("No time value: " + levelDat, RED));
        } else {
            buildWorld.setTime(timeValue);
            doSaveTag = true;
        }
        if (doSaveTag) {
            buildWorld.saveTagAsync(() -> { });
        }
        return doSaveTag;
    }

    private boolean fixEnv(CommandSender sender, BuildWorld buildWorld, World.Environment environment, String envFolderName) {
        final File envFolder = new File(buildWorld.getWorldFolder(), envFolderName);
        if (!envFolder.exists()) {
            return false;
        }
        for (String dataFolderName : List.of("data", "entities", "poi", "region")) {
            final File dataFolder = new File(envFolder, dataFolderName);
            final File targetFolder = new File(buildWorld.getWorldFolder(), dataFolderName);
            if ("region".equals(dataFolderName)) {
                if (!dataFolder.exists()) {
                    sender.sendMessage(text("Region folder not found: " + dataFolder, RED));
                    return false;
                }
                if (dataFolder.list().length == 0) {
                    sender.sendMessage(text("Region folder is empty: " + dataFolder, RED));
                    return false;
                }
                if (targetFolder.exists()) {
                    sender.sendMessage(text("Region folder exists: " + targetFolder, YELLOW));
                }
            }
            if (targetFolder.exists()) {
                Files.deleteFileStructure(targetFolder, false);
            }
            if (dataFolder.exists()) {
                final boolean moveResult = dataFolder.renameTo(targetFolder);
                if (!moveResult) {
                    sender.sendMessage(text("Could not move: " + dataFolder + " => " + targetFolder, RED));
                    return false;
                }
            }
        }
        final boolean deleteResult = envFolder.delete();
        if (!deleteResult) {
            sender.sendMessage(text("Could not delete env folder: " + envFolder, YELLOW));
        }
        return true;
    }
}
