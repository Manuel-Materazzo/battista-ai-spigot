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
        if (!(sender instanceof Player player)) {
            var message = ChatUtil.formatConfigMessage("messages.only_players", "You're not a player!");
            sender.sendMessage(message);
            return true;
        }

        // Check if the player has the required permission
        if (!player.hasPermission("battista.use")) {
            var message = ChatUtil.formatConfigMessage("messages.no_permission", "You need battista.use permission");
            player.sendMessage(message);
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

        String question = questionBuilder.toString().trim();

        // Validate the question
        if (question.isEmpty()) {
            var message = ChatUtil.formatConfigMessage("messages.empty_question", "Empty question");
            player.sendMessage(message);
            return true;
        }

        if (question.length() < 3) {
            var message = ChatUtil.formatConfigMessage("messages.question_too_short", "Question too short");
            player.sendMessage(message);
            return true;
        }

        if (question.length() > 150) {
            var message = ChatUtil.formatConfigMessage("messages.question_too_long", "Question too long");
            player.sendMessage(message);
            return true;
        }

        // Log the question if debug mode is enabled
        FileConfiguration config = BattistaAiSpigot.getConfigs();
        if (config.getBoolean("debug", false)) {
            logger.info("Command /ask executed by " + player.getName() + ": " + question);
        }

        // Send the question to the AI - private response (only to the player who executed the command)
        // Note: this will automatically handle thread switching
        var request = HttpUtil.askAI(question);
        ChatUtil.sendAiAnswer(request, player, logger);
        return true;
    }
}
