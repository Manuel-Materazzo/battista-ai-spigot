package org.itsmanu.battistaAiSpigot.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.itsmanu.battistaAiSpigot.listeners.PlayerTabListener;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class TabUtil {
    private static final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    private static final String FAKE_PLAYER_NAME = "AI-Helper";
    private static PlayerTabListener tabListener;

    private static boolean useNewRemovePacket = false;

    private static final UUID helperUUID = UUID.nameUUIDFromBytes(FAKE_PLAYER_NAME.getBytes());
    private static final WrappedGameProfile helperProfile = new WrappedGameProfile(helperUUID, FAKE_PLAYER_NAME);

    private static final Logger logger = BattistaAiSpigot.getInstance().getLogger();

    private TabUtil() {
    }

    /**
     * Enables the tab feature by registering the PlayerTabListener and refreshing the AI helper
     * for all online players. This method checks if ProtocolLib is available before proceeding.
     */
    public static void enableTabFeature() {
        // Check if ProtocolLib is available
        boolean protocolibAvailable = DependencyUtil.checkProtocolLib();

        if (!protocolibAvailable) {
            logger.warning("ProtocolLib is not available. Battista AI Tab feature will not be enabled.");
            return;
        }

        if (tabListener == null) {
            tabListener = new PlayerTabListener();

            // Register listener dynamically
            var plugin = BattistaAiSpigot.getInstance();
            plugin.getServer().getPluginManager().registerEvents(tabListener, plugin);

            // Check if the new PLAYER_INFO_REMOVE packet type exists (1.19.4+)
            useNewRemovePacket = DependencyUtil.isPacketTypeAvailable(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        }

        // Refresh tablist for existing players
        refreshAIHelper();
    }

    /**
     * Disables the tab feature by removing the AI helper from all online players and unregistering
     * the PlayerTabListener. This method ensures all tab-related functionality is properly cleaned up.
     */
    public static void disableTabFeature() {
        if (tabListener != null) {
            // Remove from all players first
            for (Player player : Bukkit.getOnlinePlayers()) {
                cleanupAIHelper(player);
            }

            // Unregister listener dynamically
            if (tabListener != null) {
                HandlerList.unregisterAll(tabListener);
            }

            tabListener = null;
        }
    }

    /**
     * Adds the AI helper to the player's tab list with a specified status.
     *
     * @param viewer The player who will see the AI helper in their tab list
     */
    public static void addAIHelper(Player viewer) {
        // Create player info packet for 1.21+
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);

        // Set action to ADD_PLAYER
        packet.getPlayerInfoActions().write(0, EnumSet.of(
                EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME,
                EnumWrappers.PlayerInfoAction.UPDATE_LISTED
        ));

        String displayName = BattistaAiSpigot.getConfigs().getString("tab.name", "Battista AI");
        // colorize name
        displayName = displayName.replace("&", "§");

        // Create player info data
        List<PlayerInfoData> playerData = Collections.singletonList(
                new PlayerInfoData(
                        helperProfile,
                        0, // ping
                        EnumWrappers.NativeGameMode.CREATIVE,
                        WrappedChatComponent.fromText(displayName)
                )
        );

        packet.getPlayerInfoDataLists().write(1, playerData);

        // Send packet
        protocolManager.sendServerPacket(viewer, packet);
    }

    /**
     * Refreshes the fake player entry in the tab list for all online players.
     * This method first removes any existing fake player entries, then re-adds them
     * with the current status message after a short delay to ensure proper initialization.
     */
    public static void refreshAIHelper() {
        // Wait 5 ticks for everything to initialize
        Bukkit.getScheduler().runTaskLater(BattistaAiSpigot.getInstance(), () -> {
            // set new skin to AI helper
            setSkinToAiHelper().thenRun(() -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // remove existing AI helper entry
                    cleanupAIHelper(player);
                    // Wait a tick, then re-add
                    Bukkit.getScheduler().runTaskLater(BattistaAiSpigot.getInstance(), () -> {
                        addAIHelper(player);
                    }, 1L);
                }
            });
        }, 5L);
    }

    /**
     * Removes the AI helper from the player's tab list.
     *
     * @param viewer The player from whose tab list the AI helper should be removed
     */
    private static void cleanupAIHelper(Player viewer) {

        PacketContainer packet;
        if (useNewRemovePacket) {
            // 1.19.4+ approach with dedicated remove packet
            packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            List<UUID> uuidsToRemove = Collections.singletonList(helperUUID);
            packet.getUUIDLists().write(0, uuidsToRemove);
        } else {
            // Legacy approach for older versions
            packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);

            packet.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER));

            List<PlayerInfoData> playerData = Collections.singletonList(
                    new PlayerInfoData(helperProfile, 0, null, null)
            );

            packet.getPlayerInfoDataLists().write(1, playerData);
        }

        protocolManager.sendServerPacket(viewer, packet);
    }


    /**
     * Sets the skin for the AI helper by fetching skin data from a specified player profile.
     * This method asynchronously fetches the skin data and applies it to the helper profile
     * on the main thread. Returns a CompletableFuture that completes with true if successful,
     * false if no textures property was found, or completes exceptionally if an error occurs.
     *
     * @return CompletableFuture<Boolean> indicating success or failure of the skin application
     */
    private static CompletableFuture<Boolean> setSkinToAiHelper() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // remove current skin
        helperProfile.getProperties().removeAll("textures");

        // async task to fetch skin data
        Bukkit.getScheduler().runTaskAsynchronously(BattistaAiSpigot.getInstance(), () -> {
            try {
                String skinOwner = BattistaAiSpigot.getConfigs().getString("tab.skin", "ItsManuX");
                PlayerProfile profile = Bukkit.createProfile(skinOwner);

                // Complete the profile to fetch skin data
                if (!profile.complete()) {
                    logger.warning("Battista AI helper Failed to complete profile for " + skinOwner);
                    future.complete(false);
                    return;
                }

                // Extract texture properties
                Set<ProfileProperty> properties = profile.getProperties();
                for (ProfileProperty property : properties) {
                    if ("textures".equals(property.getName())) {
                        String value = property.getValue();
                        String signature = property.getSignature();

                        // Apply to fake profile on main thread
                        Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> {
                            helperProfile.getProperties().put("textures",
                                    new WrappedSignedProperty("textures", value, signature));
                            logger.info("Successfully applied skin from " + skinOwner + " to Battista AI helper");

                            // Complete the future with success
                            future.complete(true);
                        });
                        return;
                    }
                }

                // No textures property found
                logger.info("No textures property found");
                future.complete(false);
            } catch (Exception e) {
                logger.severe("Error fetching skin for Battista AI helper: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });

        return future;
    }

}
