package com.example.enchants.commands;

import com.example.enchants.enchantments.DrillEnchant;
import com.example.enchants.enchantments.MagnetEnchant;
import com.example.enchants.enchantments.MegaDrillEnchant;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class EnchantCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков!");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§eИспользование: /enchantpickaxe <magnet|drill|megadrill>");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.getType().name().endsWith("_PICKAXE")) {
            player.sendMessage("§cДержите кирку в руке!");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;

        String type = args[0].toLowerCase();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        switch (type) {
            case "magnet" -> {
                meta.getPersistentDataContainer().set(MagnetEnchant.KEY, PersistentDataType.BYTE, (byte) 1);
                if (!lore.contains(MagnetEnchant.LORE)) lore.add(MagnetEnchant.LORE);
                player.sendMessage("§aЗачарование §bМагнит §aдобавлено!");
            }
            case "drill" -> {
                meta.getPersistentDataContainer().set(DrillEnchant.KEY, PersistentDataType.BYTE, (byte) 1);
                if (!lore.contains(DrillEnchant.LORE)) lore.add(DrillEnchant.LORE);
                player.sendMessage("§aЗачарование §bБур §aдобавлено!");
            }
            case "megadrill" -> {
                meta.getPersistentDataContainer().set(MegaDrillEnchant.KEY, PersistentDataType.BYTE, (byte) 1);
                if (!lore.contains(MegaDrillEnchant.LORE)) lore.add(MegaDrillEnchant.LORE);
                player.sendMessage("§aЗачарование §bОгромный бур §aдобавлено!");
            }
            default -> {
                player.sendMessage("§cНеизвестное зачарование. Доступные: magnet, drill, megadrill");
                return true;
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return true;
    }
}
