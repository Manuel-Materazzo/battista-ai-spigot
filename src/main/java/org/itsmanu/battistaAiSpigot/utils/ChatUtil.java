package org.itsmanu.battistaAiSpigot.utils;

import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ChatUtil {

    private ChatUtil() {
    }

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
        if (BattistaAiSpigot.getConfigs().getBoolean("debug", false)) {
            var formatted = formatMessage(message);
            message = PlainTextComponentSerializer.plainText().serialize(formatted);
            BattistaAiSpigot.getInstance().getLogger().info(message);
        }
    }

    /**
     * Sends a info message to logger only if file logging is enabled.
     *
     * @param message The info message to be sent.
     */
    public static void sendOnFile(String message) {
        if (BattistaAiSpigot.getConfigs().getBoolean("log-to-file", false)) {
            BattistaAiSpigot.getInstance().getLogger().info(message);
        }
    }

    /**
     * Sends a info message to logger only if file logging is enabled.
     *
     * @param message The info message to be sent.
     */
    public static void sendOnFile(Component message) {
        if (BattistaAiSpigot.getConfigs().getBoolean("log-to-file", false)) {
            String plainMessage = PlainTextComponentSerializer.plainText().serialize(message);
            BattistaAiSpigot.getInstance().getLogger().info(plainMessage);
        }
    }

    /**
     * Sends the provided AI request asynchronously and broadcasts the response to all players.
     * This method automatically handles thread switching to avoid issues with the Bukkit API.
     *
     * @param aiRequest         The ai request to send.
     * @param processingMessage Message to send while processing
     */
    public static void sendAiAnswer(CompletableFuture<String> aiRequest, Component processingMessage) {
        sendAiAnswer(aiRequest, null, processingMessage);
    }

    /**
     * Sends the provided AI request asynchronously and responds to the player.
     * This method automatically handles thread switching to avoid issues with the Bukkit API.
     *
     * @param aiRequest         The ai request to send.
     * @param player            The player who asked the question.
     * @param processingMessage Message to send while processing
     */
    public static void sendAiAnswer(CompletableFuture<String> aiRequest, Player player, Component processingMessage) {

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
                sendMessage(player, formattedResponse);

                // Log the answer on file if enabled
                ChatUtil.sendOnFile(formattedResponse);
            });
        }).exceptionally(throwable -> {
            // Handle errors
            Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> {
                var errorMessage = ChatUtil.formatMessage("An error occurred: " + throwable.getMessage());
                sendMessage(player, errorMessage);
            });

            BattistaAiSpigot.getInstance().getLogger().log(Level.SEVERE, "Error during Battista AI request", throwable);
            return null;
        });
    }

    /**
     * Validates a question based on length requirements.
     *
     * @param question The question to validate.
     * @param player   The player who asked the question.
     * @return true if the question is valid, false otherwise.
     */
    public static boolean is_question_valid(String question, Player player, boolean warnPlayer) {

        if (question == null || question.isEmpty()) {
            var message = ChatUtil.formatConfigMessage("messages.empty_question", "Empty question");
            if (warnPlayer) {
                player.sendMessage(message);
            }
            sendDebug(message.toString());
            return false;
        }

        var min_length = BattistaAiSpigot.getConfigs().getInt("chat.auto_detect_questions.min_length", 5);
        var max_length = BattistaAiSpigot.getConfigs().getInt("chat.auto_detect_questions.max_length", 150);

        if (question.length() < min_length) {
            // Question is too short, ignore it
            var message = ChatUtil.formatConfigMessage("messages.question_too_short", "Question too short.");
            if (warnPlayer) {
                player.sendMessage(message);
            }
            sendDebug(message.toString());
            return false;
        }

        if (question.length() > max_length) {
            // Question is too long, send an error message
            var message = ChatUtil.formatConfigMessage("messages.question_too_long", "Question too long.");
            if (warnPlayer) {
                player.sendMessage(message);
            }
            sendDebug(message.toString());
            return false;
        }
        return true;
    }

    /**
     * Sends a message to a player or broadcasts it to all players.
     * If logging of private answers is enabled in the configuration, the message is also logged to the console.
     *
     * @param player           The player to send the message to. If null, the message is broadcasted to all players.
     * @param formattedMessage The message to send, already formatted as a Component.
     */
    private static void sendMessage(Player player, Component formattedMessage) {
        if (player != null) {
            player.sendMessage(formattedMessage);
            // if private answers need to be logged, and log to file is disabled (private answers would be logged anyway)
            if (BattistaAiSpigot.getConfigs().getBoolean("chat.log_private_answers", false) &&
                    !BattistaAiSpigot.getConfigs().getBoolean("log-to-file", false)) {
                // log it
                var message = PlainTextComponentSerializer.plainText().serialize(formattedMessage);
                BattistaAiSpigot.getInstance().getLogger().info(message);
            }
        } else {
            Bukkit.broadcast(formattedMessage);
        }
    }
}
