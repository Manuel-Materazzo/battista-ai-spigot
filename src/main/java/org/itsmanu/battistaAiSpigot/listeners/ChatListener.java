package org.itsmanu.battistaAiSpigot.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.itsmanu.battistaAiSpigot.utils.ChatUtil;
import org.itsmanu.battistaAiSpigot.utils.HttpUtil;

import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private final Logger logger = BattistaAiSpigot.getInstance().getLogger();

    private final PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();

    // Pattern to detect questions (ends with ? optionally followed by spaces)
    private static final Pattern QUESTION_PATTERN = Pattern.compile(".*\\?\\s*$");

    public ChatListener() {
    }

    /**
     * Handles the AsyncChatEvent to detect and process AI-related questions.
     *
     * @param event The asynchronous chat event triggered by a player message.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = plainTextSerializer.serialize(event.message()).trim();

        if (message.isEmpty()) {
            return;
        }

        ChatUtil.sendDebug("Chat message from " + player.getName() + ": " + message);

        // get configs
        FileConfiguration config = BattistaAiSpigot.getInstance().getConfig();
        boolean autoDetectQuestions = config.getBoolean("chat.auto_detect_questions", true);
        String tag = config.getString("chat.tag", "@Helper");

        String question = null;
        // Check if the message contains the tag (e.g., @Helper)
        if (hasTag(config, message, tag)) {
            // Remove the tag from the message
            question = extractQuestion(message, tag);
        }
        // Check for automatic question detection
        else if (autoDetectQuestions && QUESTION_PATTERN.matcher(message).matches()) {
            question = message;
            ChatUtil.sendDebug("Automatically detected question: " + question);
        }

        // check if the question is valid
        if (!is_question_valid(config, question, player)) {
            return;
        }

        // Check if the player has the required permission
        if (!player.hasPermission("aihelper.ask")) {
            ChatUtil.sendDebug("Player " + player.getName() + " does not have permission for AI helper");
            return;
        }

        // Process the request
        // Note: HttpUtil.askAIAndRespond will automatically handle thread switching
        HttpUtil.askAIAndRespond(player, question, false);
    }

    /**
     * Checks if a message contains a specific tag.
     *
     * @param message The message to check for the tag.
     * @param tag     The tag to look for in the message.
     * @return true if the tag is found and tagging is enabled, false otherwise.
     */
    private boolean hasTag(FileConfiguration config, String message, String tag) {
        // get configs
        boolean taggingEnabled = config.getBoolean("chat.tagging.enabled", true);

        if (taggingEnabled && message.toLowerCase().contains(tag.toLowerCase())) {
            ChatUtil.sendDebug("Tag detected.");
            return true;
        }
        return false;
    }

    /**
     * Validates a question based on length requirements.
     *
     * @param question The question to validate.
     * @param player   The player who asked the question.
     * @return true if the question is valid, false otherwise.
     */
    private boolean is_question_valid(FileConfiguration config, String question, Player player) {

        if (question == null || question.isEmpty()) {
            return false;
        }

        if (question.length() < 3) {
            // Question is too short, ignore it
            if (config.getBoolean("debug", false)) {
                var message = ChatUtil.formatConfigMessage("messages.question_too_short", "Question too short.");
                logger.info(message.toString());
            }
            return false;
        }

        if (question.length() > 500) {
            // Question is too long, send an error message
            Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> {
                var message = ChatUtil.formatConfigMessage("messages.question_too_long", "Max 500 characters.");
                player.sendMessage(message);
            });
            return false;
        }
        return true;
    }

    /**
     * Extracts a question from a message by removing the tag.
     *
     * @param message The original message containing the tag.
     * @param tag     The tag to be removed.
     * @return The extracted question without the tag.
     */
    private String extractQuestion(String message, String tag) {
        Pattern tagPattern = Pattern.compile(Pattern.quote(tag) + "\\s*", Pattern.CASE_INSENSITIVE);
        return tagPattern.matcher(message).replaceAll("").trim();
    }

}
