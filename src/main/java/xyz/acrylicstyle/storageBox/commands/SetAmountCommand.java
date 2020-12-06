package xyz.acrylicstyle.storageBox.commands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import xyz.acrylicstyle.storageBox.utils.StorageBox;

public class SetAmountCommand {
    public static void onCommand(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "You don't have permission to do this.");
            return;
        }
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "数字を指定してください。");
            return;
        }
        StorageBox storageBox = StorageBox.getStorageBox(player.getInventory().getItemInMainHand());
        if (storageBox == null) {
            player.sendMessage(ChatColor.RED + "現在手に持ってるアイテムはStorage Boxではありません。");
            player.sendMessage(ChatColor.RED + "Storage Boxを手に持ってからもう一度試してください。");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "数字を指定してください。");
            return;
        }
        storageBox.setAmount(amount);
        player.getInventory().setItemInMainHand(storageBox.getItemStack());
        player.sendMessage(ChatColor.GREEN + "アイテムの数を" + ChatColor.RED + amount + ChatColor.GREEN + "にしました。");
    }
}
