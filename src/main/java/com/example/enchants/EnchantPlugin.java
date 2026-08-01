package com.example.enchants;

import com.example.enchants.commands.EnchantCommand;
import com.example.enchants.listeners.BlockBreakListener;
import org.bukkit.plugin.java.JavaPlugin;

public class EnchantPlugin extends JavaPlugin {
    private static EnchantPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new BlockBreakListener(), this);
        getCommand("enchantpickaxe").setExecutor(new EnchantCommand());
        getLogger().info("EnchantPickaxe enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("EnchantPickaxe disabled!");
    }

    public static EnchantPlugin getInstance() {
        return instance;
    }
}
