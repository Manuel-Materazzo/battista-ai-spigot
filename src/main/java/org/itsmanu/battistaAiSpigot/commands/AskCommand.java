package org.itsmanu.battistaAiSpigot.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.itsmanu.battistaAiSpigot.utils.ChatUtil;
import org.itsmanu.battistaAiSpigot.utils.HttpUtil;
import org.itsmanu.battistaAiSpigot.utils.LimitsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Handles the /ask command to send questions to the AI.
 */
public class AskCommand implements CommandExecutor {

    private final Logger logger = BattistaAiSpigot.getInstance().getLogger();

    public AskCommand() {
    }

    /**
     * Executes the /ask command.
     *
     * @param sender  The entity that executed the command.
     * @param command The command being executed.
     * @param label   The alias used for the command.
     * @param args    The arguments provided with the command.
     * @return true if the command was successfully executed, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        // Ensure the sender is a player and has permissions
        if (!has_permissions(sender)) {
            return true;
        }
        Player player = (Player) sender;

        // Check player rate limits
        if(LimitsUtil.isPlayerRateLimitExceeded(player.getUniqueId())){
            var rateLimitMessage = ChatUtil.formatConfigMessage("messages.player_ratelimit_exceded", "Player Ratelimit Exceeded");
            Bukkit.broadcast(rateLimitMessage);
            return true;
        }

        // Check global rate limits
        if(LimitsUtil.isGlobalRateLimitExceeded()){
            var rateLimitMessage = ChatUtil.formatConfigMessage("messages.global_ratelimit_exceded", "Global Ratelimit Exceeded");
            Bukkit.broadcast(rateLimitMessage);
            return true;
        }

        // If no arguments provided, enter interactive mode
        if (args.length == 0) {
            handleInteractiveAsk(player);
            return true;
        }

        // Combine all arguments into a single question
        String question = buildQuestion(args);

        // check if the question is valid
        if (!ChatUtil.is_question_valid(question, player, true)) {
            return true;
        }

        // Log the question if debug mode is enabled
        ChatUtil.sendDebug("Command /ask executed by " + player.getName() + ": " + question);

        var processingMessage = ChatUtil.formatConfigMessage("messages.processing", "Processing question...");

        // Send the question to the AI - private response (only to the player who executed the command)
        // Note: this will automatically handle thread switching
        var request = HttpUtil.askAI(question);
        ChatUtil.sendAiAnswer(request, player, processingMessage, logger);
        return true;
    }

    /**
     * Checks if the command sender has the required permissions to execute the command.
     *
     * @param sender The entity that executed the command.
     * @return true if the sender has the required permissions, false otherwise.
     */
    private boolean has_permissions(CommandSender sender) {
        // Ensure the sender is a player
        if (!(sender instanceof Player player)) {
            var message = ChatUtil.formatConfigMessage("messages.only_players", "You're not a player!");
            sender.sendMessage(message);
            return false;
        }

        // Check if the player has the required permission
        if (!player.hasPermission("battista.use")) {
            var message = ChatUtil.formatConfigMessage("messages.no_permission", "You need battista.use permission");
            player.sendMessage(message);
            return false;
        }
        return true;
    }

    /**
     * Combines the command arguments into a single question string.
     *
     * @param args The arguments provided with the command.
     * @return The combined question string with proper spacing.
     */
    private String buildQuestion(String[] args) {
        StringBuilder questionBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            questionBuilder.append(args[i]);
            if (i < args.length - 1) {
                questionBuilder.append(" ");
            }
        }
        return questionBuilder.toString().trim();
    }

    private void handleInteractiveAsk(Player player) {
        // Check if player already has a pending question
        if (LimitsUtil.hasPendingQuestions(player)) {
            var alt_message = "You are in interactive mode, just ask without commands!";
            var message = ChatUtil.formatConfigMessage("messages.ask_interactive_pending", alt_message);
            player.sendMessage(message);
            return;
        }

        // Send initial message
        var initialMessage = ChatUtil.formatConfigMessage("messages.ask_interactive", "I'm here for you, ask away!");
        player.sendMessage(initialMessage);

        // get timeout from config
        long timeout = BattistaAiSpigot.getConfigs().getInt("limits.inteactive_timeout", 60) * 20L;

        // Create timeout task
        BukkitTask timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Send timeout message
                if (player.isOnline()) {
                    var alt_message = "Question timeout! use **/ask** if you need me again.";
                    var message = ChatUtil.formatConfigMessage("messages.ask_inteactive_timeout", alt_message);
                    player.sendMessage(message);
                }

                // Remove from pending questions
                LimitsUtil.removePendingQuestions(player);
            }
        }.runTaskLater(BattistaAiSpigot.getInstance(), timeout);

        // Store the task so we can cancel it if needed
        LimitsUtil.addPendingQuestions(player, timeoutTask);
    }
}
