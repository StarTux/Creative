package com.winthier.creative;

import lombok.RequiredArgsConstructor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
final class Vault {
    private final JavaPlugin plugin;
    private Economy economy = null;

    boolean setup() {
        RegisteredServiceProvider<Economy> economyProvider =
            plugin.getServer()
            .getServicesManager()
            .getRegistration(Economy.class);
        if (economyProvider == null) {
            return false;
        }
        economy = economyProvider.getProvider();
        return true;
    }

    boolean has(final OfflinePlayer player, final double amount) {
        if (economy == null) {
            return false;
        }
        return economy.has(player, amount);
    }

    boolean take(final OfflinePlayer player, final double amount) {
        if (economy == null) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    boolean give(final OfflinePlayer player, final double amount) {
        if (economy == null) {
            return false;
        }
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    String format(final double amount) {
        if (economy == null) {
            return String.format("%.02f", amount);
        }
        return economy.format(amount);
    }
}
