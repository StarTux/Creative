package com.winthier.creative;

import com.cavetale.mytems.Mytems;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
final class KitCommand implements TabExecutor {
    private final CreativePlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 0) return false;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[creative:kit] player expected");
            return true;
        }
        Inventory inventory = Bukkit.createInventory(null, 6 * 9, text("Creative Kit", BLUE));
        inventory.addItem(new ItemStack(Material.WOODEN_AXE));
        inventory.addItem(new ItemStack(Material.BARRIER));
        inventory.addItem(new ItemStack(Material.LIGHT));
        inventory.addItem(Mytems.ARMOR_STAND_EDITOR.createItemStack());
        inventory.addItem(Mytems.MAGIC_MAP.createItemStack());
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.MASTER, 0.5f, 1.2f);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return List.of();
    }
}
