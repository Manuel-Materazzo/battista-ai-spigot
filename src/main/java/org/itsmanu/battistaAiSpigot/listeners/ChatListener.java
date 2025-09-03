package org.itsmanu.battistaAiSpigot.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.itsmanu.battistaAiSpigot.utils.HttpUtil;

import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private final FileConfiguration config = BattistaAiSpigot.getInstance().getConfig();
    private final Logger logger = BattistaAiSpigot.getInstance().getLogger();
    
    private final PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();

    private final boolean autoDetectQuestions = config.getBoolean("chat.auto_detect_questions", true);

    // Pattern to detect questions (ends with ? optionally followed by spaces)
    private static final Pattern QUESTION_PATTERN = Pattern.compile(".*\\?\\s*$");

    // Pattern to remove the tag from the question
    private final Pattern tagPattern;

    public ChatListener() {
        // Create the pattern for the configured tag
        String tag = config.getString("chat.tag", "@Helper");
        // Escape special characters for regex
        String escapedTag = Pattern.quote(tag);
        this.tagPattern = Pattern.compile(escapedTag + "\\s*", Pattern.CASE_INSENSITIVE);
    }

    /**
     * Handles the AsyncChatEvent to detect and process AI-related questions.
     *
     * @param event The asynchronous chat event triggered by a player message.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Component messageComponent = event.message();

        // Convert the Component to plain text
        String message = plainTextSerializer.serialize(messageComponent);

        if (message == null || message.trim().isEmpty()) {
            return;
        }

        message = message.trim();

        // Log the message if debug mode is enabled
        if (config.getBoolean("debug", false)) {
            logger.info("Chat message from " + player.getName() + ": " + message);
        }

        String question = null;
        boolean shouldActivate = false;

        // 1. Check if the message contains the tag (e.g., @Helper)
        String tag = config.getString("chat.tag", "@Helper");
        if (message.toLowerCase().contains(tag.toLowerCase())) {
            // Remove the tag from the message to extract the question
            question = tagPattern.matcher(message).replaceAll("").trim();
            shouldActivate = true;

            if (config.getBoolean("debug", false)) {
                logger.info("Tag detected. Extracted question: " + question);
            }
        }
        // 2. Check for automatic question detection
        else if (autoDetectQuestions && QUESTION_PATTERN.matcher(message).matches()) {
            question = message;
            shouldActivate = true;

            if (config.getBoolean("debug", false)) {
                logger.info("Automatically detected question: " + question);
            }
        }

        // If the AI helper should be activated
        if (shouldActivate && question != null && !question.isEmpty()) {

            // Validate the question
            if (question.length() < 3) {
                // Question is too short, ignore it
                if (config.getBoolean("debug", false)) {
                    logger.info("Question too short, ignored");
                }
                return;
            }

            if (question.length() > 500) {
                // Question is too long, send an error message
                Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> {
                    player.sendMessage("§c[AI Helper] The question is too long! Maximum 500 characters allowed.");
                });
                return;
            }

            // Check if the player has the required permission (optional for automatic chat detection)
            if (!player.hasPermission("aihelper.ask")) {
                if (config.getBoolean("debug", false)) {
                    logger.info("Player " + player.getName() + " does not have permission for AI helper");
                }
                return;
            }

            // Determine if the response should be private or public
            boolean isPrivate = false; // Currently always public in chat

            // If the message contained the tag, it might be a "private" question
            // However, for this plugin, everything remains public unless it's a command

            final String finalQuestion = question;

            // Process the request
            // Note: HttpUtil.askAIAndRespond will automatically handle thread switching
            HttpUtil.askAIAndRespond(player, finalQuestion, isPrivate);
        }
    }

    /**
     * Extracts a question from a message by removing the tag.
     *
     * @param message The original message containing the tag.
     * @param tag     The tag to be removed.
     * @return The extracted question without the tag.
     */
    private String extractQuestionFromTaggedMessage(String message, String tag) {
        // Remove the tag (case insensitive) and extra spaces
        String question = message.replaceAll("(?i)" + Pattern.quote(tag), "").trim();

        // Remove multiple spaces
        question = question.replaceAll("\\s+", " ");

        return question;
    }

    /**
     * Checks if a message appears to be a question.
     * This can be extended with more heuristic logic.
     *
     * @param message The message to analyze.
     * @return True if the message looks like a question, false otherwise.
     */
    private boolean looksLikeQuestion(String message) {
        message = message.toLowerCase().trim();

        // Check if it ends with a question mark
        if (message.endsWith("?")) {
            return true;
        }

        // Check for common interrogative words (expandable)
        String[] questionWords = {
                "come", "cosa", "dove", "quando", "perché", "perchè", "chi", "quale", "quanto", "quanti",
                "how", "what", "where", "when", "why", "who", "which", "how much", "how many"
        };

        for (String word : questionWords) {
            if (message.startsWith(word + " ") || message.contains(" " + word + " ")) {
                return true;
            }
        }

        return false;
    }
}
