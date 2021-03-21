package com.winthier.creative;

import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.winthier.creative.struct.Cuboid;
import com.winthier.creative.util.Json;
import com.winthier.creative.worldedit.WorldEdit;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class AreaCommand implements TabExecutor {
    private final CreativePlugin plugin;
    private CommandNode rootNode;

    public AreaCommand enable() {
        rootNode = new CommandNode("area");
        rootNode.addChild("add")
            .arguments("<file> <path>")
            .description("Add an area")
            .playerCaller(this::add);
        rootNode.addChild("remove")
            .arguments("<file> <path>")
            .description("Remove current area")
            .playerCaller(this::remove);
        return this;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return rootNode.call(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return rootNode.complete(sender, command, label, args);
    }

    boolean add(Player player, String[] args) {
        if (args.length != 2) return false;
        String fileArg = args[0];
        String nameArg = args[1];
        Cuboid cuboid = getSelection(player);
        World world = player.getWorld();
        AreasFile areasFile = getAreasFile(world, fileArg);
        areasFile.areas.computeIfAbsent(nameArg, u -> new ArrayList<>()).add(cuboid);
        saveAreasFile(world, fileArg, areasFile);
        player.sendMessage("Area added to " + world.getName() + "/" + fileArg + "/" + nameArg + ": " + cuboid);
        return true;
    }

    boolean remove(Player player, String[] args) {
        return false;
    }

    Cuboid getSelection(Player player) {
        Cuboid cuboid = WorldEdit.getSelection(player);
        if (cuboid == null) throw new CommandWarn("WorldEdit selection required!");
        return cuboid;
    }

    AreasFile getAreasFile(World world, String fileName) {
        File folder = new File(world.getWorldFolder(), "areas");
        File file = new File(folder, fileName + ".json");
        AreasFile areasFile = Json.load(file, AreasFile.class, AreasFile::new);
        return areasFile;
    }

    void saveAreasFile(World world, String fileName, AreasFile areasFile) {
        File folder = new File(world.getWorldFolder(), "areas");
        folder.mkdirs();
        File file = new File(folder, fileName + ".json");
        Json.save(file, areasFile, true);
    }

    static final class AreasFile {
        Map<String, List<Cuboid>> areas = new HashMap<>();
    }
}
