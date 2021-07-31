package com.winthier.creative;

import com.cavetale.core.command.CommandContext;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.winthier.creative.struct.Cuboid;
import com.winthier.creative.util.Json;
import com.winthier.creative.worldedit.WorldEdit;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
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
            .arguments("<file> <name>")
            .description("Add an area")
            .completer(this::fileAreaCompleter)
            .playerCaller(this::add);
        rootNode.addChild("remove")
            .arguments("<file> <name> <index>")
            .description("Remove area")
            .completer(this::fileAreaCompleter)
            .playerCaller(this::remove);
        rootNode.addChild("list")
            .arguments("[file] [name]")
            .description("List areas")
            .completer(this::fileAreaCompleter)
            .playerCaller(this::list);
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
        if (areasFile == null) areasFile = new AreasFile();
        areasFile.areas.computeIfAbsent(nameArg, u -> new ArrayList<>()).add(cuboid);
        saveAreasFile(world, fileArg, areasFile);
        player.sendMessage("Area added to " + world.getName() + "/" + fileArg + "/" + nameArg + ": " + cuboid);
        return true;
    }

    boolean list(Player player, String[] args) {
        if (args.length > 2) return false;
        World world = player.getWorld();
        if (args.length == 0) {
            File folder = new File(world.getWorldFolder(), "areas");
            if (!folder.isDirectory()) throw new CommandWarn("No areas to show!");
            List<String> names = new ArrayList<>();
            for (File file : folder.listFiles()) {
                String name = file.getName();
                if (name.endsWith(".json")) {
                    names.add(name.substring(0, name.length() - 5));
                }
            }
            player.sendMessage(Component.text(names.size() + " area files: " + String.join(", ", names),
                                              NamedTextColor.YELLOW));
            return true;
        }
        String fileArg = args[0];
        AreasFile areasFile = getAreasFile(world, fileArg);
        if (areasFile == null) {
            throw new CommandWarn("No areas file found: " + fileArg);
        }
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Map.Entry<String, List<Cuboid>> entry : areasFile.areas.entrySet()) {
                names.add(entry.getKey() + "(" + entry.getValue().size() + ")");
            }
            player.sendMessage(Component.text(fileArg + ": " + names.size() + " area lists: " + String.join(", ", names),
                                              NamedTextColor.YELLOW));
            return true;
        }
        String nameArg = args[1];
        List<Cuboid> list = areasFile.areas.get(nameArg);
        if (list == null) {
            throw new CommandWarn(fileArg + ": Area list not found: " + nameArg);
        }
        player.sendMessage(ChatColor.YELLOW + world.getName() + "/" + fileArg + "/" + nameArg
                           + ": " + list.size() + " areas");
        int index = 0;
        for (Cuboid cuboid : list) {
            player.sendMessage("" + ChatColor.YELLOW + index + ") " + ChatColor.WHITE + cuboid);
            index += 1;
        }
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
        if (!folder.isDirectory()) return null;
        File file = new File(folder, fileName + ".json");
        if (!file.isFile()) return null;
        AreasFile areasFile = Json.load(file, AreasFile.class, () -> null);
        return areasFile;
    }

    void saveAreasFile(World world, String fileName, AreasFile areasFile) {
        File folder = new File(world.getWorldFolder(), "areas");
        folder.mkdirs();
        File file = new File(folder, fileName + ".json");
        Json.save(file, areasFile, true);
    }

    List<String> fileAreaCompleter(CommandContext context, CommandNode node, String[] args) {
        System.out.println("complete " + args.length + " " + context.player);
        if (args.length == 0) return null;
        if (context.player == null) return null;
        String arg = args[args.length - 1];
        if (args.length == 1) {
            File folder = new File(context.player.getWorld().getWorldFolder(), "areas");
            if (!folder.isDirectory()) return Collections.emptyList();
            List<String> result = new ArrayList<>();
            for (File file : folder.listFiles()) {
                String name = file.getName();
                if (!name.endsWith(".json")) continue;
                name = name.substring(0, name.length() - 5);
                if (name.contains(arg)) {
                    result.add(name);
                }
            }
            return result;
        }
        if (args.length == 2) {
            AreasFile areasFile = getAreasFile(context.player.getWorld(), args[0]);
            if (areasFile == null) return Collections.emptyList();
            return areasFile.areas.keySet().stream()
                .filter(s -> s.contains(arg))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    static final class AreasFile {
        Map<String, List<Cuboid>> areas = new HashMap<>();
    }
}
