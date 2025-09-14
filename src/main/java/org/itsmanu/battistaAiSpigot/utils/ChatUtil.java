package org.itsmanu.battistaAiSpigot.utils;

import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatUtil {

    /**
     * Retrieves a message from the configuration and formats it with the configured prefix and color codes.
     *
     * @param path The path to the message in the configuration.
     * @param def  The default message to use if the path is not found.
     * @return The formatted message with the prefix and color codes.
     */
    public static Component formatConfigMessage(String path, String def) {
        var message = BattistaAiSpigot.getConfigs().getString(path, def);
        return formatMessage(message);
    }

    /**
     * Formats the chat message with the configured prefix and color codes.
     *
     * @param message The raw message.
     * @return The formatted message with the prefix.
     */
    public static Component formatMessage(String message) {
        String prefix = BattistaAiSpigot.getConfigs().getString("chat.response_prefix");
        return new MineDown(prefix + message).toComponent();
    }

    /**
     * Sends a debug message to the console if debug mode is enabled in the configuration.
     *
     * @param message The debug message to be sent.
     */
    public static void sendDebug(String message) {
        message = formatMessage(message).toString();
        if (BattistaAiSpigot.getConfigs().getBoolean("debug", false)) {
            BattistaAiSpigot.getInstance().getLogger().info(message);
        }
    }

    /**
     * Sends the provided AI request asynchronously and broadcasts the response to all players.
     * This method automatically handles thread switching to avoid issues with the Bukkit API.
     *
     * @param aiRequest The ai request to send.
     * @param logger    The logger to use for error reporting.
     */
    public static void sendAiAnswer(CompletableFuture<String> aiRequest, Component processingMessage, Logger logger) {
        sendAiAnswer(aiRequest, null, processingMessage, logger);
    }

    /**
     * Sends the provided AI request asynchronously and responds to the player.
     * This method automatically handles thread switching to avoid issues with the Bukkit API.
     *
     * @param aiRequest The ai request to send.
     * @param player    The player who asked the question.
     */
    public static void sendAiAnswer(CompletableFuture<String> aiRequest, Player player, Component processingMessage, Logger logger) {

        if (player != null) {
            player.sendMessage(processingMessage);
        } else {
            Bukkit.broadcast(processingMessage);
        }

        // Execute the request asynchronously
        aiRequest.thenAccept(response -> {
            // Switch back to the main thread to send the message
            Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> {
                var formattedResponse = ChatUtil.formatMessage(response);

                if (player != null) {
                    player.sendMessage(formattedResponse);
                } else {
                    Bukkit.broadcast(formattedResponse);
                }
            });
        }).exceptionally(throwable -> {
            // Handle errors
            Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> {
                var errorMessage = ChatUtil.formatMessage("An error occurred: " + throwable.getMessage());

                if (player != null) {
                    player.sendMessage(errorMessage);
                } else {
                    Bukkit.broadcast(errorMessage);
                }
            });

            logger.log(Level.SEVERE, "Error during Battista AI request", throwable);
            return null;
        });
    }

}
