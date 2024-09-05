package xyz.acrylicstyle.storageBox.listener;

import com.gmail.nossr50.config.WorldBlacklist;
import com.gmail.nossr50.datatypes.meta.BonusDropMeta;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.fake.FakeBlockBreakEvent;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.skills.herbalism.HerbalismManager;
import com.gmail.nossr50.util.BlockUtils;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.worldguard.WorldGuardManager;
import com.gmail.nossr50.worldguard.WorldGuardUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import xyz.acrylicstyle.storageBox.utils.StorageBox;
import xyz.acrylicstyle.storageBox.utils.StorageBoxUtils;

import java.util.*;

public class McMMOListener implements Listener {
    @EventHandler
    public void onBlockDropItemMcMMO(@NotNull BlockDropItemEvent e) {
        Set<Material> uniqueMaterials = new HashSet<>();
        boolean doNotRewardTE = false;
        int blockCount = 0;

        for (Item item : e.getItems()) {
            Material m = item.getItemStack().getType();
            uniqueMaterials.add(m);
            if (m.isBlock()) {
                blockCount++;
            }
        }
        if (uniqueMaterials.size() > 1) {
            doNotRewardTE = true;
        }
        int bonus = 0;
        if (blockCount <= 1) {
            for (Item item : e.getItems()) {
                ItemStack is = new ItemStack(item.getItemStack());

                if (is.getAmount() <= 0) continue;
                if (!mcMMO.p.getGeneralConfig().getDoubleDropsEnabled(PrimarySkillType.MINING, is.getType())
                        && !mcMMO.p.getGeneralConfig().getDoubleDropsEnabled(PrimarySkillType.HERBALISM, is.getType())
                        && !mcMMO.p.getGeneralConfig().getDoubleDropsEnabled(PrimarySkillType.WOODCUTTING, is.getType()))
                    continue;

                if (doNotRewardTE) {
                    if (!is.getType().isBlock()) {
                        continue;
                    }
                }

                if (!e.getBlock().getMetadata("mcMMO: Double Drops").isEmpty()) {
                    BonusDropMeta bonusDropMeta = (BonusDropMeta) e.getBlock().getMetadata("mcMMO: Double Drops").get(0);
                    int bonusCount = bonusDropMeta.asInt();

                    for (int i = 0; i < bonusCount; i++) {
                        if (is.getType() == Material.AIR) continue;
                        bonus++;
                    }
                }
            }
        }
        collect(e, e.getPlayer(), bonus);
    }

    private void collect(@NotNull BlockDropItemEvent e, Player p, int bonus) {
        boolean check = false;
        List<Item> toRemove = new ArrayList<>();
        for (Item item : e.getItems()) {
            //if (item.getItemStack().hasItemMeta()) continue;
            Map.Entry<Integer, StorageBox> storageBox = StorageBoxUtils.getStorageBoxForType(p.getInventory(), item.getItemStack());
            if (storageBox == null) return;
            e.setCancelled(true);
            check = true;
            long amount = item.getItemStack().getAmount();
            item.remove();
//            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8F, 1.9F);
            storageBox.getValue().setAmount(storageBox.getValue().getAmount() + amount + bonus);
            p.getInventory().setItem(storageBox.getKey(), storageBox.getValue().getItemStack());
            toRemove.add(item);
        }
        e.getItems().removeAll(toRemove);
        if (e.getBlock().hasMetadata("mcMMO: Double Drops") && check)
            e.getBlock().removeMetadata("mcMMO: Double Drops", JavaPlugin.getPlugin(mcMMO.class));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent e) {
        if (e instanceof FakeBlockBreakEvent) return;
        Block b = e.getBlock();
        Player p = e.getPlayer();

        if (WorldBlacklist.isWorldBlacklisted(b.getWorld())) {
            cleanupBlockMetadata(b);
            return;
        }
        if (WorldGuardUtils.isWorldGuardLoaded()) {
            if (!WorldGuardManager.getInstance().hasMainFlag(p)) {
                cleanupBlockMetadata(b);
                return;
            }
        }
        BlockState state = b.getState();
        McMMOPlayer mcMMOPlayer = UserManager.getPlayer(p);

        if (mcMMOPlayer == null) {
            cleanupBlockMetadata(b);
            return;
        }
        if (BlockUtils.affectedByGreenTerra(state)) {
            HerbalismManager mgr =mcMMOPlayer.getHerbalismManager();

            if (mgr.canActivateAbility()) {
                mcMMOPlayer.checkAbilityActivation(PrimarySkillType.HERBALISM);
            }
            if (mcMMO.p.getSkillTools().doesPlayerHaveSkillPermission(p, PrimarySkillType.HERBALISM)) {
                mgr.processHerbalismBlockBreakEvent(e);
            }
        }
    }

    private void cleanupBlockMetadata(@NotNull Block block) {
        if (block.hasMetadata("mcMMO: Recently Replanted")) {
            block.removeMetadata("mcMMO: Recently Replanted", JavaPlugin.getPlugin(mcMMO.class));
        }
    }
}
