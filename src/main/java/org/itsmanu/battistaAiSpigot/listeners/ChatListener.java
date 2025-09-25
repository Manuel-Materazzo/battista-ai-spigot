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
import org.itsmanu.battistaAiSpigot.dto.Question;
import org.itsmanu.battistaAiSpigot.utils.ChatUtil;
import org.itsmanu.battistaAiSpigot.utils.HttpUtil;
import org.itsmanu.battistaAiSpigot.utils.LimitsUtil;

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

        Question question = getQuestion(event, message);

        if(!shouldAnswer(player, question)){
            return;
        }

        var processingMessage = ChatUtil.formatConfigMessage("messages.processing", "Processing question...");

        // Process the request
        // Note: this will automatically handle thread switching
        var request = HttpUtil.askAI(question.question());

        if (question.privateQuestion()) {
            ChatUtil.sendAiAnswer(request, player, processingMessage, logger);
        } else {
            ChatUtil.sendAiAnswer(request, processingMessage, logger);
        }
    }

    /**
     * Extracts a question from a chat message based on various criteria.
     * <p>
     * This method handles three main cases:
     * 1. Interactive mode: If the player is in interactive mode, the message is returned as-is
     * and the event is cancelled to prevent it from appearing in chat.
     * 2. Tagged questions: If the message contains the configured tag (e.g., @Helper),
     * the tag is removed and the remaining text is returned as the question.
     * 3. Automatic detection: If automatic question detection is enabled and the message
     * ends with a question mark, it is returned as a detected question.
     *
     * @param event   The AsyncChatEvent containing the player and message information
     * @param message The raw message content to be processed
     * @return The extracted question or null if no valid question was found
     */
    private Question getQuestion(AsyncChatEvent event, String message) {
        Player player = event.getPlayer();

        // get configs
        FileConfiguration config = BattistaAiSpigot.getConfigs();
        boolean autoDetectQuestions = config.getBoolean("chat.auto_detect_questions.enabled", false);
        String tag = config.getString("chat.tag", "@Helper");

        // Check if the user is in interactive mode
        if (LimitsUtil.hasPendingQuestions(player)) {
            // Cancel the event to prevent the message from appearing in chat
            event.setCancelled(true);
            // Cancel the interactive timeout task
            LimitsUtil.removePendingQuestions(player);
            // start a validity check and warn the player
            ChatUtil.is_question_valid(message, player, true);
            // create question object
            return new Question(message, true);
        }
        // Check if the message contains the tag (e.g., @Helper)
        else if (hasTag(config, message, tag)) {
            // Remove the tag from the message
            Pattern tagPattern = Pattern.compile(Pattern.quote(tag) + "\\s*", Pattern.CASE_INSENSITIVE);
            var question = tagPattern.matcher(message).replaceAll("").trim();
            // create question object
            return new Question(question, false);
        }
        // Check for automatic question detection
        else if (autoDetectQuestions && QUESTION_PATTERN.matcher(message).matches()) {
            ChatUtil.sendDebug("Automatically detected question: " + message);
            return new Question(message, false);
        }
        return new Question(null, false);
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
     * Determines whether the AI should answer the given question based on various criteria.
     * This method performs the following checks:
     * 1. Validates the question content and warns the player if invalid (only for private questions)
     * 2. Verifies the player has the required permission to use the AI helper
     * 3. Checks if the player has exceeded their rate limits
     * 4. Checks if the global rate limits have been exceeded
     *
     * @param player The player who asked the question
     * @param question The question object containing the question text and privacy setting
     * @return true if the AI should answer the question, false otherwise
     */
    private boolean shouldAnswer(Player player, Question question) {
        // check if the question is valid and warn the player only if the question was private (avoid spamming public chat)
        if (!ChatUtil.is_question_valid(question.question(), player, question.privateQuestion())) {
            return false;
        }

        // Check if the player has the required permission
        if (!player.hasPermission("battista.use")) {
            ChatUtil.sendDebug("Player " + player.getName() + " does not have permission for AI helper");
            return false;
        }

        // Check player rate limits
        if (LimitsUtil.isPlayerRateLimitExceeded(player.getUniqueId())) {
            var rateLimitMessage = ChatUtil.formatConfigMessage("messages.player_ratelimit_exceded", "Player Ratelimit Exceeded");
            Bukkit.broadcast(rateLimitMessage);
            return false;
        }

        // Check global rate limits
        if (LimitsUtil.isGlobalRateLimitExceeded()) {
            var rateLimitMessage = ChatUtil.formatConfigMessage("messages.global_ratelimit_exceded", "Global Ratelimit Exceeded");
            Bukkit.broadcast(rateLimitMessage);
            return false;
        }

        return true;
    }
}
