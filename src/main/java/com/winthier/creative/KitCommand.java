package com.winthier.creative;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.MytemsCategory;
import com.cavetale.mytems.MytemsTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

final class KitCommand extends AbstractCommand<CreativePlugin> {
    protected KitCommand(final CreativePlugin plugin) {
        super(plugin, "ckit");
    }

    @Override
    protected void onEnable() {
        rootNode.denyTabCompletion()
            .description("Open the Creative Kit")
            .playerCaller(this::ckit);
    }

    public void ckit(Player player) {
        if (!plugin.isCreativeServer()) {
            throw new CommandWarn("Must be on the creative server");
        }
        Inventory inventory = Bukkit.createInventory(null, 6 * 9, text("Creative Kit", BLUE));
        inventory.addItem(new ItemStack(Material.WOODEN_AXE));
        inventory.addItem(new ItemStack(Material.BARRIER));
        inventory.addItem(new ItemStack(Material.LIGHT));
        inventory.addItem(Mytems.ARMOR_STAND_EDITOR.createItemStack());
        inventory.addItem(Mytems.MAGIC_MAP.createItemStack());
        for (Mytems mytems : MytemsTag.of(MytemsCategory.TREE_SEED).getMytems()) {
            inventory.addItem(mytems.createItemStack());
        }
        inventory.addItem(Mytems.FERTILIZER.createItemStack(64));
        inventory.addItem(Mytems.MONKEY_WRENCH.createItemStack());
        inventory.addItem(Mytems.BLIND_EYE.createItemStack());
        inventory.addItem(Mytems.DIVIDERS.createItemStack());
        inventory.addItem(Mytems.YARDSTICK.createItemStack());
        inventory.addItem(Mytems.LUMINATOR.createItemStack());
        inventory.addItem(Mytems.WHITE_PAINTBRUSH.createItemStack());
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.MASTER, 0.5f, 1.2f);
    }
}
