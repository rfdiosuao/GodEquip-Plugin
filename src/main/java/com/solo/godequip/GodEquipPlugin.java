package com.solo.godequip;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.event.entity.EntityShootBowEvent;

import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import org.bukkit.event.entity.EntityDamageEvent;

public class GodEquipPlugin extends JavaPlugin implements CommandExecutor, Listener {

    private final NamespacedKey GOD_ITEM_KEY = new NamespacedKey(this, "god_item");
    private final NamespacedKey GOD_FLIGHT_KEY = new NamespacedKey(this, "god_flight");
    private final double INFINITY_VALUE = 2048.0; // Safe "Infinity"
    private final int XP_COST_PER_SECOND = 1000;
    private final int SWORD_XP_REQ = 10000;
    private final int BOW_XP_REQ = 10000;

    @Override
    public void onEnable() {
        // Load default config
        saveDefaultConfig();

        // Register command
        if (getCommand("godset") != null) {
            getCommand("godset").setExecutor(this);
        }
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Flight & XP check task (runs every 20 ticks / 1 second)
        Bukkit.getScheduler().runTaskTimer(this, this::checkStatus, 20L, 20L);
        
        getLogger().info("GodEquip has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GodEquip has been disabled!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有玩家可以使用此命令。", NamedTextColor.RED));
            return true;
        }

        giveGodSet(player);
        player.sendMessage(Component.text("你已获得 [终焉奇点] 套装。", NamedTextColor.GOLD));
        return true;
    }

    @EventHandler
    public void onEntityDamageGeneral(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && hasFullGodSet(player)) {
            // If they have XP, they are invincible to everything (Warden Sonic Boom, Void, etc.)
            // Assuming "Infinite" means truly invulnerable as long as the cost is paid.
            // The scheduler deducts XP, so if they have enough to fly, they have enough to be god.
            // However, we can add a small per-hit cost if desired, but user asked for "Infinite".
            // Let's just cancel damage if they have > 0 XP.
            if (getTotalExperience(player) > 0) {
                 event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (isGodItem(item) && item.getType() == Material.NETHERITE_SWORD) {
                 if (getTotalExperience(player) < SWORD_XP_REQ) {
                     event.setDamage(1.0);
                     player.sendActionBar(Component.text("能量不足。剑刃重若千钧。", NamedTextColor.RED));
                 } else {
                     // XP is sufficient, deduct cost
                     setTotalExperience(player, getTotalExperience(player) - SWORD_XP_REQ);
                     player.sendActionBar(Component.text("轰！能量释放...", NamedTextColor.LIGHT_PURPLE));
                     // AttributeModifier handles the damage.
                 }
            }
        }
        
        if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player player) {
            ItemStack bow = player.getInventory().getItemInMainHand();
            if (bow.getType() != Material.BOW) {
                bow = player.getInventory().getItemInOffHand();
            }
            
            if (isGodItem(bow)) {
                // Deduct cost was handled in EntityShootBowEvent? No, that event is for shooting.
                // But EntityShootBowEvent handles the "can shoot" check. 
                // If we want to deduct on HIT, we do it here. 
                // BUT user said "每次攻击或者射箭都会扣除".
                // "Shooting" (EntityShootBowEvent) is when the arrow leaves the bow.
                // "Attack" (EntityDamageByEntityEvent) is when the sword hits.
                // So for bow, we should deduct on SHOOT, not on HIT, to prevent free spamming.
                // However, the damage is set here on hit.
                event.setDamage(INFINITY_VALUE);
            }
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack bow = event.getBow();
            if (isGodItem(bow)) {
                if (getTotalExperience(player) < BOW_XP_REQ) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("能量不足。弓弦纹丝不动。", NamedTextColor.RED));
                } else {
                    // XP is sufficient, deduct cost
                    setTotalExperience(player, getTotalExperience(player) - BOW_XP_REQ);
                    player.sendActionBar(Component.text("因果律打击已发射。", NamedTextColor.LIGHT_PURPLE));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // Restriction removed
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        // Restriction removed
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Restriction removed
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // Restriction removed
    }

    private void giveGodSet(Player player) {
        // Armor
        player.getInventory().addItem(createGodItem(Material.NETHERITE_HELMET, "视界之冠", 
            List.of("§7凝视深渊者，", "§7终将被深渊回望。"), EquipmentSlot.HEAD, true));
            
        player.getInventory().addItem(createGodItem(Material.NETHERITE_CHESTPLATE, "奇点核心", 
            List.of("§7万物归一，", "§7在此终结。"), EquipmentSlot.CHEST, true));
            
        player.getInventory().addItem(createGodItem(Material.NETHERITE_LEGGINGS, "虚空行者", 
            List.of("§7跨越维度，", "§7不受法则束缚。"), EquipmentSlot.LEGS, true));
            
        player.getInventory().addItem(createGodItem(Material.NETHERITE_BOOTS, "熵增之步", 
            List.of("§7所过之处，", "§7时间亦为之冻结。"), EquipmentSlot.FEET, true));

        // Weapons
        player.getInventory().addItem(createGodItem(Material.NETHERITE_SWORD, "降维打击", 
            List.of("§7一念之间，", "§7星河破碎。"), EquipmentSlot.HAND, false));
            
        player.getInventory().addItem(createGodItem(Material.BOW, "因果之矢", 
            List.of("§7结果已定，", "§7过程只是修饰。"), EquipmentSlot.HAND, false));
    }

    private ItemStack createGodItem(Material material, String name, List<String> lore, EquipmentSlot slot, boolean isArmor) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Display Name
            meta.displayName(Component.text(name, NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

            // Lore
            List<Component> componentLore = new ArrayList<>();
            for (String line : lore) {
                componentLore.add(Component.text(line).decoration(TextDecoration.ITALIC, true));
            }
            meta.lore(componentLore);

            // Attributes
            if (isArmor) {
                meta.addAttributeModifier(Attribute.GENERIC_ARMOR, 
                    new AttributeModifier(UUID.randomUUID(), "god_armor", INFINITY_VALUE, AttributeModifier.Operation.ADD_NUMBER, slot));
                meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, 
                    new AttributeModifier(UUID.randomUUID(), "god_toughness", INFINITY_VALUE, AttributeModifier.Operation.ADD_NUMBER, slot));
                meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, 
                    new AttributeModifier(UUID.randomUUID(), "god_kb_res", 1.0, AttributeModifier.Operation.ADD_NUMBER, slot));
                
                // Armor Trim: Silence Pattern + Diamond Material (Blue Quiet Look)
                if (meta instanceof ArmorMeta armorMeta) {
                    // Try to find Silence pattern and Diamond material
                    // Note: Registry access might be safer, but NamespacedKey works for standard ones
                    TrimPattern pattern = TrimPattern.SILENCE;
                    TrimMaterial trimMaterial = TrimMaterial.DIAMOND;
                    
                    if (pattern != null && trimMaterial != null) {
                        armorMeta.setTrim(new ArmorTrim(trimMaterial, pattern));
                    }
                }
            } else {
                // Weapon Attributes
                if (material == Material.NETHERITE_SWORD) {
                    meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, 
                        new AttributeModifier(UUID.randomUUID(), "god_damage", INFINITY_VALUE, AttributeModifier.Operation.ADD_NUMBER, slot));
                }
            }

            // General "Infinite" Vibe
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            
            // Curses
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);

            // Add all effective enchantments (Max Normal Level)
            for (Enchantment enchantment : Enchantment.values()) {
                if (enchantment.canEnchantItem(item) && !enchantment.isCursed()) {
                    meta.addEnchant(enchantment, enchantment.getMaxLevel(), true);
                }
            }

            // PDC Tag for identification
            meta.getPersistentDataContainer().set(GOD_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }
        return item;
    }

    private void checkStatus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasFullGodSet(player)) {
                int totalExp = getTotalExperience(player);
                if (totalExp >= XP_COST_PER_SECOND) {
                    setTotalExperience(player, totalExp - XP_COST_PER_SECOND);
                    
                    if (!player.getAllowFlight()) {
                        player.setAllowFlight(true);
                        player.getPersistentDataContainer().set(GOD_FLIGHT_KEY, PersistentDataType.BYTE, (byte) 1);
                        player.sendMessage(Component.text("飞行协议已启动。正在汲取能量...", NamedTextColor.AQUA));
                    }
                } else {
                    // Not enough XP, force de-equip
                    player.sendMessage(Component.text("能量不足。奇点拒绝了你的请求。", NamedTextColor.RED));
                    forceDeEquip(player);
                    disableFlight(player);
                }
            } else {
               disableFlight(player);
            }
        }
    }
    
    private void disableFlight(Player player) {
         if (player.getGameMode().getValue() == 0 || player.getGameMode().getValue() == 2) { // Survival or Adventure
            if (player.getAllowFlight() && !player.hasPermission("godequip.admin.fly")) {
                // Only disable if WE enabled it
                if (player.getPersistentDataContainer().has(GOD_FLIGHT_KEY, PersistentDataType.BYTE)) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.getPersistentDataContainer().remove(GOD_FLIGHT_KEY);
                }
            }
        }
    }

    private void forceDeEquip(Player player) {
        // Move armor to inventory, if full drop to ground (which is blocked by our own listener? No, listener blocks player drop, not plugin drop)
        // Wait, our drop listener uses PlayerDropItemEvent, which is triggered by player action. Plugin dropping is fine.
        ItemStack[] armor = player.getInventory().getArmorContents();
        player.getInventory().setArmorContents(null);
        for (ItemStack item : armor) {
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item).forEach((index, leftover) -> {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                });
            }
        }
    }

    private boolean hasFullGodSet(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chest = player.getInventory().getChestplate();
        ItemStack legs = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        return isGodItem(helmet) && isGodItem(chest) && isGodItem(legs) && isGodItem(boots);
    }

    private boolean isGodItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(GOD_ITEM_KEY, PersistentDataType.BYTE);
    }

    // --- XP Calculation Helpers ---
    // Source: Bukkit Community / ExperienceManager logic
    
    public int getTotalExperience(Player player) {
        int experience = 0;
        int level = player.getLevel();
        if (level >= 0 && level <= 15) {
            experience = (int) Math.ceil(Math.pow(level, 2) + 6 * level);
        } else if (level > 15 && level <= 30) {
            experience = (int) Math.ceil(2.5 * Math.pow(level, 2) - 40.5 * level + 360);
        } else {
            experience = (int) Math.ceil(4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
        }
        return experience + Math.round(getExpAtLevel(level) * player.getExp());
    }

    private int getExpAtLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    public void setTotalExperience(Player player, int xp) {
        // Clear current
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);
        
        // Add back
        int currentXp = xp;
        while (currentXp > 0) {
            int xpToNext = getExpAtLevel(player.getLevel());
            if (currentXp >= xpToNext) {
                currentXp -= xpToNext;
                player.setLevel(player.getLevel() + 1);
            } else {
                player.setExp((float) currentXp / xpToNext);
                currentXp = 0;
            }
        }
        player.setTotalExperience(xp);
    }
}
