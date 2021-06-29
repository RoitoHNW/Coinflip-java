package com.github.roitohnw.man10coinflip;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import static java.lang.Thread.sleep;

public final class Man10Coinflip extends JavaPlugin {

    private Material panelColor(){
        Random random=new Random();
        int d=random.nextInt(3);
        if(d==0){
            return Material.PINK_STAINED_GLASS_PANE;
        }
        else if(d==1){
            return Material.WHITE_STAINED_GLASS_PANE;
        }
        else return Material.LIME_STAINED_GLASS_PANE;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("start");
        Objects.requireNonNull(getCommand("cf")).setExecutor(this);
        this.saveDefaultConfig();
        vault = new VaultManager(this);
        coindata = new HashMap<>();
        config = this.getConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("stop");
    }

    VaultManager vault;
    HashMap<UUID,GameData> coindata;
    FileConfiguration config;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        if (args.length == 0) return true;
        if (args[0].equals("create")) {
            if (args.length != 3) {
                sender.sendMessage("§e§l[CF]使い方が間違ってます");
                return true;
            }
            Player comP=(Player)sender;
            if (!args[1].matches("[+-]?\\d*(\\.\\d+)?"))
            {
                sender.sendMessage("§e§l[CF]/cf create [money] [heads or tails]");
                return true;
            }

            if (vault.getBalance(comP.getUniqueId()) < Double.parseDouble(args[1])) {
                sender.sendMessage("§e§l[CF]所持金が足りません");
                return true;
            }

            if (coindata.containsKey(((Player) sender).getUniqueId())) {
                sender.sendMessage("§e§l[CF]一部屋しか立てれません。");
                return true;
            }

            if (args[2].equals("heads")) {
                double bet = Double.parseDouble(args[1]);
                vault.withdraw(((Player) sender).getUniqueId(), Double.parseDouble(args[1]));
                coindata.put(((Player)sender).getUniqueId(),new GameData(Double.parseDouble(args[1]),true));
                new Thread(() -> {
                    for (int i=0;i<30;i++) {
                        if (i % 10 == 0) sendHoverText("§6§l[CF]" + sender.getName() + "が表予想で" + bet +"円コインフリップを開いています！\n裏だと思う人は/cf join" + sender.getName() + "で参加してみよう","§bまたはここをクリック！","/cf join " + sender.getName());
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (!coindata.containsKey(((Player) sender).getUniqueId())) {
                            return;
                        }
                    }
                    Bukkit.broadcast(Component.text("§6§l[CF]" + sender.getName() + "の部屋は人が集まらなかったのでキャンセルされました"), Server.BROADCAST_CHANNEL_USERS);
                    vault.deposit(((Player) sender),(Double.parseDouble(args[1])));
                }).start();
            }
            if (args[2].equals("tails")) {
                double bet = Double.parseDouble(args[1]);
                vault.withdraw(((Player) sender).getUniqueId(), Double.parseDouble(args[1]));
                coindata.put(((Player)sender).getUniqueId(),new GameData(Double.parseDouble(args[1]),false));
                new Thread(() -> {
                    for (int i=0;i<30;i++) {
                        if (i % 10 == 0) sendHoverText("§6§l[CF]" + sender.getName() + "が裏予想で" + bet +"円コインフリップを開いています！\n表だと思う人は/cf join" + sender.getName() + "で参加してみよう","§bまたはここをクリック！","/cf join " + sender.getName());
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (!coindata.containsKey(((Player) sender).getUniqueId())) {
                            return;
                        }
                    }
                    Bukkit.broadcast(Component.text("§6§l[CF]" + sender.getName() + "の部屋は人が集まらなかったのでキャンセルされました"), Server.BROADCAST_CHANNEL_USERS);
                    vault.deposit(((Player) sender),(Double.parseDouble(args[1])));
                }).start();
            }
        }
        if (args[0].equals("join")){

            if (args.length !=2) {
                sender.sendMessage(Component.text("§e§l[CF]使い方が間違ってます"));
                return true;
            }
            Player player = Bukkit.getPlayer(args[1]);
            if (player == null){
                sender.sendMessage("§e§l[CF]プレイヤーが存在しない、またはオフラインです");
                return true;
            }
            if (!coindata.containsKey(player.getUniqueId())) {
                sender.sendMessage("§e§l[CF]その部屋は存在しません");
                return true;
            }
            if (player == sender){
                sender.sendMessage("§e§l[CF]自分の部屋には入れません");
                return true;
            }
            Double bet = coindata.get(player.getUniqueId()).bet;
            if (vault.getBalance(((Player) sender).getUniqueId()) < bet) {
                sender.sendMessage("§e§l[CF]所持金が足りません");
                return true;
            }
            vault.withdraw(((Player) sender).getUniqueId(), bet);
            boolean maincoin = coindata.get(player.getUniqueId()).heads;


            Inventory inv = Bukkit.createInventory(null, 45, Component.text("§6§lCoinFlip"));
            ((Player) sender).openInventory(inv);
            player.openInventory(inv);
            new Thread(() -> {
                coindata.remove(player.getUniqueId());
                for (int i = 0;i<9;i++) {
                    inv.setItem(i, new ItemStack(Material.WHITE_STAINED_GLASS_PANE));
                    inv.setItem(i + 36, new ItemStack(Material.WHITE_STAINED_GLASS_PANE));
                }
                for (int i = 11;i<16;i+=4) {
                    inv.setItem(i, new ItemStack(Material.PINK_STAINED_GLASS_PANE));
                    inv.setItem(i + 9, new ItemStack(Material.WHITE_STAINED_GLASS_PANE));
                    inv.setItem(i + 18, new ItemStack(Material.LIME_STAINED_GLASS_PANE));
                }
                for (int i = 1;i<3;i++) {
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta)head.getItemMeta();
                    if (i == 1){
                        meta.setOwningPlayer(player);
                    }
                    else {
                        meta.setOwningPlayer((Player)sender);
                    }
                    head.setItemMeta(meta);
                    if (i == 1){
                        inv.setItem(17,head);
                    }
                    else {
                        inv.setItem(27,head);
                    }
                }
                int heads = config.getInt("heads");
                int tails = config.getInt("tails");
                ItemStack headsitem = new ItemStack(Material.IRON_NUGGET);
                ItemMeta headsmeta = headsitem.getItemMeta();
                headsmeta.setCustomModelData(heads);
                headsmeta.displayName(Component.text("§a§l予想：表"));
                headsitem.setItemMeta(headsmeta);
                ItemStack tailsitem = new ItemStack(Material.IRON_NUGGET);
                ItemMeta tailsmeta = tailsitem.getItemMeta();
                tailsmeta.setCustomModelData(tails);
                tailsmeta.displayName(Component.text("§b§l予想：裏"));
                tailsitem.setItemMeta(tailsmeta);
                ItemStack item = new ItemStack(Material.IRON_NUGGET);
                ItemMeta meta = item.getItemMeta();
                meta.setCustomModelData(tails);
                item.setItemMeta(meta);
                inv.setItem(22, item);
                boolean change = true;
                if (maincoin) {
                    inv.setItem(16, headsitem);
                    inv.setItem(28, tailsitem);
                }

                else {
                    inv.setItem(16, tailsitem);
                    inv.setItem(28, headsitem);
                }

                for (int i = 0; i<new Random().nextInt(14)+10; i++) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.5f, 1f);
                    item = inv.getItem(22);
                    meta = item.getItemMeta();
                    if (!change) {
                        meta.setCustomModelData(heads);
                        meta.displayName(Component.text("§a§l表"));
                    }

                    else {
                        meta.setCustomModelData(tails);
                        meta.displayName(Component.text("§b§l裏"));
                    }

                    for (int t = 11;t<16;t+=4) {
                        inv.setItem(t, new ItemStack(panelColor()));
                        inv.setItem(t + 9, new ItemStack(panelColor()));
                        inv.setItem(t + 18, new ItemStack(panelColor()));
                    }

                    item.setItemMeta(meta);
                    change = !change;
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

                if (maincoin == change) {
                    if (change) {
                        sender.sendMessage(Component.text("§6§l[CF]" + player.getName() + "が表の予想を当てました！"));
                        player.sendMessage(Component.text("§6§l[CF]" + player.getName() + "が表の予想を当てました！"));
                    } else {
                        sender.sendMessage(Component.text("§6§l[CF]" + player.getName() + "が裏の予想を当てました！"));
                        player.sendMessage(Component.text("§6§l[CF]" + player.getName() + "が裏の予想を当てました！"));
                    }

                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1f, 1f);
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta winnerMeta = (SkullMeta) head.getItemMeta();
                    winnerMeta.setOwningPlayer(player);
                    head.setItemMeta(winnerMeta);
                    inv.setItem(13, head);
                    vault.deposit(player, bet * 2);

                } else {
                    if (change) {
                        sender.sendMessage(Component.text("§6§l[CF]" + sender.getName() + "が表の予想を当てました！"));
                        player.sendMessage(Component.text("§6§l[CF]" + sender.getName() + "が表の予想を当てました！"));
                    }

                    else {
                        sender.sendMessage(Component.text("§6§l[CF]" + sender.getName() + "が裏の予想を当てました！"));
                        player.sendMessage(Component.text("§6§l[CF]" + sender.getName() + "が裏の予想を当てました！"));
                    }

                    ((Player)sender).playSound(((Player)sender).getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1f, 1f);
                    ItemStack head = new  ItemStack(Material.PLAYER_HEAD);
                    SkullMeta winner2meta = (SkullMeta)head.getItemMeta();
                    winner2meta.setOwningPlayer((Player)sender);
                    head.setItemMeta(winner2meta);
                    inv.setItem(13, head);
                    vault.deposit(((Player)sender), bet * 2);
                }
                for (int i = 11;i<16;i+=4) {
                    inv.setItem(i, new ItemStack(Material.PINK_STAINED_GLASS_PANE));
                    inv.setItem(i + 9, new ItemStack(Material.WHITE_STAINED_GLASS_PANE));
                    inv.setItem(i + 18, new ItemStack(Material.LIME_STAINED_GLASS_PANE));
                }
                try {
                    sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Bukkit.getScheduler().runTask(this, new Runnable (){
                    @Override
                    public void run(){

                        ((Player) sender).closeInventory();
                        player.closeInventory();
                    }
                });
            }).start();
        }

        if (args[0].equals("help")) {
            sender.sendMessage(
                    "§f§l＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝\n" +
                            "§6§l/cf create [金額] [heads(表) or tails(表)で\n" +
                            "§6§l部屋を作成することができます。\n" +
                            "§e§l注意:部屋を複数立てることはできません。\n" +
                            "§6§l/cf join [Player]で参加できます。\n" +
                            "§6§lまた募集してるところをクリックしても参加できます。\n" +
                            "§f§l＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝"
            );
        }
        return true;
    }
    private void  sendHoverText(String msg,String hover,String cmd) {
        Component message = Component.text(msg).hoverEvent(HoverEvent.showText(Component.text(hover))).clickEvent(ClickEvent.runCommand(cmd));
        Bukkit.broadcast(message, Server.BROADCAST_CHANNEL_USERS);
    }
}

class GameData{
    double bet;
    boolean heads;

    GameData(double bet,boolean heads){
        this.bet=bet;
        this.heads=heads;
    }

}
