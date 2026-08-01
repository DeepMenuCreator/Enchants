package com.example.enchants.listeners;

import com.example.enchants.EnchantPlugin;
import com.example.enchants.enchantments.DrillEnchant;
import com.example.enchants.enchantments.MagnetEnchant;
import com.example.enchants.enchantments.MegaDrillEnchant;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.*;

public class BlockBreakListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || !tool.getType().name().endsWith("_PICKAXE")) return;

        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean hasMagnet = pdc.has(MagnetEnchant.KEY, PersistentDataType.BYTE);
        boolean hasDrill = pdc.has(DrillEnchant.KEY, PersistentDataType.BYTE);
        boolean hasMegaDrill = pdc.has(MegaDrillEnchant.KEY, PersistentDataType.BYTE);

        Block centerBlock = event.getBlock();

        // MegaDrill (VeinMiner) имеет приоритет
        if (hasMegaDrill && isOre(centerBlock.getType())) {
            event.setCancelled(true);
            veinMine(player, centerBlock, tool);
            return;
        }

        // Drill 3x3
        if (hasDrill) {
            event.setCancelled(true);
            break3x3(player, centerBlock, tool);
            return;
        }

        // Magnet для одиночного блока
        if (hasMagnet) {
            event.setDropItems(false);
            Collection<ItemStack> drops = centerBlock.getDrops(tool, player);
            for (ItemStack drop : drops) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                for (ItemStack left : leftover.values()) {
                    player.getWorld().dropItemNaturally(centerBlock.getLocation(), left);
                }
            }
            centerBlock.setType(Material.AIR);
            damageTool(player, tool, 1);
        }
    }

    private void break3x3(Player player, Block center, ItemStack tool) {
        BlockFace face = getBlockFace(player);
        List<Block> blocks = get3x3Blocks(center, face);

        int broken = 0;
        for (Block block : blocks) {
            if (block.equals(center)) continue;
            if (!canBreak(player, block)) continue;
            if (block.getType() == Material.AIR || block.getType() == Material.BEDROCK) continue;

            Collection<ItemStack> drops = block.getDrops(tool, player);
            block.setType(Material.AIR);
            broken++;

            for (ItemStack drop : drops) {
                if (hasMagnet(tool)) {
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                    for (ItemStack left : leftover.values()) {
                        player.getWorld().dropItemNaturally(block.getLocation(), left);
                    }
                } else {
                    player.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            }
        }

        // Центральный блок
        Collection<ItemStack> centerDrops = center.getDrops(tool, player);
        center.setType(Material.AIR);
        broken++;

        for (ItemStack drop : centerDrops) {
            if (hasMagnet(tool)) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                for (ItemStack left : leftover.values()) {
                    player.getWorld().dropItemNaturally(center.getLocation(), left);
                }
            } else {
                player.getWorld().dropItemNaturally(center.getLocation(), drop);
            }
        }

        damageTool(player, tool, broken);
    }

    private void veinMine(Player player, Block start, ItemStack tool) {
        Material targetType = start.getType();
        Set<Location> visited = new HashSet<>();
        List<Block> toBreak = new ArrayList<>();
        Queue<Block> queue = new LinkedList<>();

        queue.add(start);
        visited.add(start.getLocation());

        int limit = 64;
        while (!queue.isEmpty() && toBreak.size() < limit) {
            Block current = queue.poll();
            toBreak.add(current);

            for (Block relative : getNeighbors(current)) {
                if (relative.getType() == targetType && !visited.contains(relative.getLocation()) && toBreak.size() < limit) {
                    if (!canBreak(player, relative)) continue;
                    visited.add(relative.getLocation());
                    queue.add(relative);
                }
            }
        }

        int broken = 0;
        for (Block block : toBreak) {
            if (block.getType() != targetType) continue;
            Collection<ItemStack> drops = block.getDrops(tool, player);
            block.setType(Material.AIR);
            broken++;

            for (ItemStack drop : drops) {
                if (hasMagnet(tool)) {
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                    for (ItemStack left : leftover.values()) {
                        player.getWorld().dropItemNaturally(block.getLocation(), left);
                    }
                } else {
                    player.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            }
        }

        damageTool(player, tool, broken);
    }

    private List<Block> get3x3Blocks(Block center, BlockFace face) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(center);

        Vector dir = face.getDirection();
        int dx = Math.abs(dir.getBlockX());
        int dy = Math.abs(dir.getBlockY());
        int dz = Math.abs(dir.getBlockZ());

        if (dy == 1) {
            // Верх/низ - горизонтальная плоскость
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    blocks.add(center.getRelative(x, 0, z));
                }
            }
        } else if (dx == 1) {
            // Север/юг - плоскость YZ
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    blocks.add(center.getRelative(0, y, z));
                }
            }
        } else {
            // Восток/запад - плоскость XY
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    blocks.add(center.getRelative(x, y, 0));
                }
            }
        }
        return blocks;
    }

    private BlockFace getBlockFace(Player player) {
        float pitch = player.getLocation().getPitch();
        if (pitch <= -45) return BlockFace.UP;
        if (pitch >= 45) return BlockFace.DOWN;

        float yaw = player.getLocation().getYaw();
        yaw = (yaw % 360 + 360) % 360;

        if (yaw >= 45 && yaw < 135) return BlockFace.WEST;
        if (yaw >= 135 && yaw < 225) return BlockFace.NORTH;
        if (yaw >= 225 && yaw < 315) return BlockFace.EAST;
        return BlockFace.SOUTH;
    }

    private List<Block> getNeighbors(Block block) {
        List<Block> neighbors = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    neighbors.add(block.getRelative(x, y, z));
                }
            }
        }
        return neighbors;
    }

    private boolean isOre(Material material) {
        return material.name().endsWith("_ORE") || material == Material.ANCIENT_DEBRIS;
    }

    private boolean canBreak(Player player, Block block) {
        if (player.hasMetadata("enchantpickaxe_checking")) return false;
        player.setMetadata("enchantpickaxe_checking", new FixedMetadataValue(EnchantPlugin.getInstance(), true));
        BlockBreakEvent testEvent = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(testEvent);
        boolean cancelled = testEvent.isCancelled();
        player.removeMetadata("enchantpickaxe_checking", EnchantPlugin.getInstance());
        return !cancelled;
    }

    private boolean hasMagnet(ItemStack tool) {
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(MagnetEnchant.KEY, PersistentDataType.BYTE);
    }

    private void damageTool(Player player, ItemStack tool, int amount) {
        if (tool.getType().getMaxDurability() <= 0) return;

        ItemMeta meta = tool.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
            int currentDamage = damageable.getDamage();
            int newDamage = currentDamage + amount;

            // Учет прочности (Unbreaking)
            int unbreakingLevel = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
            if (unbreakingLevel > 0) {
                Random random = new Random();
                for (int i = 0; i < amount; i++) {
                    if (random.nextInt(unbreakingLevel + 1) > 0) {
                        newDamage--;
                    }
                }
            }

            if (newDamage >= tool.getType().getMaxDurability()) {
                player.getInventory().setItemInMainHand(null);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1, 1);
            } else {
                damageable.setDamage(newDamage);
                tool.setItemMeta(meta);
            }
        }
    }
}
