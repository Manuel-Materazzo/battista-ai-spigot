package org.itsmanu.battistaAiSpigot.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.itsmanu.battistaAiSpigot.utils.ChatUtil;
import org.itsmanu.battistaAiSpigot.utils.HttpUtil;
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

        // Check if arguments are provided
        if (args.length == 0) {
            String commands = "Usage: /ask <question>\nExample: /ask How do I craft a diamond sword?";
            var message = BattistaAiSpigot.getConfigs().getString("messages.ask_usage", commands);

            String[] lines = message.split("\n");

            // Send each line with color codes translated
            for (String line : lines) {
                var formattedLine = ChatUtil.formatMessage(line);
                sender.sendMessage(formattedLine);
            }
            return true;
        }

        // Ensure the sender is a player
        if (!has_permissions(sender)) {
            return true;
        }

        // Combine all arguments into a single question
        StringBuilder questionBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            questionBuilder.append(args[i]);
            if (i < args.length - 1) {
                questionBuilder.append(" ");
            }
        }

        Player player = (Player) sender;
        String question = questionBuilder.toString().trim();

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
}
