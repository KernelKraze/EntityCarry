// package io.papermc.testplugin;

// import org.bukkit.entity.Entity;
// import org.bukkit.entity.Player;
// import org.bukkit.entity.Villager;
// import org.bukkit.event.EventHandler;
// import org.bukkit.event.EventPriority;
// import org.bukkit.event.Listener;
// import org.bukkit.event.player.PlayerInteractEntityEvent;
// import org.bukkit.event.player.PlayerToggleSneakEvent;
// import org.bukkit.plugin.java.JavaPlugin;
// import org.bukkit.scheduler.BukkitRunnable;

// import java.util.HashMap;
// import java.util.UUID;

// public final class TestPlugin extends JavaPlugin implements Listener {
//     private final HashMap<UUID, Long> cooldowns = new HashMap<>();
//     private final HashMap<UUID, Boolean> carryingStates = new HashMap<>();
//     private final HashMap<UUID, Long> activationTimes = new HashMap<>(); // 新增：记录激活时间
//     private static final long COOLDOWN_TIME = 1000;
//     private static final long CARRY_TIMEOUT = 3000; // 3秒超时

//     @Override
//     public void onEnable() {
//         getServer().getPluginManager().registerEvents(this, this);
//         saveDefaultConfig();
//         getLogger().info("实体搬运插件已加载！");
//     }

//     @EventHandler(priority = EventPriority.HIGH)
//     public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
//         Player player = event.getPlayer();
//         Entity entity = event.getRightClicked();
//         UUID playerId = player.getUniqueId();

//         // 检查是否超时
//         if (carryingStates.getOrDefault(playerId, false)) {
//             long activationTime = activationTimes.getOrDefault(playerId, 0L);
//             if (System.currentTimeMillis() - activationTime > CARRY_TIMEOUT) {
//                 carryingStates.put(playerId, false);
//                 activationTimes.remove(playerId);
//                 sendActionBar(player, "§c搬运模式已超时");
//                 return;
//             }
//         }

//         // 如果是村民且玩家没有搬运状态,允许正常交互
//         if (entity instanceof Villager && !carryingStates.getOrDefault(playerId, false)) {
//             return;
//         }

//         // 基础权限检查
//         if (!player.hasPermission("entitycarry.use")) {
//             if (carryingStates.getOrDefault(playerId, false)) {
//                 sendActionBar(player, "§c你没有权限使用此功能");
//                 carryingStates.put(playerId, false);
//                 activationTimes.remove(playerId);
//             }
//             return;
//         }

//         // 实体特定权限检查
//         String entityType = entity.getType().toString().toLowerCase();
//         if (!player.hasPermission("entitycarry.entity." + entityType)) {
//             if (carryingStates.getOrDefault(playerId, false)) {
//                 sendActionBar(player, "§c你没有权限搬运此类实体");
//             }
//             return;
//         }

//         if (!carryingStates.getOrDefault(playerId, false)) {
//             return;
//         }

//         // 管理员跳过冷却
//         if (!player.hasPermission("entitycarry.bypass.cooldown")) {
//             if (cooldowns.containsKey(playerId)) {
//                 long timeLeft = cooldowns.get(playerId) + COOLDOWN_TIME - System.currentTimeMillis();
//                 if (timeLeft > 0) {
//                     sendActionBar(player, "§c请等待 " + (timeLeft / 1000) + " 秒后再试");
//                     return;
//                 }
//             }
//         }

//         if (canCarry(entity)) {
//             if (player.getPassengers().isEmpty()) {
//                 player.addPassenger(entity);
//                 event.setCancelled(true);
//                 sendActionBar(player, "§a已抱起: " + entity.getName());
//                 cooldowns.put(playerId, System.currentTimeMillis());
//                 carryingStates.put(playerId, false); // 搬运后自动关闭模式
//                 activationTimes.remove(playerId);
//             }
//         }
//     }

//     @EventHandler
//     public void onPlayerSneak(PlayerToggleSneakEvent event) {
//         Player player = event.getPlayer();
//         UUID playerId = player.getUniqueId();
        
//         // 基础权限检查
//         if (!player.hasPermission("entitycarry.use")) {
//             if (carryingStates.getOrDefault(playerId, false)) {
//                 sendActionBar(player, "§c你没有权限使用此功能");
//                 carryingStates.put(playerId, false);
//                 activationTimes.remove(playerId);
//             }
//             return;
//         }

//         if (event.isSneaking()) {
//             boolean newState = !carryingStates.getOrDefault(playerId, false);
//             carryingStates.put(playerId, newState);

//             if (newState) {
//                 // 激活搬运模式时记录时间
//                 activationTimes.put(playerId, System.currentTimeMillis());
//                 sendActionBar(player, "§a已开启抱起模式 §7(3秒内有效)");
                
//                 // 设置超时任务
//                 new BukkitRunnable() {
//                     @Override
//                     public void run() {
//                         if (carryingStates.getOrDefault(playerId, false)) {
//                             carryingStates.put(playerId, false);
//                             activationTimes.remove(playerId);
//                             sendActionBar(player, "§c搬运模式已超时");
//                         }
//                     }
//                 }.runTaskLater(this, 60L); // 3秒 = 60 ticks
//             } else if (!player.getPassengers().isEmpty()) {
//                 // 放下实体
//                 new BukkitRunnable() {
//                     @Override
//                     public void run() {
//                         for (Entity passenger : player.getPassengers()) {
//                             player.removePassenger(passenger);
//                             passenger.teleport(player.getLocation());
//                         }
//                         sendActionBar(player, "§c已放下实体");
//                     }
//                 }.runTaskLater(this, 1L);
//                 activationTimes.remove(playerId);
//             }
//         }
//     }

//     private boolean canCarry(Entity entity) {
//         return !(entity instanceof Player) &&
//                !entity.isDead() &&
//                entity.getPassengers().isEmpty() &&
//                !isBlacklisted(entity);
//     }

//     private boolean isBlacklisted(Entity entity) {
//         return entity.getType().toString().matches("(?i)ENDER_DRAGON|WITHER|SHULKER");
//     }

//     private void sendActionBar(Player player, String message) {
//         player.sendTitle("", message, 5, 20, 5);
//     }
// }

package io.papermc.testplugin;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TestPlugin extends JavaPlugin implements Listener {

    // 使用更高效的数据结构
    private final Map<UUID, Instant> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> carryingStates = ConcurrentHashMap.newKeySet();
    private final BiMap<UUID, UUID> pendingRequests = HashBiMap.create();
    private final Set<UUID> allowBeCarried = ConcurrentHashMap.newKeySet();

    // 配置项
    private Duration baseCooldown;
    private Duration playerCooldown;
    private Duration carryTimeout;
    private Set<EntityType> blacklist;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        
        // 注册命令
        Arrays.asList("carryaccept", "carryreload", "carrytoggle")
              .forEach(cmd -> Objects.requireNonNull(getCommand(cmd)).setExecutor(this));
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        FileConfiguration config = getConfig();
        
        baseCooldown = Duration.ofMillis(config.getLong("cooldown.base", 1000));
        playerCooldown = Duration.ofMillis(config.getLong("cooldown.player", 3000));
        carryTimeout = Duration.ofMillis(config.getLong("carry-timeout", 3000));
        
        // 优化黑名单处理
        blacklist = EnumSet.noneOf(EntityType.class);
        config.getStringList("blacklist").forEach(s -> {
            try {
                blacklist.add(EntityType.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException e) {
                getLogger().warning("无效的实体类型: " + s);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (!carryingStates.contains(player.getUniqueId())) return;

        if (entity instanceof Player) {
            handlePlayerCarryRequest(player, (Player) entity);
        } else {
            handleEntityCarry(player, entity);
        }
        event.setCancelled(true);
    }

    private void handlePlayerCarryRequest(Player requester, Player target) {
        // 权限检查
        if (!hasPermission(requester, "entitycarry.carry.player")) {
            sendActionBar(requester, "§c你没有搬运玩家的权限");
            return;
        }
        
        if (!canBeCarried(target)) {
            sendActionBar(requester, "§c该玩家不允许被搬运");
            return;
        }

        // 发送带超时的请求
        pendingRequests.put(requester.getUniqueId(), target.getUniqueId());
        requester.sendMessage(Component.text("§a已向 " + target.getName() + " 发送搬运请求"));
        target.sendMessage(Component.text("§e玩家 " + requester.getName() + " 想搬运你，输入 §a/carryaccept §e接受"));
        
        // 30秒后清除请求
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (pendingRequests.inverse().remove(target.getUniqueId()) != null) {
                requester.sendMessage(Component.text("§c搬运请求已过期"));
            }
        }, 600L);
    }

    private void handleEntityCarry(Player player, Entity entity) {
        // 基础检查
        if (!hasPermission(player, "entitycarry.use") || isOnCooldown(player, entity)) return;
        
        // 类型检查
        if (blacklist.contains(entity.getType())) {
            sendActionBar(player, "§c该实体不可搬运");
            return;
        }

        // 权限细粒度检查
        if (!checkCarryPermission(player, entity)) return;

        // 执行搬运
        if (player.getPassengers().isEmpty()) {
            player.addPassenger(entity);
            sendActionBar(player, "§a已抱起: " + entity.getName());
            setCooldown(player, entity);
            carryingStates.remove(player.getUniqueId()); // 搬运后关闭模式
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking()) {
            if (isCarrying(player)) {
                releasePassengers(player);
            } else {
                toggleCarryMode(player);
            }
        }
    }

    private boolean isCarrying(Player player) {
        return !player.getPassengers().isEmpty();
    }

    private void toggleCarryMode(Player player) {
        if (carryingStates.add(player.getUniqueId())) {
            sendActionBar(player, "§a搬运模式已激活 §7(右键点击实体)");
            
            // 超时检测
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (carryingStates.remove(player.getUniqueId())) {
                    sendActionBar(player, "§c搬运模式已超时");
                }
            }, carryTimeout.toMillis() / 50);
        }
    }

    private void releasePassengers(Player player) {
        List<Entity> passengers = new ArrayList<>(player.getPassengers());
        if (!passengers.isEmpty()) {
            Location safeLoc = findSafeLocation(player.getLocation());
            passengers.forEach(p -> {
                p.teleport(safeLoc);
                player.removePassenger(p);
            });
            sendActionBar(player, "§c已释放 " + passengers.size() + " 个实体");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "carryaccept" -> handleAcceptCommand((Player) sender);
            case "carryreload" -> handleReloadCommand(sender);
            case "carrytoggle" -> handleToggleCommand((Player) sender);
        }
        return true;
    }

    private void handleAcceptCommand(Player target) {
        UUID requesterId = pendingRequests.inverse().remove(target.getUniqueId());
        if (requesterId != null) {
            Player requester = Bukkit.getPlayer(requesterId);
            if (requester != null && requester.getPassengers().isEmpty()) {
                requester.addPassenger(target);
                requester.sendMessage(Component.text("§a" + target.getName() + " 接受了搬运请求"));
            }
        }
    }

    private void handleReloadCommand(CommandSender sender) {
        reloadConfig();
        sender.sendMessage(Component.text("§a配置已重载"));
    }

    private void handleToggleCommand(Player player) {
        boolean newState = !allowBeCarried.contains(player.getUniqueId());
        if (newState) {
            allowBeCarried.add(player.getUniqueId());
        } else {
            allowBeCarried.remove(player.getUniqueId());
        }
        player.sendMessage(Component.text("§a已" + (newState ? "启用" : "禁用") + "被搬运状态"));
    }

    // 实用方法
    private boolean isOnCooldown(Player player, Entity entity) {
        if (hasPermission(player, "entitycarry.bypass.cooldown")) return false;
        
        Duration cooldown = (entity instanceof Player) ? playerCooldown : baseCooldown;
        Instant lastUsed = cooldowns.get(player.getUniqueId());
        
        if (lastUsed != null && Instant.now().isBefore(lastUsed.plus(cooldown))) {
            Duration remaining = Duration.between(Instant.now(), lastUsed.plus(cooldown));
            sendActionBar(player, "§c冷却剩余: " + remaining.getSeconds() + "秒");
            return true;
        }
        return false;
    }

    private boolean checkCarryPermission(Player player, Entity entity) {
        // 通配符权限
        if (hasPermission(player, "entitycarry.entity.*")) return true;
        
        // 具体类型权限
        String type = entity.getType().name().toLowerCase();
        if (hasPermission(player, "entitycarry.entity." + type)) return true;
        
        // 类别权限
        if (isPassive(entity) && hasPermission(player, "entitycarry.type.passive")) return true;
        if (isHostile(entity) && hasPermission(player, "entitycarry.type.hostile")) return true;
        
        return false;
    }

    private boolean isPassive(Entity entity) {
        return entity instanceof Animals || entity instanceof WaterMob;
    }

    private boolean isHostile(Entity entity) {
        return entity instanceof Monster;
    }

    private boolean canBeCarried(Player target) {
        return hasPermission(target, "entitycarry.be_carried") || allowBeCarried.contains(target.getUniqueId());
    }

    private void setCooldown(Player player, Entity entity) {
        cooldowns.put(player.getUniqueId(), Instant.now());
    }

    private Location findSafeLocation(Location origin) {
        Location testLoc = origin.clone();
        for (int y = 0; y < 3; y++) {
            testLoc.add(0, 1, 0);
            Block block = testLoc.getBlock();
            if (block.getType().isAir() && block.getRelative(BlockFace.UP).getType().isAir()) {
                return testLoc;
            }
        }
        return origin.getWorld().getHighestBlockAt(origin).getLocation().add(0, 1, 0);
    }

    private boolean hasPermission(Player player, String permission) {
        return player.hasPermission("entitycarry.*") || player.hasPermission(permission);
    }

    private void sendActionBar(Player player, String message) {
        player.sendActionBar(Component.text(message));
    }
}