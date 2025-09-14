package io.papermc.entitycarry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.WaterMob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * EntityCarry Plugin - Allows players to carry entities and other players
 * 
 * Features:
 * - Carry entities by sneaking and right-clicking
 * - Player-to-player carrying with consent system
 * - Configurable cooldowns and timeouts
 * - Fine-grained permission system
 * - Safe entity placement
 * 
 * @author KernelKraze
 * @version 1.0.1
 */
public final class EntityCarryPlugin extends JavaPlugin implements Listener {

    // Thread-safe data structures for concurrent access
    private final Map<UUID, Instant> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> carryingStates = ConcurrentHashMap.newKeySet();
    private final BiMap<UUID, UUID> pendingRequests = Maps.synchronizedBiMap(HashBiMap.create());
    private final Set<UUID> allowBeCarried = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Instant> lastSneakTime = new ConcurrentHashMap<>();

    // Configuration cache - loaded once, accessed frequently
    private Duration baseCooldown;
    private Duration playerCooldown;
    private Duration carryTimeout;
    private Set<EntityType> blacklist;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            reloadConfig();
            getServer().getPluginManager().registerEvents(this, this);
            
            // Register commands with null checks
            Arrays.asList("carryaccept", "carryreload", "carrytoggle")
                  .forEach(cmd -> {
                      var command = getCommand(cmd);
                      if (command != null) {
                          command.setExecutor(this);
                      } else {
                          getLogger().warning("Failed to register command: " + cmd);
                      }
                  });
            
            getLogger().info("EntityCarry plugin enabled successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable EntityCarry plugin", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Clean shutdown - release all carried entities
        Bukkit.getOnlinePlayers().forEach(this::releasePassengers);
        
        // Cancel all pending tasks to prevent memory leaks
        Bukkit.getScheduler().cancelTasks(this);
        
        // Clear all data structures
        cooldowns.clear();
        carryingStates.clear();
        pendingRequests.clear();
        allowBeCarried.clear();
        lastSneakTime.clear();
        
        getLogger().info("EntityCarry plugin disabled - all data cleared");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        FileConfiguration config = getConfig();
        
        try {
            // Load timing configurations with validation
            baseCooldown = Duration.ofMillis(Math.max(0, config.getLong("cooldown.base", 1000)));
            playerCooldown = Duration.ofMillis(Math.max(0, config.getLong("cooldown.player", 3000)));
            carryTimeout = Duration.ofMillis(Math.max(1000, config.getLong("carry-timeout", 3000)));
            
            // Load and validate blacklist
            blacklist = EnumSet.noneOf(EntityType.class);
            config.getStringList("blacklist").forEach(entityName -> {
                try {
                    blacklist.add(EntityType.valueOf(entityName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid entity type in blacklist: " + entityName);
                }
            });
            
            getLogger().info("Configuration reloaded successfully");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error loading configuration, using defaults", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        // Only process if player is in carrying mode
        if (!carryingStates.contains(player.getUniqueId())) {
            return;
        }

        try {
            if (entity instanceof Player targetPlayer) {
                handlePlayerCarryRequest(player, targetPlayer);
            } else {
                handleEntityCarry(player, entity);
            }
            event.setCancelled(true);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error handling entity interaction", e);
            sendActionBar(player, "§cError occurred, please try again");
        }
    }

    /**
     * Handle player-to-player carry requests with consent system
     */
    private void handlePlayerCarryRequest(Player requester, Player target) {
        // Permission validation
        if (!hasPermission(requester, "entitycarry.carry.player")) {
            sendActionBar(requester, "§cNo permission to carry players");
            return;
        }
        
        if (!canBeCarried(target)) {
            sendActionBar(requester, "§cPlayer does not allow being carried");
            return;
        }

        // Prevent self-carry
        if (requester.equals(target)) {
            sendActionBar(requester, "§cCannot carry yourself");
            return;
        }

        // Send consent request with timeout
        synchronized (pendingRequests) {
            pendingRequests.put(requester.getUniqueId(), target.getUniqueId());
        }
        
        requester.sendMessage(Component.text("§aSent carry request to " + target.getName() + ""));
        target.sendMessage(Component.text("§ePlayer " + requester.getName() + " wants to carry you, type §a/carryaccept §eto accept"));
        
        // Auto-expire request after 30 seconds
        Bukkit.getScheduler().runTaskLater(this, () -> {
            synchronized (pendingRequests) {
                if (pendingRequests.inverse().remove(target.getUniqueId()) != null) {
                    requester.sendMessage(Component.text("§cCarry request to " + target.getName() + " expired"));
                }
            }
        }, 600L); // 30 seconds
    }

    /**
     * Handle entity carrying with comprehensive validation
     */
    private void handleEntityCarry(Player player, Entity entity) {
        // Basic permission and cooldown checks
        if (!hasPermission(player, "entitycarry.use") || isOnCooldown(player, entity)) {
            return;
        }
        
        // Entity state validation
        if (entity.isDead() || !entity.getPassengers().isEmpty()) {
            sendActionBar(player, "§cEntity cannot be carried");
            return;
        }
        
        // Blacklist check
        if (blacklist.contains(entity.getType())) {
            sendActionBar(player, "§cEntity type is blacklisted");
            return;
        }

        // Fine-grained permission check
        if (!checkCarryPermission(player, entity)) {
            sendActionBar(player, "§cNo permission to carry this entity type");
            return;
        }

        // Execute carry operation
        if (player.getPassengers().isEmpty()) {
            player.addPassenger(entity);
            sendActionBar(player, "§aCarrying: " + getEntityDisplayName(entity));
            setCooldown(player, entity);
            carryingStates.remove(player.getUniqueId()); // Auto-disable carry mode
        } else {
            sendActionBar(player, "§cAlready carrying another entity");
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (!event.isSneaking()) {
            return;
        }

        // Early permission check to prevent unnecessary processing
        if (!hasPermission(player, "entitycarry.use")) {
            return;
        }

        try {
            if (isCarrying(player)) {
                releasePassengers(player);
                return;
            }

            // Double-sneak activation with improved timing and thread safety
            Instant now = Instant.now();
            Instant lastSneak = lastSneakTime.get(playerId);
            
            if (lastSneak != null && Duration.between(lastSneak, now).toMillis() < 500) {
                // Double sneak detected within 500ms - activate carry mode
                lastSneakTime.remove(playerId);
                toggleCarryMode(player);
            } else {
                // First sneak - record time
                lastSneakTime.put(playerId, now);
                
                // Clear the record after 500ms if no second sneak occurred
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    // Remove the entry only if it still contains the same timestamp
                    // This prevents removing a newer timestamp from a subsequent sneak
                    Instant storedTime = lastSneakTime.get(playerId);
                    if (storedTime != null && storedTime.equals(now)) {
                        lastSneakTime.remove(playerId);
                    }
                }, 10L); // 500ms = 10 ticks
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error handling sneak event for player " + player.getName(), e);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // Clean up all player data to prevent memory leaks
        cooldowns.remove(playerId);
        carryingStates.remove(playerId);
        allowBeCarried.remove(playerId);
        lastSneakTime.remove(playerId);
        
        // Clean up any pending requests involving this player
        synchronized (pendingRequests) {
            pendingRequests.remove(playerId);
            pendingRequests.inverse().remove(playerId);
        }
        
        // Release any passengers this player was carrying
        releasePassengers(event.getPlayer());
    }

    /**
     * Check if player is currently carrying any entities
     */
    private boolean isCarrying(Player player) {
        return !player.getPassengers().isEmpty();
    }

    /**
     * Toggle carry mode with timeout mechanism and improved validation
     */
    private void toggleCarryMode(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Permission already checked in sneak handler, this is just defensive
        if (!hasPermission(player, "entitycarry.use")) {
            // Don't show message here - permission was already checked earlier
            return;
        }

        // Check if player is already in carry mode
        if (carryingStates.contains(playerId)) {
            // Player is already in carry mode, disable it instead
            carryingStates.remove(playerId);
            sendActionBar(player, "§cCarry Mode Deactivated");
            return;
        }

        // Activate carry mode
        if (carryingStates.add(playerId)) {
            sendActionBar(player, "§aCarry Mode Active §7(Right-click entities to carry)");
            
            // Schedule timeout task with proper player validation
            Bukkit.getScheduler().runTaskLater(this, () -> {
                // Only timeout if player is still in carry mode
                if (carryingStates.remove(playerId)) {
                    // Check if player is still online before sending message
                    Player onlinePlayer = Bukkit.getPlayer(playerId);
                    if (onlinePlayer != null && onlinePlayer.isOnline()) {
                        sendActionBar(onlinePlayer, "§eCarry Mode Expired");
                    }
                }
            }, Math.max(1L, carryTimeout.toMillis() / 50)); // Convert to ticks, minimum 1 tick
        }
    }

    /**
     * Safely release all passengers with proper placement
     */
    private void releasePassengers(Player player) {
        List<Entity> passengers = new ArrayList<>(player.getPassengers());
        if (passengers.isEmpty()) {
            return;
        }

        Location safeLoc = findSafeLocation(player.getLocation());
        int released = 0;
        
        for (Entity passenger : passengers) {
            try {
                player.removePassenger(passenger);
                passenger.teleport(safeLoc);
                released++;
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to release passenger: " + passenger.getType(), e);
            }
        }
        
        if (released > 0) {
            sendActionBar(player, "§cReleased " + released + " entities");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("§cThis command can only be used by players"));
            return true;
        }

        try {
            switch (cmd.getName().toLowerCase()) {
                case "carryaccept" -> handleAcceptCommand(player);
                case "carryreload" -> handleReloadCommand(sender);
                case "carrytoggle" -> handleToggleCommand(player);
                default -> sender.sendMessage(Component.text("§cUnknown command"));
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error executing command: " + cmd.getName(), e);
            sender.sendMessage(Component.text("§cCommand execution failed, please try again"));
        }
        
        return true;
    }

    /**
     * Handle carry accept command with validation
     */
    private void handleAcceptCommand(Player target) {
        UUID requesterId;
        synchronized (pendingRequests) {
            requesterId = pendingRequests.inverse().remove(target.getUniqueId());
        }
        
        if (requesterId == null) {
            target.sendMessage(Component.text("§cNo pending carry requests"));
            return;
        }

        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null) {
            target.sendMessage(Component.text("§cRequesting player is offline"));
            return;
        }

        if (!requester.getPassengers().isEmpty()) {
            target.sendMessage(Component.text("§cPlayer is already carrying another entity"));
            return;
        }

        // Execute the carry
        requester.addPassenger(target);
        requester.sendMessage(Component.text("§a" + target.getName() + " accepted the carry request"));
        target.sendMessage(Component.text("§aYou are being carried by " + requester.getName() + ""));
    }

    /**
     * Handle configuration reload command
     */
    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("entitycarry.admin")) {
            sender.sendMessage(Component.text("§cNo permission to execute this command"));
            return;
        }

        reloadConfig();
        sender.sendMessage(Component.text("§aConfiguration reloaded"));
    }

    /**
     * Handle carry toggle command
     */
    private void handleToggleCommand(Player player) {
        boolean newState = !allowBeCarried.contains(player.getUniqueId());
        
        if (newState) {
            allowBeCarried.add(player.getUniqueId());
        } else {
            allowBeCarried.remove(player.getUniqueId());
        }
        
        player.sendMessage(Component.text("§aCarry status " + (newState ? "enabled" : "disabled") + ""));
    }

    // Utility Methods

    /**
     * Check if player is on cooldown for carrying entities
     */
    private boolean isOnCooldown(Player player, Entity entity) {
        if (hasPermission(player, "entitycarry.bypass.cooldown")) {
            return false;
        }
        
        Duration cooldown = (entity instanceof Player) ? playerCooldown : baseCooldown;
        Instant lastUsed = cooldowns.get(player.getUniqueId());
        
        if (lastUsed != null && Instant.now().isBefore(lastUsed.plus(cooldown))) {
            Duration remaining = Duration.between(Instant.now(), lastUsed.plus(cooldown));
            sendActionBar(player, "§cCooldown: " + remaining.getSeconds() + "s remaining");
            return true;
        }
        
        return false;
    }

    /**
     * Check fine-grained carry permissions for specific entity types
     */
    private boolean checkCarryPermission(Player player, Entity entity) {
        // Wildcard permission grants all access
        if (hasPermission(player, "entitycarry.entity.*")) {
            return true;
        }
        
        // Specific entity type permission
        String entityType = entity.getType().name().toLowerCase();
        if (hasPermission(player, "entitycarry.entity." + entityType)) {
            return true;
        }
        
        // Category-based permissions
        if (isPassive(entity) && hasPermission(player, "entitycarry.type.passive")) {
            return true;
        }
        
        if (isHostile(entity) && hasPermission(player, "entitycarry.type.hostile")) {
            return true;
        }
        
        return false;
    }

    /**
     * Check if entity is considered passive
     */
    private boolean isPassive(Entity entity) {
        return entity instanceof Animals || 
               entity instanceof WaterMob || 
               entity instanceof Ambient;
    }

    /**
     * Check if entity is considered hostile
     */
    private boolean isHostile(Entity entity) {
        return entity instanceof Monster;
    }

    /**
     * Check if target player can be carried
     */
    private boolean canBeCarried(Player target) {
        return hasPermission(target, "entitycarry.be_carried") || 
               allowBeCarried.contains(target.getUniqueId());
    }

    /**
     * Set cooldown for player after carrying an entity
     */
    private void setCooldown(Player player, Entity entity) {
        cooldowns.put(player.getUniqueId(), Instant.now());
    }

    /**
     * Find a safe location to place entities when released
     */
    private Location findSafeLocation(Location origin) {
        Location testLoc = origin.clone();
        
        // Try locations above the origin
        for (int y = 0; y < 3; y++) {
            testLoc.add(0, 1, 0);
            Block block = testLoc.getBlock();
            Block above = block.getRelative(BlockFace.UP);
            
            if (block.getType().isAir() && above.getType().isAir()) {
                return testLoc;
            }
        }
        
        // Fallback to highest block at location
        return origin.getWorld().getHighestBlockAt(origin).getLocation().add(0, 1, 0);
    }

    /**
     * Enhanced permission check with wildcard support
     */
    private boolean hasPermission(Player player, String permission) {
        return player.hasPermission("entitycarry.*") || player.hasPermission(permission);
    }

    /**
     * Get display name for entity (handles custom names)
     */
    private String getEntityDisplayName(Entity entity) {
        Component customName = entity.customName();
        if (customName != null) {
            return PlainTextComponentSerializer.plainText().serialize(customName);
        }
        return entity.getType().name().toLowerCase().replace('_', ' ');
    }

    /**
     * Send action bar message to player
     */
    private void sendActionBar(Player player, String message) {
        player.sendActionBar(Component.text(message));
    }
}