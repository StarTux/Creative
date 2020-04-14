package com.winthier.creative;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class Msg {
    static Gson gson = new Gson();
    private Msg() { }

    public static String format(String msg, Object... args) {
        if (msg == null) return "";
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) {
            msg = String.format(msg, args);
        }
        return msg;
    }

    public static void send(CommandSender to, String msg, Object... args) {
        to.sendMessage(format(msg, args));
    }

    public static void info(CommandSender to, String msg, Object... args) {
        to.sendMessage(format("&r[&3Creative&r] ") + format(msg, args));
    }

    public static void warn(CommandSender to, String msg, Object... args) {
        to.sendMessage(format("&r[&cCreative&r] &c") + format(msg, args));
    }

    static void consoleCommand(String cmd, Object... args) {
        if (args.length > 0) cmd = String.format(cmd, args);
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd);
    }

    public static void raw(Player player, Object... obj) {
        if (obj.length == 0) return;
        if (obj.length == 1) {
            consoleCommand("minecraft:tellraw %s %s", player.getName(), gson.toJson(obj[0]));
        } else {
            consoleCommand("minecraft:tellraw %s %s",
                           player.getName(), gson.toJson(Arrays.asList(obj)));
        }
    }

    public static Object button(ChatColor color, String chat, String tooltip, String command) {
        Map<String, Object> map = new HashMap<>();
        map.put("text", format(chat));
        map.put("color", color.name().toLowerCase());
        if (command != null) {
            Map<String, Object> clickEvent = new HashMap<>();
            map.put("clickEvent", clickEvent);
            clickEvent.put("action", command.endsWith(" ") ? "suggest_command" : "run_command");
            clickEvent.put("value", command);
        }
        if (tooltip != null) {
            Map<String, Object> hoverEvent = new HashMap<>();
            map.put("hoverEvent", hoverEvent);
            hoverEvent.put("action", "show_text");
            hoverEvent.put("value", format(tooltip));
        }
        return map;
    }

    public static Object button(String chat, String tooltip, String command) {
        return button(ChatColor.WHITE, chat, tooltip, command);
    }

    public static String camelCase(String msg) {
        StringBuilder sb = new StringBuilder();
        for (String tok: msg.split("_")) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(tok.substring(0, 1).toUpperCase());
            sb.append(tok.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public static String jsonToString(Object json) {
        if (json == null) {
            return "";
        } else if (json instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Object o: (List) json) {
                sb.append(jsonToString(o));
            }
            return sb.toString();
        } else if (json instanceof Map) {
            Map map = (Map) json;
            StringBuilder sb = new StringBuilder();
            sb.append(map.get("text"));
            sb.append(map.get("extra"));
            return sb.toString();
        } else if (json instanceof String) {
            return (String) json;
        } else {
            return json.toString();
        }
    }


    public static String wrap(String what, int maxLineLength, String endl) {
        String[] words = what.split(" ");
        if (words.length == 0) return "";
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder(words[0]);
        int i = 1;
        while (i < words.length) {
            String word = words[i++];
            if (line.length() + word.length() > maxLineLength) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line.append(" ");
                line.append(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        if (lines.isEmpty()) return "";
        line = new StringBuilder(lines.get(0));
        for (i = 1; i < lines.size(); ++i) {
            line.append(endl).append(lines.get(i));
        }
        return line.toString();
    }

    public static String fold(List<String> ls, String glue) {
        if (ls.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(ls.get(0));
        for (int i = 1; i < ls.size(); ++i) sb.append(glue).append(ls.get(i));
        return sb.toString();
    }

    public static ComponentBuilder componentBuilder() {
        return new ComponentBuilder("[Creative]").color(ChatColor.AQUA)
            .append(" ").reset();
    }

    public static BaseComponent[] lore(String... lines) {
        return Stream.of(lines)
            .map(TextComponent::new)
            .toArray(BaseComponent[]::new);
    }

    public static String toString(Block block) {
        return block.getWorld().getName() + " "
            + block.getX() + "," + block.getY() + "," + block.getZ();
    }
}
