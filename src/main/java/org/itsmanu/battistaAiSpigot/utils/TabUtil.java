package org.itsmanu.battistaAiSpigot.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.itsmanu.battistaAiSpigot.listeners.PlayerTabListener;

import java.util.*;

public class TabUtil {
    private static final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    private static final String FAKE_PLAYER_NAME = "AI-Helper";
    private static PlayerTabListener tabListener;

    private static boolean useNewRemovePacket = false;

    private TabUtil() {
    }

    /**
     * Enables the tab feature by registering the PlayerTabListener and refreshing the AI helper
     * for all online players. This method checks if ProtocolLib is available before proceeding.
     */
    public static void enableTabFeature() {
        // Check if ProtocolLib is available
        boolean protocolibAvailable = DependencyUtil.checkProtocolLib();

        if (protocolibAvailable && tabListener == null) {
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
        UUID aiUUID = UUID.nameUUIDFromBytes(FAKE_PLAYER_NAME.getBytes());
        WrappedGameProfile profile = new WrappedGameProfile(aiUUID, FAKE_PLAYER_NAME);

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
                        profile,
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
        // Clean up any existing fake players for all online players
        Bukkit.getScheduler().runTaskLater(BattistaAiSpigot.getInstance(), () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                cleanupAIHelper(player);
                // Wait a tick, then re-add
                Bukkit.getScheduler().runTaskLater(BattistaAiSpigot.getInstance(), () -> {
                    addAIHelper(player);
                }, 1L);
            }
        }, 5L); // Wait 5 ticks for everything to initialize
    }


    /**
     * Removes the AI helper from the player's tab list.
     *
     * @param viewer The player from whose tab list the AI helper should be removed
     */
    private static void cleanupAIHelper(Player viewer) {
        UUID aiUUID = UUID.nameUUIDFromBytes(FAKE_PLAYER_NAME.getBytes());

        if (useNewRemovePacket) {
            // 1.19.4+ approach with dedicated remove packet
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            List<UUID> uuidsToRemove = Collections.singletonList(aiUUID);
            packet.getUUIDLists().write(0, uuidsToRemove);
            protocolManager.sendServerPacket(viewer, packet);
        } else {
            // Legacy approach for older versions
            WrappedGameProfile profile = new WrappedGameProfile(aiUUID, FAKE_PLAYER_NAME);
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);

            packet.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER));

            List<PlayerInfoData> playerData = Collections.singletonList(
                    new PlayerInfoData(profile, 0, null, null)
            );

            packet.getPlayerInfoDataLists().write(1, playerData);
            protocolManager.sendServerPacket(viewer, packet);
        }
    }
}
