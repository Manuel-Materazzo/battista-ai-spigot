package org.itsmanu.battistaAiSpigot.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.itsmanu.battistaAiSpigot.dto.Question;
import org.itsmanu.battistaAiSpigot.utils.ChatUtil;
import org.itsmanu.battistaAiSpigot.utils.HttpUtil;
import org.itsmanu.battistaAiSpigot.utils.LimitsUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private final Logger logger = BattistaAiSpigot.getInstance().getLogger();

    private final PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();

    // Pattern to detect questions (ends with ? optionally followed by spaces)
    private static final Pattern QUESTION_PATTERN = Pattern.compile(".*\\?\\s*$");

    public ChatListener() {
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void blockLegacyChat(AsyncPlayerChatEvent event) {
        // Always cancel the legacy event to block shithead plugins
        event.setCancelled(true);
    }


    /**
     * Handles the AsyncChatEvent to detect and process AI-related questions.
     *
     * @param event The asynchronous chat event triggered by a player message.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        SignedMessage signedMessage = event.signedMessage();
        String message = signedMessage.message().trim();

        if (message.isEmpty()) {
            return;
        }

        //TODO: weird log
        ChatUtil.sendDebug("Chat message from " + player.getName() + ": " + message);

        // get configs
        FileConfiguration config = BattistaAiSpigot.getConfigs();
        boolean moderationEnabled = config.getBoolean("chat.moderation.enabled", false);
        boolean clientSideFiltering = config.getBoolean("chat.moderation.client_side_filtering", true);

        // moderate
        if (moderationEnabled) {
            // Check recency: only moderate if the message is <50ms old (not re-thrown)
            long ageMs = Duration.between(signedMessage.timestamp(), Instant.now()).toMillis();
            if (ageMs < 50) {
                if (clientSideFiltering) {
                    // start moderation in background, and delete message if it's harmful
                    clientSideBackgroundModerate(player, signedMessage);
                } else {
                    // Always cancel the chat event
                    event.setCancelled(true);
                    // start moderation in background, and rethrow event (send message) if it's clean
                    serverSideBackgroundModerate(player, signedMessage, event);
                }
            }
        }

        // extract question and the "privacy" status of the said question
        Question question = getQuestion(event, message);

        if (!shouldAnswer(player, question)) {
            return;
        }

        var processingMessage = ChatUtil.formatConfigMessage("messages.processing", "Processing question...");

        // Process the request
        // Note: this will automatically handle thread switching
        var request = HttpUtil.askAI(question.question());

        if (question.privateQuestion()) {
            ChatUtil.sendAiAnswer(request, player, processingMessage);
        } else {
            ChatUtil.sendAiAnswer(request, processingMessage);
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
     * @param player   The player who asked the question
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

    /**
     * Performs background moderation of a player's message.
     * <p>
     * This method checks if the player is excluded from moderation based on permissions.
     * If not excluded, it sends the message to the moderation service asynchronously.
     * If the moderation service flags the message, it deletes the message retroactively
     * for all online players and notifies the sender.
     *
     * @param player        The player who sent the message
     * @param signedMessage The signed message containing the content to be moderated
     */
    private void clientSideBackgroundModerate(Player player, SignedMessage signedMessage) {
        // Check if the player has an exclusion
        if (player.hasPermission("battista.moderation.exclude")) {
            ChatUtil.sendDebug("Player " + player.getName() + " is excluded from chat moderation");
            return;
        }

        String message = signedMessage.message();

        // Start moderation in background
        HttpUtil.askModerator(message).thenAccept(response -> {
            if (response.isFlag()) {
                // Delete the message retroactively for all players
                Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> {
                    // Delete from all online players
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.deleteMessage(signedMessage);
                    }

                    // Notify the sender
                    var moderatedMessage = ChatUtil.formatConfigMessage(
                            "messages.moderated",
                            "Your message was removed by automatic moderation."
                    );
                    player.sendMessage(moderatedMessage);
                });
                ChatUtil.sendDebug("Player " + player.getName() + " was moderated");
            }
        }).exceptionally(throwable -> {
            logger.log(Level.SEVERE, "Error during Battista AI request", throwable);
            return null;
        });
    }

    /**
     * Performs background moderation of a player's message and sends it to the provided audience.
     * <p>
     * This method checks if the player is excluded from moderation based on permissions and interactive mode status.
     * If not excluded, it sends the message to the moderation service asynchronously.
     * If the moderation service flags the message, it does not send it.
     *
     * @param player        The player who sent the message
     * @param signedMessage The signed message containing the content to be moderated
     * @param event         The original chat event that triggered this moderation
     */
    private void serverSideBackgroundModerate(Player player, SignedMessage signedMessage, AsyncChatEvent event) {

        // Check if the player has an exclusion
        if (player.hasPermission("battista.moderation.exclude")) {
            ChatUtil.sendDebug("Player " + player.getName() + " is excluded from chat moderation");
            return;
        }

        // Check if the player is in interactive mode
        if (LimitsUtil.hasPendingQuestions(player)) {
            ChatUtil.sendDebug("Player " + player.getName() + " is in interactive mode, skipping chat moderation");
            return;
        }

        // Store viewers BEFORE clearing
        Set<Audience> originalViewers = new HashSet<>(event.viewers());

        // Nuclear option: Remove ALL viewers so nobody receives the message
        event.viewers().clear();

        String message = signedMessage.message();

        // Execute moderation asynchronously
        HttpUtil.askModerator(message).thenAccept(response -> {
            if (response.isFlag()) {
                // Message blocked, notify player
                var moderatedMessage = ChatUtil.formatConfigMessage(
                        "messages.moderated",
                        "Your message was removed by automatic moderation."
                );
                Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> player.sendMessage(moderatedMessage));
                ChatUtil.sendDebug("Player " + player.getName() + " was moderated");
            } else {
                // Message is safe, re-dispatch the event so other plugins can process it
                Bukkit.getScheduler().runTaskAsynchronously(BattistaAiSpigot.getInstance(), () -> {
                    AsyncChatEvent newEvent = new AsyncChatEvent(
                            true,
                            player,
                            originalViewers,
                            event.renderer(),
                            event.message(),
                            event.originalMessage(),
                            event.signedMessage()
                    );
                    Bukkit.getPluginManager().callEvent(newEvent);
                });
                ChatUtil.sendDebug("Player " + player.getName() + " wasn't moderated");
            }
        }).exceptionally(throwable -> {
            Bukkit.getScheduler().runTask(BattistaAiSpigot.getInstance(), () -> {
                var errorMessage = ChatUtil.formatMessage("An error occurred: " + throwable.getMessage());
                Bukkit.broadcast(errorMessage);
            });
            logger.log(Level.SEVERE, "Error during Battista AI request", throwable);
            return null;
        });
    }
}
